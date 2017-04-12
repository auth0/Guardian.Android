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
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.auth0.android.guardian.sdk.Guardian;
import com.auth0.android.guardian.sdk.GuardianException;
import com.auth0.android.guardian.sdk.ParcelableNotification;
import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.guardian.sample.events.GuardianNotificationReceivedEvent;
import com.auth0.guardian.sample.gcm.GcmUtils;
import com.auth0.guardian.sample.views.TOTPCodeView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity implements GcmUtils.GcmTokenListener {

    private static final String TAG = MainActivity.class.getName();

    private static final int ENROLL_REQUEST = 123;

    private View loadingView;
    private View enrollView;
    private View accountView;
    private TextView deviceNameText;
    private TextView gcmTokenText;
    private TextView userText;
    private TOTPCodeView otpView;

    private EventBus eventBus;
    private Guardian guardian;
    private ParcelableEnrollment enrollment;
    private String gcmToken;

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
                .url(Uri.parse(getString(R.string.guardian_url)))
                .enableLogging()
                .build();

        GcmUtils gcmUtils = new GcmUtils(this, getString(R.string.google_app_id));
        gcmUtils.fetchGcmToken(this);

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
        gcmTokenText = (TextView) findViewById(R.id.gcmTokenText);
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
                loadingView.setVisibility(gcmToken != null ? View.GONE : View.VISIBLE);
                if (enrollment == null) {
                    gcmTokenText.setText(gcmToken != null ? gcmToken : null);
                    accountView.setVisibility(View.GONE);
                    enrollView.setVisibility(gcmToken != null ? View.VISIBLE : View.GONE);
                } else {
                    userText.setText(enrollment.getUserId());
                    otpView.setEnrollment(enrollment);
                    enrollView.setVisibility(View.GONE);
                    accountView.setVisibility(gcmToken != null ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private void onEnrollRequested() {
        Intent enrollIntent = EnrollActivity
                .getStartIntent(this, deviceNameText.getText().toString(), gcmToken);
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
        Intent intent = NotificationActivity
                .getStartIntent(this, notification, enrollment);
        startActivity(intent);
    }

    @Override
    public void onGcmTokenObtained(String gcmToken) {
        this.gcmToken = gcmToken;

        updateUI();
    }

    @Override
    public void onGcmFailure(Throwable exception) {
        Log.e(TAG, "Error obtaining GCM token", exception);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_title_error)
                .setMessage(getString(R.string.alert_message_gcm_error))
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
