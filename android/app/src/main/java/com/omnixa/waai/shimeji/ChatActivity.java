package com.omnixa.waai.shimeji;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatActivity extends Activity {
    public static final String EXTRA_INITIAL_MESSAGE = "initial_message";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private LinearLayout messageList;
    private ScrollView transcript;
    private EditText composer;
    private TextView status;
    private Button sendButton;
    private boolean initialMessageHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(buildUi());
        loadHistory();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 16, 23));
        root.setPadding(dp(12), dp(12), dp(12), dp(8));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Waai-Fu", 21, 0xFFF9F3FF, true);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button home = button("Home", false);
        bar.addView(home, wrap());
        home.setOnClickListener(v -> {
            try { WaaiNet.openSafeUrl(this, WaaiNet.homeUrl(this)); }
            catch (Exception e) { toast(e.getMessage()); }
        });
        root.addView(bar);

        TextView subtitle = text("Habla, pregunta o pídele que busque contenido.", 12, 0xFFB9A9C7, false);
        LinearLayout.LayoutParams subtitleParams = wrap();
        subtitleParams.bottomMargin = dp(8);
        root.addView(subtitle, subtitleParams);

        transcript = new ScrollView(this);
        transcript.setFillViewport(true);
        transcript.setBackground(background(0xFF1A1522, 18, 0xFF40314B));
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(dp(9), dp(11), dp(9), dp(12));
        transcript.addView(messageList, new ScrollView.LayoutParams(-1, -2));
        root.addView(transcript, new LinearLayout.LayoutParams(-1, 0, 1));

        status = text("Los enlaces y medios solo se abrirán cuando pulses el botón correspondiente.", 11, 0xFFB9A9C7, false);
        LinearLayout.LayoutParams statusParams = wrap();
        statusParams.topMargin = dp(6);
        statusParams.bottomMargin = dp(4);
        root.addView(status, statusParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        composer = new EditText(this);
        composer.setSingleLine(false);
        composer.setMaxLines(4);
        composer.setTextSize(14);
        composer.setTextColor(0xFFF9F3FF);
        composer.setHintTextColor(0xFF927FA3);
        composer.setHint("Escribe un mensaje a Waai…");
        composer.setPadding(dp(12), dp(8), dp(12), dp(8));
        composer.setBackground(background(0xFF201A29, 16, 0xFF4B3957));
        inputRow.addView(composer, new LinearLayout.LayoutParams(0, dp(52), 1));
        sendButton = button("Enviar", true);
        LinearLayout.LayoutParams sendParams = wrap();
        sendParams.leftMargin = dp(7);
        inputRow.addView(sendButton, sendParams);
        sendButton.setOnClickListener(v -> sendMessage(composer.getText().toString()));
        composer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage(composer.getText().toString());
                return true;
            }
            return false;
        });
        root.addView(inputRow);

        return root;
    }

    private void loadHistory() {
        status.setText("Conectando con Waai Core…");
        io.execute(() -> {
            try {
                JSONArray messages = WaaiNet.requestArray(this, "GET", "/api/chat/history?limit=40", null);
                main.post(() -> {
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject message = messages.optJSONObject(i);
                        if (message == null) continue;
                        String role = message.optString("role", "assistant");
                        addMessage(role.equals("user") ? "Tú" : "Waai", message.optString("content", ""),
                            role.equals("user"));
                    }
                    if (messageList.getChildCount() == 0) {
                        addMessage("Waai", "Hola. ¿Qué tienes en mente? Podemos conversar, investigar o buscar imágenes, videos y música.", false);
                    }
                    status.setText("Listo. El modelo depende de la configuración del servidor.");
                    scrollToBottom();
                    if (!initialMessageHandled) {
                        String initial = getIntent().getStringExtra(EXTRA_INITIAL_MESSAGE);
                        initialMessageHandled = true;
                        if (initial != null && !initial.trim().isEmpty()) {
                            composer.setText(initial);
                            sendMessage(initial);
                        }
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (messageList.getChildCount() == 0) addMessage("Waai", "No pude recuperar el historial, pero puedes intentar enviar un mensaje.", false);
                    status.setText("No pude leer el historial: " + e.getMessage());
                });
            }
        });
    }

    private void sendMessage(String raw) {
        String textValue = raw == null ? "" : raw.trim();
        if (textValue.isEmpty() || !sendButton.isEnabled()) return;
        composer.setText("");
        addMessage("Tú", textValue, true);
        status.setText("Waai está pensando…");
        sendButton.setEnabled(false);
        hideKeyboard();
        io.execute(() -> {
            try {
                JSONObject payload = new JSONObject().put("message", textValue);
                JSONObject response = WaaiNet.request(this, "POST", "/api/companion/turn", payload);
                main.post(() -> {
                    addMessage("Waai", response.optString("reply", "Ya procesé el mensaje."), false);
                    JSONObject action = response.optJSONObject("companion_action");
                    if (action != null) showAction(action);
                    JSONArray sources = response.optJSONArray("sources");
                    if (sources != null) {
                        for (int i = 0; i < Math.min(sources.length(), 5); i++) {
                            JSONObject source = sources.optJSONObject(i);
                            if (source != null) showSource(source);
                        }
                    }
                    boolean llm = response.optBoolean("llm_used", false);
                    status.setText(llm ? "Respuesta generativa recibida." : "Respuesta recibida. Si Waai usa reglas básicas, configura un modelo en el servidor.");
                    sendButton.setEnabled(true);
                    scrollToBottom();
                });
            } catch (Exception e) {
                main.post(() -> {
                    addMessage("Sistema", "No pude llegar al servidor: " + e.getMessage(), false);
                    status.setText("Comprueba la URL, la conexión y el token.");
                    sendButton.setEnabled(true);
                    scrollToBottom();
                });
            }
        });
    }

    private void showAction(JSONObject action) {
        String type = action.optString("type", "none");
        String url = action.optString("url", "");
        String label = action.optString("label", "Abrir resultado");
        String query = action.optString("query", "");
        if ("none".equals(type)) return;
        if (!url.isEmpty() && !url.startsWith("https://")) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(9), dp(10), dp(9));
        card.setBackground(background(0xFF2A2135, 14, 0xFF75538B));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(6);
        p.bottomMargin = dp(8);

        card.addView(text("Acción sugerida · " + type.replace('_', ' '), 11, 0xFFFFB56E, true), wrap());
        if (!query.isEmpty()) card.addView(text(query, 13, 0xFFF0E7F8, false), wrap());
        Button open = button(label, true);
        LinearLayout.LayoutParams bp = wrap();
        bp.topMargin = dp(6);
        card.addView(open, bp);
        if (!url.isEmpty()) {
            open.setOnClickListener(v -> {
                try { WaaiNet.openSafeUrl(this, url); }
                catch (Exception e) { toast(e.getMessage()); }
            });
        } else if ("research".equals(type)) {
            open.setOnClickListener(v -> {
                try { WaaiNet.openSafeUrl(this, WaaiNet.homeUrl(this)); }
                catch (Exception e) { toast(e.getMessage()); }
            });
        } else {
            open.setEnabled(false);
        }
        messageList.addView(card, p);
    }

    private void showSource(JSONObject source) {
        String title = source.optString("title", "Fuente");
        String url = source.optString("url", "");
        if (!url.startsWith("https://")) return;
        Button open = button("Abrir fuente · " + title, false);
        open.setTextSize(12);
        LinearLayout.LayoutParams p = wrap();
        p.topMargin = dp(5);
        p.bottomMargin = dp(4);
        messageList.addView(open, p);
        open.setOnClickListener(v -> {
            try { WaaiNet.openSafeUrl(this, url); }
            catch (Exception e) { toast(e.getMessage()); }
        });
    }

    private void addMessage(String speaker, String message, boolean isUser) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(11), dp(8), dp(11), dp(8));
        box.setBackground(background(isUser ? 0xFF5C4380 : 0xFF241C2E, 15,
            isUser ? 0xFF8C6EAA : 0xFF4A3857));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            isUser ? dp(285) : -1, -2);
        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.topMargin = dp(5);
        params.bottomMargin = dp(5);
        TextView name = text(speaker, 11, isUser ? 0xFFEBD7FF : 0xFFFFB56E, true);
        box.addView(name, wrap());
        TextView content = text(message == null ? "" : message, 14, 0xFFF9F3FF, false);
        LinearLayout.LayoutParams contentParams = wrap();
        contentParams.topMargin = dp(3);
        box.addView(content, contentParams);
        messageList.addView(box, params);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(primary ? 0xFF241633 : 0xFFF9F3FF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? 0xFFC6A0FF : 0xFF30243C));
        return b;
    }

    private android.graphics.drawable.GradientDrawable background(int fill, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private void scrollToBottom() {
        transcript.post(() -> transcript.fullScroll(View.FOCUS_DOWN));
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(composer.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
