package ng.com.stemcoders.campusspace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.shrikanthravi.customnavigationdrawer2.data.MenuItem;
import com.shrikanthravi.customnavigationdrawer2.widget.SNavigationDrawer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iammert.com.library.Status;
import ng.com.stemcoders.campusspace.fragments.AboutFragment;
import ng.com.stemcoders.campusspace.fragments.DownloadsFragment;
import ng.com.stemcoders.campusspace.fragments.FragmentLoading;
import ng.com.stemcoders.campusspace.fragments.HomeFragment;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.fragments.NewsFragment;
import ng.com.stemcoders.campusspace.fragments.PreferencesFragment;
import ng.com.stemcoders.campusspace.fragments.ResourcesFragment;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity
{
    private SNavigationDrawer navigationDrawer;
    private Class currentFragmentClass;
    private static Fragment currentFragment;
    private static Fragment loaderFragment;
    private static Map<Class, Fragment> fragments;

    private Handler handler = new Handler();
    private Runnable runnable;

    private ConnectivityListener connectivityListener;
    private ConnectivityManager connectivityManager;

    public static final int HOME_POSITION = 0;
    public static final int RESOURCES_POSITION = 1;
    //public static final int NEWS_POSITION = 2;
    public static final int MODERATION_POSITION = 2;
    public static final int DOWNLOADS_POSITION = 3;
    public static final int PREFS_POSITION = 4;
    public static final int ABOUT_POSITION = 5;

    static
    { loaderFragment = new FragmentLoading(); }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        List<MenuItem> navMenuItems = new ArrayList<>();
        navMenuItems.add(new MenuItem("Home", R.drawable.stemlogo));
        navMenuItems.add(new MenuItem("Resources", R.drawable.stemlogo));
        //navMenuItems.add(new MenuItem("News/Updates", R.drawable.stemlogo));
        navMenuItems.add(new MenuItem("Moderation", R.drawable.stemlogo));
        navMenuItems.add(new MenuItem("Downloads", R.drawable.stemlogo));
        navMenuItems.add(new MenuItem("Preferences", R.drawable.stemlogo));
        navMenuItems.add(new MenuItem("About", R.drawable.stemlogo));

        navigationDrawer = findViewById(R.id.navigationDrawer);
        navigationDrawer.setMenuItemList(navMenuItems);

        if (savedInstanceState == null || fragments == null)
        {
            fragments = new HashMap<>();
            currentFragmentClass = HomeFragment.class;
            fragments.put(currentFragmentClass, new HomeFragment());
            loadCurrentFragment();
        }

        setupDrawer();

        connectivityListener = new ConnectivityListener();
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void setupDrawer()
    {
        navigationDrawer.setOnMenuItemClickListener(position ->
        {
            switch (position)
            {
                case HOME_POSITION:
                    currentFragmentClass = HomeFragment.class;
                    break;
                case RESOURCES_POSITION:
                    currentFragmentClass = ResourcesFragment.class;
                    break;
                case MODERATION_POSITION:
                    currentFragmentClass = ModerationFragment.class;
                    break;
                case DOWNLOADS_POSITION:
                    currentFragmentClass = DownloadsFragment.class;
                    break;
                case PREFS_POSITION:
                    currentFragmentClass = PreferencesFragment.class;
                    break;
                case ABOUT_POSITION:
                    currentFragmentClass = AboutFragment.class;
                    break;
            }
        });

        navigationDrawer.setDrawerListener(new SNavigationDrawer.DrawerListener()
        {
            @Override
            public void onDrawerOpening()
            {
            }

            @Override
            public void onDrawerClosing(boolean contentChanged)
            {

            }

            @Override
            public void onDrawerOpened()
            {

            }

            @Override
            public void onDrawerClosed(boolean contentChanged)
            {
                if (contentChanged)
                    loadCurrentFragment();
            }

            @Override
            public void onDrawerStateChanged(int newState)
            {

            }
        });
    }

    private void loadCurrentFragment()
    {
        final FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.contentFrameLayout, loaderFragment).commit();

        runnable = () ->
        {
            if (currentFragmentClass != null)
            {
                try
                {
                    if (fragments.containsKey(currentFragmentClass))
                        currentFragment = fragments.get(currentFragmentClass);
                    else
                    {
                        if (currentFragmentClass == ResourcesFragment.class)
                        {
                            currentFragment = new ResourcesFragment();
                            fragments.put(currentFragmentClass, currentFragment);
                        }
                        else if (currentFragmentClass == NewsFragment.class)
                        {
                            currentFragment = new NewsFragment();
                            fragments.put(currentFragmentClass, currentFragment);
                        }
                        else if (currentFragmentClass == ModerationFragment.class)
                        {
                            currentFragment = new ModerationFragment();
                            fragments.put(currentFragmentClass, currentFragment);
                        }
                        else if (currentFragmentClass == DownloadsFragment.class)
                        {
                            currentFragment = new DownloadsFragment();
                            fragments.put(currentFragmentClass, currentFragment);
                        }
                        else if (currentFragmentClass == PreferencesFragment.class)
                        {
                            currentFragment = new PreferencesFragment();
                            fragments.put(currentFragmentClass, currentFragment);
                        }
                        else if (currentFragmentClass == AboutFragment.class)
                        {
                            currentFragment = new AboutFragment();
                            fragments.put(currentFragmentClass, currentFragment);
                        }
                    }
                } catch (Exception e)
                {
                    Timber.tag("Deb").e(e);
                }
            }

            if (currentFragment != null)
            {
                fragmentManager.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .addToBackStack(null)
                        .replace(R.id.contentFrameLayout, currentFragment).commit();
            }
        };

        handler = new Handler();
        handler.post(runnable);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        navigationDrawer.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        navigationDrawer.restoreState(savedInstanceState);
    }

    @Override
    protected void onDestroy()
    {
        if (runnable != null && handler.hasCallbacks(runnable))
            handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        EventBus.getDefault().post(new BackPressedEvent());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        EventBus.getDefault().register(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            connectivityManager.registerDefaultNetworkCallback(connectivityListener);
        else
        {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            connectivityManager.registerNetworkCallback(networkRequest, connectivityListener);
        }
    }

    @Override
    protected void onPause()
    {
        EventBus.getDefault().unregister(this);
        connectivityManager.unregisterNetworkCallback(connectivityListener);
        super.onPause();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeAppBarTitle(ChangeAppbarTitleEvent changeAppbarTitleEvent)
    {
        navigationDrawer.setAppbarTitleTV(changeAppbarTitleEvent.title);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeMenu(ChangeMenuEvent changeMenuEvent)
    {
        if (navigationDrawer.getCurrentPos() != changeMenuEvent.position)
        {
            navigationDrawer.openDrawer();
            Handler handler = new Handler();
            handler.postDelayed(() -> navigationDrawer.selectMenuItem(changeMenuEvent.position, false),
                    500);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void exitApp(ExitAppEvent exitAppEvent)
    {
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void openDrawer(OpenDrawerEvent openDrawerEvent)
    {
        navigationDrawer.openDrawer();
    }

    private class ConnectivityListener extends ConnectivityManager.NetworkCallback
    {
        @Override
        public void onAvailable(@NonNull Network network)
        {
            runOnUiThread(() -> navigationDrawer.setConnectionStatus(Status.COMPLETE));
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> navigationDrawer.setConnectionStatus(Status.IDLE),
                    1000);
        }

        @Override
        public void onLost(@NonNull Network network)
        {
            runOnUiThread(() -> navigationDrawer.setConnectionStatus(Status.ERROR));
        }

        @Override
        public void onUnavailable()
        {
            runOnUiThread(() -> navigationDrawer.setConnectionStatus(Status.ERROR));
        }
    }

    public static class ChangeAppbarTitleEvent
    {
        public final String title;

        public ChangeAppbarTitleEvent(String title)
        { this.title = title; }
    }

    public static class ChangeMenuEvent
    {
        public final int position;

        public ChangeMenuEvent(int position)
        {
            this.position = position;
        }
    }

    public static class BackPressedEvent{}

    public static class ExitAppEvent{}

    public static class OpenDrawerEvent{}
}
































