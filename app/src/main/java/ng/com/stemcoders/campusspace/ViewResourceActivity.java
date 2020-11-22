package ng.com.stemcoders.campusspace;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.ResourceCommentAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.ResourceCommentModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ResourcesService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.FileUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class ViewResourceActivity extends AppCompatActivity
{

    private ProgressBar loaderDisplay;
    private EditText commentInput;
    private MaterialButton btnAddComment;
    private ImageView categoryImg;
    private TextView titleView, departmentView, courseView, downloadsView;
    private MaterialButton btnDownload;
    private TextView dateView, descView;
    private RecyclerView commentsList;
    private NestedScrollView scrollView;

    private List<ResourceCommentModel> comments = new ArrayList<>();
    private List<ResourceCommentModel> loadedComments = new ArrayList<>();
    private ResourceCommentAvailableEvent commentAvailableEvent;

    private String author;

    private ResourceModel resourceModel = null;

    private static AsyncTask<Void, ResourceCommentModel, Void> asyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_resource);

        loaderDisplay = findViewById(R.id.view_resource_progress_bar);
        commentInput = findViewById(R.id.view_resource_comment_input);
        btnAddComment = findViewById(R.id.btn_view_resource_add_comment);
        categoryImg = findViewById(R.id.view_news_img);
        titleView = findViewById(R.id.view_resource_title);
        departmentView = findViewById(R.id.view_resource_department);
        courseView = findViewById(R.id.view_resource_course);
        downloadsView = findViewById(R.id.view_resource_downloads);
        btnDownload = findViewById(R.id.btn_view_resource_download);
        dateView = findViewById(R.id.view_resource_date);
        descView = findViewById(R.id.view_resource_description);

        commentsList = findViewById(R.id.view_resource_comment_list);
        commentsList.setItemAnimator(new DefaultItemAnimator());
        commentsList.setAdapter(new CommentsAdapter());
        commentsList.setLayoutManager(new LinearLayoutManager(this));

        scrollView = findViewById(R.id.view_resource_scrollview);

        Intent intent = getIntent();

        if (savedInstanceState == null)
        {
            resourceModel = (ResourceModel)intent.getExtras().getSerializable("resource");
            requestComments();
        }
        else
            resourceModel = (ResourceModel)savedInstanceState.getSerializable("resource");

        if (resourceModel == null)
            finish();

        getSupportActionBar().setTitle(resourceModel.getTitle());

        author = PreferenceUtil.displayName(getApplicationContext());

        btnAddComment.setOnClickListener(v ->
        {
            if (TextUtils.isEmpty(commentInput.getText().toString().trim()))
                showSnackMessage("Please write a comment text first!", false);
            else
            {
                requestCommentAddition(commentInput.getText().toString());
            }
        });

        btnDownload.setOnClickListener(v ->
        {
            downloadResource();
        });

        int imageDrawableRes = R.drawable.videos;
        String category = resourceModel.getCategory();
        if (TextUtils.equals("Material", category))
            imageDrawableRes = R.drawable.materials;
        else if (TextUtils.equals("Textbook", category))
            imageDrawableRes = R.drawable.textbooks;
        else if (TextUtils.equals("Document", category))
            imageDrawableRes = R.drawable.documents;
        categoryImg.setImageDrawable(getDrawable(imageDrawableRes));

        titleView.setText(resourceModel.getTitle());
        departmentView.setText(resourceModel.getDepartment());
        courseView.setText(resourceModel.getCourse_title() + "  [" + resourceModel.getCourse_code() + "]");
        downloadsView.setText(resourceModel.getDownloads() + " " + getString(R.string.downloads));
        dateView.setText(getString(R.string.added_on) + " " + resourceModel.getDate_added());
        descView.setText(resourceModel.getDescription());

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.view_resource_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);
            requestComments();
        });
    }

    private void toggleLoaderDisplay(boolean visible)
    {
        if (visible)
            loaderDisplay.setVisibility(View.VISIBLE);
        else
            loaderDisplay.setVisibility(View.GONE);
    }

    private boolean isLoading()
    { return loaderDisplay.getVisibility() == View.VISIBLE; }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (commentAvailableEvent != null)
            outState.putSerializable("comments", commentAvailableEvent);

        outState.putSerializable("resource", resourceModel);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.getSerializable("comments") != null)
        {
            commentAvailableEvent = (ResourceCommentAvailableEvent)savedInstanceState.getSerializable("comments");
            comments = commentAvailableEvent.resourceCommentModels;
            loadComments(false);
        }
        else
            requestComments();
    }

    private void downloadResource()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
        {
            AlertUtil.showConfirmDialog(this, "Permission Required", getString(R.string.storage_permission_alert_message),
                    ()-> requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            FileUtil.PERMISSION_REQUEST_CODE));
        }
        else
        {
            boolean fileExists = FileUtil.resourceFileExists(getApplicationContext(), resourceModel.getFile());
            if (fileExists)
            {
                AlertUtil.showConfirmDialog(ViewResourceActivity.this, getString(R.string.warning), getString(R.string.resource_exists_warning),
                        () -> startDownloadTask());
            }
            else
                startDownloadTask();
        }
    }

    private void startDownloadTask()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for previous task to complete!", false);
            return;
        }

        Intent intent = new Intent(this, DownloaderService.class);
        intent.putExtra(DownloaderService.EXTRA_RESOURCE, resourceModel);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FileUtil.PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            boolean fileExists = FileUtil.resourceFileExists(getApplicationContext(), resourceModel.getFile());
            if (fileExists)
            {
                AlertUtil.showConfirmDialog(ViewResourceActivity.this, getString(R.string.warning), getString(R.string.resource_exists_warning),
                        () -> startDownloadTask());
            }
            else
                startDownloadTask();
        }
    }

    private void reloadList()
    {
        commentsList.getAdapter().notifyItemInserted(loadedComments.size());
        commentsList.smoothScrollToPosition(loadedComments.size()-1);
    }

    private void addComment(ResourceCommentModel resourceModel)
    {
        loadedComments.add(resourceModel);
        reloadList();
    }

    private void loadComments()
    { loadComments(true); }

    private void loadComments(boolean showLoadedAlert)
    {
        if (comments == null || comments.isEmpty())
            return;

        loadedComments.clear();
        commentsList.removeAllViews();

        asyncTask = new AsyncTask<Void, ResourceCommentModel, Void>()
        {
            @Override
            protected Void doInBackground(Void... objects)
            {
                for (ResourceCommentModel commentModel : comments)
                {
                    publishProgress(commentModel);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(ResourceCommentModel... values)
            {
                addComment(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                toggleLoaderDisplay(false);

                scrollView.smoothScrollTo(0, (int)commentsList.getY());

                if (showLoadedAlert)
                    showSnackMessage("Resource Comments Loaded!", false);
            }
        };
        asyncTask.execute();
    }

    private void requestComments()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        commentsList.removeAllViews();
        loadedComments.clear();

        try
        {
            ResourcesService resourcesService = RetroServiceGenerator.generateService(ResourcesService.class);
            Call<List<ResourceCommentModel>> requestCall = resourcesService.getResourceComments(resourceModel.getId(), false);
            requestCall.enqueue(new Callback<List<ResourceCommentModel>>()
            {
                @Override
                public void onResponse(Call<List<ResourceCommentModel>> call, Response<List<ResourceCommentModel>> response)
                {
                    switch (response.code())
                    {
                        case 200:
                            if (ViewResourceActivity.this != null)
                                EventBus.getDefault().post(new ResourceCommentAvailableEvent(response.body()));
                            break;
                        case 404:
                            toggleLoaderDisplay(false);
                            showSnackMessage("No comments found!",
                                    false);
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadCommentsSnackMessage("An error occurred! Please try again.");
                    }
                }

                @Override
                public void onFailure(Call<List<ResourceCommentModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadCommentsSnackMessage("A network error occurred!");
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadCommentsSnackMessage("An unknown error occurred! Please try again.");
        }
    }

    private void requestCommentAddition(String comment)
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        try
        {
            ResourcesService resourcesService = RetroServiceGenerator.generateService(ResourcesService.class);
            Call<Void> call = resourcesService.addResourceComment(author, comment, resourceModel.getId());
            call.enqueue(new Callback<Void>()
            {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response)
                {
                    toggleLoaderDisplay(false);
                    if (response.isSuccessful())
                    {
                        showSnackMessage("Comment added!", false);
                        commentInput.setText("");
                        requestComments();
                    }
                    else
                    {
                        showRetryAddCommentSnackMessage("Something went wrong! Please try again.", comment);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showRetryAddCommentSnackMessage("A network error occurred! Please try again.", comment);
                }
            });
        } catch (Exception e)
        {
            toggleLoaderDisplay(false);
            showRetryAddCommentSnackMessage("Something went wrong! Please try again.", comment);
        }
    }

    private void showError(String message)
{
    AlertUtil.showAlert(this, "ERROR", message);
}

    private Snackbar showSnackMessage(String message, boolean indefinite)
    {
        View root = findViewById(android.R.id.content);
        if (root == null)
            return null;

        Snackbar snackbar = Snackbar.make(root, message, indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        snackbar.show();
        return snackbar;
    }

    private Snackbar showSnackMessage(String message, boolean indefinite, String actionText, Runnable action)
    {
        View root = findViewById(android.R.id.content);
        if (root == null)
            return null;

        final Snackbar snackbar = Snackbar.make(root, message, indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        snackbar.setAction(actionText, v -> { snackbar.dismiss(); action.run(); });
        snackbar.show();
        return snackbar;
    }

    private Snackbar showConnectionErrorSnack(String message, Runnable onRetry)
    {
        return showSnackMessage(message, true, "Retry", onRetry);
    }

    private Snackbar showReloadCommentsSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            requestComments();
        });
    }

    private Snackbar showRetryAddCommentSnackMessage(String message, String comment)
    {
        return showConnectionErrorSnack(message, ()->
        {
            requestCommentAddition(comment);
        });
    }

    private Snackbar showOpenDownloadedResourceSnackMessage(String message, File resourceFile)
    {
        return showSnackMessage(message, true, "Open",
                () -> startActivity(FileUtil.buildViewFileIntent(this, resourceFile)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void commentsAvailable(ResourceCommentAvailableEvent commentAvailableEvent)
    {
        this.commentAvailableEvent = commentAvailableEvent;
        comments = commentAvailableEvent.resourceCommentModels;
        loadComments();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void downloadCompleted(DownloaderService.DownloadCompletedEvent downloadCompletedEvent)
    {
        showOpenDownloadedResourceSnackMessage(getString(R.string.resource_download_completed_message),
                downloadCompletedEvent.resourceFile);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void downloadFailed(DownloaderService.DownloadFailedEvent downloadFailedEvent)
    {
        if (downloadFailedEvent.exception == null)
        {
            showError(getString(R.string.resource_not_found_message));
            Timber.e("Attempted to download a non-existing resource");
        }
        else
        {
            showSnackMessage(getString(R.string.network_error_message), false);
        }
    }

    private static class CommentsViewHolder extends RecyclerView.ViewHolder
    {
        private TextView commentAuthor;
        private TextView commentDate;
        private TextView commentContent;

        public CommentsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            commentAuthor = itemView.findViewById(R.id.comment_author);
            commentDate = itemView.findViewById(R.id.comment_date);
            commentContent = itemView.findViewById(R.id.comment_content);
        }

        public void bindView (String author, String date, String content)
        {
            commentAuthor.setText(author);
            commentDate.setText(date);
            commentContent.setText(content);
        }
    }

    private class CommentsAdapter extends RecyclerView.Adapter<CommentsViewHolder>
    {

        @NonNull
        @Override
        public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new CommentsViewHolder(getLayoutInflater().inflate(R.layout.comment_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CommentsViewHolder holder, int position)
        {
            ResourceCommentModel commentModel = loadedComments.get(position);
            String author = commentModel.getAuthor();
            String date = "on " + commentModel.getDate();
            String content = commentModel.getComment();

            holder.bindView(author, date, content);
        }

        @Override
        public int getItemCount()
        {
            return loadedComments.size();
        }
    }
}





















