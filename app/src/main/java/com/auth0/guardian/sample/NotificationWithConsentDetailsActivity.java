package com.auth0.guardian.sample;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.auth0.android.guardian.sdk.Guardian;
import com.auth0.android.guardian.sdk.ParcelableNotification;
import com.auth0.android.guardian.sdk.RichConsent;
import com.auth0.android.guardian.sdk.networking.Callback;

public class NotificationWithConsentDetailsActivity extends AppCompatActivity {

    private TextView bindingMessageText;
    private TextView scopeText;
    private TextView dateText;

    private Guardian guardian;
    private ParcelableEnrollment enrollment;
    private ParcelableNotification notification;
    private RichConsent consentDetails;

    static Intent getStartIntent(@NonNull Context context,
                                 @NonNull ParcelableNotification notification,
                                 @NonNull ParcelableEnrollment enrollment,
                                 @NonNull  ParcelableRichConsent consent) {
        if (!enrollment.getId().equals(notification.getEnrollmentId())) {
            final String message = String.format("Notification doesn't match enrollment (%s != %s)",
                    notification.getEnrollmentId(), enrollment.getId());
            throw new IllegalArgumentException(message);
        }

        Intent intent = new Intent(context, NotificationWithConsentDetailsActivity.class);
        intent.putExtra(Constants.ENROLLMENT, enrollment);
        intent.putExtra(Constants.NOTIFICATION, notification);
        intent.putExtra(Constants.CONSENT, consent);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_with_consent_details);

        guardian = new Guardian.Builder()
                .url(Uri.parse(getString(R.string.guardian_url)))
                .enableLogging()
                .build();

        Intent intent = getIntent();
        enrollment = intent.getParcelableExtra(Constants.ENROLLMENT);
        notification = intent.getParcelableExtra(Constants.NOTIFICATION);
        consentDetails = intent.getParcelableExtra(Constants.CONSENT);

        setupUI();
        updateUI();
    }

    private void setupUI() {
        bindingMessageText = (TextView) findViewById(R.id.bindingMessage);
        scopeText = (TextView) findViewById(R.id.scope);
        dateText = (TextView) findViewById(R.id.dateText);

        Button rejectButton = (Button) findViewById(R.id.rejectButton);
        assert rejectButton != null;
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectRequested();
            }
        });

        Button allowButton = (Button) findViewById(R.id.allowButton);
        assert allowButton != null;
        allowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allowRequested();
            }
        });
    }

    private void updateUI() {
        if (consentDetails != null) {
            bindingMessageText.setText(consentDetails.getRequestedDetails().getBindingMessage());
            scopeText.setText(String.join(", ", consentDetails.getRequestedDetails().getScope()));
        } else {
            bindingMessageText.setText("N/A");
            scopeText.setText("N/A");
        }
        dateText.setText(notification.getDate().toString());
    }

    private void rejectRequested() {
        guardian
                .reject(notification, enrollment)
                .start(new DialogCallback<>(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_reject,
                        new Callback<Void>() {
                            @Override
                            public void onSuccess(Void response) {
                                finish();
                            }

                            @Override
                            public void onFailure(Throwable exception) {

                            }
                        }));
    }

    private void allowRequested() {
        guardian
                .allow(notification, enrollment)
                .start(new DialogCallback<>(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_allow,
                        new Callback<Void>() {
                            @Override
                            public void onSuccess(Void response) {
                                finish();
                            }

                            @Override
                            public void onFailure(Throwable exception) {

                            }
                        }));
    }
}
