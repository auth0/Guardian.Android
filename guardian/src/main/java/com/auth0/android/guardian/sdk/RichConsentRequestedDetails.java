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
     * Rich Authorization Details ([RFC 9396](https://datatracker.ietf.org/doc/html/rfc9396)).
     * If the consent record did not include authorization details, it returns `null`.
     */
    List<Map<String, Object>> getAuthorizationDetails();

    /**
     * Rich Authorization Details ([RFC 9396](https://datatracker.ietf.org/doc/html/rfc9396)) filtered
     * by the provided type key and converted to the provided class.
     *
     * @param type  Type key.
     * @param clazz Class to convert the item into.
     * @return The list of types found by the provided key. If none found, returns an empty list.
     */
    <T> List<T> getAuthorizationDetails(String type, Class<T> clazz);
}
