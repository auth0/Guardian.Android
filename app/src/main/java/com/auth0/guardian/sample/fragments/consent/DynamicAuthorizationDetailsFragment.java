package com.auth0.guardian.sample.fragments.consent;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.auth0.guardian.sample.R;

import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DynamicAuthorizationDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DynamicAuthorizationDetailsFragment extends Fragment {

    private static final String BINDING_MESSAGE = "binding_message";
    private static final String DATE = "date";
    private static final String AUTHORIZATION_DETAILS = "authorization_details";

    private String bindingMessage;
    private String date;
    private Bundle authorizationDetails;

    private LinearLayout layout;
    private TextView bindingMessageText;
    private TextView dateText;

    public DynamicAuthorizationDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of the fragment.
     *
     * @param bindingMessage       Binding message.
     * @param date                 Date.
     * @param authorizationDetails Authorization Details Item.
     * @return A new instance of fragment DynamicAuthorizationDetailsView.
     */
    public static DynamicAuthorizationDetailsFragment newInstance(String bindingMessage, String date, Map<String, Object> authorizationDetails) {
        DynamicAuthorizationDetailsFragment fragment = new DynamicAuthorizationDetailsFragment();
        Bundle args = new Bundle();
        args.putString(BINDING_MESSAGE, bindingMessage);
        args.putString(DATE, date);
        args.putBundle(AUTHORIZATION_DETAILS, convertAuthorizationDetailsToBundle(authorizationDetails));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bindingMessage = getArguments().getString(BINDING_MESSAGE);
            date = getArguments().getString(DATE);
            authorizationDetails = getArguments().getBundle(AUTHORIZATION_DETAILS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dynamic_authorization_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layout = view.findViewById(R.id.dynamicAuthorizationDetailsLayout);
        bindingMessageText = view.findViewById(R.id.bindingMessage);
        dateText = view.findViewById(R.id.dateText);

        updateUI();
    }

    private static Bundle convertAuthorizationDetailsToBundle(Map<String, Object> authorizationDetails) {
        Bundle bundle = new Bundle();
        for (String key : authorizationDetails.keySet()) {
            Object value = authorizationDetails.get(key);
            // For simplicity we don't search nested objects or arrays in this example
            if (key.equals("type") || value == null || value.getClass().isArray() || value instanceof Map) {
                continue;
            }
            bundle.putString(key, String.valueOf(value));
        }
        return bundle;
    }

    private void updateUI() {
        bindingMessageText.setText(bindingMessage);
        dateText.setText(date);

        layout.removeAllViews();
        for (String key : authorizationDetails.keySet()) {
            drawDetail(key.replace("_", " "), authorizationDetails.getString(key));
        }
    }

    private void drawDetail(String label, String value) {
        LinearLayout row = createDetailRow();

        TextView labelText = new TextView(new ContextThemeWrapper(this.getActivity(), R.style.Label_Header));
        labelText.setText(label);

        TextView valueText = new TextView(new ContextThemeWrapper(this.getActivity(), R.style.Label_Small));
        valueText.setText(value);

        row.addView(labelText);
        row.addView(valueText);

        this.layout.addView(row);
    }

    private LinearLayout createDetailRow() {

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.bottomMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        LinearLayout row = new LinearLayout(this.getActivity());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(layoutParams);

        return row;
    }
}