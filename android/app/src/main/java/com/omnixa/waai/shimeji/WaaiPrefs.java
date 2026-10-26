package com.omnixa.waai.shimeji;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;

final class WaaiPrefs {
    static final String FILE = "waai_private_settings";
    static final String BASE_URL = "backend_url";
    static final String TOKEN = "access_token";
    static final String PROACTIVE = "proactive_enabled";
    static final String QUIET = "quiet_hours_enabled";
    static final String SERVICE_ACTIVE = "overlay_service_active";
    static final String HIDE_UNTIL = "overlay_hide_until";

    private WaaiPrefs() {}

    static SharedPreferences store(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String baseUrl(Context context) {
        return store(context).getString(BASE_URL, "").trim().replaceAll("/+$", "");
    }

    static String token(Context context) {
        return store(context).getString(TOKEN, "");
    }

    static boolean proactive(Context context) {
        return store(context).getBoolean(PROACTIVE, false);
    }

    static boolean quietHours(Context context) {
        return store(context).getBoolean(QUIET, true);
    }

    static boolean isQuietNow(Context context) {
        if (!quietHours(context)) return false;
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(8, 0));
    }

    static boolean isOverlayHidden(Context context) {
        return store(context).getLong(HIDE_UNTIL, 0L) > System.currentTimeMillis();
    }

    static boolean validBaseUrl(String value) {
        try {
            URI uri = new URI(value.trim().replaceAll("/+$", ""));
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getHost() == null || uri.getUserInfo() != null) return false;
            if (uri.getQuery() != null || uri.getFragment() != null) return false;
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
