package ng.com.stemcoders.campusspace.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

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
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.ProviderAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.DepartmentModel;
import ng.com.stemcoders.campusspace.net.models.FacultyModel;
import ng.com.stemcoders.campusspace.net.models.LevelModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCategoryModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferencesFragment extends Fragment
{
    private ProgressBar loaderDisplay;
    private Spinner facultySpinner, departmentSpinner, levelSpinner, courseSpinner;
    private TextInputEditText displayNameInput;
    private Button btnSavePrefs;

    private Map<String, Integer> faculties = new TreeMap<>();
    private Map<String, Integer> departments = new TreeMap<>();
    private Map<String, Integer> levels = new TreeMap<>();
    private Map<String, Integer> courses = new TreeMap<>();

    private ArrayAdapter<List<String>> facultiesArrayAdapter, departmentsArrayAdapter, levelsArrayAdapter, coursesArrayAdapter;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.fragment_preferences, container, false);

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.prefs_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);
            fetchFaculties();
        });

        loaderDisplay = root.findViewById(R.id.loader_display);
        facultySpinner = root.findViewById(R.id.faculty_spinner);
        facultySpinner.setEnabled(false);
        departmentSpinner = root.findViewById(R.id.department_spinner);
        departmentSpinner.setEnabled(false);
        levelSpinner = root.findViewById(R.id.level_spinner);
        levelSpinner.setEnabled(false);
        courseSpinner = root.findViewById(R.id.course_spinner);
        courseSpinner.setEnabled(false);
        displayNameInput = root.findViewById(R.id.display_name_input);
        btnSavePrefs = root.findViewById(R.id.btn_save_prefs);
        btnSavePrefs.setOnClickListener(v -> savePrefs());

        return root;
    }

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

        if (!faculties.isEmpty())
            facultiesArrayAdapter = (ArrayAdapter<List<String>>)facultySpinner.getAdapter();
        else
            facultiesArrayAdapter = null;

        if (!departments.isEmpty())
            departmentsArrayAdapter = (ArrayAdapter<List<String>>)departmentSpinner.getAdapter();
        else
            departmentsArrayAdapter = null;

        if (!levels.isEmpty())
            levelsArrayAdapter = (ArrayAdapter<List<String>>)levelSpinner.getAdapter();
        else
            levelsArrayAdapter = null;

        if (!courses.isEmpty())
            coursesArrayAdapter = (ArrayAdapter<List<String>>)courseSpinner.getAdapter();
        else
            coursesArrayAdapter = null;

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (facultySpinner != null && facultySpinner.isEnabled() && !faculties.isEmpty())
        {
            outState.putBoolean("faculties_available", true);
            outState.putInt("current_faculty", faculties.get(facultySpinner.getSelectedItem()));
            outState.putSerializable("faculties_titles", faculties.keySet().toArray());
            outState.putIntegerArrayList("faculties_ids", new ArrayList<>(faculties.values()));
        }

        if (departmentSpinner != null && departmentSpinner.isEnabled() && !departments.isEmpty())
        {
            outState.putBoolean("departments_available", true);
            outState.putInt("current_department", departments.get(departmentSpinner.getSelectedItem()));
            outState.putSerializable("departments_titles", departments.keySet().toArray());
            outState.putIntegerArrayList("departments_ids", new ArrayList<>(departments.values()));
        }

        if (levelSpinner != null && levelSpinner.isEnabled() && !levels.isEmpty())
        {
            outState.putBoolean("levels_available", true);
            outState.putInt("current_level", levels.get(levelSpinner.getSelectedItem()));
            outState.putSerializable("levels_titles", levels.keySet().toArray());
            outState.putIntegerArrayList("levels_ids", new ArrayList<>(levels.values()));
        }

        if (courseSpinner != null && courseSpinner.isEnabled() && !courses.isEmpty())
        {
            outState.putBoolean("courses_available", true);
            outState.putInt("current_course", courses.get(courseSpinner.getSelectedItem()));
            outState.putSerializable("courses_titles", courses.keySet().toArray());
            outState.putIntegerArrayList("courses_ids", new ArrayList<>(courses.values()));
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null)
        {
            if (savedInstanceState.getBoolean("faculties_available", false))
            {
                Object[] titles = (Object[])savedInstanceState.getSerializable("faculties_titles");
                ArrayList<Integer> ids = savedInstanceState.getIntegerArrayList("faculties_ids");

                faculties.clear();
                for (int i=0; i<titles.length; i++)
                    faculties.put((String)titles[i], ids.get(i));

                ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                        new ArrayList(faculties.keySet()));
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                facultySpinner.setEnabled(true);
                facultySpinner.setAdapter(arrayAdapter);

                int current_pos = findPosition(faculties.values(),
                        savedInstanceState.getInt("current_faculty"));
                if (current_pos != -1)
                    facultySpinner.setSelection(current_pos);

                if (savedInstanceState.getBoolean("levels_available", false))
                {
                    Object[] levels_titles = (Object[])savedInstanceState.getSerializable("levels_titles");
                    ArrayList<Integer> levels_ids = savedInstanceState.getIntegerArrayList("levels_ids");

                    levels.clear();
                    for (int i=0; i<levels_titles.length; i++)
                        levels.put((String)levels_titles[i], levels_ids.get(i));

                    ArrayAdapter<List<String>> levelsArrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                            new ArrayList(levels.keySet()));
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    levelSpinner.setEnabled(true);
                    levelSpinner.setAdapter(levelsArrayAdapter);

                    current_pos = findPosition(levels.values(),
                            savedInstanceState.getInt("current_level"));
                    if (current_pos != -1)
                        levelSpinner.setSelection(current_pos);

                    if (savedInstanceState.getBoolean("departments_available", false))
                    {
                        Object[] departments_titles = (Object[])savedInstanceState.getSerializable("departments_titles");
                        ArrayList<Integer> departments_ids = savedInstanceState.getIntegerArrayList("departments_ids");

                        departments.clear();
                        for (int i=0; i<departments_titles.length; i++)
                            departments.put((String)departments_titles[i], departments_ids.get(i));

                        ArrayAdapter<List<String>> departmentsArrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                                new ArrayList(departments.keySet()));
                        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        departmentSpinner.setEnabled(true);
                        departmentSpinner.setAdapter(departmentsArrayAdapter);

                        current_pos = findPosition(departments.values(),
                                savedInstanceState.getInt("current_department"));
                        if (current_pos != -1)
                            departmentSpinner.setSelection(current_pos);

                        if (savedInstanceState.getBoolean("courses_available", false))
                        {
                            Object[] courses_titles = (Object[])savedInstanceState.getSerializable("courses_titles");
                            ArrayList<Integer> courses_ids = savedInstanceState.getIntegerArrayList("courses_ids");

                            courses.clear();
                            for (int i=0; i<courses_titles.length; i++)
                                courses.put((String)courses_titles[i], courses_ids.get(i));

                            ArrayAdapter<List<String>> coursesArrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                                    new ArrayList(courses.keySet()));
                            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            courseSpinner.setEnabled(true);
                            courseSpinner.setAdapter(coursesArrayAdapter);

                            current_pos = findPosition(courses.values(),
                                    savedInstanceState.getInt("current_course"));
                            if (current_pos != -1)
                                courseSpinner.setSelection(current_pos);
                        }
                        else
                            fetchCourses();
                    }
                    else
                        fetchDepartments();
                }
                else
                    fetchLevels();
            }
            else
                fetchFaculties();
        }
        else
        {
            if (faculties.isEmpty())
            {
                fetchFaculties();
            }
            else if (facultiesArrayAdapter != null)
            {
                facultySpinner.setEnabled(true);
                facultySpinner.setAdapter(facultiesArrayAdapter);

                int facultyId = PreferenceUtil.facultyId(requireContext().getApplicationContext());
                if (facultyId != -1)
                {
                    int current_pos = findPosition(faculties.values(), facultyId);
                    if (current_pos != -1)
                        facultySpinner.setSelection(current_pos);
                }

                if (!levels.isEmpty() && levelsArrayAdapter != null)
                {
                    levelSpinner.setEnabled(true);
                    levelSpinner.setAdapter(levelsArrayAdapter);

                    int levelId = PreferenceUtil.levelId(requireContext().getApplicationContext());
                    if (levelId != -1)
                    {
                        int current_pos = findPosition(levels.values(), levelId);
                        if (current_pos != -1)
                            levelSpinner.setSelection(current_pos);
                    }

                    if (!departments.isEmpty() && departmentsArrayAdapter != null)
                    {
                        departmentSpinner.setEnabled(true);
                        departmentSpinner.setAdapter(departmentsArrayAdapter);

                        int departmentId = PreferenceUtil.departmentId(requireContext().getApplicationContext());
                        if (departmentId != -1)
                        {
                            int current_pos = findPosition(departments.values(), departmentId);
                            if (current_pos != -1)
                                departmentSpinner.setSelection(current_pos);
                        }

                        if (!courses.isEmpty() && coursesArrayAdapter != null)
                        {
                            courseSpinner.setEnabled(true);
                            courseSpinner.setAdapter(coursesArrayAdapter);
                        }
                        else
                            fetchCourses();
                    }
                    else
                        fetchDepartments();
                }
                else
                    fetchLevels();
            }

            displayNameInput.setText(PreferenceUtil.displayName(requireContext().getApplicationContext()));
        }

        facultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (!isLoading() && facultySpinner.isEnabled())
                {
                    toggleLoaderDisplay(true);
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
                if (!isLoading() && levelSpinner.isEnabled())
                {
                    toggleLoaderDisplay(true);
                    fetchCourses();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    private void fetchFaculties()
    {
        facultySpinner.setEnabled(false);
        departmentSpinner.setEnabled(false);
        levelSpinner.setEnabled(false);
        courseSpinner.setEnabled(false);
        loaderDisplay.setVisibility(View.VISIBLE);

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
                    toggleLoaderDisplay(false);
                    showError("No faculties found! Please contact us for more details!");
                }
            }

            @Override
            public void onFailure(Call<List<FacultyModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showError("A network error occurred!");
            }
        });
    }

    private void fetchLevels()
    {
        if (!facultySpinner.isEnabled())
            return;

        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<LevelModel>>  levelModelCall = providersService.getLevels();
        levelModelCall.enqueue(new Callback<List<LevelModel>>()
        {
            @Override
            public void onResponse(Call<List<LevelModel>> call, Response<List<LevelModel>> response)
            {
                if (response.isSuccessful())
                {
                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setLevelModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
                }
                else
                {
                    toggleLoaderDisplay(false);
                    showError("No levels found! Please contact us for more details!");
                }
            }

            @Override
            public void onFailure(Call<List<LevelModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showSnackMessage("A network error occurred!", false);
            }
        });
    }

    private void fetchDepartments()
    {
        if (!facultySpinner.isEnabled())
            return;

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
                    ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                    providerAvailableEvent.setDepartmentModels(response.body());
                    EventBus.getDefault().post(providerAvailableEvent);
                }
                else
                {
                    toggleLoaderDisplay(false);
                    departmentSpinner.setEnabled(false);
                    showError("No departments found for the selected faculty! Please choose another faculty!");
                }
            }

            @Override
            public void onFailure(Call<List<DepartmentModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                departmentSpinner.setEnabled(false);
                showSnackMessage("A network error occurred!", false);
            }
        });
    }

    private void fetchCourses()
    {
        if (!departmentSpinner.isEnabled())
            return;

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

    private void savePrefs()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the current task to complete!", false);
            return;
        }

        if (!facultySpinner.isEnabled() || !departmentSpinner.isEnabled() || !levelSpinner.isEnabled() ||
                !courseSpinner.isEnabled())
        {
            showSnackMessage("Please make sure all fields are selected!", false);
            return;
        }

        String displayName = displayNameInput.getText().toString();

        if (TextUtils.isEmpty(displayName))
        {
            showSnackMessage("Please enter a display name!", false);
            return;
        }

        int facultyId = faculties.get(facultySpinner.getSelectedItem());
        int departmentId = departments.get(departmentSpinner.getSelectedItem());
        int levelId = levels.get(levelSpinner.getSelectedItem());
        int courseId = courses.get(courseSpinner.getSelectedItem());

        PreferenceUtil.setUserData(requireContext().getApplicationContext(), displayName, facultyId, departmentId, levelId, courseId);
        showSnackMessage("Preferences updated!", false);
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

    private void toggleLoaderDisplay(boolean visible)
    {
        if (visible)
            loaderDisplay.setVisibility(View.VISIBLE);
        else
            loaderDisplay.setVisibility(View.GONE);
    }

    private boolean isLoading()
    { return loaderDisplay.getVisibility() == View.VISIBLE; }

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
                        ProviderAvailableEvent providerAvailableEvent = new ProviderAvailableEvent();
                        providerAvailableEvent.setLevelModels(response.body());
                        EventBus.getDefault().post(providerAvailableEvent);
                    }
                    else
                    {
                        showError("No levels found! Please contact us for more details!");
                    }
                }

                @Override
                public void onFailure(Call<List<LevelModel>> call, Throwable t)
                {
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

            int courseId = PreferenceUtil.courseId(requireContext().getApplicationContext());
            if (courseId != -1)
            {
                int current_pos = findPosition(courses.values(), courseId);
                if (current_pos != -1)
                    courseSpinner.setSelection(current_pos);
            }

            toggleLoaderDisplay(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }
}



































