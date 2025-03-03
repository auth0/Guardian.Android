package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

import com.auth0.android.guardian.sdk.annotations.AuthorizationDetailsType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuardianRichConsentRequestedDetails implements RichConsentRequestedDetails {
    private static final Gson JSON = new GsonBuilder().create();

    private final String audience;
    private final String[] scope;
    @SerializedName("binding_message")
    private final String bindingMessage;
    @SerializedName("authorization_details")
    private final List<JsonObject> rawAuthorizationDetails;
    private transient List<Map<String, Object>> authorizationDetails;

    public GuardianRichConsentRequestedDetails(
            String audience,
            String[] scope,
            String bindingMessage,
            List<JsonObject> rawAuthorizationDetails
    ) {
        this.bindingMessage = bindingMessage;
        this.audience = audience;
        this.scope = scope;
        this.rawAuthorizationDetails = rawAuthorizationDetails;
    }

    @NonNull
    @Override
    public String getAudience() {
        return audience;
    }

    @NonNull
    @Override
    public String[] getScope() {
        return scope;
    }

    @Override
    public String getBindingMessage() {
        return bindingMessage;
    }

    @Override
    public List<Map<String, Object>> getAuthorizationDetails() {
        if (rawAuthorizationDetails == null) {
            return List.of();
        }

        if (authorizationDetails != null) {
            return authorizationDetails;
        }

        authorizationDetails = new ArrayList<>();
        final Type type = new TypeToken<Map<String, Object>>() {
        }.getType();

        for (JsonObject item : rawAuthorizationDetails) {
            authorizationDetails.add(JSON.fromJson(item, type));
        }
        return authorizationDetails;
    }

    @Override
    public <T> List<T> filterAuthorizationDetailsByType(@NonNull Class<T> clazz) {
        List<T> types = new ArrayList<>();

        if (!clazz.isAnnotationPresent(AuthorizationDetailsType.class)) {
            throw new GuardianException(clazz.getName() +
                    " must be annotated with @AuthorizationDetailsType to used as authorization details item type.");
        }

        if (rawAuthorizationDetails == null) {
            return types;
        }

        for (JsonObject item : rawAuthorizationDetails) {
            String type = Objects.requireNonNull(clazz.getAnnotation(AuthorizationDetailsType.class)).value();
            if (Objects.equals(item.get("type").getAsString(), type)) {
                types.add(JSON.fromJson(item, clazz));
            }
        }
        return types;
    }
}
