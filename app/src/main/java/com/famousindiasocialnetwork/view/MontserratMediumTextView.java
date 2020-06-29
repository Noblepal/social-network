package com.famousindiasocialnetwork.view;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

public class MontserratMediumTextView extends androidx.appcompat.widget.AppCompatTextView {
    public MontserratMediumTextView(Context context) {
        super(context);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "Montserrat_Medium.ttf"));
    }

    public MontserratMediumTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "Montserrat_Medium.ttf"));
    }

    public MontserratMediumTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "Montserrat_Medium.ttf"));
    }

}
