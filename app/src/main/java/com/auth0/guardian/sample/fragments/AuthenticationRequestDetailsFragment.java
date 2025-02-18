package com.auth0.guardian.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.auth0.guardian.sample.R;

import org.w3c.dom.Text;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AuthenticationRequestDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AuthenticationRequestDetailsFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String USER = "user";
    private static final String BROWSER = "browser";
    private static final String OS = "os";
    private static final String LOCATION = "location";
    private static final String DATE = "date";

    private TextView userText;
    private TextView browserText;
    private TextView osText;
    private TextView locationText;
    private TextView dateText;

    private String user;
    private String browser;
    private String os;
    private String location;
    private String date;

    public AuthenticationRequestDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param user User text
     * @param browser Browser text
     * @param os OS text
     * @param location Location text
     * @param date Date text
     *
     * @return A new instance of fragment AuthenticationRequestDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AuthenticationRequestDetailsFragment newInstance(String user,
                                                                   String browser,
                                                                   String os,
                                                                   String location,
                                                                   String date
    ) {
        AuthenticationRequestDetailsFragment fragment = new AuthenticationRequestDetailsFragment();
        Bundle args = new Bundle();
        args.putString(USER, user);
        args.putString(BROWSER, browser);
        args.putString(OS, os);
        args.putString(LOCATION, location);
        args.putString(DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = getArguments().getString(USER);
            browser = getArguments().getString(BROWSER);
            os = getArguments().getString(OS);
            location = getArguments().getString(LOCATION);
            date = getArguments().getString(DATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_authentication_request_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userText = view.findViewById(R.id.userText);
        browserText = view.findViewById(R.id.browserText);
        osText = view.findViewById(R.id.osText);
        locationText = view.findViewById(R.id.locationText);
        dateText = view.findViewById(R.id.dateText);

        updateUI();
    }

    private void updateUI() {
        userText.setText(user);
        browserText.setText(browser);
        osText.setText(os);
        locationText.setText(location);
        dateText.setText(date);
    }
}