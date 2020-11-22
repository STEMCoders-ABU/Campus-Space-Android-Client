package ng.com.stemcoders.campusspace.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import ng.com.stemcoders.campusspace.MainActivity;
import ng.com.stemcoders.campusspace.R;

public class AboutFragment extends Fragment
{
    MaterialButton btnVisitCS, btnVisitStem;

    public AboutFragment() {}

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
        View root = inflater.inflate(R.layout.fragment_about, container, false);

        btnVisitCS = root.findViewById(R.id.btnVisitCS);
        btnVisitCS.setOnClickListener(v ->
        {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.cs_link)));
            startActivity(intent);
        });

        btnVisitStem = root.findViewById(R.id.btnVisitStem);
        btnVisitStem.setOnClickListener(v ->
        {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.stem_link)));
            startActivity(intent);
        });

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backPressed(MainActivity.BackPressedEvent backPressedEvent)
    {
        EventBus.getDefault().post(new MainActivity.OpenDrawerEvent());
    }
}




















