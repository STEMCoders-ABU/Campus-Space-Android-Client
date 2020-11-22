package ng.com.stemcoders.campusspace.fragments.resources;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.DownloaderService;
import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.ViewResourceActivity;
import ng.com.stemcoders.campusspace.fragments.ResourcesFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.ProviderAvailableEvent;
import ng.com.stemcoders.campusspace.net.events.ResourcesAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.DepartmentModel;
import ng.com.stemcoders.campusspace.net.models.FacultyModel;
import ng.com.stemcoders.campusspace.net.models.LevelModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCategoryModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.net.services.ResourcesService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.FileUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class Home extends Fragment
{
    private SpeedDialView speedDialView;

    SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView resourcesList;
    private List<ResourceModel> resources = new ArrayList<>();
    private List<ResourceModel> loadedResources = new ArrayList<>();
    private ResourcesAvailableEvent resourcesAvailableEvent;

    private ArrayAdapter<List<String>> categoriesArrayAdapter;

    private int faculty_id = -1;
    private int department_id = -1;
    private int level_id = -1;
    private int category_id = 2;
    private int course_id = -2;

    private ProgressBar resourcesLoaderDisplay;

    private Spinner categorySpinner;

    private AlertDialog optionsDialog;
    private ProgressBar loaderDisplay;
    private Spinner facultySpinner;
    private Spinner departmentSpinner;
    private Spinner levelSpinner;
    private Spinner courseSpinner;
    private CheckBox checkOverrideOptions;
    private MaterialButton btnClose;
    private MaterialButton btnDisplayResources;

    private AlertDialog searchDialog;
    private TextView searchDialogTitle;
    private TextInputEditText searchDialogInput;
    private MaterialButton btnCloseSearchDialog;
    private MaterialButton btnSearchResources;

    private Map<String, Integer> faculties = new TreeMap<>();
    private Map<String, Integer> departments = new TreeMap<>();
    private Map<String, Integer> levels = new TreeMap<>();
    private Map<String, Integer> courses = new TreeMap<>();
    private Map<String, Integer> resource_categories = new TreeMap<>();

    private String appTitle = null;

    // This helps prevent category_spinner from executing any listener code when categories are automatically loaded
    private boolean categoriesAutoLoaded;

    private static AsyncTask<Void, ResourceModel, Void> asyncTask;

    public Home()
    {}

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
        course_id = PreferenceUtil.courseId(requireContext().getApplicationContext());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);

        if (resourcesAvailableEvent == null)
            requestResources();
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);
        if (!resource_categories.isEmpty())
            categoriesArrayAdapter = (ArrayAdapter<List<String>>)categorySpinner.getAdapter();
        else
            categoriesArrayAdapter = null;

        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.resources_home, container, false);

        resourcesLoaderDisplay = root.findViewById(R.id.resources_loader_display);

        categorySpinner = root.findViewById(R.id.resourceCategorySpinner);
        categorySpinner.setEnabled(false);

        speedDialView = root.findViewById(R.id.speedDial);
        speedDialView.inflate(R.menu.resources_home_speeddial);
        speedDialView.setOnActionSelectedListener(actionItem ->
        {
            switch (actionItem.getId())
            {
                case R.id.resources_change_options:
                    speedDialView.close(true);
                    displayOptionsDialog();
                    break;
                case R.id.search_resources:
                    speedDialView.close(true);
                    displaySearchDialog();
                    break;
            }

            return true;
        });
        toggleLoaderDisplay(false);

        swipeRefreshLayout = root.findViewById(R.id.resources_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);

            if (categorySpinner.isEnabled())
                requestResources();
            else
                showReloadCategoriesSnackMessage("Please reload the categories!");
        });

        resourcesList = swipeRefreshLayout.findViewById(R.id.resources_recycler_view);
        resourcesList.setItemAnimator(new DefaultItemAnimator());
        resourcesList.setAdapter(new ResourcesAdapter());
        resourcesList.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.recycler_grid_display_count),
                GridLayoutManager.VERTICAL, false));

        View optionsDialogView = getLayoutInflater().inflate(R.layout.resources_options_dialog, null);

        loaderDisplay = optionsDialogView.findViewById(R.id.loaderDisplay);
        facultySpinner = optionsDialogView.findViewById(R.id.facultySpinner);
        facultySpinner.setEnabled(false);
        departmentSpinner = optionsDialogView.findViewById(R.id.departmentSpinner);
        departmentSpinner.setEnabled(false);
        levelSpinner = optionsDialogView.findViewById(R.id.levelSpinner);
        levelSpinner.setEnabled(false);
        courseSpinner = optionsDialogView.findViewById(R.id.courseSpinner);
        courseSpinner.setEnabled(false);
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

        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (loaderDisplay.getVisibility() != View.VISIBLE)
                {
                    loaderDisplay.setVisibility(View.VISIBLE);
                    fetchCourses();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        btnClose = optionsDialogView.findViewById(R.id.btnCloseDialog);
        btnDisplayResources = optionsDialogView.findViewById(R.id.btnDisplayItems);

        optionsDialog = AlertUtil.buildAlert(getContext(), optionsDialogView);

        btnClose.setOnClickListener(v ->
        {
            optionsDialog.dismiss();
        });

        btnDisplayResources.setOnClickListener(v ->
        {
            if (facultySpinner.isEnabled() && departmentSpinner.isEnabled() && levelSpinner.isEnabled()
                && courseSpinner.isEnabled())
            {
                faculty_id = faculties.get(facultySpinner.getSelectedItem());
                department_id = departments.get(departmentSpinner.getSelectedItem());
                level_id = levels.get(levelSpinner.getSelectedItem());
                course_id = courses.get(courseSpinner.getSelectedItem());

                if (checkOverrideOptions.isChecked())
                    PreferenceUtil.setUserData(requireContext().getApplicationContext(),
                            PreferenceUtil.displayName(requireContext().getApplicationContext()), faculty_id, department_id, level_id,
                            course_id
                    );

                optionsDialog.dismiss();
                requestResources();
            }
            else
            {
                showError("Please make sure all fields are selected(enabled)!" +
                        "\n\nIf you're having troubles changing options, please contact us for support.");
            }
        });

        View searchDialogView = getLayoutInflater().inflate(R.layout.search_resources_dialog, null);

        searchDialogTitle = searchDialogView.findViewById(R.id.resource_search_title);
        searchDialogInput = searchDialogView.findViewById(R.id.resource_search_input);
        btnCloseSearchDialog = searchDialogView.findViewById(R.id.btn_cancel_resource_search);
        btnSearchResources = searchDialogView.findViewById(R.id.btn_search_resource);

        btnCloseSearchDialog.setOnClickListener(v ->
        {
            searchDialog.dismiss();
        });

        btnSearchResources.setOnClickListener(v ->
        {
            if (TextUtils.isEmpty(searchDialogInput.getText().toString().trim()))
            {
                showError("Please enter the keyword to search!");
            }
            else
            {
                searchDialog.dismiss();
                String search = searchDialogInput.getText().toString();
                searchResources(search);
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

    private void fetchCourses()
    {
        String selected_department = (String)departmentSpinner.getSelectedItem();
        int department_id = departments.get(selected_department);
        String selected_level = (String)levelSpinner.getSelectedItem();
        int level_id = levels.get(selected_level);
        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<CourseModel>> call = providersService.getCourses(department_id, level_id);
        call.enqueue(new Callback<List<CourseModel>>()
        {
            @Override
            public void onResponse(Call<List<CourseModel>> call, Response<List<CourseModel>> response)
            {
                if (response.isSuccessful())
                {
                    if (!optionsDialog.isShowing())
                        return;

                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setCourseModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
                }
                else
                {
                    loaderDisplay.setVisibility(View.GONE);
                    courseSpinner.setEnabled(false);
                    showError("No courses found for the selected combination! Please choose another faculty and/or department!");
                }
            }

            @Override
            public void onFailure(Call<List<CourseModel>> call, Throwable t)
            {
                loaderDisplay.setVisibility(View.GONE);
                courseSpinner.setEnabled(false);
                showError("A network error occurred!");
            }
        });
    }

    private void displayOptionsDialog()
    {
        facultySpinner.setEnabled(false);
        departmentSpinner.setEnabled(false);
        levelSpinner.setEnabled(false);
        courseSpinner.setEnabled(false);
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

        if (resourcesAvailableEvent != null)
            outState.putSerializable("resources", (ArrayList<ResourceModel>)resourcesAvailableEvent.resourceModels);

        outState.putInt("faculty_id", faculty_id);
        outState.putInt("department_id", department_id);
        outState.putInt("level_id", level_id);
        outState.putInt("category_id", category_id);
        outState.putInt("course_id", course_id);

        if (categorySpinner != null && categorySpinner.isEnabled() && !resource_categories.isEmpty())
        {
            outState.putBoolean("categories_available", true);
            outState.putSerializable("categories_titles", resource_categories.keySet().toArray());
            outState.putIntegerArrayList("categories_ids", new ArrayList<>(resource_categories.values()));
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
            course_id = savedInstanceState.getInt("course_id");

            if (savedInstanceState.getBoolean("categories_available", false))
            {
                Object[] categories_titles = (Object[])savedInstanceState.getSerializable("categories_titles");
                ArrayList<Integer> categories_ids = savedInstanceState.getIntegerArrayList("categories_ids");

                resource_categories.clear();
                for (int i=0; i<categories_titles.length; i++)
                    resource_categories.put((String)categories_titles[i], categories_ids.get(i));

                ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                        new ArrayList(resource_categories.keySet()));
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setEnabled(true);
                categorySpinner.setAdapter(arrayAdapter);

                int current_category_pos = findPosition(resource_categories.values(), category_id);
                if (current_category_pos != -1)
                    categorySpinner.setSelection(current_category_pos);
            }

            if (savedInstanceState.getSerializable("resources") != null)
            {
                resources = (ArrayList<ResourceModel>)savedInstanceState.getSerializable("resources");
                loadResources(false);
            }
            else
                requestResources();
        }
        else
        {
            if (resource_categories.isEmpty())
            {
                categoriesAutoLoaded = true;
                loadCategories();
            }
            else if (categoriesArrayAdapter != null)
            {
                categorySpinner.setEnabled(true);
                categorySpinner.setAdapter(categoriesArrayAdapter);

                int current_category_pos = findPosition(resource_categories.values(), category_id);
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
                        if (resourcesAvailableEvent == null)
                            requestResources();

                        categoriesAutoLoaded = false;
                        return;
                    }

                    String category = (String)categorySpinner.getSelectedItem();
                    category_id = resource_categories.get(category);
                    requestResources();
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

    private void downloadResource(ResourceModel resourceModel)
    {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            AlertUtil.showConfirmDialog(getContext(), "Permission Required", getString(R.string.storage_permission_alert_message),
                    ()-> requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            FileUtil.PERMISSION_REQUEST_CODE));
        }
        else
        {
            boolean fileExists = FileUtil.resourceFileExists(getContext(), resourceModel.getFile());
            if (fileExists)
            {
                AlertUtil.showConfirmDialog(getContext(), getString(R.string.warning), getString(R.string.resource_exists_warning),
                        () -> startDownloadTask(resourceModel));
            }
            else
                startDownloadTask(resourceModel);
        }
    }

    private void startDownloadTask(ResourceModel resourceModel)
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for previous task to complete!", false);
            return;
        }

        Intent intent = new Intent(getContext(), DownloaderService.class);
        intent.putExtra(DownloaderService.EXTRA_RESOURCE, resourceModel);
        getActivity().startService(intent);
    }

    private void loadCategories()
    {
        if (isLoading())
            return;

        categorySpinner.setEnabled(false);

        toggleLoaderDisplay(true);

        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<ResourceCategoryModel>> call = providersService.getResourceCategories();
        call.enqueue(new Callback<List<ResourceCategoryModel>>()
        {
            @Override
            public void onResponse(Call<List<ResourceCategoryModel>> call, Response<List<ResourceCategoryModel>> response)
            {
                toggleLoaderDisplay(false);

                if (response.isSuccessful())
                {
                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setResourceCategoryModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
                }
                else
                {
                    showSnackMessage("No Resource category found!\n\nThis is an unexpected error, please contact us ASAP", false);
                }
            }

            @Override
            public void onFailure(Call<List<ResourceCategoryModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showReloadCategoriesSnackMessage("A network error occurred!");
            }
        });
    }

    private void toggleLoaderDisplay(boolean visible)
    {
        if (visible)
            resourcesLoaderDisplay.setVisibility(View.VISIBLE);
        else
            resourcesLoaderDisplay.setVisibility(View.GONE);
    }

    private boolean isLoading()
    { return resourcesLoaderDisplay.getVisibility() == View.VISIBLE; }

    private void searchResources(String searchQuery)
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        resourcesList.removeAllViews();
        loadedResources.clear();

        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Resources"));
        appTitle = null;

        try
        {
            ResourcesService resourcesService = RetroServiceGenerator.generateService(ResourcesService.class);
            Call<List<ResourceModel>> requestCall = resourcesService.searchResources(searchQuery, faculty_id, department_id, level_id,
                    course_id, category_id, true);
            requestCall.enqueue(new Callback<List<ResourceModel>>()
            {
                @Override
                public void onResponse(Call<List<ResourceModel>> call, Response<List<ResourceModel>> response)
                {
                    switch (response.code())
                    {
                        case 200:
                            if (getActivity() != null)
                                EventBus.getDefault().post(new ResourcesAvailableEvent(response.body()));
                            break;
                        case 404:
                            toggleLoaderDisplay(false);
                            showSnackMessage("No resources matched!",
                                    false);
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadSearchedResourcesSnackMessage("An error occurred! Please search again.", searchQuery);
                    }
                }

                @Override
                public void onFailure(Call<List<ResourceModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadSearchedResourcesSnackMessage("A network error occurred!", searchQuery);
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadSearchedResourcesSnackMessage("An unknown error occurred! Please search again.", searchQuery);
        }
    }

    private void requestResources()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        resourcesList.removeAllViews();
        loadedResources.clear();
        resourcesAvailableEvent = null;

        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Resources"));
        appTitle = null;

        try
        {
            ResourcesService resourcesService = RetroServiceGenerator.generateService(ResourcesService.class);
            Call<List<ResourceModel>> requestCall = resourcesService.getResources(faculty_id, department_id, level_id,
                    course_id, category_id, false, true);
            requestCall.enqueue(new Callback<List<ResourceModel>>()
            {
                @Override
                public void onResponse(Call<List<ResourceModel>> call, Response<List<ResourceModel>> response)
                {
                    switch (response.code())
                    {
                        case 200:
                            if (getActivity() != null)
                                EventBus.getDefault().post(new ResourcesAvailableEvent(response.body()));
                            break;
                        case 404:
                            toggleLoaderDisplay(false);
                            showSnackMessage("No resources found!",
                                    false);
                            EventBus.getDefault().post(new ResourcesFragment.ResourcesCombinationChangedEvent(
                                    faculty_id, department_id, level_id, category_id, course_id
                            ));
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadResourcesSnackMessage("An error occurred! Please try again.");
                            EventBus.getDefault().post(new ResourcesFragment.ResourcesCombinationChangedEvent(
                                    faculty_id, department_id, level_id, category_id, course_id
                            ));
                    }
                }

                @Override
                public void onFailure(Call<List<ResourceModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadResourcesSnackMessage("A network error occurred!");
                    EventBus.getDefault().post(new ResourcesFragment.ResourcesCombinationChangedEvent(
                            faculty_id, department_id, level_id, category_id, course_id
                    ));
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadResourcesSnackMessage("An unknown error occurred! Please try again.");
            EventBus.getDefault().post(new ResourcesFragment.ResourcesCombinationChangedEvent(
                    faculty_id, department_id, level_id, category_id, course_id
            ));
        }
    }

    private void showError(String message)
    {
        AlertUtil.showAlert(getContext(), "Oops!", message);
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

    private Snackbar showReloadResourcesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            requestResources();
        });
    }

    private Snackbar showReloadSearchedResourcesSnackMessage(String message, String searchQuery)
    {
        return showConnectionErrorSnack(message, ()->
        {
            searchResources(searchQuery);
        });
    }

    private Snackbar showReloadCategoriesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            loadCategories();
        });
    }

    private Snackbar showOpenDownloadedResourceSnackMessage(String message, File resourceFile)
    {
        return showSnackMessage(message, true, "Open",
                () -> startActivity(FileUtil.buildViewFileIntent(getContext(), resourceFile)));
    }

    private void loadResources()
    { loadResources(true); }

    private void loadResources(boolean showLoadedAlert)
    {
        if (resources.isEmpty())
            return;

        loadedResources.clear();
        resourcesList.removeAllViews();

        asyncTask = new AsyncTask<Void, ResourceModel, Void>()
        {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                ResourceModel resource = resources.get(0);
                appTitle = resource.getCourse_code() + "  [" + resource.getCategory() + "s]";
                EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(appTitle));
            }

            @Override
            protected Void doInBackground(Void... objects)
            {
                for (ResourceModel resourceModel : resources)
                {
                    publishProgress(resourceModel);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(ResourceModel... values)
            {
                addResource(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                toggleLoaderDisplay(false);

                if (showLoadedAlert)
                    showSnackMessage("Resources Loaded!", false);

                if (!categorySpinner.isEnabled())
                    loadCategories();
            }
        };
        asyncTask.execute();
    }

    private void reloadList()
    {
        resourcesList.getAdapter().notifyItemInserted(loadedResources.size());
        resourcesList.smoothScrollToPosition(loadedResources.size()-1);
    }

    private void addResource(ResourceModel resourceModel)
    {
        loadedResources.add(resourceModel);
        reloadList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void resourcesAvailable(ResourcesAvailableEvent resourcesAvailableEvent)
    {
        this.resourcesAvailableEvent = resourcesAvailableEvent;
        resources = resourcesAvailableEvent.resourceModels;
        EventBus.getDefault().post(new ResourcesFragment.ResourcesCombinationChangedEvent(
                faculty_id, department_id, level_id, category_id, course_id
        ));
        loadResources();
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

            int facultyId = PreferenceUtil.facultyId(getContext().getApplicationContext());
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

            int levelId = PreferenceUtil.levelId(getContext().getApplicationContext());
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

            int departmentId = PreferenceUtil.departmentId(getContext().getApplicationContext());
            if (departmentId != -1)
            {
                int current_pos = findPosition(departments.values(), departmentId);
                if (current_pos != -1)
                    departmentSpinner.setSelection(current_pos);
            }

            fetchCourses();
        }

        else if (providerAvailableEvent.getCourseModels() != null)
        {
            courses.clear();

            for (CourseModel courseModel : providerAvailableEvent.getCourseModels())
                courses.put(courseModel.getCourse_code(), courseModel.getId());

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                    new ArrayList(courses.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            courseSpinner.setEnabled(true);
            courseSpinner.setAdapter(arrayAdapter);

            int courseId = PreferenceUtil.courseId(getContext().getApplicationContext());
            if (courseId != -1)
            {
                int current_pos = findPosition(courses.values(), courseId);
                if (current_pos != -1)
                    courseSpinner.setSelection(current_pos);
            }

            loaderDisplay.setVisibility(View.GONE);
        }

        else if (providerAvailableEvent.getResourceCategoryModels() != null)
        {
            resource_categories.clear();

            for (ResourceCategoryModel resourceCategoryModel : providerAvailableEvent.getResourceCategoryModels())
                resource_categories.put(resourceCategoryModel.getCategory() + "s", resourceCategoryModel.getId());

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                    new ArrayList(resource_categories.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setEnabled(true);
            categorySpinner.setAdapter(arrayAdapter);

            int current_category_pos = findPosition(resource_categories.values(), category_id);
            if (current_category_pos != -1)
                categorySpinner.setSelection(current_category_pos);
        }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }

    private class ResourcesViewHolder extends RecyclerView.ViewHolder
    {
        private ImageView cardImg;
        private TextView cardTitle;
        private TextView cardDesc;
        private TextView cardDate;
        private MaterialButton btnView;
        private MaterialButton btnDownload;

        public ResourcesViewHolder(@NonNull View itemView)
        {
            super(itemView);

            cardImg = itemView.findViewById(R.id.resource_card_img);
            cardTitle = itemView.findViewById(R.id.resource_card_title);
            cardDesc = itemView.findViewById(R.id.resource_card_description);
            cardDate = itemView.findViewById(R.id.resource_card_date);

            btnView = itemView.findViewById(R.id.btn_view_resource);
            btnDownload = itemView.findViewById(R.id.btn_download_resource);
        }

        public void bindView (ResourceModel resourceModel, Activity activity)
        {
            String title = resourceModel.getTitle();
            String desc = resourceModel.getDescription();
            String date = resourceModel.getDate_added();

            int imageDrawableRes = R.drawable.videos;
            String category = resourceModel.getCategory();
            if (TextUtils.equals("Material", category))
                imageDrawableRes = R.drawable.materials;
            else if (TextUtils.equals("Textbook", category))
                imageDrawableRes = R.drawable.textbooks;
            else if (TextUtils.equals("Document", category))
                imageDrawableRes = R.drawable.documents;

            cardTitle.setText(title);
            cardDesc.setText(desc);
            cardDate.setText(date);
            cardImg.setImageDrawable(activity.getResources().getDrawable(imageDrawableRes));

            btnView.setOnClickListener(v ->
            {
                Intent intent = new Intent(activity, ViewResourceActivity.class);
                intent.putExtra("resource", resourceModel);
                activity.startActivity(intent);
            });

            btnDownload.setOnClickListener(v ->
            {
                downloadResource(resourceModel);
            });
        }
    }

    private class ResourcesAdapter extends RecyclerView.Adapter<ResourcesViewHolder>
    {

        @NonNull
        @Override
        public ResourcesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new ResourcesViewHolder(getLayoutInflater().inflate(R.layout.resource_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ResourcesViewHolder holder, int position)
        {
            ResourceModel resourceModel = loadedResources.get(position);
            holder.bindView(resourceModel, getActivity());
        }

        @Override
        public int getItemCount()
        {
            return loadedResources.size();
        }
    }
}































