package com.famousindiasocialnetwork.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class MontserratEditText extends androidx.appcompat.widget.AppCompatEditText {
    public MontserratEditText(Context context) {
        super(context);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "montserrat_regular.ttf"));
    }

    public MontserratEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "montserrat_regular.ttf"));
    }

    public MontserratEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "montserrat_regular.ttf"));
    }
}
