package com.auth0.android.guardian.sdk;

public interface RichConsent {
    String getId();

    RichConsentRequestedDetails getRequestedDetails();

    String getCreatedAt();

    String getExpiresAt();
}
