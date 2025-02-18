package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

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
    private final List<Map<String, Object>> authorizationDetails;

    public GuardianRichConsentRequestedDetails(
            String audience,
            String[] scope,
            String bindingMessage,
            List<Map<String, Object>> authorizationDetails
    ) {
        this.bindingMessage = bindingMessage;
        this.audience = audience;
        this.scope = scope;
        this.authorizationDetails = authorizationDetails;
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
        return authorizationDetails;
    }

    @Override
    public <T> List<T> getAuthorizationDetails(String type, Class<T> clazz) {
        List<T> types = new ArrayList<>();

        if (authorizationDetails == null) {
            return types;
        }

        for (Map<String, Object> item : authorizationDetails) {
            if (Objects.equals(item.get("type"), type)) {
                types.add(JSON.fromJson(JSON.toJson(item), clazz));
            }
        }
        return types;
    }
}
