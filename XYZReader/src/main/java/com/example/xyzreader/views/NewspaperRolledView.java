package com.example.xyzreader.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.xyzreader.R;

/**
 * Created by Douglas on 1/16/2016.
 */
public class NewspaperRolledView extends View {

    Paint mBlackPaint;
    Path mOutSideRollLine;
    RectF mOval;

    public NewspaperRolledView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);

        mBlackPaint = new Paint();
        mBlackPaint.setColor(getResources().getColor(R.color.theme_primary_dark));
        mBlackPaint.setStyle(Paint.Style.STROKE);
        mBlackPaint.setStrokeWidth(1);
        mBlackPaint.setAntiAlias(true);

        mOutSideRollLine = new Path();
        mOval = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float center = 60;
        float radius = 130;
        mOval.set(5,10,30,410);

        mOutSideRollLine.addArc(mOval,0,270);
        canvas.drawPath(mOutSideRollLine,mBlackPaint);
        canvas.drawText("Test",100,100,mBlackPaint);
    }
}
