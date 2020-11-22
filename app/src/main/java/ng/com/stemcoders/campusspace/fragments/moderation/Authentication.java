package ng.com.stemcoders.campusspace.fragments.moderation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import ng.com.stemcoders.campusspace.net.models.ModeratorModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class Authentication extends Fragment
{
    private ProgressBar loaderDisplay;
    private TextInputEditText usernameInput, passwordInput;
    private MaterialButton btnLogin;
    private TextView linkForgotPassword;

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Log In"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.moderation_authentication, container, false);

        loaderDisplay = root.findViewById(R.id.authentication_loader_display);
        usernameInput = root.findViewById(R.id.username_input);
        passwordInput = root.findViewById(R.id.password_input);

        btnLogin = root.findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> authenticate());

        linkForgotPassword = root.findViewById(R.id.link_forgot_password);
        linkForgotPassword.setOnClickListener(v ->
        {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://campus-space.com.ng/moderation/reset_password"));
            startActivity(intent);
        });
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

    private void authenticate()
    {
        if (isLoading())
            return;

        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
        {
            usernameInput.setError(getString(R.string.invalid_username));
            showSnackMessage(getString(R.string.authentication_hint), false);
            return;
        }

        if (username.length() < 4 || username.length() > 12)
        {
            usernameInput.setError(getString(R.string.invalid_username));
            showSnackMessage(getString(R.string.invalid_username), false);
            return;
        }

        toggleLoaderDisplay(true);
        btnLogin.setEnabled(false);

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<ModeratorModel> call = moderatorService.getModerator(true);
        call.enqueue(new Callback<ModeratorModel>()
        {
            @Override
            public void onResponse(Call<ModeratorModel> call, Response<ModeratorModel> response)
            {
                toggleLoaderDisplay(false);
                btnLogin.setEnabled(true);

                if (response.isSuccessful())
                {
                    usernameInput.setText("");
                    passwordInput.setText("");

                    PreferenceUtil.setupModeratorData(requireContext().getApplicationContext(), response.body(), password);
                    EventBus.getDefault().post(new ModerationFragment.ChangeFragmentEvent(Home.class));
                }
                else
                {
                    showSnackMessage(getString(R.string.authentication_failed_message), false);
                }
            }

            @Override
            public void onFailure(Call<ModeratorModel> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                btnLogin.setEnabled(true);
                showSnackMessage("A network error occurred!", false);
                Timber.e(t, "Moderator login failed");
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }
}

























