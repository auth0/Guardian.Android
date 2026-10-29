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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.auth0.android.guardian.sdk.CurrentDevice;
import com.auth0.android.guardian.sdk.Enrollment;
import com.auth0.android.guardian.sdk.Guardian;
import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.guardian.sample.scanner.CaptureView;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class EnrollActivity extends AppCompatActivity implements CaptureView.Listener {

    private static final String TAG = EnrollActivity.class.getName();

    private static final String DEVICE_NAME = "com.auth0.guardian.sample.EnrollActivity.DEVICE_NAME";
    private static final String FCM_TOKEN = "com.auth0.guardian.sample.EnrollActivity.FCM_TOKEN";
    /**
     * Optional enrollment URI (the content of the Guardian QR code). When present the
     * camera is skipped and enrollment starts immediately, so integration tests and
     * emulators can enroll without scanning a QR code.
     */
    static final String ENROLLMENT_DATA = "com.auth0.guardian.sample.EnrollActivity.ENROLLMENT_DATA";

    private Guardian guardian;
    private String deviceName;
    private String fcmToken;
    private String enrollmentData;
    // False for the ENROLLMENT_DATA test hook and on devices without a camera (e.g. emulators
    // and some tablets): the enrollment link is entered by hand instead of scanned.
    private boolean useCamera;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    Log.i(TAG, "CAMERA permission has now been granted. Showing preview.");
                    showScanView();
                } else {
                    Log.i(TAG, "CAMERA permission was NOT granted.");
                }
            });

    private static final String STATE_LINK_DIALOG_OPEN = "linkDialogOpen";
    private static final String STATE_LINK_TEXT = "linkText";

    private View permissionLayout;
    private View scannerLayout;
    private CaptureView scanner;
    private AlertDialog linkDialog;
    private EditText linkInput;

    static Intent getStartIntent(@NonNull Context context,
                                 @NonNull String deviceName,
                                 @NonNull String fcmToken) {
        Intent intent = new Intent(context, EnrollActivity.class);
        intent.putExtra(DEVICE_NAME, deviceName);
        intent.putExtra(FCM_TOKEN, fcmToken);
        return intent;
    }

    static Intent getStartIntent(@NonNull Context context,
                                 @NonNull String deviceName,
                                 @NonNull String fcmToken,
                                 @Nullable String enrollmentData) {
        Intent intent = getStartIntent(context, deviceName, fcmToken);
        intent.putExtra(ENROLLMENT_DATA, enrollmentData);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);
        SystemBars.applyEdgeToEdge(this);

        setupGuardian();
        setupUI();
        useCamera = enrollmentData == null && hasCamera();
        if (enrollmentData == null && !useCamera) {
            showManualEntryOnly();
            if (savedInstanceState == null) {
                showEnrollmentLinkDialog(null);
            }
        } else if (enrollmentData != null) {
            // Test hook: no camera, no scanner UI. An enrollment already in flight can't be
            // resumed after a recreation (it belongs to the destroyed instance), so give up.
            scannerLayout.setVisibility(View.GONE);
            permissionLayout.setVisibility(View.GONE);
            if (savedInstanceState == null) {
                onCodeScanned(enrollmentData);
            } else {
                abortEnrollment();
            }
        } else {
            checkCameraPermission();
        }

        // The paste dialog is rebuilt after a rotation, keeping what was typed.
        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_LINK_DIALOG_OPEN)) {
            showEnrollmentLinkDialog(savedInstanceState.getString(STATE_LINK_TEXT));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (linkDialog != null && linkDialog.isShowing()) {
            outState.putBoolean(STATE_LINK_DIALOG_OPEN, true);
            outState.putString(STATE_LINK_TEXT, linkInput.getText().toString());
        }
    }

    @Override
    protected void onDestroy() {
        if (linkDialog != null) {
            linkDialog.dismiss();
            linkDialog = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (useCamera) {
            scanner.start(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (useCamera) {
            scanner.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (useCamera) {
            scanner.stop();
        }
    }

    @Override
    public void onCodeScanned(String code) {
        try {
            KeyPair keyPair = generateKeyPair();
            CurrentDevice device = new CurrentDevice(this, fcmToken, deviceName);
            guardian.enroll(code, device, keyPair)
                    .start(new DialogCallback<>(this,
                            R.string.progress_title_please_wait,
                            R.string.progress_message_enroll,
                            new Callback<Enrollment>() {
                                @Override
                                public void onSuccess(Enrollment enrollment) {
                                    Log.d(TAG, "enroll success");
                                    onEnrollSuccess(enrollment);
                                }

                                @Override
                                public void onFailure(Throwable exception) {
                                    // DialogCallback has already shown the error and the user
                                    // dismissed it. Scanning resumes on its own (onResume); with
                                    // the test hook there is nothing to retry.
                                    if (enrollmentData != null) {
                                        abortEnrollment();
                                    }
                                }
                            }));
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "enroll throw an exception", exception);
            onEnrollFailure(exception);
        }
    }

    private void setupGuardian() {
        Intent intent = getIntent();
        deviceName = intent.getStringExtra(DEVICE_NAME);
        fcmToken = intent.getStringExtra(FCM_TOKEN);
        enrollmentData = intent.getStringExtra(ENROLLMENT_DATA);

        if (deviceName == null || fcmToken == null) {
            throw new IllegalStateException("Missing deviceName or fcmToken");
        }

        guardian = SampleGuardian.create();
    }

    private void setupUI() {
        permissionLayout = findViewById(R.id.permissionLayout);
        scannerLayout = findViewById(R.id.scannerLayout);
        scanner = (CaptureView) findViewById(R.id.scanner);

        // Reachable from both the scanner and the camera permission screen, so a user who
        // denies the camera can still enroll with the link.
        findViewById(R.id.enterLinkButton)
                .setOnClickListener(v -> showEnrollmentLinkDialog(null));
        findViewById(R.id.enterLinkPermissionButton)
                .setOnClickListener(v -> showEnrollmentLinkDialog(null));

        Button requestPermissionButton = (Button) findViewById(R.id.requestPermissionButton);
        assert requestPermissionButton != null;
        requestPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermission();
            }
        });
    }

    private boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private void showManualEntryOnly() {
        scanner.setVisibility(View.GONE);
        permissionLayout.setVisibility(View.GONE);
        scannerLayout.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.scanHintText)).setText(R.string.no_camera_hint);
    }

    /**
     * Lets the user paste the enrollment link (the content of the Guardian QR code) instead of
     * scanning it. It takes the same path as a scanned code.
     */
    private void showEnrollmentLinkDialog(@Nullable String prefill) {
        if (linkDialog != null && linkDialog.isShowing()) {
            return;
        }
        EditText input = new EditText(this);
        linkInput = input;
        if (prefill != null) {
            input.setText(prefill);
        }
        input.setId(R.id.enrollmentLinkInput);
        input.setContentDescription(getString(R.string.cd_enrollment_link_input));
        input.setHint(R.string.enter_link_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);

        int padding = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input);

        linkDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.enter_link_title)
                .setMessage(R.string.enter_link_message)
                .setView(container)
                .setPositiveButton(R.string.enroll_action, (dialog, which) -> {
                    String link = input.getText().toString().trim();
                    if (!link.isEmpty()) {
                        onCodeScanned(link);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Camera permission has not been granted
            showCameraPermissionUnavailable();
            tryRequestCameraPermissionDirectly();
        } else {
            // Camera permissions is already available, show the camera preview.
            Log.i(TAG, "CAMERA permission has already been granted. Displaying camera preview.");

            // Set up QR code scanning
            showScanView();
        }
    }

    private void tryRequestCameraPermissionDirectly() {
        Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA)) {
            // The user has previously denied the permission
        } else {
            // Camera permission has not been granted yet. Request it directly.
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
    }

    private void showCameraPermissionUnavailable() {
        scannerLayout.setVisibility(View.GONE);
        permissionLayout.setVisibility(View.VISIBLE);
    }

    private void showScanView() {
        permissionLayout.setVisibility(View.GONE);
        scannerLayout.setVisibility(View.VISIBLE);
    }

    private void resumeScanning() {
        if (useCamera) {
            scanner.resume();
        }
    }

    private void abortEnrollment() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void onEnrollSuccess(Enrollment enrollment) {
        Intent data = new Intent();
        ParcelableEnrollment parcelableEnrollment = new ParcelableEnrollment(enrollment);
        // Persist here too: when this activity is started on its own (test hook) there is no
        // MainActivity result callback to do it, and pushes would be ignored afterwards.
        EnrollmentStorage.save(this, parcelableEnrollment);
        data.putExtra(Constants.ENROLLMENT, parcelableEnrollment);
        setResult(RESULT_OK, data);
        finish();
    }

    private void onEnrollFailure(final Throwable exception) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EnrollActivity.this)
                        .setTitle(R.string.alert_title_error)
                        .setMessage(exception.getMessage())
                        .setPositiveButton(
                                android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (enrollmentData != null) {
                                    abortEnrollment();
                                } else {
                                    resumeScanning();
                                }
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // at least 2048 bits!
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error generating keys", e);
        }

        return null;
    }
}
