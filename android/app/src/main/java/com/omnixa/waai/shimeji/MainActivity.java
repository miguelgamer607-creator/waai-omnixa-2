package com.omnixa.waai.shimeji;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 701;
    private static final int REQUEST_NOTIFICATIONS = 702;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private EditText serverField;
    private EditText tokenField;
    private CheckBox proactiveBox;
    private CheckBox quietBox;
    private TextView statusText;
    private boolean pendingStartOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        loadSettingsToUi();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(18, 16, 23));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView eyebrow = text("OMNIXA · ANDROID COMPANION", 11, Color.rgb(255, 181, 110), true);
        root.addView(eyebrow);
        TextView title = text("Waai Shimeji", 29, Color.rgb(249, 243, 255), true);
        LinearLayout.LayoutParams titleParams = params(-1, -2);
        titleParams.topMargin = dp(3);
        root.addView(title, titleParams);
        TextView intro = text(
            "Tu compañera puede flotar encima de otras aplicaciones, abrir una conversación y compartir investigaciones. " +
            "La activación y las interrupciones dependen de los permisos y controles que elijas.",
            14, Color.rgb(190, 176, 202), false
        );
        LinearLayout.LayoutParams introParams = params(-1, -2);
        introParams.topMargin = dp(7);
        introParams.bottomMargin = dp(18);
        root.addView(intro, introParams);

        section(root, "1 · Conectar Waai Core");
        serverField = edit("URL HTTPS del backend, por ejemplo https://…-8000.app.github.dev");
        serverField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(serverField, params(-1, dp(52)));

        tokenField = edit("Token privado (solo si el servidor lo exige)");
        tokenField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams tokenParams = params(-1, dp(52));
        tokenParams.topMargin = dp(8);
        root.addView(tokenField, tokenParams);

        Button connect = button("Guardar y probar conexión", true);
        LinearLayout.LayoutParams connectParams = params(-1, dp(50));
        connectParams.topMargin = dp(10);
        root.addView(connect, connectParams);
        connect.setOnClickListener(v -> {
            if (!saveSettings()) return;
            testConnection();
        });

        TextView connectionHint = text(
            "En Codespaces, copia la URL de Puertos del puerto 8000. Debe ser HTTPS y no incluir /api ni una clave.",
            12, Color.rgb(170, 155, 184), false
        );
        LinearLayout.LayoutParams hintParams = params(-1, -2);
        hintParams.topMargin = dp(7);
        root.addView(connectionHint, hintParams);

        section(root, "2 · Presencia e iniciativa");
        proactiveBox = new CheckBox(this);
        proactiveBox.setText("Permitir que Waai inicie interacciones");
        proactiveBox.setTextColor(Color.rgb(249, 243, 255));
        proactiveBox.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(198, 160, 255)));
        proactiveBox.setChecked(false);
        root.addView(proactiveBox, params(-1, -2));
        TextView initiativeHint = text(
            "Si lo activas, el servicio consulta el backend aproximadamente cada 15 minutos mientras está activo. " +
            "El servidor limita los mensajes espontáneos y prioriza investigaciones o tareas reales. No se abre ni reproduce contenido sin tocarlo.",
            12, Color.rgb(190, 176, 202), false
        );
        root.addView(initiativeHint, params(-1, -2));

        quietBox = new CheckBox(this);
        quietBox.setText("Pausar avisos entre 22:00 y 08:00");
        quietBox.setTextColor(Color.rgb(249, 243, 255));
        quietBox.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(198, 160, 255)));
        quietBox.setChecked(true);
        LinearLayout.LayoutParams quietParams = params(-1, -2);
        quietParams.topMargin = dp(6);
        root.addView(quietBox, quietParams);
        proactiveBox.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettingsSilently());
        quietBox.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettingsSilently());

        section(root, "3 · Activar la mascota");
        TextView permHint = text(
            "Android pide permiso explícito para dibujar sobre otras apps y puede pedir permiso para mostrar avisos. " +
            "Waai no lee la pantalla ni controla otras aplicaciones; las búsquedas y enlaces se abren solo cuando los tocas.",
            12, Color.rgb(190, 176, 202), false
        );
        root.addView(permHint, params(-1, -2));

        Button start = button("Activar Shimeji flotante", true);
        LinearLayout.LayoutParams startParams = params(-1, dp(50));
        startParams.topMargin = dp(12);
        root.addView(start, startParams);
        start.setOnClickListener(v -> {
            if (!saveSettings()) return;
            beginOverlayPermissionFlow();
        });

        Button stop = button("Detener y quitar la mascota", false);
        LinearLayout.LayoutParams stopParams = params(-1, dp(48));
        stopParams.topMargin = dp(8);
        root.addView(stop, stopParams);
        stop.setOnClickListener(v -> {
            Intent intent = new Intent(this, OverlayService.class);
            intent.setAction(OverlayService.ACTION_STOP);
            startService(intent);
            status("Waai ha sido detenida.");
        });

        Button talk = button("Conversar con Waai", false);
        LinearLayout.LayoutParams talkParams = params(-1, dp(48));
        talkParams.topMargin = dp(8);
        root.addView(talk, talkParams);
        talk.setOnClickListener(v -> {
            saveSettings();
            startActivity(new Intent(this, ChatActivity.class));
        });

        Button home = button("Abrir Waai Home", false);
        LinearLayout.LayoutParams homeParams = params(-1, dp(48));
        homeParams.topMargin = dp(8);
        root.addView(home, homeParams);
        home.setOnClickListener(v -> {
            saveSettings();
            String url = WaaiNet.homeUrl(this);
            try { WaaiNet.openSafeUrl(this, url); }
            catch (Exception e) { status(e.getMessage()); }
        });

        statusText = text("Configura el servidor para comenzar.", 13, Color.rgb(190, 176, 202), false);
        LinearLayout.LayoutParams statusParams = params(-1, -2);
        statusParams.topMargin = dp(12);
        statusParams.bottomMargin = dp(8);
        root.addView(statusText, statusParams);

        section(root, "Sobre la versión");
        TextView footer = text(
            "Primera versión nativa de prueba. La mascota flota sobre otras aplicaciones mientras el servicio esté activo. " +
            "No se inicia al encender el teléfono: tú la activas y la detienes. El modelo lingüístico se configura en el backend de Waai.",
            12, Color.rgb(160, 146, 174), false
        );
        root.addView(footer, params(-1, -2));
        return scroll;
    }

    private void loadSettingsToUi() {
        serverField.setText(WaaiPrefs.baseUrl(this));
        tokenField.setText(WaaiPrefs.token(this));
        proactiveBox.setChecked(WaaiPrefs.proactive(this));
        quietBox.setChecked(WaaiPrefs.quietHours(this));
    }

    private boolean saveSettings() {
        String base = serverField.getText().toString().trim().replaceAll("/+$", "");
        if (!base.isEmpty() && !WaaiPrefs.validBaseUrl(base)) {
            status("La URL debe ser HTTPS, con dominio válido y sin /api ni parámetros.");
            return false;
        }
        WaaiPrefs.store(this).edit()
            .putString(WaaiPrefs.BASE_URL, base)
            .putString(WaaiPrefs.TOKEN, tokenField.getText().toString().trim())
            .putBoolean(WaaiPrefs.PROACTIVE, proactiveBox.isChecked())
            .putBoolean(WaaiPrefs.QUIET, quietBox.isChecked())
            .apply();
        return true;
    }

    private void saveSettingsSilently() {
        if (serverField == null || proactiveBox == null || quietBox == null) return;
        String base = serverField.getText().toString().trim().replaceAll("/+$", "");
        WaaiPrefs.store(this).edit()
            .putString(WaaiPrefs.BASE_URL, base)
            .putString(WaaiPrefs.TOKEN, tokenField.getText().toString().trim())
            .putBoolean(WaaiPrefs.PROACTIVE, proactiveBox.isChecked())
            .putBoolean(WaaiPrefs.QUIET, quietBox.isChecked())
            .apply();
    }

    private void testConnection() {
        if (!WaaiPrefs.validBaseUrl(WaaiPrefs.baseUrl(this))) {
            status("Pega la URL HTTPS del puerto 8000 y vuelve a probar.");
            return;
        }
        status("Conectando con Waai Core…");
        io.execute(() -> {
            try {
                JSONObject config = WaaiNet.request(this, "GET", "/api/config", null);
                String model = config.optBoolean("llm_connected", false)
                    ? "Modelo configurado: " + config.optString("llm_model", "proveedor")
                    : "Servidor conectado, pero sigue en modo de reglas: falta configurar un modelo.";
                String message = "Waai Core respondió correctamente. " + model;
                main.post(() -> status(message));
            } catch (Exception e) {
                main.post(() -> status("No pude conectar: " + e.getMessage()));
            }
        });
    }

    private void beginOverlayPermissionFlow() {
        if (WaaiPrefs.baseUrl(this).isEmpty()) {
            status("Primero configura la URL HTTPS del servidor.");
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            pendingStartOnResume = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
            return;
        }
        requestNotificationOrStart();
    }

    private void requestNotificationOrStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingStartOnResume = false;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        startOverlayService();
    }

    private void startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            status("No se concedió el permiso de superposición.");
            return;
        }
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        pendingStartOnResume = false;
        status(WaaiPrefs.proactive(this)
            ? "Waai está activa. La iniciativa está permitida y respeta las horas de silencio."
            : "Waai está activa. La iniciativa automática permanece desactivada.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            pendingStartOnResume = false;
            if (Settings.canDrawOverlays(this)) requestNotificationOrStart();
            else {
                pendingStartOnResume = false;
                status("Para flotar sobre otras apps debes conceder el permiso de superposición.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            // The user may still use the overlay if notifications are denied; Android
            // will expose foreground-service status through system UI instead.
            startOverlayService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingStartOnResume && Settings.canDrawOverlays(this)) requestNotificationOrStart();
    }

    private void section(LinearLayout root, String label) {
        TextView heading = text(label, 16, Color.rgb(221, 195, 255), true);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(20);
        p.bottomMargin = dp(8);
        root.addView(heading, p);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(android.view.Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setPadding(dp(13), dp(5), dp(13), dp(5));
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(139, 124, 154));
        e.setTextColor(Color.rgb(249, 243, 255));
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(89, 66, 105)));
        return e;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(34, 22, 47) : Color.rgb(249, 243, 255));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary
            ? Color.rgb(198, 160, 255) : Color.rgb(47, 37, 58)));
        return b;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void status(String message) {
        if (statusText != null) statusText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
