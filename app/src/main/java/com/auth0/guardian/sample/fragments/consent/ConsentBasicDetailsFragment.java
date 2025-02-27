package com.auth0.guardian.sample.fragments.consent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.auth0.guardian.sample.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConsentBasicDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConsentBasicDetailsFragment extends Fragment {

    private static final String BINDING_MESSAGE = "binding_message";
    private static final String SCOPE = "scope";
    private static final String DATE = "date";

    private TextView bindingMessageText;
    private TextView scopeText;
    private TextView dateText;

    private String bindingMessage;
    private String[] scope;
    private String date;

    public ConsentBasicDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bindingMessage Binding message.
     * @param scope          Scope.
     * @param date           Date.
     * @return A new instance of fragment ConsentBasicDetailsFragment.
     */
    public static ConsentBasicDetailsFragment newInstance(String bindingMessage, String[] scope, String date) {
        ConsentBasicDetailsFragment fragment = new ConsentBasicDetailsFragment();
        Bundle args = new Bundle();
        args.putString(BINDING_MESSAGE, bindingMessage);
        args.putStringArray(SCOPE, scope);
        args.putString(DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bindingMessage = getArguments().getString(BINDING_MESSAGE);
            scope = getArguments().getStringArray(SCOPE);
            date = getArguments().getString(DATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_consent_basic_details, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI(view);
        updateUI();
    }

    private void setupUI(@NonNull View view) {
        bindingMessageText = (TextView) view.findViewById(R.id.bindingMessage);
        scopeText = (TextView) view.findViewById(R.id.scope);
        dateText = (TextView) view.findViewById(R.id.dateText);
    }

    private void updateUI() {
        bindingMessageText.setText(bindingMessage);
        scopeText.setText(String.join(", ", scope));
        dateText.setText(date);
    }
}