package com.auth0.guardian.sample;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.auth0.android.guardian.sdk.RichConsent;
import com.auth0.android.guardian.sdk.RichConsentRequestedDetails;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class ParcelableRichConsent implements RichConsent, Parcelable {
    public static final Creator<ParcelableRichConsent> CREATOR = new Creator<ParcelableRichConsent>() {
        @Override
        public ParcelableRichConsent createFromParcel(Parcel in) {
            return new ParcelableRichConsent(in);
        }

        @Override
        public ParcelableRichConsent[] newArray(int size) {
            return new ParcelableRichConsent[size];
        }
    };

    private static final Gson JSON = new GsonBuilder().create();

    @SerializedName("id")
    private final String id;
    @SerializedName("requested_details")
    private final ParcelableRichConsentRequestedDetails requestedDetails;
    @SerializedName("created_at")
    private final String createdAt;
    @SerializedName("expires_at")
    private final String expiresAt;

    public ParcelableRichConsent(RichConsent richConsent) {
        this.id = richConsent.getId();
        this.createdAt = this.getCreatedAt();
        this.expiresAt = this.getExpiresAt();
        this.requestedDetails = new ParcelableRichConsentRequestedDetails(richConsent.getRequestedDetails());
    }

    protected ParcelableRichConsent(Parcel in) {
        id = in.readString();
        createdAt = in.readString();
        expiresAt = in.readString();
        requestedDetails = in.readParcelable(ParcelableRichConsentRequestedDetails.class.getClassLoader());
    }

    public static ParcelableEnrollment fromJSON(String json) {
        return JSON.fromJson(json, ParcelableEnrollment.class);
    }

    public String toJSON() {
        return JSON.toJson(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public RichConsentRequestedDetails getRequestedDetails() {
        return requestedDetails;
    }

    @Override
    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getExpiresAt() {
        return expiresAt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(createdAt);
        dest.writeString(expiresAt);
        dest.writeParcelable(requestedDetails, flags);
    }
}
