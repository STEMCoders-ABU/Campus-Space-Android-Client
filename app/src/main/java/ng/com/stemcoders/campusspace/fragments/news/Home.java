package ng.com.stemcoders.campusspace.fragments.news;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.leinardi.android.speeddial.SpeedDialView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.ViewNewsActivity;
import ng.com.stemcoders.campusspace.fragments.NewsFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.NewsAvailableEvent;
import ng.com.stemcoders.campusspace.net.events.ProviderAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.DepartmentModel;
import ng.com.stemcoders.campusspace.net.models.FacultyModel;
import ng.com.stemcoders.campusspace.net.models.LevelModel;
import ng.com.stemcoders.campusspace.net.models.NewsCategoryModel;
import ng.com.stemcoders.campusspace.net.models.NewsModel;
import ng.com.stemcoders.campusspace.net.services.NewsService;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Home extends Fragment
{
    private SpeedDialView speedDialView;

    SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView newsList;
    private List<NewsModel> news = new ArrayList<>();
    private List<NewsModel> loadedNews = new ArrayList<>();
    private NewsAvailableEvent newsAvailableEvent;

    private ArrayAdapter<List<String>> categoriesArrayAdapter;

    private int faculty_id = 1;
    private int department_id = 1;
    private int level_id = 1;
    private int category_id = 1;

    private ProgressBar newsLoaderDisplay;

    private Spinner categorySpinner;

    private AlertDialog optionsDialog;
    private ProgressBar loaderDisplay;
    private Spinner facultySpinner;
    private Spinner departmentSpinner;
    private Spinner levelSpinner;
    private CheckBox checkOverrideOptions;
    private MaterialButton btnClose;
    private MaterialButton btnDisplayNews;

    private AlertDialog searchDialog;
    private TextView searchDialogTitle;
    private TextInputEditText searchDialogInput;
    private MaterialButton btnCloseSearchDialog;
    private MaterialButton btnSearchNews;

    private Map<String, Integer> faculties = new TreeMap<>();
    private Map<String, Integer> departments = new TreeMap<>();
    private Map<String, Integer> levels = new TreeMap<>();
    private Map<String, Integer> news_categories = new TreeMap<>();

    private String appTitle = null;

    // This helps prevent category_spinner from executing any listener code when categories are automatically loaded
    private boolean categoriesAutoLoaded;

    private static AsyncTask<Void, NewsModel, Void> asyncTask;

    public Home(){}

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

        faculty_id = PreferenceUtil.facultyId(requireContext().getApplicationContext());
        department_id = PreferenceUtil.departmentId(requireContext().getApplicationContext());
        level_id = PreferenceUtil.levelId(requireContext().getApplicationContext());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);

        if (newsAvailableEvent == null)
            requestNews();
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);
        if (!news_categories.isEmpty())
            categoriesArrayAdapter = (ArrayAdapter<List<String>>)categorySpinner.getAdapter();
        else
            categoriesArrayAdapter = null;

        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.news_home, container, false);

        newsLoaderDisplay = root.findViewById(R.id.news_loader_display);

        categorySpinner = root.findViewById(R.id.newsCategorySpinner);
        categorySpinner.setEnabled(false);

        speedDialView = root.findViewById(R.id.news_speedDial);
        speedDialView.inflate(R.menu.news_home_speeddial);
        speedDialView.setOnActionSelectedListener(actionItem ->
        {
            switch (actionItem.getId())
            {
                case R.id.news_change_options:
                    speedDialView.close(true);
                    displayOptionsDialog();
                    break;
                case R.id.search_news:
                    speedDialView.close(true);
                    displaySearchDialog();
                    break;
            }

            return true;
        });
        toggleLoaderDisplay(false);

        swipeRefreshLayout = root.findViewById(R.id.news_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);

            if (categorySpinner.isEnabled())
                requestNews();
            else
                showReloadCategoriesSnackMessage("Please reload the categories!");
        });

        newsList = swipeRefreshLayout.findViewById(R.id.news_recycler_view);
        newsList.setItemAnimator(new DefaultItemAnimator());
        newsList.setAdapter(new NewsAdapter());

        newsList.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.recycler_grid_display_count),
                GridLayoutManager.VERTICAL, false));

        View optionsDialogView = getLayoutInflater().inflate(R.layout.news_options_dialog, null);

        loaderDisplay = optionsDialogView.findViewById(R.id.loaderDisplay);
        facultySpinner = optionsDialogView.findViewById(R.id.facultySpinner);
        facultySpinner.setEnabled(false);
        departmentSpinner = optionsDialogView.findViewById(R.id.departmentSpinner);
        departmentSpinner.setEnabled(false);
        levelSpinner = optionsDialogView.findViewById(R.id.levelSpinner);
        levelSpinner.setEnabled(false);
        checkOverrideOptions = optionsDialogView.findViewById(R.id.checkOverrideOptions);

        facultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (loaderDisplay.getVisibility() != View.VISIBLE)
                {
                    loaderDisplay.setVisibility(View.VISIBLE);
                    fetchDepartments();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        btnClose = optionsDialogView.findViewById(R.id.btnCloseDialog);
        btnDisplayNews = optionsDialogView.findViewById(R.id.btnDisplayItems);

        optionsDialog = AlertUtil.buildAlert(getContext(), optionsDialogView);

        btnClose.setOnClickListener(v ->
        {
            optionsDialog.dismiss();
        });

        btnDisplayNews.setOnClickListener(v ->
        {
            if (facultySpinner.isEnabled() && departmentSpinner.isEnabled() && levelSpinner.isEnabled())
            {
                faculty_id = faculties.get(facultySpinner.getSelectedItem());
                department_id = departments.get(departmentSpinner.getSelectedItem());
                level_id = levels.get(levelSpinner.getSelectedItem());

                if (checkOverrideOptions.isChecked())
                    PreferenceUtil.setUserData(requireContext().getApplicationContext(),
                            PreferenceUtil.displayName(requireContext().getApplicationContext()), faculty_id, department_id, level_id,
                            PreferenceUtil.courseId(requireContext().getApplicationContext())
                    );

                optionsDialog.dismiss();
                requestNews();
            }
            else
            {
                showError("Please make sure all fields are selected(enabled)!" +
                        "\n\nIf you're having troubles changing options, please contact us for support.");
            }
        });

        View searchDialogView = getLayoutInflater().inflate(R.layout.search_news_dialog, null);

        searchDialogTitle = searchDialogView.findViewById(R.id.news_search_title);
        searchDialogInput = searchDialogView.findViewById(R.id.news_search_input);
        btnCloseSearchDialog = searchDialogView.findViewById(R.id.btn_cancel_news_search);
        btnSearchNews = searchDialogView.findViewById(R.id.btn_search_news);

        btnCloseSearchDialog.setOnClickListener(v ->
        {
            searchDialog.dismiss();
        });

        btnSearchNews.setOnClickListener(v ->
        {
            if (TextUtils.isEmpty(searchDialogInput.getText().toString().trim()))
            {
                showError("Please enter the keyword to search!");
            }
            else
            {
                searchDialog.dismiss();
                String search = searchDialogInput.getText().toString();
                searchNews(search);
            }
        });

        searchDialog = AlertUtil.buildAlert(getContext(), searchDialogView);

        return root;
    }

    private void fetchDepartments()
    {
        String selected_faculty = (String)facultySpinner.getSelectedItem();
        int faculty_id = faculties.get(selected_faculty);
        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<DepartmentModel>> call = providersService.getDepartments(faculty_id);
        call.enqueue(new Callback<List<DepartmentModel>>()
        {
            @Override
            public void onResponse(Call<List<DepartmentModel>> call, Response<List<DepartmentModel>> response)
            {
                if (response.isSuccessful())
                {
                    if (!optionsDialog.isShowing())
                        return;

                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setDepartmentModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
                }
                else
                {
                    departmentSpinner.setEnabled(false);
                    showError("No departments found for the selected faculty! Please choose another faculty!");
                }
            }

            @Override
            public void onFailure(Call<List<DepartmentModel>> call, Throwable t)
            {
                departmentSpinner.setEnabled(false);
                showError("A network error occurred!");
            }
        });
    }

    private void displayOptionsDialog()
    {
        facultySpinner.setEnabled(false);
        departmentSpinner.setEnabled(false);
        levelSpinner.setEnabled(false);
        loaderDisplay.setVisibility(View.VISIBLE);
        checkOverrideOptions.setChecked(false);

        optionsDialog.show();

        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<FacultyModel>> facultyModelCall = providersService.getFaculties();
        facultyModelCall.enqueue(new Callback<List<FacultyModel>>()
        {
            @Override
            public void onResponse(Call<List<FacultyModel>> call, Response<List<FacultyModel>> response)
            {
                if (response.isSuccessful())
                {
                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setFacultyModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
                }
                else
                {
                    optionsDialog.dismiss();
                    showError("No faculties found! Please contact us for more details!");
                }
            }

            @Override
            public void onFailure(Call<List<FacultyModel>> call, Throwable t)
            {
                optionsDialog.dismiss();
                showError("A network error occurred!");
            }
        });
    }

    private void displaySearchDialog()
    {
        if (!categorySpinner.isEnabled())
        {
            showReloadCategoriesSnackMessage("Please reload categories!");
            return;
        }

        if (appTitle != null)
            searchDialogTitle.setText(getString(R.string.search) + "  " + appTitle);
        else
            searchDialogTitle.setText(getString(R.string.search));

        searchDialog.show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (newsAvailableEvent != null)
            outState.putSerializable("news", (ArrayList<NewsModel>) newsAvailableEvent.newsModels);

        outState.putInt("faculty_id", faculty_id);
        outState.putInt("department_id", department_id);
        outState.putInt("level_id", level_id);
        outState.putInt("category_id", category_id);

        if (categorySpinner != null && categorySpinner.isEnabled() && !news_categories.isEmpty())
        {
            outState.putBoolean("categories_available", true);
            outState.putSerializable("categories_titles", news_categories.keySet().toArray());
            outState.putIntegerArrayList("categories_ids", new ArrayList<>(news_categories.values()));
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null)
        {
            faculty_id = savedInstanceState.getInt("faculty_id");
            department_id = savedInstanceState.getInt("department_id");
            level_id = savedInstanceState.getInt("level_id");
            category_id = savedInstanceState.getInt("category_id");

            if (savedInstanceState.getBoolean("categories_available", false))
            {
                Object[] categories_titles = (Object[])savedInstanceState.getSerializable("categories_titles");
                ArrayList<Integer> categories_ids = savedInstanceState.getIntegerArrayList("categories_ids");

                news_categories.clear();
                for (int i=0; i<categories_titles.length; i++)
                    news_categories.put((String)categories_titles[i], categories_ids.get(i));

                ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                        new ArrayList(news_categories.keySet()));
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setEnabled(true);
                categorySpinner.setAdapter(arrayAdapter);

                int current_category_pos = findPosition(news_categories.values(), category_id);
                if (current_category_pos != -1)
                    categorySpinner.setSelection(current_category_pos);
            }

            if (savedInstanceState.getSerializable("news") != null)
            {
                news = (ArrayList<NewsModel>)savedInstanceState.getSerializable("news");
                loadNews(false);
            }
            else
                requestNews();
        }
        else
        {
            if (news_categories.isEmpty())
            {
                categoriesAutoLoaded = true;
                loadCategories();
            }
            else if (categoriesArrayAdapter != null)
            {
                categorySpinner.setEnabled(true);
                categorySpinner.setAdapter(categoriesArrayAdapter);

                int current_category_pos = findPosition(news_categories.values(), category_id);
                if (current_category_pos != -1)
                    categorySpinner.setSelection(current_category_pos);
            }
        }

        Handler handler = new Handler();
        handler.postDelayed(()->
        {
            categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    if (categoriesAutoLoaded)
                    {
                        if (newsAvailableEvent == null)
                            requestNews();

                        categoriesAutoLoaded = false;
                        return;
                    }

                    String category = (String)categorySpinner.getSelectedItem();
                    category_id = news_categories.get(category);
                    requestNews();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                }
            });
        }, 100);

        if (appTitle != null)
            EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(appTitle));
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
                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setNewsCategoryModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
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

    private void toggleLoaderDisplay(boolean visible)
    {
        if (visible)
            newsLoaderDisplay.setVisibility(View.VISIBLE);
        else
            newsLoaderDisplay.setVisibility(View.GONE);
    }

    private boolean isLoading()
    { return newsLoaderDisplay.getVisibility() == View.VISIBLE; }

    private void searchNews(String searchQuery)
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        newsList.removeAllViews();
        loadedNews.clear();

        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("News/Updates"));
        appTitle = null;

        try
        {
            NewsService newsService = RetroServiceGenerator.generateService(NewsService.class);
            Call<List<NewsModel>> requestCall = newsService.searchNews(searchQuery, faculty_id, department_id, level_id,
                    category_id, true);
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
                            showSnackMessage("No resources matched!",
                                    false);
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadSearchedNewsSnackMessage("An error occurred! Please search again.", searchQuery);
                    }
                }

                @Override
                public void onFailure(Call<List<NewsModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadSearchedNewsSnackMessage("A network error occurred!", searchQuery);
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadSearchedNewsSnackMessage("An unknown error occurred! Please search again.", searchQuery);
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

        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("News/Updates"));
        appTitle = null;

        try
        {
            NewsService newsService = RetroServiceGenerator.generateService(NewsService.class);
            Call<List<NewsModel>> requestCall = newsService.getNews(faculty_id, department_id, level_id,
                    category_id, true);
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
                            EventBus.getDefault().post(new NewsFragment.NewsCombinationChangedEvent(
                                    faculty_id, department_id, level_id, category_id
                            ));
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadNewsSnackMessage("An error occurred! Please try again.");
                            EventBus.getDefault().post(new NewsFragment.NewsCombinationChangedEvent(
                                    faculty_id, department_id, level_id, category_id
                            ));
                    }
                }

                @Override
                public void onFailure(Call<List<NewsModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadNewsSnackMessage("A network error occurred!");
                    EventBus.getDefault().post(new NewsFragment.NewsCombinationChangedEvent(
                            faculty_id, department_id, level_id, category_id
                    ));
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadNewsSnackMessage("An unknown error occurred! Please try again.");
            EventBus.getDefault().post(new NewsFragment.NewsCombinationChangedEvent(
                    faculty_id, department_id, level_id, category_id
            ));
        }
    }

    private void showError(String message)
    {
        AlertUtil.showAlert(getContext(), "Oops!", message);
    }

    private void showShortSnackMessage(String message)
    {
        if (getView() == null)
            return;

        Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);
        snackbar.show();
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

    private Snackbar showReloadNewsSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            requestNews();
        });
    }

    private Snackbar showReloadSearchedNewsSnackMessage(String message, String searchQuery)
    {
        return showConnectionErrorSnack(message, ()->
        {
            searchNews(searchQuery);
        });
    }

    private Snackbar showReloadCategoriesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            loadCategories();
        });
    }

    private void loadNews()
    { loadNews(true); }

    private void loadNews(boolean showLoadedAlert)
    {
        if (news.isEmpty())
            return;

        loadedNews.clear();
        newsList.removeAllViews();

        asyncTask = new AsyncTask<Void, NewsModel, Void>()
        {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                NewsModel news = Home.this.news.get(0);
                String category = news.getCategory();
                if (!category.endsWith("s"))
                    category += "s";
                appTitle = getString(R.string.news) + "  [" + category + "]";
                EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(appTitle));
            }

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
                    showSnackMessage("News/Update Loaded!", false);

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

    private void addNews(NewsModel resourceModel)
    {
        loadedNews.add(resourceModel);
        reloadList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void newsAvailable(NewsAvailableEvent newsAvailableEvent)
    {
        this.newsAvailableEvent = newsAvailableEvent;
        news = this.newsAvailableEvent.newsModels;
        EventBus.getDefault().post(new NewsFragment.NewsCombinationChangedEvent(
                faculty_id, department_id, level_id, category_id
        ));
        loadNews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void providerAvailable(ProviderAvailableEvent providerAvailableEvent)
    {
        if (providerAvailableEvent.getFacultyModels() != null)
        {
            faculties.clear();
            for (FacultyModel facultyModel : providerAvailableEvent.getFacultyModels())
                faculties.put(facultyModel.getFaculty(), facultyModel.getId());

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                    new ArrayList(faculties.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            facultySpinner.setEnabled(true);
            facultySpinner.setAdapter(arrayAdapter);

            int facultyId = PreferenceUtil.facultyId(requireContext().getApplicationContext());
            if (facultyId != -1)
            {
                int current_pos = findPosition(faculties.values(), facultyId);
                if (current_pos != -1)
                    facultySpinner.setSelection(current_pos);
            }

            ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
            Call<List<LevelModel>>  levelModelCall = providersService.getLevels();
            levelModelCall.enqueue(new Callback<List<LevelModel>>()
            {
                @Override
                public void onResponse(Call<List<LevelModel>> call, Response<List<LevelModel>> response)
                {
                    if (response.isSuccessful())
                    {
                        if (!optionsDialog.isShowing())
                            return;

                        ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                        providerAvailableEvent.setLevelModels(response.body());
                        EventBus.getDefault().post(providerAvailableEvent);
                    }
                    else
                    {
                        optionsDialog.dismiss();
                        showError("No levels found! Please contact us for more details!");
                    }
                }

                @Override
                public void onFailure(Call<List<LevelModel>> call, Throwable t)
                {
                    optionsDialog.dismiss();
                    showError("A network error occurred!");
                }
            });
        }

        else if (providerAvailableEvent.getLevelModels() != null)
        {
            levels.clear();

            for (LevelModel levelModel : providerAvailableEvent.getLevelModels())
                levels.put(levelModel.getLevel(), levelModel.getId());

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                    new ArrayList(levels.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            levelSpinner.setEnabled(true);
            levelSpinner.setAdapter(arrayAdapter);

            int levelId = PreferenceUtil.levelId(requireContext().getApplicationContext());
            if (levelId != -1)
            {
                int current_pos = findPosition(levels.values(), levelId);
                if (current_pos != -1)
                    levelSpinner.setSelection(current_pos);
            }

            fetchDepartments();
        }

        else if (providerAvailableEvent.getDepartmentModels() != null)
        {
            departments.clear();

            for (DepartmentModel departmentModel : providerAvailableEvent.getDepartmentModels())
                departments.put(departmentModel.getDepartment(), departmentModel.getId());

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                    new ArrayList(departments.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            departmentSpinner.setEnabled(true);
            departmentSpinner.setAdapter(arrayAdapter);

            int departmentId = PreferenceUtil.departmentId(requireContext().getApplicationContext());
            if (departmentId != -1)
            {
                int current_pos = findPosition(departments.values(), departmentId);
                if (current_pos != -1)
                    departmentSpinner.setSelection(current_pos);
            }

            loaderDisplay.setVisibility(View.GONE);
        }

        else if (providerAvailableEvent.getNewsCategoryModels() != null)
        {
            news_categories.clear();

            for (NewsCategoryModel newsCategoryModel : providerAvailableEvent.getNewsCategoryModels())
            {
                String category = newsCategoryModel.getCategory();
                if (!category.endsWith("s"))
                    category += "s";
                news_categories.put(category, newsCategoryModel.getId());
            }

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                    new ArrayList(news_categories.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setEnabled(true);
            categorySpinner.setAdapter(arrayAdapter);

            int current_category_pos = findPosition(news_categories.values(), category_id);
            if (current_category_pos != -1)
                categorySpinner.setSelection(current_category_pos);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }

    private static class NewsViewHolder extends RecyclerView.ViewHolder
    {
        private TextView cardTitle;
        private TextView cardContent;
        private TextView cardDate;
        private MaterialButton btnView;

        public NewsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            cardTitle = itemView.findViewById(R.id.news_card_title);
            cardContent = itemView.findViewById(R.id.news_card_content);
            cardDate = itemView.findViewById(R.id.news_card_date);

            btnView = itemView.findViewById(R.id.btn_view_news);
        }

        public void bindView (NewsModel newsModel, Activity activity)
        {
            String title = newsModel.getTitle();
            String content = newsModel.getContent();
            String date = newsModel.getDate_added();

            cardTitle.setText(title);
            cardContent.setText(content);
            cardDate.setText(date);

            btnView.setOnClickListener(v ->
            {
                Intent intent = new Intent(activity, ViewNewsActivity.class);
                intent.putExtra("news", newsModel);
                activity.startActivity(intent);
            });
        }
    }

    private class NewsAdapter extends RecyclerView.Adapter<NewsViewHolder>
    {

        @NonNull
        @Override
        public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new NewsViewHolder(getLayoutInflater().inflate(R.layout.news_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull NewsViewHolder holder, int position)
        {
            NewsModel newsModel = loadedNews.get(position);
            holder.bindView(newsModel, getActivity());
        }

        @Override
        public int getItemCount()
        {
            return loadedNews.size();
        }
    }
}































