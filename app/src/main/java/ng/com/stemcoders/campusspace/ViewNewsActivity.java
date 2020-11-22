package ng.com.stemcoders.campusspace;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;

import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.NewsCommentAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.NewsCommentModel;
import ng.com.stemcoders.campusspace.net.models.NewsModel;
import ng.com.stemcoders.campusspace.net.services.NewsService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewNewsActivity extends AppCompatActivity
{

    private ProgressBar loaderDisplay;
    private EditText commentInput;
    private MaterialButton btnAddComment;
    private TextView titleView, departmentView, levelView, categoryView;
    private TextView dateView, contentView;
    private RecyclerView commentsList;
    private NestedScrollView scrollView;

    private List<NewsCommentModel> comments = new ArrayList<>();
    private List<NewsCommentModel> loadedComments = new ArrayList<>();
    private NewsCommentAvailableEvent commentAvailableEvent;

    private String author;

    private NewsModel newsModel = null;

    private static AsyncTask<Void, NewsCommentModel, Void> asyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_news);

        loaderDisplay = findViewById(R.id.view_news_progress_bar);
        commentInput = findViewById(R.id.view_news_comment_input);
        btnAddComment = findViewById(R.id.btn_view_news_add_comment);
        titleView = findViewById(R.id.view_news_title);
        departmentView = findViewById(R.id.view_news_department);
        levelView = findViewById(R.id.view_news_level);
        categoryView = findViewById(R.id.view_news_category);
        dateView = findViewById(R.id.view_news_date);
        contentView = findViewById(R.id.view_news_contents);

        commentsList = findViewById(R.id.view_news_comment_list);
        commentsList.setItemAnimator(new DefaultItemAnimator());
        commentsList.setAdapter(new CommentsAdapter());
        commentsList.setLayoutManager(new LinearLayoutManager(this));

        scrollView = findViewById(R.id.view_news_scrollview);

        Intent intent = getIntent();

        if (savedInstanceState == null)
        {
            newsModel = (NewsModel) intent.getExtras().getSerializable("news");
            requestComments();
        }
        else
            newsModel = (NewsModel)savedInstanceState.getSerializable("news");

        if (newsModel == null)
            finish();

        getSupportActionBar().setTitle(newsModel.getTitle());

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

        titleView.setText(newsModel.getTitle());
        departmentView.setText(newsModel.getDepartment());
        levelView.setText(newsModel.getLevel() + " " + getString(R.string.level));
        categoryView.setText(newsModel.getCategory());
        dateView.setText(getString(R.string.added_on) + " " + newsModel.getDate_added());
        contentView.setText(newsModel.getContent());

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.view_news_swipe_layout);
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

        outState.putSerializable("news", newsModel);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.getSerializable("comments") != null)
        {
            commentAvailableEvent = (NewsCommentAvailableEvent)savedInstanceState.getSerializable("comments");
            comments = commentAvailableEvent.newsCommentModels;
            loadComments(false);
        }
        else
            requestComments();
    }

    private void reloadList()
    {
        commentsList.getAdapter().notifyItemInserted(loadedComments.size());
        commentsList.smoothScrollToPosition(loadedComments.size()-1);
    }

    private void addComment(NewsCommentModel resourceModel)
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

        asyncTask = new AsyncTask<Void, NewsCommentModel, Void>()
        {
            @Override
            protected Void doInBackground(Void... objects)
            {
                for (NewsCommentModel commentModel : comments)
                {
                    publishProgress(commentModel);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(NewsCommentModel... values)
            {
                addComment(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                toggleLoaderDisplay(false);

                scrollView.smoothScrollTo(0, (int)commentsList.getY());

                if (showLoadedAlert)
                    showSnackMessage("News Comments Loaded!", false);
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
            NewsService newsService = RetroServiceGenerator.generateService(NewsService.class);
            Call<List<NewsCommentModel>> requestCall = newsService.getNewsComments(newsModel.getId(), false);
            requestCall.enqueue(new Callback<List<NewsCommentModel>>()
            {
                @Override
                public void onResponse(Call<List<NewsCommentModel>> call, Response<List<NewsCommentModel>> response)
                {
                    switch (response.code())
                    {
                        case 200:
                            EventBus.getDefault().post(new NewsCommentAvailableEvent(response.body()));
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
                public void onFailure(Call<List<NewsCommentModel>> call, Throwable t)
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
            NewsService newsService = RetroServiceGenerator.generateService(NewsService.class);
            Call<Void> call = newsService.addNewsComment(author, comment, newsModel.getId());
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void commentsAvailable(NewsCommentAvailableEvent commentAvailableEvent)
    {
        this.commentAvailableEvent = commentAvailableEvent;
        comments = commentAvailableEvent.newsCommentModels;
        loadComments();
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
            NewsCommentModel commentModel = loadedComments.get(position);
            String author = commentModel.getAuthor();
            String date = "on " + commentModel.getDate_added();
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




























