package ng.com.stemcoders.campusspace.fragments.moderation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.models.ModeratorModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class EditProfile extends Fragment
{
    private ProgressBar loaderDisplay;
    private TextInputEditText emailInput, fullNameInput, phoneInput;
    private Spinner genderSpinner;
    private MaterialButton btnUpdateProfile;
    private TextInputEditText oldPasswordInput, newPasswordInput, confirmPasswordInput;
    private MaterialButton btnChangePassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.moderation_edit_profile, container, false);

        loaderDisplay = root.findViewById(R.id.edit_profile_loader_display);
        emailInput = root.findViewById(R.id.email_input);
        fullNameInput = root.findViewById(R.id.fullname_input);
        phoneInput = root.findViewById(R.id.phone_input);
        genderSpinner = root.findViewById(R.id.gender_spinner);
        btnUpdateProfile = root.findViewById(R.id.btn_update_profile);
        btnUpdateProfile.setOnClickListener(v -> updateProfile());

        oldPasswordInput = root.findViewById(R.id.old_password_input);
        newPasswordInput = root.findViewById(R.id.new_password_input);
        confirmPasswordInput = root.findViewById(R.id.confirm_password_input);
        btnChangePassword = root.findViewById(R.id.btn_change_password);
        btnChangePassword.setOnClickListener(v -> changePassword());

        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Edit Profile"));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null)
        {
            emailInput.setText(PreferenceUtil.moderatorEmail(requireContext().getApplicationContext()));
            fullNameInput.setText(PreferenceUtil.moderatorName(requireContext().getApplicationContext()));
            phoneInput.setText(PreferenceUtil.moderatorPhone(requireContext().getApplicationContext()));

            String gender = PreferenceUtil.moderatorGender(requireContext().getApplicationContext());
            if (TextUtils.equals(gender, getString(R.string.male)))
                genderSpinner.setSelection(0);
            else
                genderSpinner.setSelection(1);

            oldPasswordInput.setText("");
            newPasswordInput.setText("");
            confirmPasswordInput.setText("");
        }
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

    private void updateProfile()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the existing task to finish!", false);
            return;
        }

        String email = emailInput.getText().toString();
        String fullName = fullNameInput.getText().toString();
        String phone = phoneInput.getText().toString();
        String gender = (String)genderSpinner.getSelectedItem();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(gender))
        {
            showSnackMessage("Please fill out all the fields!", false);
            return;
        }

        toggleLoaderDisplay(true);
        btnUpdateProfile.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<ModeratorModel> call = moderatorService.updateModerator(email, null, fullName, gender, phone, false);
        call.enqueue(new Callback<ModeratorModel>()
        {
            @Override
            public void onResponse(Call<ModeratorModel> call, Response<ModeratorModel> response)
            {
                toggleLoaderDisplay(false);
                btnUpdateProfile.setEnabled(true);

                if (response.isSuccessful())
                {
                    showSnackMessage("Profile updated!", false);
                    PreferenceUtil.setupModeratorData(requireContext().getApplicationContext(), response.body(), password);
                }
                else
                {
                    showError("Failed to update profile. Something went wrong, please request for assistance!");
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
            public void onFailure(Call<ModeratorModel> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                btnUpdateProfile.setEnabled(true);
                showSnackMessage("A network error occurred!", false);
            }
        });
    }

    private void changePassword()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the existing task to finish!", false);
            return;
        }

        String old_password = oldPasswordInput.getText().toString();
        String new_password = newPasswordInput.getText().toString();
        String confirm_password = confirmPasswordInput.getText().toString();

        if (TextUtils.isEmpty(old_password) || TextUtils.isEmpty(new_password) || TextUtils.isEmpty(confirm_password))
        {
            showSnackMessage("Please fill out all the password fields!", false);
            return;
        }

        if (! TextUtils.equals(old_password, PreferenceUtil.moderatorPassword(requireContext().getApplicationContext())))
        {
            showSnackMessage("Old password is incorrect!", false);
            return;
        }

        if (! TextUtils.equals(new_password, confirm_password))
        {
            showSnackMessage("Passwords do not match!", false);
            return;
        }

        toggleLoaderDisplay(true);
        btnChangePassword.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<ModeratorModel> call = moderatorService.updateModerator(null, new_password, null, null, null, false);
        call.enqueue(new Callback<ModeratorModel>()
        {
            @Override
            public void onResponse(Call<ModeratorModel> call, Response<ModeratorModel> response)
            {
                toggleLoaderDisplay(false);
                btnChangePassword.setEnabled(true);

                if (response.isSuccessful())
                {
                    oldPasswordInput.setText("");
                    newPasswordInput.setText("");
                    confirmPasswordInput.setText("");
                    showSnackMessage("Password changed!", false);
                    PreferenceUtil.setupModeratorData(requireContext().getApplicationContext(), response.body(), new_password);
                }
                else
                {
                    showError("Failed to change password. Something went wrong, please request for assistance!");
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
            public void onFailure(Call<ModeratorModel> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                btnChangePassword.setEnabled(true);
                showSnackMessage("A network error occurred!", false);
            }
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































