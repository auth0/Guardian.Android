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
     * Rich Authorization Details Type
     *
     * @param type  Type key
     * @param clazz Class to cast the item
     * @return The list of types found by the provided key. If none found, returns an empty list.
     */
    <T> List<T> getAuthorizationDetails(String type, Class<T> clazz);
}
