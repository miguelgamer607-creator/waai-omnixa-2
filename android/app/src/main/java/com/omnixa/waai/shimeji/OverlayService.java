package com.omnixa.waai.shimeji;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ServiceInfo;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OverlayService extends Service {
    public static final String ACTION_STOP = "com.omnixa.waai.shimeji.STOP";
    private static final String CHANNEL = "waai_companion_active";
    private static final String EVENTS_CHANNEL = "waai_companion_events";
    private static final int RUNNING_ID = 401;
    private static final int EVENT_ID = 402;
    private static final long POLL_MS = 15 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicBoolean pollInFlight = new AtomicBoolean(false);
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private LinearLayout overlayRoot;
    private LinearLayout eventPanel;
    private TextView eventTitle;
    private TextView eventMessage;
    private Button actionButton;
    private Button dismissButton;
    private WaaiPetView pet;
    private boolean overlayAdded = false;
    private boolean dragged = false;
    private float downRawX, downRawY;
    private int downWindowX, downWindowY;
    private long currentEventId = -1L;
    private String currentEventTitle = "";
    private String currentEventMessage = "";
    private String currentActionUrl = "";
    private String currentActionType = "none";

    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            tryPollProactiveEvent();
            handler.postDelayed(this, POLL_MS);
        }
    };

    private final Runnable restoreOverlayRunnable = () -> {
        WaaiPrefs.store(this).edit().putLong(WaaiPrefs.HIDE_UNTIL, 0L).apply();
        if (SettingsCompat.canDraw(this)) addOverlay();
    };

    @Override public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannels();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            Notification ongoing = buildOngoingNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(RUNNING_ID, ongoing, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(RUNNING_ID, ongoing);
            }
        } catch (Exception e) {
            Toast.makeText(this, "No pude iniciar el servicio de Waai: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!SettingsCompat.canDraw(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        WaaiPrefs.store(this).edit().putBoolean(WaaiPrefs.SERVICE_ACTIVE, true).apply();
        if (!WaaiPrefs.isOverlayHidden(this)) addOverlay();
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, 25_000L);
        return START_NOT_STICKY;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(
            CHANNEL, "Waai Shimeji activo", NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(
            EVENTS_CHANNEL, "Avisos de Waai", NotificationManager.IMPORTANCE_DEFAULT));
    }

    private Notification buildOngoingNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 401, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, OverlayService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 402, stop,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Waai Shimeji está activa")
            .setContentText(WaaiPrefs.proactive(this)
                ? "Mascota visible · iniciativa autorizada" : "Mascota visible · iniciativa pausada")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Detener", stopPending).build());
        return builder.build();
    }

    private void addOverlay() {
        if (overlayAdded || !SettingsCompat.canDraw(this) || WaaiPrefs.isOverlayHidden(this)) return;
        overlayRoot = new LinearLayout(this);
        overlayRoot.setOrientation(LinearLayout.VERTICAL);
        overlayRoot.setGravity(Gravity.CENTER_HORIZONTAL);
        overlayRoot.setPadding(dp(7), dp(7), dp(7), dp(7));
        overlayRoot.setBackground(rounded(0xEE1B1524, 22, 0xFF5B426B));
        overlayRoot.setElevation(dp(9));

        pet = new WaaiPetView(this);
        LinearLayout.LayoutParams petParams = new LinearLayout.LayoutParams(dp(88), dp(102));
        petParams.gravity = Gravity.CENTER_HORIZONTAL;
        overlayRoot.addView(pet, petParams);
        pet.setOnTouchListener((view, event) -> handlePetTouch(event));

        eventPanel = new LinearLayout(this);
        eventPanel.setOrientation(LinearLayout.VERTICAL);
        eventPanel.setPadding(dp(9), dp(6), dp(9), dp(8));
        eventPanel.setVisibility(View.GONE);
        overlayRoot.addView(eventPanel, new LinearLayout.LayoutParams(dp(256), -2));

        TextView heading = label("WAAl · COMPAÑERA", 10, 0xFFFFB56E, true);
        eventPanel.addView(heading);
        eventTitle = label("Estoy aquí", 15, 0xFFF9F3FF, true);
        LinearLayout.LayoutParams titleParams = wrap();
        titleParams.topMargin = dp(3);
        eventPanel.addView(eventTitle, titleParams);
        eventMessage = label("Tócame para charlar, revisar una tarea o preguntarme por una investigación.", 12, 0xFFE0D4E9, false);
        LinearLayout.LayoutParams messageParams = wrap();
        messageParams.topMargin = dp(4);
        messageParams.bottomMargin = dp(7);
        eventPanel.addView(eventMessage, messageParams);

        Button talk = compactButton("Hablar con Waai", true);
        eventPanel.addView(talk, buttonParams());
        talk.setOnClickListener(v -> openChatFromEvent());
        Button research = compactButton("Preguntar por investigaciones", false);
        LinearLayout.LayoutParams researchParams = buttonParams();
        researchParams.topMargin = dp(4);
        eventPanel.addView(research, researchParams);
        research.setOnClickListener(v -> {
            openChat("¿Qué investigaciones recientes hay y qué vale la pena revisar?");
            hidePanel();
        });

        actionButton = compactButton("Abrir enlace", false);
        LinearLayout.LayoutParams actionParams = buttonParams();
        actionParams.topMargin = dp(4);
        eventPanel.addView(actionButton, actionParams);
        actionButton.setVisibility(View.GONE);
        actionButton.setOnClickListener(v -> {
            String url = currentActionUrl;
            if (url != null && url.startsWith("https://")) {
                try { WaaiNet.openSafeUrl(this, url); }
                catch (Exception e) { toast(e.getMessage()); }
            } else if ("research".equals(currentActionType)) {
                openHome();
            }
        });
        dismissButton = compactButton("Descartar novedad", false);
        LinearLayout.LayoutParams dismissParams = buttonParams();
        dismissParams.topMargin = dp(4);
        eventPanel.addView(dismissButton, dismissParams);
        dismissButton.setVisibility(View.GONE);
        dismissButton.setOnClickListener(v -> dismissCurrentEvent());

        LinearLayout manage = new LinearLayout(this);
        manage.setOrientation(LinearLayout.HORIZONTAL);
        manage.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(-1, -2);
        manageParams.topMargin = dp(5);
        eventPanel.addView(manage, manageParams);
        Button pause = compactButton("Pausar 30 min", false);
        Button close = compactButton("Cerrar Waai", false);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(35), 1f);
        half.rightMargin = dp(3);
        manage.addView(pause, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(35), 1f);
        half2.leftMargin = dp(3);
        manage.addView(close, half2);
        pause.setOnClickListener(v -> pauseOverlay());
        close.setOnClickListener(v -> stopSelf());

        layoutParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = Math.max(dp(8), screenWidth() - dp(104));
        layoutParams.y = dp(210);
        try {
            windowManager.addView(overlayRoot, layoutParams);
            overlayAdded = true;
        } catch (Exception e) {
            overlayAdded = false;
            Toast.makeText(this, "No pude dibujar la mascota. Comprueba el permiso de superposición.", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private boolean handlePetTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downWindowX = layoutParams.x;
                downWindowY = layoutParams.y;
                dragged = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragged && Math.hypot(dx, dy) > dp(7)) dragged = true;
                if (dragged) {
                    layoutParams.x = Math.max(0, Math.min(screenWidth() - dp(74), downWindowX + Math.round(dx)));
                    layoutParams.y = Math.max(dp(30), Math.min(screenHeight() - dp(130), downWindowY + Math.round(dy)));
                    try { windowManager.updateViewLayout(overlayRoot, layoutParams); } catch (Exception ignored) {}
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragged) togglePanel();
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return false;
        }
    }

    private void togglePanel() {
        if (eventPanel == null || overlayRoot == null) return;
        boolean visible = eventPanel.getVisibility() == View.VISIBLE;
        if (visible) {
            eventPanel.setVisibility(View.GONE);
            layoutParams.x = Math.max(dp(8), screenWidth() - dp(104));
            pet.setMood("idle");
        } else {
            eventPanel.setVisibility(View.VISIBLE);
            layoutParams.x = Math.max(dp(8), screenWidth() - dp(286));
            pet.setMood("happy");
        }
        try { windowManager.updateViewLayout(overlayRoot, layoutParams); } catch (Exception ignored) {}
    }

    private void hidePanel() {
        if (eventPanel == null) return;
        eventPanel.setVisibility(View.GONE);
        if (layoutParams != null) {
            layoutParams.x = Math.max(dp(8), screenWidth() - dp(104));
            try { windowManager.updateViewLayout(overlayRoot, layoutParams); } catch (Exception ignored) {}
        }
    }

    private void pauseOverlay() {
        long until = System.currentTimeMillis() + 30L * 60L * 1000L;
        WaaiPrefs.store(this).edit().putLong(WaaiPrefs.HIDE_UNTIL, until).apply();
        removeOverlay();
        handler.removeCallbacks(restoreOverlayRunnable);
        handler.postDelayed(restoreOverlayRunnable, 30L * 60L * 1000L);
        Toast.makeText(this, "Waai descansará durante 30 minutos.", Toast.LENGTH_SHORT).show();
    }

    private void openChatFromEvent() {
        String prompt = currentEventMessage.isEmpty()
            ? "Hola Waai, ¿qué te gustaría conversar?"
            : "Cuéntame más sobre esto: " + currentEventTitle + ". " + currentEventMessage;
        openChat(prompt);
        hidePanel();
    }

    private void openChat(String initialMessage) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            intent.putExtra(ChatActivity.EXTRA_INITIAL_MESSAGE, initialMessage);
        }
        startActivity(intent);
    }

    private void openHome() {
        try { WaaiNet.openSafeUrl(this, WaaiNet.homeUrl(this)); }
        catch (Exception e) { toast(e.getMessage()); }
    }

    private void tryPollProactiveEvent() {
        if (!WaaiPrefs.proactive(this) || WaaiPrefs.isQuietNow(this) || WaaiPrefs.isOverlayHidden(this)) return;
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null && !power.isInteractive()) return;
        if (!pollInFlight.compareAndSet(false, true)) return;
        io.execute(() -> {
            try {
                JSONObject response = WaaiNet.request(this, "POST", "/api/companion/proactive",
                    new JSONObject().put("enabled", true));
                JSONObject event = response.optJSONObject("event");
                if (event != null) main.post(() -> showProactiveEvent(event));
            } catch (Exception e) {
                android.util.Log.i("WaaiShimeji", "Proactive poll skipped: " + e.getMessage());
            } finally {
                pollInFlight.set(false);
            }
        });
    }

    private void showProactiveEvent(JSONObject event) {
        currentEventId = event.optLong("id", -1L);
        currentEventTitle = event.optString("title", "Waai encontró algo");
        currentEventMessage = event.optString("message", "¿Quieres que revisemos esto?");
        currentActionType = event.optString("action_type", "none");
        currentActionUrl = safeEventUrl(event.optString("action_url", ""));
        if (eventTitle != null) eventTitle.setText(currentEventTitle);
        if (eventMessage != null) eventMessage.setText(currentEventMessage);
        if (actionButton != null) {
            boolean canOpen = !currentActionUrl.isEmpty();
            actionButton.setVisibility(canOpen ? View.VISIBLE : View.GONE);
            if (canOpen) actionButton.setText("Abrir fuente o contenido");
        }
        if (dismissButton != null) dismissButton.setVisibility(View.VISIBLE);
        if (pet != null) pet.setMood("research");
        if (eventPanel != null && eventPanel.getVisibility() != View.VISIBLE) togglePanel();
        showEventNotification(currentEventTitle, currentEventMessage);
    }

    private String safeEventUrl(String value) {
        if (value == null || !value.startsWith("https://")) return "";
        android.net.Uri uri = android.net.Uri.parse(value);
        if (uri.getHost() == null || uri.getUserInfo() != null) return "";
        return value;
    }

    private void showEventNotification(String title, String message) {
        Intent tap = new Intent(this, ChatActivity.class);
        tap.putExtra(ChatActivity.EXTRA_INITIAL_MESSAGE, "Háblame de esta novedad: " + title + ". " + message);
        PendingIntent pending = PendingIntent.getActivity(this, (int) (System.currentTimeMillis() & 0x0fffffff),
            tap, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, EVENTS_CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(message.length() > 120 ? message.substring(0, 117) + "…" : message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pending);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(EVENT_ID, builder.build());
    }

    private void dismissCurrentEvent() {
        long id = currentEventId;
        if (id > 0) io.execute(() -> {
            try { WaaiNet.request(this, "POST", "/api/companion/events/" + id + "/dismiss", new JSONObject()); }
            catch (Exception e) { android.util.Log.i("WaaiShimeji", "Could not dismiss event: " + e.getMessage()); }
        });
        currentEventId = -1L;
        currentEventTitle = "";
        currentEventMessage = "";
        currentActionUrl = "";
        currentActionType = "none";
        if (eventTitle != null) eventTitle.setText("Estoy aquí");
        if (eventMessage != null) eventMessage.setText("Tócame para charlar, revisar una tarea o preguntarme por una investigación.");
        if (actionButton != null) actionButton.setVisibility(View.GONE);
        if (dismissButton != null) dismissButton.setVisibility(View.GONE);
        if (pet != null) pet.setMood("idle");
        hidePanel();
    }

    private void removeOverlay() {
        if (overlayAdded && overlayRoot != null) {
            try { windowManager.removeView(overlayRoot); } catch (Exception ignored) {}
        }
        overlayAdded = false;
        overlayRoot = null; eventPanel = null; eventTitle = null; eventMessage = null;
        actionButton = null; dismissButton = null; pet = null;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        // Do not silently restart. The foreground notification makes the active
        // session visible, and the user can explicitly reopen the companion.
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        handler.removeCallbacks(restoreOverlayRunnable);
        removeOverlay();
        io.shutdownNow();
        WaaiPrefs.store(this).edit().putBoolean(WaaiPrefs.SERVICE_ACTIVE, false).apply();
        try { stopForeground(STOP_FOREGROUND_REMOVE); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private Button compactButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setMinHeight(dp(34));
        b.setMinimumHeight(dp(34));
        b.setPadding(dp(5), 0, dp(5), 0);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(35, 23, 48) : Color.rgb(249, 243, 255));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary
            ? Color.rgb(198, 160, 255) : Color.rgb(54, 43, 65)));
        return b;
    }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams buttonParams() {
        return new LinearLayout.LayoutParams(-1, dp(35));
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int screenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int screenHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static final class SettingsCompat {
        static boolean canDraw(Context context) {
            return Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(context);
        }
    }
}
