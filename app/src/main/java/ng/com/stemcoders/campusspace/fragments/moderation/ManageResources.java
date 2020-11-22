package ng.com.stemcoders.campusspace.fragments.moderation;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
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

import ng.com.stemcoders.campusspace.EditResourceActivity;
import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.ResourcesAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCategoryModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class ManageResources extends Fragment
{
    private ProgressBar loaderDisplay;
    private Spinner categorySpinner, courseSpinner;
    private Button btnFilter;
    private RecyclerView resourcesList;

    private List<ResourceModel> resources = new ArrayList<>();
    private List<ResourceModel> loadedResources = new ArrayList<>();
    private ResourcesAvailableEvent resourcesAvailableEvent;

    private Map<String, Integer> categories = new TreeMap<>();
    private ArrayAdapter<List<String>> categoriesArrayAdapter;

    private Map<String, Integer> courses = new TreeMap<>();
    private ArrayAdapter<List<String>> coursesArrayAdapter;

    private int category_id = 2;
    private int course_id = 2;

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
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Manage Resources"));

        if (resourcesAvailableEvent == null)
            requestResources();
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
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout)inflater.inflate(R.layout.moderation_manage_resources, container, false);

        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);

            if (categorySpinner.isEnabled())
                requestResources();
            else
                showReloadCategoriesSnackMessage("Please reload the categories!");
        });

        loaderDisplay = swipeRefreshLayout.findViewById(R.id.loader_display);
        categorySpinner = swipeRefreshLayout.findViewById(R.id.category_spinner);
        categorySpinner.setEnabled(false);
        courseSpinner = swipeRefreshLayout.findViewById(R.id.course_spinner);
        courseSpinner.setEnabled(false);
        btnFilter = swipeRefreshLayout.findViewById(R.id.btn_filter);
        btnFilter.setOnClickListener(v -> filterResources());

        resourcesList = swipeRefreshLayout.findViewById(R.id.items_list_view);
        resourcesList.setItemAnimator(new DefaultItemAnimator());
        resourcesList.setAdapter(new ResourcesAdapter());
        resourcesList.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.recycler_grid_display_count),
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

    private Snackbar showReloadResourcesSnackMessage(String message)
    {
        return showConnectionErrorSnack(message, ()->
        {
            requestResources();
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

        if (!resources.isEmpty())
            outState.putSerializable("resources", (ArrayList<ResourceModel>)resources);

        outState.putInt("category_id", category_id);
        outState.putInt("course_id", course_id);

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
            category_id = savedInstanceState.getInt("category_id");
            course_id = savedInstanceState.getInt("course_id");

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

    private void requestResources()
    {
        if (isLoading())
            return;

        toggleLoaderDisplay(true);

        resourcesList.removeAllViews();
        loadedResources.clear();
        resourcesAvailableEvent = null;

        try
        {
            String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
            String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

            ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
            Call<List<ResourceModel>> requestCall = moderatorService.getModeratorResources(course_id, category_id, true);
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
                            break;
                        default:
                            toggleLoaderDisplay(false);
                            showReloadResourcesSnackMessage("An error occurred! Please try again.");
                    }
                }

                @Override
                public void onFailure(Call<List<ResourceModel>> call, Throwable t)
                {
                    toggleLoaderDisplay(false);
                    showReloadResourcesSnackMessage("A network error occurred!");
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
            toggleLoaderDisplay(false);
            showReloadResourcesSnackMessage("An unknown error occurred! Please try again.");
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

    private void filterResources()
    {
        if (!categorySpinner.isEnabled() || !courseSpinner.isEnabled())
        {
            showReloadCategoriesSnackMessage("Please reload the categories and courses!");
            return;
        }

        course_id = courses.get(courseSpinner.getSelectedItem());
        category_id = categories.get(categorySpinner.getSelectedItem());
        requestResources();
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
        category_id = categories.get(categorySpinner.getSelectedItem());

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
        course_id = courses.get(courseSpinner.getSelectedItem());

        toggleLoaderDisplay(false);
        requestResources();
    }

    private void loadResources()
    { loadResources(true); }

    private void loadResources(boolean showLoadedAlert)
    {
        if (resources.isEmpty())
            return;

        loadedResources.clear();
        resourcesList.removeAllViews();

        AsyncTask<Void, ResourceModel, Void> asyncTask = new AsyncTask<Void, ResourceModel, Void>()
        {
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

    private void reloadListAfterRemoval(int position)
    {
        resourcesList.getAdapter().notifyItemRemoved(position);
        if (!loadedResources.isEmpty())
            resourcesList.smoothScrollToPosition(loadedResources.size()-1);
    }

    private void addResource(ResourceModel resourceModel)
    {
        loadedResources.add(resourceModel);
        reloadList();
    }

    private void removeResource(int position)
    {
        loadedResources.remove(position);
        reloadListAfterRemoval(position);
    }

    private void deleteResource(int resourceId, int position)
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
        Call<Void> call = moderatorService.removeResource(resourceId);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);

                if (response.isSuccessful())
                {
                    removeResource(position);
                    showSnackMessage("Resource deleted!", false);
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
    public void resourcesAvailable(ResourcesAvailableEvent resourcesAvailableEvent)
    {
        this.resourcesAvailableEvent = resourcesAvailableEvent;
        resources = resourcesAvailableEvent.resourceModels;
        loadResources();
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

    private class ResourcesViewHolder extends RecyclerView.ViewHolder
    {
        private ImageView cardImg;
        private TextView cardTitle;
        private TextView cardDesc;
        private TextView cardDate;
        private MaterialButton btnEdit;
        private MaterialButton btnRemove;

        public ResourcesViewHolder(@NonNull View itemView)
        {
            super(itemView);

            cardImg = itemView.findViewById(R.id.resource_card_img);
            cardTitle = itemView.findViewById(R.id.resource_card_title);
            cardDesc = itemView.findViewById(R.id.resource_card_description);
            cardDate = itemView.findViewById(R.id.resource_card_date);

            btnEdit = itemView.findViewById(R.id.btn_edit_resource);
            btnRemove = itemView.findViewById(R.id.btn_remove_resource);
        }

        public void bindView (ResourceModel resourceModel, Activity activity, int position)
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

            btnEdit.setOnClickListener(v ->
            {
                Intent intent = new Intent(activity, EditResourceActivity.class);
                intent.putExtra("resource", resourceModel);
                activity.startActivity(intent);
            });

            btnRemove.setOnClickListener(v ->
            {
                AlertUtil.showConfirmDialog(activity, "Delete Resource?",
                        "Do you really want to permanently remove this resource now?\n\n" +
                                "Please note, this can not be reversed!",
                        () -> deleteResource(resourceModel.getId(), position));
            });
        }
    }

    private class ResourcesAdapter extends RecyclerView.Adapter<ResourcesViewHolder>
    {

        @NonNull
        @Override
        public ResourcesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new ResourcesViewHolder(getLayoutInflater().inflate(R.layout.manage_resource_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ResourcesViewHolder holder, int position)
        {
            ResourceModel resourceModel = loadedResources.get(position);
            holder.bindView(resourceModel, getActivity(), position);
        }

        @Override
        public int getItemCount()
        {
            return loadedResources.size();
        }
    }
}

































