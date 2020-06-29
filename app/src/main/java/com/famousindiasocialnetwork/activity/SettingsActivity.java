package com.famousindiasocialnetwork.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.famousindiasocialnetwork.fragment.ConfirmationDialogFragment;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.fragment.SettingsPushNotification;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private ActionBar actionBar;
    private FragmentManager supportFragmentManager;
    private static String CONFIRM_TAG = "confirmtag";

    TextView[] textViews = new TextView[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textViews[0] = findViewById(R.id.settings_profile_edit);
        textViews[1] = findViewById(R.id.settings_push_notification);
        textViews[2] = findViewById(R.id.feedback);
        textViews[3] = findViewById(R.id.share);
        textViews[4] = findViewById(R.id.rate);
        textViews[5] = findViewById(R.id.logout);

        findViewById(R.id.ll_top).setVisibility(View.GONE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this, R.style.MontserratBoldTextAppearance);
        toolbar.setTitle("SETTINGS");
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left);
        }
        supportFragmentManager = getSupportFragmentManager();


        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setOnClickListener(this);
            Animation inAnimation = AnimationUtils.makeInAnimation(this, false);
            inAnimation.setDuration(500);
            inAnimation.setStartOffset(i * 100);
            textViews[i].startAnimation(inAnimation);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.activity_settings_menu_done).setVisible(false);
        setActionBarSubtitle("");
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void setActionBarSubtitle(String subtitle) {
        actionBar.setSubtitle(subtitle);
    }

    /**
     * Opens appropriate settings fragment according to the selection by the user
     *
     * @param view
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.settings_profile_edit:
                ProfileResponse profileMe = Helper.getProfileMe(new SharedPreferenceUtil(this));
                if (profileMe != null)
                    startActivity(EditProfileActivityActivity.newInstance(this, profileMe, false));
                break;
            case R.id.settings_push_notification:
                openFragment(new SettingsPushNotification(), SettingsPushNotification.class.getName());
                break;
            case R.id.share:
                Helper.openShareIntent(this, null, "http://play.google.com/store/apps/details?id=" + getPackageName());
                break;
            case R.id.rate:
                Helper.openPlayStoreIntent(this);
                break;
            case R.id.feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                break;
            case R.id.logout:
                FragmentManager manager = getSupportFragmentManager();
                Fragment frag = manager.findFragmentByTag(CONFIRM_TAG);
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }

                ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance("Logout",
                        "Are you sure you want to logout?",
                        "Logout",
                        "Cancel",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                FirebaseAuth.getInstance().signOut();
                                logout();
                                Intent intent = new Intent(SettingsActivity.this, SplashScreenActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                            }
                        });
                confirmationDialogFragment.show(manager, CONFIRM_TAG);
                break;
        }
    }

    private void logout() {
        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(SettingsActivity.this);
        sharedPreferenceUtil.removePreference(Constants.USER);
        sharedPreferenceUtil.removePreference(Constants.NOTIFICATION_SETTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.BROADCAST_LOGOUT));
    }

    /**
     * It opens the fragment object passed to the function
     *
     * @param fragment     Fragment to be opened
     * @param fragmentName Tag name of the Fragment
     */
    private void openFragment(Fragment fragment, String fragmentName) {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.activity_settings_container, fragment, fragmentName)
                .addToBackStack(fragmentName)
                .commit();
    }
}
