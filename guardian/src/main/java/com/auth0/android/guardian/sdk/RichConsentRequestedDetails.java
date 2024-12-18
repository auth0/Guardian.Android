package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

public interface RichConsentRequestedDetails {
    /**
     * Requested audience
     */
    @NonNull
    String getAudience();

    /**
     * Requested scopes
     */
    @NonNull
    String[] getScope();

    /**
     * CIBA binding message
     */
    String getBindingMessage();
}

