package ng.com.stemcoders.campusspace.fragments;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

import it.sephiroth.android.library.bottomnavigation.BottomNavigation;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.resources.Comments;
import ng.com.stemcoders.campusspace.fragments.resources.Home;
import ng.com.stemcoders.campusspace.fragments.resources.Popular;
import timber.log.Timber;

public class ResourcesFragment extends Fragment
{
    private static Class currentFragmentClass;
    private static Fragment currentChildFragment;
    private static Fragment loaderFragment;
    private static Map<Class, Fragment> fragments;

    private Handler handler = new Handler();
    private Runnable runnable;

    private boolean newInstance;

    private ResourcesCombinationChangedEvent resourcesCombinationChangedEvent;

    private FragmentManager.OnBackStackChangedListener onBackStackChangedListener;

    public ResourcesFragment()
    {
        newInstance = true;
    }

    static
    { loaderFragment = new FragmentLoading(); }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);

        if (onBackStackChangedListener != null)
            getChildFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);

        onBackStackChangedListener = () ->
        {
            if (resourcesCombinationChangedEvent != null)
            {
                EventBus.getDefault().post(resourcesCombinationChangedEvent);
            }
        };

        getChildFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);
        super.onPause();
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
        View root = inflater.inflate(R.layout.fragment_resources, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            ChipNavigationBar chipNavigationBar = root.findViewById(R.id.chipNavBar);
            chipNavigationBar.setItemSelected(R.id.resources_home, true);
            chipNavigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(int id)
                {
                    menuItemSelected(id);
                }
            });
        }
        else
        {
            BottomNavigation bottomNavigation = root.findViewById(R.id.BottomNavigation);
            bottomNavigation.setMenuItemSelectionListener(new BottomNavigation.OnMenuItemSelectionListener()
            {
                @Override
                public void onMenuItemSelect(int id, int i1, boolean b)
                {
                    menuItemSelected(id);
                }

                @Override
                public void onMenuItemReselect(int i, int i1, boolean b)
                {
                }
            });
        }

        return root;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null && newInstance)
        {
            fragments = new HashMap<>();
            currentFragmentClass = Home.class;
            fragments.put(currentFragmentClass, new Home());
            loadChildFragment();
        }
    }

    @Override
    public void onDestroyView()
    {
        newInstance = false;
        super.onDestroyView();
    }

    private void menuItemSelected (@IdRes int itemId)
    {
        switch (itemId)
        {
            case R.id.resources_home:
                currentFragmentClass = Home.class;
                break;
            case R.id.resources_popular:
                currentFragmentClass = Popular.class;
                break;
            case R.id.resources_comments:
                currentFragmentClass = Comments.class;
                break;
        }

        loadChildFragment();
    }

    private void loadChildFragment()
    {
        final FragmentManager fragmentManager = getChildFragmentManager();

        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.resources_content_frame, loaderFragment).commit();

        runnable = () ->
        {
            if (currentFragmentClass != null)
            {
                try
                {
                    if (fragments.containsKey(currentFragmentClass))
                        currentChildFragment = fragments.get(currentFragmentClass);
                    else
                    {
                        if (currentFragmentClass == Popular.class)
                        {
                            currentChildFragment = new Popular();
                            fragments.put(currentFragmentClass, currentChildFragment);
                        }
                        else if (currentFragmentClass == Comments.class)
                        {
                            currentChildFragment = new Comments();
                            fragments.put(currentFragmentClass, currentChildFragment);
                        }
                    }
                } catch (Exception e)
                {
                    Timber.tag("Deb").e(e);
                }
            }

            if (currentChildFragment != null)
            {
                fragmentManager.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .addToBackStack(null)
                        .replace(R.id.resources_content_frame, currentChildFragment).commit();
            }
        };

        handler = new Handler();
        handler.post(runnable);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void resourcesCombinationChanged(ResourcesCombinationChangedEvent resourcesCombinationChangedEvent)
    { this.resourcesCombinationChangedEvent = resourcesCombinationChangedEvent; }

    public static class ResourcesCombinationChangedEvent
    {
        public final int faculty_id;
        public final int department_id;
        public final int level_id;
        public final int category_id;
        public final int course_id;

        public ResourcesCombinationChangedEvent(int faculty_id, int department_id, int level_id, int category_id, int course_id)
        {
            this.faculty_id = faculty_id;
            this.department_id = department_id;
            this.level_id = level_id;
            this.category_id = category_id;
            this.course_id = course_id;
        }

        public boolean similar(ResourcesCombinationChangedEvent other)
        {
            if (faculty_id == other.faculty_id && department_id == other.department_id
                && level_id == other.level_id && category_id == other.category_id
                && course_id == other.course_id)
                return true;

            return false;
        }
    }
}





































