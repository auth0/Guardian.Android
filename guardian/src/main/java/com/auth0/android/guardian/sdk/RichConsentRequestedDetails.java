package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

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

    /**
     * Rich Authorization Details
     */
    List<Map<String, Object>> getAuthorizationDetails();

    /**
     * Authorization Details Type
     * @param type
     * @param clazz
     * @return
     */
    <T> List<T> getAuthorizationDetails(String type, Class<T> clazz);
}
