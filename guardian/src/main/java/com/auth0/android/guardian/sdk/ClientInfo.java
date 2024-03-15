package com.auth0.android.guardian.sdk;

import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class ClientInfo {
    final String name = "Guardian.Android";
    final String version = BuildConfig.VERSION_NAME;

    @SerializedName("env")
    @Nullable
    TelemetryInfo telemetryInfo;

    public ClientInfo (TelemetryInfo telemetryInfo) {
        this.telemetryInfo = telemetryInfo;
    }

    public ClientInfo () {
        this.telemetryInfo = null;
    }

    String toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    String toBase64() {
        byte[] bytes = this.toJson().getBytes();
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static class TelemetryInfo {
        String appName;
        String appVersion;

        public TelemetryInfo(String appName, String appVersion) {
            this.appName = appName;
            this.appVersion = appVersion;
        }
    }
}
