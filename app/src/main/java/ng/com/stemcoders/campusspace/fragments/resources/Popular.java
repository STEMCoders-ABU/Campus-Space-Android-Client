package ng.com.stemcoders.campusspace.fragments.resources;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ng.com.stemcoders.campusspace.DownloaderService;
import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.ViewResourceActivity;
import ng.com.stemcoders.campusspace.fragments.ResourcesFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.events.ResourcesAvailableEvent;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ResourcesService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.FileUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class Popular extends Fragment
{
    SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView resourcesList;
    private List<ResourceModel> resources = new ArrayList<>();
    private List<ResourceModel> loadedResources = new ArrayList<>();
    private ResourcesAvailableEvent resourcesAvailableEvent;

    private int faculty_id = -1;
    private int department_id = -1;
    private int level_id = -1;
    private int category_id = 2;
    private int course_id = -1;

    private ProgressBar resourcesLoaderDisplay;

    private ResourcesFragment.ResourcesCombinationChangedEvent resourcesCombinationChangedEvent;

    private String appTitle = null;

    private static AsyncTask<Void, ResourceModel, Void> asyncTask;

    public Popular(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        faculty_id = PreferenceUtil.facultyId(getContext().getApplicationContext());
        department_id = PreferenceUtil.departmentId(getContext().getApplicationContext());
        level_id = PreferenceUtil.levelId(getContext().getApplicationContext());
        course_id = PreferenceUtil.courseId(getContext().getApplicationContext());
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
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.resources_popular, container, false);

        resourcesLoaderDisplay = root.findViewById(R.id.resources_popular_loader_display);
        toggleLoaderDisplay(false);

        swipeRefreshLayout = root.findViewById(R.id.resources_popular_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() ->
        {
            swipeRefreshLayout.setRefreshing(false);
            requestResources();
        });

        resourcesList = swipeRefreshLayout.findViewById(R.id.resources_popular_recycler_view);
        resourcesList.setItemAnimator(new DefaultItemAnimator());
        resourcesList.setAdapter(new Popular.ResourcesAdapter());

        resourcesList.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.recycler_grid_display_count),
                GridLayoutManager.VERTICAL, false));

        return root;
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

            if (savedInstanceState.getSerializable("resources") != null)
            {
                resources = (ArrayList<ResourceModel>)savedInstanceState.getSerializable("resources");
                loadResources(false);
            }
            else
                requestResources();
        }

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

    private boolean isLoading()
    { return resourcesLoaderDisplay.getVisibility() == View.VISIBLE; }

    private void requestResources()
    {
        if (isLoading())
            return;

        Timber.e("f: " + faculty_id + " d: " + department_id + " l: " + level_id + " c: " + course_id + " cat: " + category_id);
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
                    course_id, category_id, true, true);
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
                appTitle = getString(R.string.popular_in) + " " + resource.getCourse_code() + "  [" + resource.getCategory() + "s]";
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
                    showSnackMessage("Popular Resources Loaded!", false);
            }
        };
        asyncTask.execute();
    }

    private void toggleLoaderDisplay(boolean visible)
    {
        if (visible)
            resourcesLoaderDisplay.setVisibility(View.VISIBLE);
        else
            resourcesLoaderDisplay.setVisibility(View.GONE);
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

    private Snackbar showOpenDownloadedResourceSnackMessage(String message, File resourceFile)
    {
        return showSnackMessage(message, true, "Open",
                () -> startActivity(FileUtil.buildViewFileIntent(getContext(), resourceFile)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void resourcesCombinationChanged(ResourcesFragment.ResourcesCombinationChangedEvent resourcesCombinationChangedEvent)
    {
        if (this.resourcesCombinationChangedEvent == null)
        {
            this.resourcesCombinationChangedEvent = resourcesCombinationChangedEvent;
            faculty_id = resourcesCombinationChangedEvent.faculty_id;
            department_id = resourcesCombinationChangedEvent.department_id;
            level_id = resourcesCombinationChangedEvent.level_id;
            category_id = resourcesCombinationChangedEvent.category_id;
            course_id = resourcesCombinationChangedEvent.course_id;

            requestResources();
        }

        else if (! this.resourcesCombinationChangedEvent.similar(resourcesCombinationChangedEvent) || loadedResources.isEmpty())
        {
            this.resourcesCombinationChangedEvent = resourcesCombinationChangedEvent;
            faculty_id = resourcesCombinationChangedEvent.faculty_id;
            department_id = resourcesCombinationChangedEvent.department_id;
            level_id = resourcesCombinationChangedEvent.level_id;
            category_id = resourcesCombinationChangedEvent.category_id;
            course_id = resourcesCombinationChangedEvent.course_id;
            requestResources();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void resourcesAvailable(ResourcesAvailableEvent resourcesAvailableEvent)
    {
        this.resourcesAvailableEvent = resourcesAvailableEvent;
        resources = resourcesAvailableEvent.resourceModels;
        loadResources();
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
        private TextView cardDownloads;
        private MaterialButton btnView;
        private MaterialButton btnDownload;

        public ResourcesViewHolder(@NonNull View itemView)
        {
            super(itemView);

            cardImg = itemView.findViewById(R.id.resource_card_img);
            cardTitle = itemView.findViewById(R.id.resource_card_title);
            cardDesc = itemView.findViewById(R.id.resource_card_description);
            cardDate = itemView.findViewById(R.id.resource_card_popular_date);
            cardDownloads = itemView.findViewById(R.id.resource_card_downloads);
            btnView = itemView.findViewById(R.id.btn_view_resource);
            btnDownload = itemView.findViewById(R.id.btn_download_resource);
        }

        public void bindView (ResourceModel resourceModel, Activity activity)
        {
            String title = resourceModel.getTitle();
            String desc = resourceModel.getDescription();
            String date = resourceModel.getDate_added();
            String downloads = resourceModel.getDownloads() + "  " + activity.getString(R.string.downloads);

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
            cardDownloads.setText(downloads);
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

    private class ResourcesAdapter extends RecyclerView.Adapter<Popular.ResourcesViewHolder>
    {

        @NonNull
        @Override
        public Popular.ResourcesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new Popular.ResourcesViewHolder(getLayoutInflater().inflate(R.layout.resource_card_popular, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Popular.ResourcesViewHolder holder, int position)
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

































