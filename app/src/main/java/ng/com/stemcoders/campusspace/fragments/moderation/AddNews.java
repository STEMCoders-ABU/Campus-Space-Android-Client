package ng.com.stemcoders.campusspace.fragments.moderation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.fragments.ModerationFragment;
import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.models.NewsCategoryModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class AddNews extends Fragment
{
    private ProgressBar loaderDisplay;
    private Spinner categorySpinner;
    private TextInputEditText titleInput, contentsInput;
    private Button btnUpload;

    private Map<String, Integer> categories = new TreeMap<>();
    private ArrayAdapter<List<String>> categoriesArrayAdapter;

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
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent("Add News/Update"));
    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);

        if (!categories.isEmpty())
            categoriesArrayAdapter = (ArrayAdapter<List<String>>)categorySpinner.getAdapter();
        else
            categoriesArrayAdapter = null;

        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.moderation_add_news, container, false);

        loaderDisplay = root.findViewById(R.id.add_news_loader_display);
        categorySpinner = root.findViewById(R.id.category_spinner);
        titleInput = root.findViewById(R.id.news_title_input);
        contentsInput = root.findViewById(R.id.news_contents_input);
        btnUpload = root.findViewById(R.id.btn_upload_news);
        btnUpload.setOnClickListener(v -> uploadNews());

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

    private void showError(String message)
    {
        AlertUtil.showAlert(getContext(), "Oops!", message);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (categorySpinner != null && categorySpinner.isEnabled() && !categories.isEmpty())
        {
            outState.putBoolean("categories_available", true);
            outState.putInt("selected_category_pos", findPosition(categories.keySet(),
                    (String)categorySpinner.getSelectedItem()));
            outState.putSerializable("categories_titles", categories.keySet().toArray());
            outState.putIntegerArrayList("categories_ids", new ArrayList<>(categories.values()));
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null)
        {
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
            }
        }
    }

    private void loadCategories()
    {
        if (isLoading())
            return;

        categorySpinner.setEnabled(false);

        toggleLoaderDisplay(true);

        ProvidersService providersService = RetroServiceGenerator.generateService(ProvidersService.class);
        Call<List<NewsCategoryModel>> call = providersService.getNewsCategories();
        call.enqueue(new Callback<List<NewsCategoryModel>>()
        {
            @Override
            public void onResponse(Call<List<NewsCategoryModel>> call, Response<List<NewsCategoryModel>> response)
            {
                toggleLoaderDisplay(false);

                if (response.isSuccessful())
                {
                    categoriesAvailable(response.body());
                }
                else
                {
                    showSnackMessage("No News category found!\n\nThis is an unexpected error, please contact us ASAP", false);
                }
            }

            @Override
            public void onFailure(Call<List<NewsCategoryModel>> call, Throwable t)
            {
                toggleLoaderDisplay(false);
                showReloadCategoriesSnackMessage("A network error occurred!");
            }
        });
    }

    private void categoriesAvailable(List<NewsCategoryModel> newsCategoryModels)
    {
        categories.clear();

        for (NewsCategoryModel newsCategoryModel : newsCategoryModels)
            categories.put(newsCategoryModel.getCategory(), newsCategoryModel.getId());

        ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,
                new ArrayList(categories.keySet()));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setEnabled(true);
        categorySpinner.setAdapter(arrayAdapter);
    }

    private void uploadNews()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the current task to finish first!", false);
            return;
        }

        if (!categorySpinner.isEnabled())
        {
            showReloadCategoriesSnackMessage("No category selected!");
            return;
        }

        if (TextUtils.isEmpty(titleInput.getText().toString()) || TextUtils.isEmpty(contentsInput.getText().toString()))
        {
            showSnackMessage("Please fill out the fields!", false);
            return;
        }

        toggleLoaderDisplay(true);
        btnUpload.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(requireContext().getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(requireContext().getApplicationContext());
        String title = titleInput.getText().toString();
        String contents = contentsInput.getText().toString();
        int categoryId = categories.get(categorySpinner.getSelectedItem());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<Void> call = moderatorService.addNews(title, contents, categoryId);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);
                btnUpload.setEnabled(true);

                if (response.isSuccessful())
                {
                    showSnackMessage("News uploaded!", false);
                    titleInput.setText("");
                    contentsInput.setText("");
                }
                else
                {
                    try
                    {
                        showError("An error occurred while attempting to upload the news!\n\nPlease try again:\n\n" +
                                response.errorBody().string());
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
                btnUpload.setEnabled(true);
                showSnackMessage("A network error occurred!", false);
                Timber.e(t, "News upload failed");
            }
        });
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




























