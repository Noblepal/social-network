package com.famousindiasocialnetwork.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.request.UserUpdateRequest;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This activity is used as the starting Activity to display animation
 */
public class SplashScreenActivity extends AppCompatActivity {
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager facebookCallbackManager;
    private LoginButton facebookLoginButton;
    private FirebaseAuth mAuth;

    private SharedPreferenceUtil sharedPreferenceUtil;

    private TextView title;
    private LinearLayout authOptionsContainer;
    private SignInButton google_sign_in_button;
    private ProgressBar authProgress;

    private String post_id_deep_linked;
    private View titleContainer;

    private static final String TAG = "SplashScreenActivity";


    @Override
    public void onStart() {
        super.onStart();
        // Branch init
        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null && referringParams != null && referringParams.has("post_id")) {
                    try {
                        post_id_deep_linked = referringParams.getString("post_id");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, this.getIntent().getData(), this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        final boolean isLoggedIn = Helper.getLoggedInUser(sharedPreferenceUtil) != null;
        //final boolean isPaid = sharedPreferenceUtil.getBooleanPreference(Constants.KEY_PAID, false);
        initUi();
        if (isLoggedIn) {
            refreshToken();
        } else {
            setupAuth();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isLoggedIn) {
                    //openActivity(isPaid ? post_id_deep_linked != null ? DetailHomeItemActivity.newIntent(SplashScreenActivity.this, post_id_deep_linked) : new Intent(SplashScreenActivity.this, MainActivity.class) : new Intent(SplashScreenActivity.this, StripePaymentActivity.class));
                    openActivity(post_id_deep_linked != null ? DetailHomeItemActivity.newIntent(SplashScreenActivity.this, post_id_deep_linked) : new Intent(SplashScreenActivity.this, MainActivity.class));
                } else {
                    showAuthOptions();
                }
            }
        }, 800);
    }

    private void refreshToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    Log.d("Authorization", "Bearer " + idToken);
                    sharedPreferenceUtil.setStringPreference(Constants.KEY_API_KEY, "Bearer " + idToken);
                } else {
                    Log.e(SplashScreenActivity.class.getName(), task.getException().getMessage());
                    // Handle error -> task.getException();
                }
            }
        });
    }

    private void setupAuth() {
        mAuth = FirebaseAuth.getInstance();

        facebookCallbackManager = CallbackManager.Factory.create();
        facebookLoginButton.setReadPermissions("email", "public_profile");
        facebookLoginButton.registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e("FacebookLogin", exception.toString());
            }
        });
        facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authProgress.setVisibility(View.VISIBLE);
                google_sign_in_button.setClickable(false);
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(getString(R.string.web_client_id)).build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        google_sign_in_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
                authProgress.setVisibility(View.VISIBLE);
                facebookLoginButton.setClickable(false);
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        Log.d("FacebookLogin", "handleFacebookAccessToken:" + accessToken);
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            getToken(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FacebookLogin", "signInWithCredential:failure", task.getException());
                        }

                        // ...
                    }
                });
    }

    private void initUi() {
        title = findViewById(R.id.title);
        title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Montserrat_Bold.ttf"));
        titleContainer = findViewById(R.id.titleContainer);
        authOptionsContainer = findViewById(R.id.authOptionsContainer);
        facebookLoginButton = findViewById(R.id.login_button);
        google_sign_in_button = findViewById(R.id.google_sign_in_button);
        authProgress = findViewById(R.id.authProgress);

        TextView gSignInButtonText = (TextView) google_sign_in_button.getChildAt(0);
        if (gSignInButtonText != null)
            gSignInButtonText.setText("Google");
    }

    private void showAuthOptions() {
        Animation slide_up = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slide_up.setFillAfter(true);
        authOptionsContainer.setVisibility(View.VISIBLE);
        authOptionsContainer.startAnimation(slide_up);
        titleContainer.animate().translationY(-0.5f * authOptionsContainer.getHeight()).setDuration(600).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("GoogleSignIn", "Google sign in failed", e);
                // ...
            }
        }
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("GoogleSignIn", "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            getToken(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("GoogleSignIn", "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }

    private void getToken(FirebaseUser user) {
        user.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult getTokenResult) {
                String idToken = getTokenResult.getToken();
                sharedPreferenceUtil.setStringPreference(Constants.KEY_API_KEY, "Bearer " + idToken);
                getUser(idToken);
            }
        });
    }

    private void getUser(String token) {
        DrService service = ApiUtils.getClient().create(DrService.class);
        Log.e(TAG, "getUser: TOKEN: " + "Bearer " + token );
        service.createUpdateUser("Bearer " + token,
                new UserUpdateRequest("m",
                        FirebaseInstanceId.getInstance().getToken(),
                        true,
                        true,
                        true), 0).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                authProgress.setVisibility(View.INVISIBLE);
                if (response.isSuccessful()) {
                    Helper.setLoggedInUser(sharedPreferenceUtil, response.body());
                    //openActivity(response.body().getIs_paid() == 1 ? post_id_deep_linked != null ? DetailHomeItemActivity.newIntent(SplashScreenActivity.this, post_id_deep_linked) : new Intent(SplashScreenActivity.this, MainActivity.class) : new Intent(SplashScreenActivity.this, StripePaymentActivity.class));
                    openActivity(post_id_deep_linked != null ? DetailHomeItemActivity.newIntent(SplashScreenActivity.this, post_id_deep_linked) : new Intent(SplashScreenActivity.this, MainActivity.class));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                authProgress.setVisibility(View.INVISIBLE);
                t.getMessage();
            }
        });
    }

    private void openActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}