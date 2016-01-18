package com.example.xyzreader.animation;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

/**
 * Created by Douglas on 1/18/2016.
 */
public class ExpandingCardViewAnimation extends Animation {

    Context mContext;
    View mExpandingView;
    View mParentView;
    public TranslateAnimation mTranslate;

    public ExpandingCardViewAnimation(Context context, View parentview,View view){
        this.mContext = context;
        this.mExpandingView = view;
        this.mParentView = parentview;
    }

    public void configureAnimation(){
        AnimationSet animationSet = new AnimationSet(false);
        int bottombound = mParentView.getHeight();
        float center = bottombound/2;
        mTranslate = new TranslateAnimation(mExpandingView.getX(),mExpandingView.getX(),mExpandingView.getY(),center);
        mTranslate.setDuration(1000);
        animationSet.addAnimation(mTranslate);
    }

}
