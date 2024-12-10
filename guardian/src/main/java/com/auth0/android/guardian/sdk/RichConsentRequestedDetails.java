package com.auth0.android.guardian.sdk;

public interface RichConsentRequestedDetails {
    String getAudience();
    String[] getScope();
    String getBindingMessage();
}

