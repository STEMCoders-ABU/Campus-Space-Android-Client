package ng.com.stemcoders.campusspace.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.moderation.AddNews;
import ng.com.stemcoders.campusspace.fragments.moderation.AddResource;
import ng.com.stemcoders.campusspace.fragments.moderation.Authentication;
import ng.com.stemcoders.campusspace.fragments.moderation.EditProfile;
import ng.com.stemcoders.campusspace.fragments.moderation.Home;
import ng.com.stemcoders.campusspace.fragments.moderation.ManageNews;
import ng.com.stemcoders.campusspace.fragments.moderation.ManageResources;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import timber.log.Timber;

public class ModerationFragment extends Fragment
{
    private static Class currentFragmentClass;
    private static Fragment currentChildFragment;
    private static Fragment loaderFragment;
    private static Map<Class, Fragment> fragments;

    private Handler handler = new Handler();
    private Runnable runnable;

    private boolean newInstance;

    static
    { loaderFragment = new FragmentLoading(); }

    public ModerationFragment() { newInstance = true; }

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
        View root = inflater.inflate(R.layout.fragment_moderation, container, false);
        return root;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null && newInstance)
        {
            fragments = new HashMap<>();

            if (!PreferenceUtil.isModeratorLogged(requireContext().getApplicationContext()))
            {
                currentFragmentClass = Authentication.class;
                fragments.put(currentFragmentClass, new Authentication());
            }
            else
            {
                currentFragmentClass = Home.class;
                fragments.put(currentFragmentClass, new Home());
            }

            loadChildFragment();
        }
    }

    @Override
    public void onDestroyView()
    {
        newInstance = false;
        super.onDestroyView();
    }

    private void loadChildFragment()
    {
        final FragmentManager fragmentManager = getChildFragmentManager();

        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.moderation_content_frame, loaderFragment).commit();

        runnable = () ->
        {
            if (currentFragmentClass != null)
            {
                try
                {
                    if (fragments.containsKey(currentFragmentClass))
                        currentChildFragment = fragments.get(currentFragmentClass);
                    else if (currentFragmentClass == Home.class)
                    {
                        currentChildFragment = new Home();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                    else if (currentFragmentClass == Authentication.class)
                    {
                        currentChildFragment = new Authentication();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                    else if (currentFragmentClass == EditProfile.class)
                    {
                        currentChildFragment = new EditProfile();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                    else if (currentFragmentClass == AddResource.class)
                    {
                        currentChildFragment = new AddResource();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                    else if (currentFragmentClass == AddNews.class)
                    {
                        currentChildFragment = new AddNews();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                    else if (currentFragmentClass == ManageResources.class)
                    {
                        currentChildFragment = new ManageResources();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                    else if (currentFragmentClass == ManageNews.class)
                    {
                        currentChildFragment = new ManageNews();
                        fragments.put(currentFragmentClass, currentChildFragment);
                    }
                } catch (Exception e)
                {
                    Timber.e(e);
                }
            }

            if (currentChildFragment != null)
            {
                fragmentManager.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .addToBackStack(null)
                        .replace(R.id.moderation_content_frame, currentChildFragment).commit();
            }
        };

        handler = new Handler();
        handler.post(runnable);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeFragment(ChangeFragmentEvent changeFragmentEvent)
    {
        if (changeFragmentEvent.fragmentClass != null)
        {
            currentFragmentClass = changeFragmentEvent.fragmentClass;
            loadChildFragment();
        }
    }

    public static class ChangeFragmentEvent
    {
        public final Class fragmentClass;

        public ChangeFragmentEvent(Class fragmentClass)
        {
            this.fragmentClass = fragmentClass;
        }
    }
}































