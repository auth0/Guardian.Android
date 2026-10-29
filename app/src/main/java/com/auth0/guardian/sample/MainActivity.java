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

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

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

    private final ActivityResultLauncher<Intent> enrollLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ParcelableEnrollment enrollment = IntentCompat.getParcelableExtra(
                            result.getData(), Constants.ENROLLMENT, ParcelableEnrollment.class);
                    updateEnrollment(enrollment);
                }
            });

    // Denying is not fatal: notifications that arrive while the app is visible are
    // delivered through EventBus and don't need the permission.
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> Log.i(TAG, "POST_NOTIFICATIONS granted: " + granted));

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
        SystemBars.applyEdgeToEdge(this);

        setupUI();
        requestNotificationPermission();

        eventBus = EventBus.getDefault();

        guardian = SampleGuardian.create();

        /*
         * The following fetch token call is NOT required in a production app
         * as the registration token is generated automatically by the Firebase SDK.
         * This is just here for display purposes on this Activity's layout.
         *
         * See: https://developers.google.com/cloud-messaging/android/android-migrate-iid-service
         */
        FcmUtils fcmUtils = new FcmUtils();
        fcmUtils.fetchFcmToken(this);

        enrollment = EnrollmentStorage.load(this);
        if (enrollment != null) {
            updateUI();

            // Only on the first creation: after a rotation the intent still carries the
            // notification and the request screen would open a second time.
            ParcelableNotification notification = IntentCompat.getParcelableExtra(
                    getIntent(), Constants.NOTIFICATION, ParcelableNotification.class);
            if (notification != null && savedInstanceState == null) {
                onPushNotificationReceived(notification);
            }
        }
    }

    // Subscribed only while visible. FcmListenerService posts to EventBus whenever there is a
    // subscriber and shows a system notification otherwise. Staying subscribed while in the
    // background would swallow the push, because Android 10+ blocks starting
    // NotificationActivity from the background.
    @Override
    protected void onStart() {
        super.onStart();
        // Pick up an enrollment saved while this activity was stopped (EnrollActivity
        // persists it itself, and a rotation during enrollment loses the activity result).
        enrollment = EnrollmentStorage.load(this);
        updateUI();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        eventBus.unregister(this);
        super.onStop();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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
        enrollLauncher.launch(enrollIntent);
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
        EnrollmentStorage.save(this, enrollment);

        this.enrollment = enrollment;

        updateUI();
    }

    private void onPushNotificationReceived(ParcelableNotification notification) {
        if (enrollment == null) {
            Log.w(TAG, "Received a notification but the device is not enrolled, ignoring it");
            return;
        }
        try {
            startActivity(NotificationActivity.getStartIntent(this, notification, enrollment));
        } catch (IllegalArgumentException e) {
            // e.g. a stale notification for an enrollment that has since been replaced
            Log.w(TAG, "Ignoring a notification that doesn't match the current enrollment", e);
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
