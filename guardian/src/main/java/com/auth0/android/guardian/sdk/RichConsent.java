package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

public interface RichConsent {
    /**
     * Consent record id
     */
    @NonNull
    String getId();

    /**
     * Requested details
     */
    @NonNull
    RichConsentRequestedDetails getRequestedDetails();

    /**
     * When the consent was created
     */
    @NonNull
    String getCreatedAt();

    /**
     * When the consent expires
     */
    @NonNull
    String getExpiresAt();
}
