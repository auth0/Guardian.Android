/*
 * Copyright (c) 2016 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.auth0.guardian.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.auth0.android.guardian.sdk.Guardian;
import com.auth0.android.guardian.sdk.GuardianException;
import com.auth0.android.guardian.sdk.ParcelableNotification;
import com.auth0.android.guardian.sdk.RichConsent;
import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.guardian.sample.events.GuardianNotificationReceivedEvent;
import com.auth0.guardian.sample.fcm.FcmUtils;
import com.auth0.guardian.sample.views.TOTPCodeView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MainActivity extends AppCompatActivity implements FcmUtils.FcmTokenListener {

    private static final String TAG = MainActivity.class.getName();

    private static final int ENROLL_REQUEST = 123;

    private View loadingView;
    private View enrollView;
    private View accountView;
    private TextView deviceNameText;
    private TextView fcmTokenText;
    private TextView userText;
    private TOTPCodeView otpView;

    private EventBus eventBus;
    private Guardian guardian;
    private ParcelableEnrollment enrollment;
    private String fcmToken;

    public static Intent getStartIntent(@NonNull Context context,
                                        @NonNull ParcelableNotification notification) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(Constants.NOTIFICATION, notification);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();

        eventBus = EventBus.getDefault();
        eventBus.register(this);

        guardian = new Guardian.Builder()
                .url(Uri.parse(getString(R.string.tenant_url)))
                .enableLogging()
                .build();

        /*
         * The following fetch token call is NOT required in a production app
         * as the registration token is generated automatically by the Firebase SDK.
         * This is just here for display purposes on this Activity's layout.
         *
         * See: https://developers.google.com/cloud-messaging/android/android-migrate-iid-service
         */
        FcmUtils fcmUtils = new FcmUtils();
        fcmUtils.fetchFcmToken(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String enrollmentJSON = sharedPreferences.getString(Constants.ENROLLMENT, null);
        if (enrollmentJSON != null) {
            enrollment = ParcelableEnrollment.fromJSON(enrollmentJSON);
            updateUI();

            ParcelableNotification notification = getIntent().getParcelableExtra(Constants.NOTIFICATION);
            if (notification != null) {
                onPushNotificationReceived(notification);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        eventBus.unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENROLL_REQUEST) {
            if (resultCode == RESULT_OK) {
                ParcelableEnrollment enrollment = data.getParcelableExtra(Constants.ENROLLMENT);
                updateEnrollment(enrollment);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupUI() {
        loadingView = findViewById(R.id.loadingLayout);
        enrollView = findViewById(R.id.enrollLayout);
        accountView = findViewById(R.id.accountLayout);
        deviceNameText = (TextView) findViewById(R.id.deviceNameText);
        fcmTokenText = (TextView) findViewById(R.id.fcmTokenText);
        userText = (TextView) findViewById(R.id.userText);
        otpView = (TOTPCodeView) findViewById(R.id.otpView);

        deviceNameText.setText(Build.MODEL);

        Button enrollButton = (Button) findViewById(R.id.enrollButton);
        assert enrollButton != null;
        enrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnrollRequested();
            }
        });

        Button unenrollButton = (Button) findViewById(R.id.unenrollButton);
        assert unenrollButton != null;
        unenrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUnEnrollRequested();
            }
        });
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingView.setVisibility(fcmToken != null ? View.GONE : View.VISIBLE);
                if (enrollment == null) {
                    fcmTokenText.setText(fcmToken);
                    accountView.setVisibility(View.GONE);
                    enrollView.setVisibility(fcmToken != null ? View.VISIBLE : View.GONE);
                } else {
                    userText.setText(enrollment.getUserId());
                    otpView.setEnrollment(enrollment);
                    enrollView.setVisibility(View.GONE);
                    accountView.setVisibility(fcmToken != null ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private void onEnrollRequested() {
        Intent enrollIntent = EnrollActivity
                .getStartIntent(this, deviceNameText.getText().toString(), fcmToken);
        startActivityForResult(enrollIntent, ENROLL_REQUEST);
    }

    private void onUnEnrollRequested() {
        guardian.delete(enrollment)
                .start(new DialogCallback<>(this,
                        R.string.progress_title_please_wait,
                        R.string.progress_message_unenroll,
                        new Callback<Void>() {
                            @Override
                            public void onSuccess(Void response) {
                                updateEnrollment(null);
                            }

                            @Override
                            public void onFailure(Throwable exception) {
                                if (exception instanceof GuardianException) {
                                    GuardianException guardianException = (GuardianException) exception;
                                    if (guardianException.isEnrollmentNotFound()) {
                                        // the enrollment doesn't exist on the server
                                        updateEnrollment(null);
                                    }
                                }
                            }
                        }));
    }

    private void updateEnrollment(ParcelableEnrollment enrollment) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.ENROLLMENT, enrollment != null ? enrollment.toJSON() : null);
        editor.apply();

        this.enrollment = enrollment;

        updateUI();
    }

    private void onPushNotificationReceived(ParcelableNotification notification) {
        Context context = this;
        Intent standardNotificationActivityIntent = NotificationActivity.getStartIntent(context, notification, enrollment);

        if (notification.getTransactionLinkingId() == null) {
            startActivity(standardNotificationActivityIntent);
        } else {
            try {
                guardian.fetchConsent(notification, enrollment).start(new Callback<RichConsent>() {
                    @Override
                    public void onSuccess(RichConsent consent) {
                        Intent intent = NotificationWithConsentDetailsActivity.getStartIntent(
                                context,
                                notification,
                                enrollment,
                                new ParcelableRichConsent(consent)
                        );
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                        if (exception instanceof  GuardianException) {
                            GuardianException guardianException = (GuardianException) exception;
                            if (guardianException.isResourceNotFound()) {
                                startActivity(standardNotificationActivityIntent);
                            }
                        }
                        Log.e(TAG, "Error obtaining consent details", exception);

                    }
                });
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                Log.e(TAG, "Error requesting consent details", e);
            }
        }
    }

    @Override
    public void onFcmTokenObtained(String fcmToken) {
        this.fcmToken = fcmToken;

        updateUI();
    }

    @Override
    public void onFcmFailure(Throwable exception) {
        Log.e(TAG, "Error obtaining FCM token", exception);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_title_error)
                .setMessage(getString(R.string.alert_message_fcm_error))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGuardianNotificationReceived(GuardianNotificationReceivedEvent event) {
        onPushNotificationReceived(event.getData());
    }
}
