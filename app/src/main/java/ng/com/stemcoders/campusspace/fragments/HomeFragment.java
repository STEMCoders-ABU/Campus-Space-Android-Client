package ng.com.stemcoders.campusspace.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.utils.AlertUtil;

public class HomeFragment extends Fragment
{
    private MaterialCardView cardResources, cardNews, cardDownloads, cardPrefs;

    public HomeFragment() {}

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
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Campus Space"));
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
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        cardResources = root.findViewById(R.id.card_resources);
        cardResources.setOnClickListener(v -> EventBus.getDefault().post(new MainActivity.ChangeMenuEvent(MainActivity.RESOURCES_POSITION)));

        //cardNews = root.findViewById(R.id.card_news);
        //cardNews.setOnClickListener(v -> EventBus.getDefault().post(new MainActivity.ChangeMenuEvent(MainActivity.NEWS_POSITION)));

        cardDownloads = root.findViewById(R.id.card_downloads);
        cardDownloads.setOnClickListener(v -> EventBus.getDefault().post(new MainActivity.ChangeMenuEvent(MainActivity.DOWNLOADS_POSITION)));

        cardPrefs = root.findViewById(R.id.card_preferences);
        cardPrefs.setOnClickListener(v -> EventBus.getDefault().post(new MainActivity.ChangeMenuEvent(MainActivity.PREFS_POSITION)));

        return root;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        AlertUtil.showConfirmDialog(getContext(), "Exit?",
                "Do you really want to exit the app now?", () ->
                {
                    EventBus.getDefault().post(new MainActivity.ExitAppEvent());
                });
    }
}


































