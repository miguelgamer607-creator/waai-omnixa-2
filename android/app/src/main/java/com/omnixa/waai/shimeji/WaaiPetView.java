package com.omnixa.waai.shimeji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

final class WaaiPetView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long startAt = SystemClock.uptimeMillis();
    private String mood = "idle";

    WaaiPetView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Waai, tocar para conversar");
    }

    void setMood(String value) {
        mood = value == null ? "idle" : value;
        invalidate();
    }

    private int color(String value) { return Color.parseColor(value); }

    private void fill(Canvas c, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.clearShadowLayer();
    }

    private void line(Canvas c, int color, float width) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.clearShadowLayer();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float sx = getWidth() / 106f;
        float sy = getHeight() / 112f;
        long elapsed = SystemClock.uptimeMillis() - startAt;
        float bob = (float) Math.sin(elapsed / (mood.equals("thinking") ? 250.0 : 560.0)) * (mood.equals("happy") ? 3.4f : 1.8f);
        canvas.save();
        canvas.scale(sx, sy);
        canvas.translate(0, bob);

        // Ground shadow and curved tiger tail.
        fill(canvas, 0x55300038);
        canvas.drawOval(new RectF(14, 99, 94, 110), paint);
        line(canvas, color("#E66B55"), 9);
        Path tail = new Path();
        tail.moveTo(72, 83); tail.cubicTo(102, 94, 104, 58, 86, 59); tail.cubicTo(76, 59, 80, 71, 87, 70);
        canvas.drawPath(tail, paint);
        line(canvas, color("#3B293E"), 4);
        canvas.drawLine(88, 61, 85, 67, paint);

        // Ears.
        Path leftEar = new Path();
        leftEar.moveTo(20, 39); leftEar.lineTo(14, 6); leftEar.quadTo(14, 1, 20, 5); leftEar.lineTo(43, 23); leftEar.close();
        fill(canvas, color("#3B293E")); canvas.drawPath(leftEar, paint);
        Path leftInner = new Path();
        leftInner.moveTo(19, 13); leftInner.lineTo(29, 27); leftInner.lineTo(21, 26); leftInner.close();
        fill(canvas, color("#F39A86")); canvas.drawPath(leftInner, paint);
        Path rightEar = new Path();
        rightEar.moveTo(62, 23); rightEar.lineTo(88, 5); rightEar.quadTo(94, 1, 92, 9); rightEar.lineTo(84, 42); rightEar.close();
        fill(canvas, color("#3B293E")); canvas.drawPath(rightEar, paint);
        Path rightInner = new Path();
        rightInner.moveTo(84, 12); rightInner.lineTo(81, 29); rightInner.lineTo(73, 27); rightInner.close();
        fill(canvas, color("#F39A86")); canvas.drawPath(rightInner, paint);

        // Hair silhouette and face.
        fill(canvas, color("#EC704D"));
        canvas.drawRoundRect(new RectF(13, 23, 92, 88), 33, 33, paint);
        fill(canvas, color("#FFB36A"));
        canvas.drawRoundRect(new RectF(22, 27, 84, 84), 28, 28, paint);
        fill(canvas, color("#F8D5BF"));
        canvas.drawOval(new RectF(25, 33, 81, 83), paint);

        // Side locks and forehead fringe.
        fill(canvas, color("#E95E4D"));
        Path side = new Path();
        side.moveTo(23, 36); side.quadTo(11, 50, 24, 78); side.lineTo(33, 84); side.lineTo(33, 58); side.close();
        canvas.drawPath(side, paint);
        Path fringe = new Path();
        fringe.moveTo(18, 45); fringe.cubicTo(17, 21, 40, 17, 52, 23);
        fringe.cubicTo(74, 16, 91, 31, 86, 48); fringe.lineTo(73, 40);
        fringe.lineTo(64, 48); fringe.lineTo(55, 35); fringe.lineTo(43, 49); fringe.lineTo(36, 36); fringe.close();
        fill(canvas, color("#FF8B51")); canvas.drawPath(fringe, paint);
        line(canvas, color("#FFD49B"), 3);
        canvas.drawLine(47, 27, 42, 39, paint);
        canvas.drawLine(65, 25, 73, 35, paint);

        // Tiger stripes.
        fill(canvas, color("#39263D"));
        Path stripe1 = new Path(); stripe1.moveTo(43, 31); stripe1.lineTo(37, 42); stripe1.lineTo(47, 39); stripe1.close();
        Path stripe2 = new Path(); stripe2.moveTo(57, 29); stripe2.lineTo(64, 42); stripe2.lineTo(54, 39); stripe2.close();
        canvas.drawPath(stripe1, paint); canvas.drawPath(stripe2, paint);

        // Blinking eyes, blush and smile.
        boolean blink = (elapsed % 4300) > 3650 && (elapsed % 4300) < 3750;
        if (mood.equals("sleeping") || blink) {
            line(canvas, color("#653C4C"), 2.3f);
            canvas.drawArc(new RectF(32, 48, 45, 57), 5, 170, false, paint);
            canvas.drawArc(new RectF(61, 48, 74, 57), 5, 170, false, paint);
        } else {
            fill(canvas, Color.WHITE);
            canvas.drawOval(new RectF(31, 46, 46, 62), paint);
            canvas.drawOval(new RectF(60, 46, 75, 62), paint);
            fill(canvas, color(mood.equals("happy") ? "#8F79DA" : "#8A91D7"));
            canvas.drawOval(new RectF(34, 48, 43, 62), paint);
            canvas.drawOval(new RectF(63, 48, 72, 62), paint);
            fill(canvas, color("#2B2038"));
            canvas.drawOval(new RectF(37, 49, 41, 59), paint);
            canvas.drawOval(new RectF(66, 49, 70, 59), paint);
            fill(canvas, Color.WHITE);
            canvas.drawCircle(36, 49, 2.2f, paint);
            canvas.drawCircle(65, 49, 2.2f, paint);
        }
        fill(canvas, color("#E99A98"));
        canvas.drawOval(new RectF(25, 61, 35, 67), paint);
        canvas.drawOval(new RectF(72, 61, 82, 67), paint);
        fill(canvas, color("#8D5265"));
        Path nose = new Path(); nose.moveTo(50, 63); nose.quadTo(53, 60, 56, 63); nose.lineTo(53, 66); nose.close();
        canvas.drawPath(nose, paint);
        line(canvas, color("#8D5265"), 1.8f);
        canvas.drawArc(new RectF(45, 64, 54, 71), 20, 135, false, paint);
        canvas.drawArc(new RectF(52, 64, 61, 71), 25, 135, false, paint);

        // Neck, jacket and teal scarf.
        fill(canvas, color("#F1B59E"));
        canvas.drawRoundRect(new RectF(43, 77, 64, 91), 7, 7, paint);
        fill(canvas, color("#292033"));
        Path body = new Path();
        body.moveTo(25, 81); body.quadTo(34, 75, 44, 82);
        body.quadTo(54, 91, 65, 81); body.quadTo(81, 77, 88, 90);
        body.lineTo(93, 105); body.quadTo(53, 112, 13, 105); body.lineTo(17, 91); body.close();
        canvas.drawPath(body, paint);
        fill(canvas, color("#F0E9EF"));
        Path shirt = new Path(); shirt.moveTo(44, 82); shirt.lineTo(53, 91); shirt.lineTo(64, 82); shirt.lineTo(71, 105); shirt.lineTo(34, 105); shirt.close();
        canvas.drawPath(shirt, paint);
        fill(canvas, color("#6CCFD2"));
        Path scarf = new Path(); scarf.moveTo(35, 79); scarf.quadTo(53, 89, 73, 78);
        scarf.lineTo(79, 86); scarf.quadTo(53, 101, 31, 88); scarf.close();
        canvas.drawPath(scarf, paint);
        Path scarfTail = new Path(); scarfTail.moveTo(69, 84); scarfTail.lineTo(88, 92); scarfTail.lineTo(76, 98); scarfTail.lineTo(64, 88); scarfTail.close();
        canvas.drawPath(scarfTail, paint);
        fill(canvas, color("#FFB56E"));
        canvas.drawCircle(53, 98, 3.2f, paint);

        // Small thought glint while the model is working.
        if (mood.equals("thinking") || mood.equals("research")) {
            fill(canvas, color("#C6A0FF"));
            canvas.drawCircle(94, 26, 3.2f, paint);
            canvas.drawCircle(99, 17, 1.7f, paint);
        }
        canvas.restore();
        postInvalidateDelayed(100);
    }
}
