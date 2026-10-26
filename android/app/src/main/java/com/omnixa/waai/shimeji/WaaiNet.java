package com.omnixa.waai.shimeji;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class WaaiNet {
    private WaaiNet() {}

    static JSONObject request(Context context, String method, String path, JSONObject body) throws Exception {
        String base = WaaiPrefs.baseUrl(context);
        if (!WaaiPrefs.validBaseUrl(base)) {
            throw new IllegalStateException("Guarda una URL HTTPS válida del servidor Waai en Ajustes.");
        }
        URL url = new URL(base + (path.startsWith("/") ? path : "/" + path));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String token = WaaiPrefs.token(context);
        if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            connection.setDoOutput(true);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
        }
        int code = connection.getResponseCode();
        InputStream input = code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream();
        byte[] bytes;
        try (InputStream in = input; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (in != null) {
                byte[] chunk = new byte[4096];
                int count;
                while ((count = in.read(chunk)) != -1 && buffer.size() < 1_000_000) {
                    buffer.write(chunk, 0, count);
                }
            }
            bytes = buffer.toByteArray();
        } finally {
            connection.disconnect();
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        JSONObject data;
        try {
            data = text.isEmpty() ? new JSONObject() : new JSONObject(text);
        } catch (Exception ignored) {
            data = new JSONObject().put("detail", text);
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(data.optString("detail", "El servidor respondió HTTP " + code));
        }
        return data;
    }

    static org.json.JSONArray requestArray(Context context, String method, String path, JSONObject body) throws Exception {
        String base = WaaiPrefs.baseUrl(context);
        if (!WaaiPrefs.validBaseUrl(base)) {
            throw new IllegalStateException("Guarda una URL HTTPS válida del servidor Waai en Ajustes.");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(base + (path.startsWith("/") ? path : "/" + path)).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String token = WaaiPrefs.token(context);
        if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            connection.setDoOutput(true);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
        }
        int code = connection.getResponseCode();
        InputStream input = code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream();
        String text;
        try (InputStream in = input; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (in != null) {
                byte[] chunk = new byte[4096]; int count;
                while ((count = in.read(chunk)) != -1 && buffer.size() < 1_000_000) buffer.write(chunk, 0, count);
            }
            text = buffer.toString(StandardCharsets.UTF_8.name());
        } finally { connection.disconnect(); }
        if (code < 200 || code >= 300) {
            try { throw new IllegalStateException(new JSONObject(text).optString("detail", "HTTP " + code)); }
            catch (org.json.JSONException ignored) { throw new IllegalStateException(text.isEmpty() ? "HTTP " + code : text); }
        }
        return new org.json.JSONArray(text);
    }

    static void openSafeUrl(Context context, String value) throws Exception {
        Uri uri = Uri.parse(value == null ? "" : value.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Por seguridad solo puedo abrir enlaces HTTPS válidos.");
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    static String homeUrl(Context context) {
        return WaaiPrefs.baseUrl(context) + "/";
    }
}
