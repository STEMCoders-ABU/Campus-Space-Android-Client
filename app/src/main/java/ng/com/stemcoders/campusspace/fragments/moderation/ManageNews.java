package ng.com.stemcoders.campusspace.fragments.moderation;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.EditNewsActivity;
import ng.com.stemcoders.campusspace.EditResourceActivity;
import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.NewsAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.NewsCategoryModel;
import ng.com.stemcoders.campusspace.net.models.NewsModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class ManageNews extends Fragment
{
    private ProgressBar loaderDisplay;
    private Spinner categorySpinner;
    private Button btnFilter;
    private RecyclerView newsList;

    private List<NewsModel> news = new ArrayList<>();
    private List<NewsModel> loadedNews = new ArrayList<>();
    private NewsAvailableEvent newsAvailableEvent;

    private Map<String, Integer> categories = new TreeMap<>();
    private ArrayAdapter<List<String>> categoriesArrayAdapter;

    private int category_id = 2;

    private static <T> int findPosition(Collection<T> collection, T item)
    {
        T[] array = (T[])collection.toArray();
        for (int i=0; i<array.length; i++)
        {
            if (array[i] == item)
                return i;
        }

        return -1;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Manage News/Updates"));

        if (newsAvailableEvent == null)
            requestNews();
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);

        if (!categories.isEmpty())
            categoriesArrayAdapter = (ArrayAdapter<List<String>>)categorySpinner.getAdapter();
        else
            categoriesArrayAdapter = null;

        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout)inflater.inflate(R.layout.moderation_manage_news, container, false);

        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);

            if (categorySpinner.isEnabled())
                requestNews();
            else
                showReloadCategoriesSnackMessage("Please reload the categories!");
        });

        loaderDisplay = swipeRefreshLayout.findViewById(R.id.loader_display);
        categorySpinner = swipeRefreshLayout.findViewById(R.id.category_spinner);
        categorySpinner.setEnabled(false);
        btnFilter = swipeRefreshLayout.findViewById(R.id.btn_filter);
        btnFilter.setOnClickListener(v -> filterNews());

        newsList = swipeRefreshLayout.findViewById(R.id.items_list_view);
        newsList.setItemAnimator(new DefaultItemAnimator());
        newsList.setAdapter(new NewsAdapter());
        newsList.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.recycler_grid_display_count),
                GridLayoutManager.VERTICAL, false));
        return swipeRefreshLayout;
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

    private Snackbar showReloadCategoriesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            loadCategories();
        });
    }

    private Snackbar showReloadNewsSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            requestNews();
        });
    }

    private void showError(String message)
    {
        AlertUtil.showAlert(getContext(), "Oops!", message);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (!news.isEmpty())
            outState.putSerializable("news", (ArrayList<NewsModel>) news);

        outState.putInt("category_id", category_id);

        if (categorySpinner != null && categorySpinner.isEnabled() && !categories.isEmpty())
        {
            outState.putBoolean("categories_available", true);
            outState.putInt("selected_category_pos", findPosition(categories.keySet(),
                    (String)categorySpinner.getSelectedItem()));
            outState.putSerializable("categories_titles", categories.keySet().toArray());
            outState.putIntegerArrayList("categories_ids", new ArrayList<>(categories.values()));
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null)
        {
            category_id = savedInstanceState.getInt("category_id");

            if (savedInstanceState.getBoolean("categories_available", false))
            {
                Object[] categories_titles = (Object[])savedInstanceState.getSerializable("categories_titles");
                ArrayList<Integer> categories_ids = savedInstanceState.getIntegerArrayList("categories_ids");

                categories.clear();
                for (int i=0; i<categories_titles.length; i++)
                    categories.put((String)categories_titles[i], categories_ids.get(i));

                ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                        new ArrayList(categories.keySet()));
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setEnabled(true);
                categorySpinner.setAdapter(arrayAdapter);

                int current_category_pos = savedInstanceState.getInt("selected_category_pos");
                if (current_category_pos != -1)
                    categorySpinner.setSelection(current_category_pos);
            }

            if (savedInstanceState.getSerializable("news") != null)
            {
                news = (ArrayList<NewsModel>)savedInstanceState.getSerializable("resources");
                loadNews(false);
            }
            else
                requestNews();
        }
        else
        {
            if (categories.isEmpty())
            {
                loadCategories();
            }
            else if (categoriesArrayAdapter != null)
            {
                categorySpinner.setEnabled(true);
                categorySpinner.setAdapter(categoriesArrayAdapter);
            }
        }
    }

    private void requestNews()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        newsList.removeAllViews();
        loadedNews.clear();
        newsAvailableEvent = null;

        try
        {
            String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
            String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

            ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
            Call<List<NewsModel>> requestCall = moderatorService.getModeratorNews(category_id, true);
            requestCall.enqueue(new Callback<List<NewsModel>>()
            {
                @Override
                public void onResponse(Call<List<NewsModel>> call, Response<List<NewsModel>> response)
                {
                    switch (response.code())
                    {
                        case 200:
                            if (getActivity() != null)
                                EventBus.getDefault().post(new NewsAvailableEvent(response.body()));
                            break;
                        case 404:
                            toggleLoaderDisplay(false);
                            showSnackMessage("No news/updates found!",
                                    false);
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadNewsSnackMessage("An error occurred! Please try again.");
                    }
                }

                @Override
                public void onFailure(Call<List<NewsModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadNewsSnackMessage("A network error occurred!");
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadNewsSnackMessage("An unknown error occurred! Please try again.");
        }
    }

    private void loadCategories()
    {
        if (isLoading())
            return;

        categorySpinner.setEnabled(false);

        toggleLoaderDisplay(true);

        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<NewsCategoryModel>> call = providersService.getNewsCategories();
        call.enqueue(new Callback<List<NewsCategoryModel>>()
        {
            @Override
            public void onResponse(Call<List<NewsCategoryModel>> call, Response<List<NewsCategoryModel>> response)
            {
                toggleLoaderDisplay(false);

                if (response.isSuccessful())
                {
                    categoriesAvailable(response.body());
                }
                else
                {
                    showSnackMessage("No News category found!\n\nThis is an unexpected error, please contact us ASAP", false);
                }
            }

            @Override
            public void onFailure(Call<List<NewsCategoryModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showReloadCategoriesSnackMessage("A network error occurred!");
            }
        });
    }

    private void filterNews()
    {
        if (!categorySpinner.isEnabled())
        {
            showReloadCategoriesSnackMessage("Please reload the categories and courses!");
            return;
        }

        category_id = categories.get(categorySpinner.getSelectedItem());
        requestNews();
    }

    private void categoriesAvailable(List<NewsCategoryModel> newsCategoryModels)
    {
        categories.clear();

        for (NewsCategoryModel categoryModel : newsCategoryModels)
            categories.put(categoryModel.getCategory(), categoryModel.getId());

        ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                new ArrayList(categories.keySet()));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setEnabled(true);
        categorySpinner.setAdapter(arrayAdapter);
        category_id = categories.get(categorySpinner.getSelectedItem());

        requestNews();
    }

    private void loadNews()
    { loadNews(true); }

    private void loadNews(boolean showLoadedAlert)
    {
        if (news.isEmpty())
            return;

        loadedNews.clear();
        newsList.removeAllViews();

        AsyncTask<Void, NewsModel, Void> asyncTask = new AsyncTask<Void, NewsModel, Void>()
        {
            @Override
            protected Void doInBackground(Void... objects)
            {
                for (NewsModel newsModel : news)
                {
                    publishProgress(newsModel);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(NewsModel... values)
            {
                addNews(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                toggleLoaderDisplay(false);

                if (showLoadedAlert)
                    showSnackMessage("News/Updates Loaded!", false);

                if (!categorySpinner.isEnabled())
                    loadCategories();
            }
        };
        asyncTask.execute();
    }

    private void reloadList()
    {
        newsList.getAdapter().notifyItemInserted(loadedNews.size());
        newsList.smoothScrollToPosition(loadedNews.size()-1);
    }

    private void reloadListAfterRemoval(int position)
    {
        newsList.getAdapter().notifyItemRemoved(position);
        if (!loadedNews.isEmpty())
            newsList.smoothScrollToPosition(loadedNews.size()-1);
    }

    private void addNews(NewsModel newsModel)
    {
        loadedNews.add(newsModel);
        reloadList();
    }

    private void removeNews(int position)
    {
        loadedNews.remove(position);
        reloadListAfterRemoval(position);
    }

    private void deleteNews(int newsId, int position)
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the existing task to complete!", false);
            return;
        }

        toggleLoaderDisplay(true);

        String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<Void> call = moderatorService.removeNews(newsId);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);

                if (response.isSuccessful())
                {
                    removeNews(position);
                    showSnackMessage("News deleted!", false);
                }
                else
                {
                    showSnackMessage("An unknown error occurred, please try again!", false);
                    try
                    {
                        Timber.e(response.errorBody().string());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showSnackMessage("A network error occurred!", false);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void newsAvailable(NewsAvailableEvent newsAvailableEvent)
    {
        this.newsAvailableEvent = newsAvailableEvent;
        news = newsAvailableEvent.newsModels;
        loadNews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        AlertUtil.showConfirmDialog(getContext(), "Exit?",
                "Do you really want to exit now?", () ->
                {
                    toggleLoaderDisplay(false);
                    EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(Home.class));
                });
    }

    private class NewsViewHolder extends RecyclerView.ViewHolder
    {
        private ImageView cardImg;
        private TextView cardTitle;
        private TextView cardContent;
        private TextView cardDate;
        private MaterialButton btnEdit;
        private MaterialButton btnRemove;

        public NewsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            cardImg = itemView.findViewById(R.id.news_card_img);
            cardTitle = itemView.findViewById(R.id.news_card_title);
            cardContent = itemView.findViewById(R.id.news_card_content);
            cardDate = itemView.findViewById(R.id.news_card_date);

            btnEdit = itemView.findViewById(R.id.btn_edit_news);
            btnRemove = itemView.findViewById(R.id.btn_remove_news);
        }

        public void bindView (NewsModel newsModel, Activity activity, int position)
        {
            String title = newsModel.getTitle();
            String content = newsModel.getContent();
            String date = newsModel.getDate_added();

            cardTitle.setText(title);
            cardContent.setText(content);
            cardDate.setText(date);

            btnEdit.setOnClickListener(v ->
            {
                Intent intent = new Intent(activity, EditNewsActivity.class);
                intent.putExtra("news", newsModel);
                activity.startActivity(intent);
            });

            btnRemove.setOnClickListener(v ->
            {
                AlertUtil.showConfirmDialog(activity, "Delete News?",
                        "Do you really want to permanently remove this news/update now?\n\n" +
                                "Please note, this can not be reversed!",
                        () -> deleteNews(newsModel.getId(), position));
            });
        }
    }

    private class NewsAdapter extends RecyclerView.Adapter<NewsViewHolder>
    {

        @NonNull
        @Override
        public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new NewsViewHolder(getLayoutInflater().inflate(R.layout.manage_news_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull NewsViewHolder holder, int position)
        {
            NewsModel newsModel = loadedNews.get(position);
            holder.bindView(newsModel, getActivity(), position);
        }

        @Override
        public int getItemCount()
        {
            return loadedNews.size();
        }
    }
}






























