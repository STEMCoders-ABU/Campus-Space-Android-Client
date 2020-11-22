package ng.com.stemcoders.campusspace.fragments.moderation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class Home extends Fragment
{
    private ProgressBar loaderDisplay;
    private TextInputEditText courseTitleInput;
    private TextInputEditText courseCodeInput;
    private MaterialButton btnLogout, btnAddCourse;
    private MaterialButton btnEditProfile, btnAddResource, btnAddNews, btnManageResources, btnManageNews;

    private String username, password;

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);

        username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(
                PreferenceUtil.moderatorName(requireContext().getApplicationContext())
        ));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.moderation_home, container, false);

        loaderDisplay = root.findViewById(R.id.moderation_home_loader_display);
        courseTitleInput = root.findViewById(R.id.course_title_input);
        courseCodeInput = root.findViewById(R.id.course_code_input);
        btnLogout = root.findViewById(R.id.btn_log_out);
        btnLogout.setOnClickListener(v -> logout());

        btnAddCourse = root.findViewById(R.id.btn_add_course);
        btnAddCourse.setOnClickListener(v -> addCourse());

        btnEditProfile = root.findViewById(R.id.btn_edit_profile);
        btnEditProfile.setOnClickListener(v -> EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(EditProfile.class)));

        btnAddResource = root.findViewById(R.id.btn_add_resource);
        btnAddResource.setOnClickListener(v -> EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(AddResource.class)));

        //btnAddNews = root.findViewById(R.id.btn_add_news);
        //btnAddNews.setOnClickListener(v -> EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(AddNews.class)));

        btnManageResources = root.findViewById(R.id.btn_manage_resources);
        btnManageResources.setOnClickListener(v -> EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(ManageResources.class)));

        //btnManageNews = root.findViewById(R.id.btn_manage_news);
        //btnManageNews.setOnClickListener(v -> EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(ManageNews.class)));

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

    private void addCourse()
    {
        if (isLoading())
            return;

        String courseTitle = courseTitleInput.getText().toString();
        String courseCode = courseCodeInput.getText().toString();

        if (TextUtils.isEmpty(courseTitle) || TextUtils.isEmpty(courseCode))
        {
            showSnackMessage(getString(R.string.message_invalid_course_title_code), false);
            return;
        }

        toggleLoaderDisplay(true);
        btnAddCourse.setEnabled(false);

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<Void> call = moderatorService.addCourse(courseTitle, courseCode);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);
                btnAddCourse.setEnabled(true);

                if (response.isSuccessful())
                {
                    showSnackMessage(getString(R.string.message_course_added), false);
                    courseTitleInput.setText("");
                    courseCodeInput.setText("");
                }
                else
                {
                    showError(getString(R.string.message_course_exists));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                btnAddCourse.setEnabled(true);
                showSnackMessage("A network error occurred!", false);
                Timber.e(t, "Failed to add course!");
            }
        });
    }

    private void logout()
    {
        AlertUtil.showConfirmDialog(getContext(), "Logout?", "Do you really want to logout now?",
                () ->
                {
                    PreferenceUtil.clearModeratorData(requireContext().getApplicationContext());
                    EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(Authentication.class));
                });
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

    private void showError(String message)
    {
        AlertUtil.showAlert(getContext(), "Oops!", message);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }
}





























