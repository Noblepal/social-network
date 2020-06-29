package com.famousindiasocialnetwork.view;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

public class MontserratBoldTextView extends androidx.appcompat.widget.AppCompatTextView {
    public MontserratBoldTextView(Context context) {
        super(context);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "Montserrat_Bold.ttf"));
    }

    public MontserratBoldTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "Montserrat_Bold.ttf"));
    }

    public MontserratBoldTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "Montserrat_Bold.ttf"));
    }

}
