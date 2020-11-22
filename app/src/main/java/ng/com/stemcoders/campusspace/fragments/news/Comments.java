package ng.com.stemcoders.campusspace.fragments.news;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.NewsFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.NewsCategoryCommentAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.NewsCategoryCommentModel;
import ng.com.stemcoders.campusspace.net.services.NewsService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Comments extends Fragment
{
    SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView commentsList;
    private List<NewsCategoryCommentModel> comments = new ArrayList<>();
    private List<NewsCategoryCommentModel> loadedComments = new ArrayList<>();
    private NewsCategoryCommentAvailableEvent newsCategoryCommentAvailableEvent;

    private int department_id = -1;
    private int level_id = -1;
    private int category_id = 1;

    private ProgressBar commentsLoaderDisplay;

    private EditText commentInput;
    private MaterialButton btnAddComment;

    private NewsFragment.NewsCombinationChangedEvent newsCombinationChangedEvent;

    private String appTitle = null;

    private String author;

    private static AsyncTask<Void, NewsCategoryCommentModel, Void> asyncTask;

    public Comments(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        department_id = PreferenceUtil.departmentId(requireContext().getApplicationContext());
        level_id = PreferenceUtil.levelId(requireContext().getApplicationContext());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
        author = PreferenceUtil.displayName(requireContext().getApplicationContext());
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.news_comments, container, false);

        commentsLoaderDisplay = root.findViewById(R.id.news_comments_loader_display);
        toggleLoaderDisplay(false);

        swipeRefreshLayout = root.findViewById(R.id.news_comments_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);
            requestComments();
        });

        commentsList = swipeRefreshLayout.findViewById(R.id.news_comments_recycler_view);
        commentsList.setItemAnimator(new DefaultItemAnimator());
        commentsList.setAdapter(new CommentsAdapter());

        commentsList.setLayoutManager(new LinearLayoutManager(getContext()));

        commentInput = root.findViewById(R.id.news_comments_input);
        btnAddComment = root.findViewById(R.id.btn_add_news_category_comment);
        btnAddComment.setOnClickListener(v ->
        {
            if (TextUtils.isEmpty(commentInput.getText().toString().trim()))
                showSnackMessage("Please write a comment text first!", false);
            else
            {
                requestCommentAddition(commentInput.getText().toString());
            }
        });

        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (newsCategoryCommentAvailableEvent != null)
            outState.putSerializable("comments", (ArrayList<NewsCategoryCommentModel>) newsCategoryCommentAvailableEvent.newsCategoryCommentModels);

        outState.putInt("department_id", department_id);
        outState.putInt("level_id", level_id);
        outState.putInt("category_id", category_id);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null)
        {
            department_id = savedInstanceState.getInt("department_id");
            level_id = savedInstanceState.getInt("level_id");
            category_id = savedInstanceState.getInt("category_id");

            if (savedInstanceState.getSerializable("comments") != null)
            {
                comments = (ArrayList<NewsCategoryCommentModel>)savedInstanceState.getSerializable("comments");
                loadComments(false);
            }
            else
                requestComments();
        }

        if (appTitle != null)
            EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(appTitle));
    }

    private void reloadList()
    {
        commentsList.getAdapter().notifyItemInserted(loadedComments.size());
        commentsList.smoothScrollToPosition(loadedComments.size()-1);
    }

    private void addComment(NewsCategoryCommentModel resourceModel)
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

        asyncTask = new AsyncTask<Void, NewsCategoryCommentModel, Void>()
        {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                NewsCategoryCommentModel comment = comments.get(0);
                String category = comment.getCategory();
                if (!category.endsWith("s"))
                    category += "s";
                appTitle = getString(R.string.comments) + "  [" + category + "]";
                EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(appTitle));
            }

            @Override
            protected Void doInBackground(Void... objects)
            {
                for (NewsCategoryCommentModel categoryCommentModel : comments)
                {
                    publishProgress(categoryCommentModel);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(NewsCategoryCommentModel... values)
            {
                addComment(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                toggleLoaderDisplay(false);

                if (showLoadedAlert)
                    showSnackMessage("Category Comments Loaded!", false);
            }
        };
        asyncTask.execute();
    }

    private void toggleLoaderDisplay(boolean visible)
    {
        if (visible)
            commentsLoaderDisplay.setVisibility(View.VISIBLE);
        else
            commentsLoaderDisplay.setVisibility(View.GONE);
    }

    private boolean isLoading()
    { return commentsLoaderDisplay.getVisibility() == View.VISIBLE; }

    private void requestComments()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        commentsList.removeAllViews();
        loadedComments.clear();
        newsCategoryCommentAvailableEvent = null;

        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("News/Updates"));
        appTitle = null;

        try
        {
            NewsService newsService = RetroServiceGenerator.generateService(NewsService.class);
            Call<List<NewsCategoryCommentModel>> requestCall = newsService.getCategoryComments(department_id, level_id,
                    category_id, true);
            requestCall.enqueue(new Callback<List<NewsCategoryCommentModel>>()
            {
                @Override
                public void onResponse(Call<List<NewsCategoryCommentModel>> call, Response<List<NewsCategoryCommentModel>> response)
                {
                    toggleLoaderDisplay(false);
                    switch (response.code())
                    {
                        case 200:
                            if (getActivity() != null)
                                EventBus.getDefault().post(new NewsCategoryCommentAvailableEvent(response.body()));
                            break;
                        case 404:
                            showSnackMessage("No comments found!",
                                    false);
                            break;
                        default:
                            showReloadCommentsSnackMessage("An error occurred! Please try again.");
                    }
                }

                @Override
                public void onFailure(Call<List<NewsCategoryCommentModel>> call, Throwable t)
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
            Call<Void> call = newsService.addCategoryComment(author, comment, category_id, department_id, level_id);
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
        AlertUtil.showAlert(getContext(), "ERROR", message);
    }

    private Snackbar showSnackMessage(String message, boolean indefinite)
    {
        if (getView() == null)
            return null;

        Snackbar snackbar = Snackbar.make(getView(), message, indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        snackbar.show();
        return snackbar;
    }

    private Snackbar showSnackMessage(String message, boolean indefinite, String actionText, Runnable action)
    {
        if (getView() == null)
            return null;

        final Snackbar snackbar = Snackbar.make(getView(), message, indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
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
    public void resourcesCombinationChanged(NewsFragment.NewsCombinationChangedEvent newsCombinationChangedEvent)
    {
        if (this.newsCombinationChangedEvent == null)
        {
            this.newsCombinationChangedEvent = newsCombinationChangedEvent;
            department_id = this.newsCombinationChangedEvent.department_id;
            level_id = this.newsCombinationChangedEvent.level_id;
            category_id = this.newsCombinationChangedEvent.category_id;

            requestComments();
        }

        else if (! this.newsCombinationChangedEvent.similar(newsCombinationChangedEvent) || loadedComments.isEmpty())
        {
            this.newsCombinationChangedEvent = newsCombinationChangedEvent;
            department_id = this.newsCombinationChangedEvent.department_id;
            level_id = this.newsCombinationChangedEvent.level_id;
            category_id = this.newsCombinationChangedEvent.category_id;
            requestComments();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void commentsAvailable(NewsCategoryCommentAvailableEvent resourcesAvailableEvent)
    {
        this.newsCategoryCommentAvailableEvent = resourcesAvailableEvent;
        comments = resourcesAvailableEvent.newsCategoryCommentModels;
        loadComments();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
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
            NewsCategoryCommentModel commentModel = loadedComments.get(position);
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





























