package com.auth0.android.guardian.sdk.utils;

import android.os.Bundle;

import java.util.Map;

public class BundleUtils {
    public static Bundle mapToBundle(Map<String, String> map) {
        Bundle data = new Bundle(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            data.putString(e.getKey(), e.getValue());
        }
        return data;
    }
}
