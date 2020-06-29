package com.famousindiasocialnetwork.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

//import com.stripe.android.Stripe;
//import com.stripe.android.TokenCallback;
//import com.stripe.android.model.Card;
//import com.stripe.android.model.Token;
//import com.stripe.android.view.CardInputWidget;
import androidx.fragment.app.Fragment;

import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.activity.MainActivity;
import com.famousindiasocialnetwork.listener.StripeInteracter;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.request.PaymentRequest;
import com.famousindiasocialnetwork.network.response.PaymentResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StripeCardFragment extends Fragment {
    private StripeInteracter stripeInteracter;

    private TextInputEditText cardCvv, cardYear, cardMonth, cardNumber;
    private ProgressBar progress_bar;
    private SharedPreferenceUtil sharedPreferenceUtil;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceUtil = new SharedPreferenceUtil(getContext());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof StripeInteracter) {
            stripeInteracter = (StripeInteracter) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement StripeInteracter");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stripeInteracter = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_stripe_card, container, false);
        cardCvv = view.findViewById(R.id.cardCvv);
        cardYear = view.findViewById(R.id.cardYear);
        cardMonth = view.findViewById(R.id.cardMonth);
        cardNumber = view.findViewById(R.id.cardNumber);
        progress_bar = view.findViewById(R.id.progress_bar);
        view.findViewById(R.id.continueCard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    Card card = new Card(cardNumber.getText().toString(), Integer.valueOf(cardMonth.getText().toString()), Integer.valueOf(cardYear.getText().toString()), cardCvv.getText().toString());
//                    if (!card.validateCard()) {
//                        Toast.makeText(getContext(), "Invalid Card Data", Toast.LENGTH_SHORT).show();
//                    } else {
//                        progress_bar.setVisibility(View.VISIBLE);
//                        Helper.closeKeyboard(getContext(), v);
//                        getStripeToken(card);
//                    }
//                } catch (NumberFormatException ex) {
//                    Toast.makeText(getContext(), "Invalid Card Data", Toast.LENGTH_SHORT).show();
//                }
            }
        });
        return view;
    }

//    private void getStripeToken(Card card) {
//        Stripe stripe = new Stripe(getContext(), "pk_test_8G7dB3nDfFrmNrv67pNNb9iV");
//        stripe.createToken(card, new TokenCallback() {
//            @Override
//            public void onError(Exception error) {
//                Log.e("STRIPE", error.getLocalizedMessage());
//            }
//
//            @Override
//            public void onSuccess(Token token) {
//                Log.d("STRIPE", token.getId());
//                initPaymentRequest(new PaymentRequest(token.getId()));
//            }
//        });
//    }

    private void initPaymentRequest(PaymentRequest paymentRequest) {
        ApiUtils.getClient().create(DrService.class).payment(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), paymentRequest).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                if (progress_bar != null) {
                    progress_bar.setVisibility(View.INVISIBLE);
                    if (response.isSuccessful()) {
                        if (response.body().getObject().equals("charge")) {
                            Toast.makeText(getContext(), "Payment success!", Toast.LENGTH_SHORT).show();

                            UserResponse userResponse = Helper.getLoggedInUser(sharedPreferenceUtil);
                            userResponse.setIs_paid(1);
                            Helper.setLoggedInUser(sharedPreferenceUtil, userResponse);

                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            getActivity().finish();
                        } else {
                            Toast.makeText(getContext(), "Payment failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                if (progress_bar != null) progress_bar.setVisibility(View.INVISIBLE);
            }
        });
    }
}
