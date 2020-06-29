
package com.famousindiasocialnetwork.activity;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

//import com.stripe.android.Stripe;
//import com.stripe.android.TokenCallback;
//import com.stripe.android.model.Card;
//import com.stripe.android.model.Token;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.fragment.StripeCardFragment;
import com.famousindiasocialnetwork.listener.StripeInteracter;

public class StripePaymentActivity extends AppCompatActivity implements StripeInteracter {
    final String FRAG_TAG_CARD = "GET_CARD_FRAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stripe_payment);
        findViewById(R.id.initStripe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragment(FRAG_TAG_CARD);
            }
        });
    }

    private void openFragment(String fragTag) {
        Fragment fragment = null;
        switch (fragTag) {
            case FRAG_TAG_CARD:
                fragment = new StripeCardFragment();
                break;
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down, R.anim.bottom_up, R.anim.bottom_down)
                    .add(R.id.stripeFrame, fragment, fragTag)
                    .addToBackStack(fragTag)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
    }
}

