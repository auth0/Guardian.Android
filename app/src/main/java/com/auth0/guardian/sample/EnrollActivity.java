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
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

    private static final int REQUEST_CAMERA = 55;

    private Guardian guardian;
    private String deviceName;
    private String fcmToken;

    private View permissionLayout;
    private View scannerLayout;
    private CaptureView scanner;

    static Intent getStartIntent(@NonNull Context context,
                                 @NonNull String deviceName,
                                 @NonNull String fcmToken) {
        Intent intent = new Intent(context, EnrollActivity.class);
        intent.putExtra(DEVICE_NAME, deviceName);
        intent.putExtra(FCM_TOKEN, fcmToken);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);

        setupGuardian();
        setupUI();
        checkCameraPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        scanner.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        scanner.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            // Received permission result for camera permission.
            Log.i(TAG, "Received response for Camera permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Log.i(TAG, "CAMERA permission has now been granted. Showing preview.");
                // Set up QR code scanning
                showScanView();
            } else {
                Log.i(TAG, "CAMERA permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onCodeScanned(String enrollmentData) {
        try {
            KeyPair keyPair = generateKeyPair();
            CurrentDevice device = new CurrentDevice(this, fcmToken, deviceName);
            guardian.enroll(enrollmentData, device, keyPair)
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
                                    // TODO might not need here
                                    // It looks like the toast resumes scanning as well
                                    // This resume happens as the error is popped up,
                                    // wait to dismiss instead
                                    // resumeScanning();
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

        if (deviceName == null || fcmToken == null) {
            throw new IllegalStateException("Missing deviceName or fcmToken");
        }

        guardian = new Guardian.Builder()
                .url(Uri.parse(getString(R.string.tenant_url)))
                .enableLogging()
                .build();
    }

    private void setupUI() {
        permissionLayout = findViewById(R.id.permissionLayout);
        scannerLayout = findViewById(R.id.scannerLayout);
        scanner = (CaptureView) findViewById(R.id.scanner);

        Button requestPermissionButton = (Button) findViewById(R.id.requestPermissionButton);
        assert requestPermissionButton != null;
        requestPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermission();
            }
        });
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
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.CAMERA},
                REQUEST_CAMERA);
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
        scanner.resume();
    }

    private void onEnrollSuccess(Enrollment enrollment) {
        Intent data = new Intent();
        ParcelableEnrollment parcelableEnrollment = new ParcelableEnrollment(enrollment);
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
                                resumeScanning();
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
