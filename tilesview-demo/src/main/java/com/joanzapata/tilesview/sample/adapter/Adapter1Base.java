package com.joanzapata.tilesview.sample.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.joanzapata.tilesview.adapter.DefaultAdapter;

import static com.joanzapata.tilesview.sample.utils.AndroidUtils.dp;

public class Adapter1Base extends DefaultAdapter {

    public static final float BASE_STROKE_WIDTH = 5f;
    private final Context context;
    private final Paint paint;
    private final RectF rect;

    public Adapter1Base(Context context) {
        this.context = context;
        this.rect = new RectF();
        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeCap(Paint.Cap.BUTT);
        this.paint.setColor(Color.RED);
    }

    @Override
    public void drawTile(Canvas canvas,
            float xRatio, float yRatio,
            float widthRatio, float heightRatio,
            float contentInitialWidth, float contentInitialHeight, float scale) {

        paint.setStrokeWidth(dp(context, BASE_STROKE_WIDTH * scale));
        float left = -xRatio * contentInitialWidth * scale;
        float top = -yRatio * contentInitialHeight * scale;
        float right = (1 - xRatio) * contentInitialWidth * scale;
        float bottom = (1 - yRatio) * contentInitialHeight * scale;
        float inset = dp(context, BASE_STROKE_WIDTH / 2 * scale);

        rect.set(left, top, right, bottom);
        canvas.clipRect(rect);
        canvas.drawColor(Color.WHITE);
        canvas.drawLine(left, top, right, bottom, paint);
        canvas.drawLine(right, top, left, bottom, paint);
        rect.inset(inset, inset);
        canvas.drawRect(rect, paint);
    }

}
