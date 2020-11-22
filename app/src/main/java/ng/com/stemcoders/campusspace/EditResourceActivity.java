package ng.com.stemcoders.campusspace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class EditResourceActivity extends AppCompatActivity
{
    private ProgressBar loaderDisplay;
    private Spinner courseSpinner;
    private TextInputEditText nameInput, descriptionInput;
    private Button btnUpdate;

    private ResourceModel resourceModel = null;

    private Map<String, Integer> courses = new TreeMap<>();

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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_resource);

        loaderDisplay = findViewById(R.id.loader_display);
        courseSpinner = findViewById(R.id.course_spinner);
        courseSpinner.setEnabled(false);
        nameInput = findViewById(R.id.resource_name_input);
        descriptionInput = findViewById(R.id.resource_description_input);
        btnUpdate = findViewById(R.id.btn_update_resource);
        btnUpdate.setOnClickListener(v -> updateResource());

        Intent intent = getIntent();

        if (savedInstanceState == null)
        {
            resourceModel = (ResourceModel)intent.getExtras().getSerializable("resource");
            nameInput.setText(resourceModel.getTitle());
            descriptionInput.setText(resourceModel.getDescription());
            fetchCourses();
        }
        else
            resourceModel = (ResourceModel)savedInstanceState.getSerializable("resource");

        if (resourceModel == null)
            finish();

        getSupportActionBar().setTitle(resourceModel.getTitle());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (courseSpinner != null && courseSpinner.isEnabled() && !courses.isEmpty())
        {
            outState.putBoolean("courses_available", true);
            outState.putInt("selected_course_pos", findPosition(courses.keySet(),
                    (String)courseSpinner.getSelectedItem()));
            outState.putSerializable("courses_titles", courses.keySet().toArray());
            outState.putIntegerArrayList("courses_ids", new ArrayList<>(courses.values()));
        }

        outState.putSerializable("resource", resourceModel);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.getBoolean("courses_available", false))
        {
            Object[] courses_titles = (Object[])savedInstanceState.getSerializable("courses_titles");
            ArrayList<Integer> courses_ids = savedInstanceState.getIntegerArrayList("courses_ids");

            courses.clear();
            for (int i=0; i<courses_titles.length; i++)
                courses.put((String)courses_titles[i], courses_ids.get(i));

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
                    new ArrayList(courses.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            courseSpinner.setEnabled(true);
            courseSpinner.setAdapter(arrayAdapter);

            int current_category_pos = savedInstanceState.getInt("selected_course_pos");
            if (current_category_pos != -1)
                courseSpinner.setSelection(current_category_pos);
        }
        else
            fetchCourses();
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

    private void fetchCourses()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);
        courseSpinner.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<List<CourseModel>> call = moderatorService.getModeratorCourses(true);
        call.enqueue(new Callback<List<CourseModel>>()
        {
            @Override
            public void onResponse(Call<List<CourseModel>> call, Response<List<CourseModel>> response)
            {
                toggleLoaderDisplay(false);

                if (response.isSuccessful())
                {
                    coursesAvailable(response.body());
                }
                else
                {
                    showError("No courses that you own was found!");
                }
            }

            @Override
            public void onFailure(Call<List<CourseModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showReloadCoursesSnackMessage("A network error occurred!");
            }
        });
    }

    private void coursesAvailable(List<CourseModel> courseModels)
    {
        courses.clear();

        for (CourseModel courseModel : courseModels)
            courses.put(courseModel.getCourse_code(), courseModel.getId());

        ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
                new ArrayList(courses.keySet()));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setEnabled(true);
        courseSpinner.setAdapter(arrayAdapter);

        int selected = findPosition(courses.values(), resourceModel.getCourse_id());
        courseSpinner.setSelection(selected);

        toggleLoaderDisplay(false);
    }

    private void updateResource()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the current task to complete!", false);
            return;
        }

        String resourceName = nameInput.getText().toString();
        String resourceDesc = descriptionInput.getText().toString();
        int courseId = courses.get((String)courseSpinner.getSelectedItem());

        if (TextUtils.isEmpty(resourceName) || TextUtils.isEmpty(resourceDesc))
        {
            showSnackMessage("Please fill out all the fields!", false);
            return;
        }

        toggleLoaderDisplay(true);
        btnUpdate.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<Void> call = moderatorService.updateResource(resourceModel.getId(), resourceName, resourceDesc, courseId);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);
                btnUpdate.setEnabled(true);

                if (response.isSuccessful())
                {
                    showSnackMessage("Resource updated!", false);
                    EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(resourceName));
                }
                else
                {
                    showError("This resource name already exists!\nPlease choose another one");
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
                toggleLoaderDisplay(true);
                btnUpdate.setEnabled(false);
                showSnackMessage("A network error occurred!", false);
            }
        });
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

    private Snackbar showReloadCoursesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            fetchCourses();
        });
    }

    private void showError(String message)
    {
        AlertUtil.showAlert(this, "Oops!", message);
    }

    @Override
    public void onBackPressed()
    {
        AlertUtil.showConfirmDialog(this, "Exit?", "Do you really want to exit now?\n\n" +
                "All unsaved data will be lost!", super::onBackPressed);
    }
}

























