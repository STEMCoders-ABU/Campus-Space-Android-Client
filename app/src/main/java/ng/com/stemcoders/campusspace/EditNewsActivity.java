package ng.com.stemcoders.campusspace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.models.NewsCategoryModel;
import ng.com.stemcoders.campusspace.net.models.NewsModel;
import ng.com.stemcoders.campusspace.net.services.ModeratorService;
import ng.com.stemcoders.campusspace.net.services.ProvidersService;
import ng.com.stemcoders.campusspace.utils.AlertUtil;
import ng.com.stemcoders.campusspace.utils.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class EditNewsActivity extends AppCompatActivity
{
    private ProgressBar loaderDisplay;
    private Spinner categorySpinner;
    private TextInputEditText titleInput, contentsInput;
    private Button btnUpdate;

    private NewsModel newsModel = null;

    private Map<String, Integer> categories = new TreeMap<>();

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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_news);

        loaderDisplay = findViewById(R.id.loader_display);
        categorySpinner = findViewById(R.id.category_spinner);
        categorySpinner.setEnabled(false);
        titleInput = findViewById(R.id.news_name_input);
        contentsInput = findViewById(R.id.news_content_input);
        btnUpdate = findViewById(R.id.btn_update_news);
        btnUpdate.setOnClickListener(v -> updateNews());

        Intent intent = getIntent();

        if (savedInstanceState == null)
        {
            newsModel = (NewsModel) intent.getExtras().getSerializable("news");
            titleInput.setText(newsModel.getTitle());
            contentsInput.setText(newsModel.getContent());
            loadCategories();
        }
        else
            newsModel = (NewsModel) savedInstanceState.getSerializable("news");

        if (newsModel == null)
            finish();

        getSupportActionBar().setTitle(newsModel.getTitle());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (categorySpinner != null && categorySpinner.isEnabled() && !categories.isEmpty())
        {
            outState.putBoolean("categories_available", true);
            outState.putInt("selected_category_pos", findPosition(categories.keySet(),
                    (String) categorySpinner.getSelectedItem()));
            outState.putSerializable("categories_titles", categories.keySet().toArray());
            outState.putIntegerArrayList("categories_ids", new ArrayList<>(categories.values()));
        }

        outState.putSerializable("news", newsModel);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.getBoolean("categories_available", false))
        {
            Object[] categories_titles = (Object[])savedInstanceState.getSerializable("categories_titles");
            ArrayList<Integer> categories_ids = savedInstanceState.getIntegerArrayList("categories_ids");

            categories.clear();
            for (int i=0; i<categories_titles.length; i++)
                categories.put((String)categories_titles[i], categories_ids.get(i));

            ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
                    new ArrayList(categories.keySet()));
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setEnabled(true);
            categorySpinner.setAdapter(arrayAdapter);

            int current_category_pos = savedInstanceState.getInt("selected_category_pos");
            if (current_category_pos != -1)
                categorySpinner.setSelection(current_category_pos);
        }
        else
            loadCategories();
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

        for (NewsCategoryModel categoryModel : newsCategoryModels)
            categories.put(categoryModel.getCategory(), categoryModel.getId());

        ArrayAdapter<List<String>> arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
                new ArrayList(categories.keySet()));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setEnabled(true);
        categorySpinner.setAdapter(arrayAdapter);

        int selected = findPosition(categories.values(), newsModel.getCategory_id());
        categorySpinner.setSelection(selected);
    }

    private void updateNews()
    {
        if (isLoading())
        {
            showSnackMessage("Please wait for the current task to complete!", false);
            return;
        }

        String newsTitle = titleInput.getText().toString();
        String newsContents = contentsInput.getText().toString();
        int categoryId = categories.get((String)categorySpinner.getSelectedItem());

        if (TextUtils.isEmpty(newsTitle) || TextUtils.isEmpty(newsContents))
        {
            showSnackMessage("Please fill out all the fields!", false);
            return;
        }

        toggleLoaderDisplay(true);
        btnUpdate.setEnabled(false);

        String username = PreferenceUtil.moderatorUsername(getApplicationContext());
        String password = PreferenceUtil.moderatorPassword(getApplicationContext());

        ModeratorService moderatorService = RetroServiceGenerator.generateService(ModeratorService.class, username, password);
        Call<Void> call = moderatorService.updateNews(newsModel.getId(), newsTitle, newsContents, categoryId);
        call.enqueue(new Callback<Void>()
        {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response)
            {
                toggleLoaderDisplay(false);
                btnUpdate.setEnabled(true);

                if (response.isSuccessful())
                {
                    showSnackMessage("News updated!", false);
                    EventBus.getDefault().post(new MainActivity.ChangeAppbarTitleEvent(newsTitle));
                }
                else
                {
                    showError("This news/update title already exists!\nPlease choose another one");
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
            public void onFailure(Call<Void> call, Throwable t)
            {
                toggleLoaderDisplay(true);
                btnUpdate.setEnabled(false);
                showSnackMessage("A network error occurred!", false);
            }
        });
    }

    private Snackbar showSnackMessage(String message, boolean indefinite)
    {
        View root = findViewById(android.R.id.content);
        if (root == null)
            return null;

        Snackbar snackbar = Snackbar.make(root, message, indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        snackbar.show();
        return snackbar;
    }

    private Snackbar showSnackMessage(String message, boolean indefinite, String actionText, Runnable action)
    {
        View root = findViewById(android.R.id.content);
        if (root == null)
            return null;

        final Snackbar snackbar = Snackbar.make(root, message, indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
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
        AlertUtil.showAlert(this, "Oops!", message);
    }

    @Override
    public void onBackPressed()
    {
        AlertUtil.showConfirmDialog(this, "Exit?", "Do you really want to exit now?\n\n" +
                "All unsaved data will be lost!", super::onBackPressed);
    }
}






























