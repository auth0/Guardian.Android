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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.auth0.android.guardian.sdk.Guardian;
import com.auth0.android.guardian.sdk.GuardianException;
import com.auth0.android.guardian.sdk.ParcelableNotification;
import com.auth0.android.guardian.sdk.RichConsent;
import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.guardian.sample.fragments.AuthenticationRequestDetailsFragment;
import com.auth0.guardian.sample.fragments.consent.ConsentBasicDetailsFragment;
import com.auth0.guardian.sample.fragments.consent.ConsentPaymentInitiationFragment;
import com.auth0.guardian.sample.fragments.consent.DynamicAuthorizationDetailsFragment;
import com.auth0.guardian.sample.payments.PaymentInitiationDetails;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = NotificationActivity.class.getName();

    private Guardian guardian;
    private ParcelableEnrollment enrollment;
    private ParcelableNotification notification;

    private RichConsent richConsent;

    static Intent getStartIntent(@NonNull Context context,
                                 @NonNull ParcelableNotification notification,
                                 @NonNull ParcelableEnrollment enrollment) {
        if (!enrollment.getId().equals(notification.getEnrollmentId())) {
            final String message = String.format("Notification doesn't match enrollment (%s != %s)",
                    notification.getEnrollmentId(), enrollment.getId());
            throw new IllegalArgumentException(message);
        }

        Intent intent = new Intent(context, NotificationActivity.class);
        intent.putExtra(Constants.ENROLLMENT, enrollment);
        intent.putExtra(Constants.NOTIFICATION, notification);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        guardian = new Guardian.Builder()
                .url(Uri.parse(getString(R.string.tenant_url)))
                .enableLogging()
                .build();

        Intent intent = getIntent();
        enrollment = intent.getParcelableExtra(Constants.ENROLLMENT);
        notification = intent.getParcelableExtra(Constants.NOTIFICATION);

        setupUI();

        if (notification.getTransactionLinkingId() != null) {
            try {
                guardian.fetchConsent(notification, enrollment).start(new Callback<RichConsent>() {
                    @Override
                    public void onSuccess(RichConsent response) {
                        richConsent = response;
                        updateUI();
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                        if (exception instanceof GuardianException) {
                            GuardianException guardianException = (GuardianException) exception;
                            if (guardianException.isResourceNotFound()) {
                                // Render regular authentication request details
                                updateUI();
                            }
                        } else {
                            Log.e(TAG, "Error requesting consent details", exception);
                        }
                    }
                });
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        updateUI();
    }

    private void setupUI() {
        // TODO: spinner fragment
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
        Fragment fragment;
        if (richConsent == null) {
            fragment = AuthenticationRequestDetailsFragment.newInstance(
                    enrollment.getUserId(),

                    String.format("%s, %s",
                            notification.getBrowserName(),
                            notification.getBrowserVersion()),
                    String.format("%s, %s",
                            notification.getOsName(),
                            notification.getOsVersion()),

                    notification.getLocation(),
                    notification.getDate().toString()
            );
        } else {
            if (richConsent.getRequestedDetails().getAuthorizationDetails().isEmpty()) {
                fragment = ConsentBasicDetailsFragment.newInstance(
                        richConsent.getRequestedDetails().getBindingMessage(),
                        richConsent.getRequestedDetails().getScope(),
                        notification.getDate().toString()
                );
            } else {
                List<PaymentInitiationDetails> paymentInitiationDetailsList = richConsent
                        .getRequestedDetails()
                        .filterAuthorizationDetailsByType(PaymentInitiationDetails.class);
                if (!paymentInitiationDetailsList.isEmpty()) {
                    PaymentInitiationDetails paymentDetails = paymentInitiationDetailsList.get(0);
                    fragment = ConsentPaymentInitiationFragment.newInstance(
                            richConsent.getRequestedDetails().getBindingMessage(),
                            paymentDetails.getRemittanceInformation(),
                            paymentDetails.getCreditorAccount().getAccountNumber(),
                            paymentDetails.getInstructedAmount().getCurrency(),
                            paymentDetails.getInstructedAmount().getAmount()
                    );
                } else {
                    fragment = DynamicAuthorizationDetailsFragment.newInstance(
                            richConsent.getRequestedDetails().getBindingMessage(),
                            notification.getDate().toString(),
                            // For simplicity, in this example we render one single type
                            richConsent.getRequestedDetails().getAuthorizationDetails().get(0)
                    );
                }
            }
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.authenticationDetailsFragmentContainer, fragment)
                .commit();

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
