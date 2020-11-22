package ng.com.stemcoders.campusspace.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.FileUtil;

public class DownloadsFragment extends Fragment
{
    private ProgressBar loaderDisplay;
    private TextView labelNoDownloads;
    private RecyclerView downloadsList;

    private List<File> downloadsMetas = new ArrayList<>();
    private List<String> loadedDownloads = new ArrayList<>();

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
        SwipeRefreshLayout root = (SwipeRefreshLayout)inflater.inflate(R.layout.fragment_downloads, container, false);
        root.setOnRefreshListener(() ->
        {
            fetchDownloads();
            root.setRefreshing(false);
        });

        loaderDisplay = root.findViewById(R.id.loader_display);
        labelNoDownloads = root.findViewById(R.id.label_no_downloads);
        downloadsList = root.findViewById(R.id.downloads_list);
        downloadsList.setAdapter(new DownloadsAdapter());
        downloadsList.setLayoutManager(new LinearLayoutManager(getContext()));
        downloadsList.setItemAnimator(new DefaultItemAnimator());

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
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (!downloadsMetas.isEmpty())
            outState.putSerializable("downloads", downloadsMetas.toArray());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("downloads") != null)
            {
                Object[] objs = (Object[])savedInstanceState.getSerializable("downloads");
                downloadsMetas.clear();

                for (Object obj : objs)
                    downloadsMetas.add((File)obj);

                loadDownloads(false);
            }
        }
        else
            fetchDownloads();
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

    private void fetchDownloads()
    {
        if (isLoading())
            return;

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            AlertUtil.showConfirmDialog(getContext(), "Permission Required", getString(R.string.storage_permission_alert_message),
                    ()-> requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            FileUtil.PERMISSION_REQUEST_CODE));
            return;
        }

        downloadsList.setVisibility(View.GONE);
        labelNoDownloads.setVisibility(View.VISIBLE);
        toggleLoaderDisplay(true);

        AsyncTask asyncTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] objects)
            {
                downloadsMetas = FileUtil.getResourcesMetaFiles(getContext());
                getActivity().runOnUiThread(() ->
                {
                    if (downloadsMetas.isEmpty())
                    {
                        showSnackMessage("No downloads found!", false);
                        toggleLoaderDisplay(false);
                    }
                    else
                        loadDownloads(true);
                });

                return null;
            }
        };
        asyncTask.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == FileUtil.PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            fetchDownloads();
    }

    private void loadDownloads(boolean  showLoadedAlert)
    {
        loadedDownloads.clear();
        downloadsList.removeAllViews();
        downloadsList.setVisibility(View.VISIBLE);
        labelNoDownloads.setVisibility(View.GONE);

        AsyncTask<Void, String, Object> asyncTask = new AsyncTask<Void, String, Object>()
        {
            @Override
            protected Object doInBackground(Void... voids)
            {
                try
                {
                    for (File file : downloadsMetas)
                    {
                        try
                        {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            StringBuilder stringBuilder = new StringBuilder();
                            String line = reader.readLine();

                            while (line != null)
                            {
                                stringBuilder.append(line).append("\n");
                                line = reader.readLine();
                            }

                            publishProgress(stringBuilder.toString());
                        } catch (FileNotFoundException fe){}
                    }
                } catch (Exception e)
                {
                    getActivity().runOnUiThread(() ->
                    {
                        showError("An error prevented the downloads from loading!\n\nPlease try again.");
                    });

                    return e;
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values)
            {
                addDownload(values[0]);
            }

            @Override
            protected void onPostExecute(Object object)
            {
                if (object != null)
                    return;

                toggleLoaderDisplay(false);

                if (showLoadedAlert)
                    showSnackMessage("Downloads Loaded!", false);
            }
        };
        asyncTask.execute();
    }

    private void reloadList()
    {
        downloadsList.getAdapter().notifyItemInserted(loadedDownloads.size());
        downloadsList.smoothScrollToPosition(loadedDownloads.size()-1);
    }

    private void addDownload(String metaContent)
    {
        loadedDownloads.add(metaContent);
        reloadList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }

    private class DownloadsViewHolder extends RecyclerView.ViewHolder
    {
        private TextView title, course, size;
        private Button btnOpen, btnDelete;

        public DownloadsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            title = itemView.findViewById(R.id.resource_title);
            course = itemView.findViewById(R.id.resource_course);
            size = itemView.findViewById(R.id.resource_size);
            btnOpen = itemView.findViewById(R.id.btn_open_resource);
            btnDelete = itemView.findViewById(R.id.btn_delete_resource);
        }

        public void bind(String metaContents, int position)
        {
            String[] lines = metaContents.split("\n");

            title.setText(lines[0]);
            course.setText(lines[1]);
            size.setText(lines[2]);

            String fileName = lines[3];
            File resourceFile = new File(FileUtil.getResourcesDir(getContext()), fileName);

            btnOpen.setOnClickListener(v -> startActivity(FileUtil.buildViewFileIntent(getContext(), resourceFile)));

            btnDelete.setOnClickListener(v ->
            {
                try
                {
                    File metaFile = FileUtil.newResourceFileMeta(getContext(), fileName);

                    AlertUtil.showConfirmDialog(getContext(), "Delete Resource?", "Do you really want to delete this resource now?\n\n" +
                            "Please note, this process cannot be reversed!", () ->
                    {
                        if (metaFile.delete())
                        {
                            resourceFile.delete();
                            loadedDownloads.remove(metaContents);
                            downloadsList.getAdapter().notifyItemRemoved(position);

                            showSnackMessage("Resource deleted!", false);
                        }
                        else
                            showError("Failed to delete resource!");
                    });
                } catch (IOException e)
                {
                    showError("An unknown error occurred!");
                }
            });
        }
    }

    private class DownloadsAdapter extends RecyclerView.Adapter<DownloadsViewHolder>
    {
        @NonNull
        @Override
        public DownloadsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new DownloadsViewHolder(getLayoutInflater().inflate(R.layout.downloads_row_card, parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadsViewHolder holder, int position)
        {
            String meta = loadedDownloads.get(position);
            holder.bind(meta, position);
        }

        @Override
        public int getItemCount()
        {
            return loadedDownloads.size();
        }
    }
}
















































