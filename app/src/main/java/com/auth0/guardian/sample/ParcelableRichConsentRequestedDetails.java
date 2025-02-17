package com.auth0.guardian.sample;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.auth0.android.guardian.sdk.RichConsentRequestedDetails;
import com.auth0.guardian.sample.parcel.utils.ParcelableEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParcelableRichConsentRequestedDetails implements RichConsentRequestedDetails, Parcelable {
    public static final Creator<ParcelableRichConsentRequestedDetails> CREATOR = new Creator<ParcelableRichConsentRequestedDetails>() {
        @Override
        public ParcelableRichConsentRequestedDetails createFromParcel(Parcel in) {
            return new ParcelableRichConsentRequestedDetails(in);
        }

        @Override
        public ParcelableRichConsentRequestedDetails[] newArray(int size) {
            return new ParcelableRichConsentRequestedDetails[size];
        }
    };
    private static final Gson JSON = new GsonBuilder().create();

    @SerializedName("audience")
    private final String audience;
    @SerializedName("scope")
    private final String[] scope;
    @SerializedName("bindingMessage")
    private final String bindingMessage;
    @SerializedName("authorizationDetails")
    private List<Map<String, Object>> authorizationDetails;

    public ParcelableRichConsentRequestedDetails(RichConsentRequestedDetails requestedDetails) {
        audience = requestedDetails.getAudience();
        scope = requestedDetails.getScope();
        bindingMessage = requestedDetails.getBindingMessage();
        authorizationDetails = requestedDetails.getAuthorizationDetails();
    }

    protected ParcelableRichConsentRequestedDetails(Parcel in) {
        audience = in.readString();
        scope = in.createStringArray();
        bindingMessage = in.readString();

        List<ParcelableEntry> authorizationDetailsEntries = new ArrayList<>();
        in.readList(authorizationDetailsEntries, ParcelableEntry.class.getClassLoader());
        if (!authorizationDetailsEntries.isEmpty()) {
            authorizationDetails = new ArrayList<>();
            for (ParcelableEntry entry : authorizationDetailsEntries) {
                Map<String, Object> detail = new HashMap<>();
                detail.put(entry.getKey(), entry.getValue());
                authorizationDetails.add(detail);
            }
        }
    }

    public static ParcelableEnrollment fromJSON(String json) {
        return JSON.fromJson(json, ParcelableEnrollment.class);
    }

    public String toJSON() {
        return JSON.toJson(this);
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
        for (Map<String, Object> item : authorizationDetails) {
            if (Objects.equals(item.get("type"), type)) {
                types.add(JSON.fromJson(JSON.toJsonTree(item), clazz));
            }
        }
        return types;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(audience);
        dest.writeStringArray(scope);
        dest.writeString(bindingMessage);
        dest.writeList(authorizationDetails);
    }
}
