package ng.com.stemcoders.campusspace.fragments.moderation;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileUtils;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCategoryModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class AddResource extends Fragment
{
    private ProgressBar loaderDisplay;
    private TextView selectedFileDisplay;
    private Spinner categorySpinner, courseSpinner;
    private Button btnBrowseFiles;
    private TextInputEditText nameInput, descriptionInput;
    private Button btnUpload;

    private Map<String, Integer> categories = new TreeMap<>();
    private ArrayAdapter<List<String>> categoriesArrayAdapter;

    private Map<String, Integer> courses = new TreeMap<>();
    private ArrayAdapter<List<String>> coursesArrayAdapter;

    private Uri selectedFileUri;

    private static final int BROWSE_FILES_REQUEST_CODE = 1004;

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
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Add Resource"));
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);

        if (!categories.isEmpty())
            categoriesArrayAdapter = (ArrayAdapter<List<String>>)categorySpinner.getAdapter();
        else
            categoriesArrayAdapter = null;

        if (!courses.isEmpty())
            coursesArrayAdapter = (ArrayAdapter<List<String>>)courseSpinner.getAdapter();
        else
            coursesArrayAdapter = null;

        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.moderation_add_resource, container, false);

        loaderDisplay = root.findViewById(R.id.add_resource_loader_display);
        selectedFileDisplay = root.findViewById(R.id.file_selected_display);
        categorySpinner = root.findViewById(R.id.category_spinner);
        categorySpinner.setEnabled(false);

        courseSpinner = root.findViewById(R.id.course_spinner);
        courseSpinner.setEnabled(false);

        btnBrowseFiles = root.findViewById(R.id.btn_browse_files);
        btnBrowseFiles.setOnClickListener(v -> browseFiles());

        nameInput = root.findViewById(R.id.resource_name_input);
        descriptionInput = root.findViewById(R.id.resource_description_input);
        btnUpload = root.findViewById(R.id.btn_upload_resource);
        btnUpload.setOnClickListener(v -> uploadResource());

        return root;
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

    private void browseFiles()
    {
        if (! categorySpinner.isEnabled())
        {
            showSnackMessage("No category selected!", false);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(getRequiredMIME());
        startActivityForResult(intent, BROWSE_FILES_REQUEST_CODE);
    }

    private void uploadResource()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the current task to finish first!", false);
            return;
        }

        if (!categorySpinner.isEnabled() && !courseSpinner.isEnabled())
        {
            showReloadCategoriesSnackMessage("No category or course selected!");
            return;
        }

        if (TextUtils.isEmpty(nameInput.getText().toString()) || TextUtils.isEmpty(descriptionInput.getText().toString()))
        {
            showSnackMessage("Please fill out the fields!", false);
            return;
        }

        InputStream inputStream = null;

        if (selectedFileUri == null)
        {
            showSnackMessage("Please choose a file first!", false);
            return;
        }
        else
        {
            try
            {
                inputStream = getActivity().getContentResolver().openInputStream(selectedFileUri);
            } catch (FileNotFoundException e)
            {
                Timber.e(e,"Selected Resource File not found!");
                showSnackMessage("The selected file is missing!", false);
                return;
            }
        }

        toggleLoaderDisplay(true);
        btnUpload.setEnabled(false);

        AsyncTask<InputStream, Void, byte[]> asyncTask = new AsyncTask<InputStream, Void, byte[]>()
        {
            @Override
            protected byte[] doInBackground(InputStream... streams)
            {
                try
                {
                    byte[] data = new byte[streams[0].available()];
                    streams[0].read(data);
                    return data;
                } catch (Exception e)
                {
                    Timber.e(e, "Resource file processing failed!");
                    return null;
                }
            }

            @Override
            protected void onPostExecute(byte[] bytes)
            {
                if (bytes == null)
                {
                    showSnackMessage("File processing failed!", false);
                    toggleLoaderDisplay(false);
                    btnUpload.setEnabled(true);
                }
                else
                    completeUploadTask(bytes);
            }
        };
        asyncTask.execute(inputStream);
    }

    private void completeUploadTask(byte[] data)
    {
        String title = nameInput.getText().toString();
        String description = descriptionInput.getText().toString();
        int categoryId = categories.get(categorySpinner.getSelectedItem());
        int courseId = courses.get(courseSpinner.getSelectedItem());

        RequestBody requestFile = RequestBody.create(MediaType.parse(getActivity().getContentResolver().getType(selectedFileUri)),
                data);
        MultipartBody.Part file = MultipartBody.Part.createFormData("file", "resource_file", requestFile);
        RequestBody titleBody = RequestBody.create(MultipartBody.FORM, title);
        RequestBody descBody = RequestBody.create(MultipartBody.FORM, description);
        RequestBody categoryBody = RequestBody.create(MultipartBody.FORM, String.valueOf(categoryId));
        RequestBody courseBody = RequestBody.create(MultipartBody.FORM, String.valueOf(courseId));

        String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<Void> call = moderatorService.addResource(file, titleBody, descBody, categoryBody, courseBody);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);
                btnUpload.setEnabled(true);

                if (response.isSuccessful())
                {
                    showSnackMessage("Resource uploaded!", false);
                    nameInput.setText("");
                    descriptionInput.setText("");
                    selectedFileDisplay.setText(getString(R.string.no_file_selected));
                }
                else
                {
                    try
                    {
                        showError("An error occurred while attempting to upload the resource!\n\nPlease try again:\n\n" +
                                response.errorBody().string());
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
                btnUpload.setEnabled(true);
                showSnackMessage("A network error occurred!", false);
                Timber.e(t, "Resource upload failed");
            }
        });
    }

    private String getRequiredMIME()
    {
        String mime = "*/*";

        switch ((String)categorySpinner.getSelectedItem())
        {
            case "Video":
                mime = "video/*";
                break;
            case "Textbook":
            case "Material":
                mime = "application/pdf";
                break;
            case "Document":
                mime = "*/*";
                break;
        }

        return mime;
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

    private Snackbar showReloadCategoriesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            loadCategories();
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

        if (categorySpinner != null && categorySpinner.isEnabled() && !categories.isEmpty())
        {
            outState.putBoolean("categories_available", true);
            outState.putInt("selected_category_pos", findPosition(categories.keySet(),
                    (String)categorySpinner.getSelectedItem()));
            outState.putSerializable("categories_titles", categories.keySet().toArray());
            outState.putIntegerArrayList("categories_ids", new ArrayList<>(categories.values()));
        }

        if (courseSpinner != null && courseSpinner.isEnabled() && !courses.isEmpty())
        {
            outState.putBoolean("courses_available", true);
            outState.putInt("selected_course_pos", findPosition(courses.keySet(),
                    (String)courseSpinner.getSelectedItem()));
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

            if (savedInstanceState.getBoolean("courses_available", false))
            {
                Object[] courses_titles = (Object[])savedInstanceState.getSerializable("courses_titles");
                ArrayList<Integer> courses_ids = savedInstanceState.getIntegerArrayList("courses_ids");

                courses.clear();
                for (int i=0; i<courses_titles.length; i++)
                    courses.put((String)courses_titles[i], courses_ids.get(i));

                ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                        new ArrayList(courses.keySet()));
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                courseSpinner.setEnabled(true);
                courseSpinner.setAdapter(arrayAdapter);

                int current_category_pos = savedInstanceState.getInt("selected_course_pos");
                if (current_category_pos != -1)
                    courseSpinner.setSelection(current_category_pos);
            }
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

                if (coursesArrayAdapter != null)
                {
                    courseSpinner.setEnabled(true);
                    courseSpinner.setAdapter(coursesArrayAdapter);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (requestCode == BROWSE_FILES_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            if (data != null)
            {
                selectedFileUri = data.getData();
                selectedFileDisplay.setText("File Selected: " + selectedFileUri.getPath());
            }
        }
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
                    categoriesAvailable(response.body());
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

    private void fetchCourses()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);
        courseSpinner.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

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
                showSnackMessage("A network error occurred!", false);
            }
        });
    }

    private void categoriesAvailable(List<ResourceCategoryModel> resourceCategoryModels)
    {
        categories.clear();

        for (ResourceCategoryModel resourceCategoryModel : resourceCategoryModels)
            categories.put(resourceCategoryModel.getCategory(), resourceCategoryModel.getId());

        ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                new ArrayList(categories.keySet()));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setEnabled(true);
        categorySpinner.setAdapter(arrayAdapter);

        fetchCourses();
    }

    private void coursesAvailable(List<CourseModel> courseModels)
    {
        courses.clear();

        for (CourseModel courseModel : courseModels)
            courses.put(courseModel.getCourse_code(), courseModel.getId());

        ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                new ArrayList(courses.keySet()));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setEnabled(true);
        courseSpinner.setAdapter(arrayAdapter);
        toggleLoaderDisplay(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        AlertUtil.showConfirmDialog(getContext(), "Exit?",
                "Do you really want to exit now?\n\nAll unsaved data will be lost!", () ->
                {
                    toggleLoaderDisplay(false);
                    EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(Home.class));
                });
    }
}























