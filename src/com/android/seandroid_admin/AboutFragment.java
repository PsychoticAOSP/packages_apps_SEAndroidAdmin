package com.android.seandroid_admin;

import android.app.Fragment;
import android.os.Bundle;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.seandroid_admin.R;

public class AboutFragment extends Fragment {

    private static String TAG = "SEAdminAboutFragment";

    static TextView about;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.config_about_fragment,
                                     container, false);

        about = (TextView)view.findViewById(R.id.help_page_intro);
        about.setText(R.string.about);

        return view;
    }
}
