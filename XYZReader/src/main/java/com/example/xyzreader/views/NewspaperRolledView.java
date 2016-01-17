package com.example.xyzreader.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.example.xyzreader.R;

/**
 * Created by Douglas on 1/16/2016.
 */
public class NewspaperRolledView extends View {

    Paint mBlackPaint;

    public NewspaperRolledView(Context context) {
        super(context);

        mBlackPaint = new Paint();
        mBlackPaint.setColor(getResources().getColor(R.color.theme_primary_dark));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Test View",100,100,mBlackPaint);

    }
}
