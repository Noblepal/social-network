package com.famousindiasocialnetwork.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.fragment.HomeFeedsFragment;
import com.famousindiasocialnetwork.model.UserRealm;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.network.response.ProfileFollowResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by a_man on 09-02-2018.
 */

public class UserProfileDetailActivity extends AppCompatActivity {
    private static String EXTRA_DATA_USER_ID = "UserResponseId";
    private static String EXTRA_DATA_USER_NAME = "UserResponseName";
    private static String EXTRA_DATA_USER_IMAGE = "UserResponseImage";

    private ImageView profileImage;
    private TextView userPostsCount, userFollowersCount, userFollowingCount;
    private TextView profileName;
    private ProgressBar progressBar;
    private FloatingActionButton floatingActionButton;
    private View followerCountContainer, followingCountContainer;

    private DrService owhloService;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private ProfileResponse userProfile;

    private int userId;
    private String userName, userImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_detail);

        owhloService = ApiUtils.getClient().create(DrService.class);
        sharedPreferenceUtil = new SharedPreferenceUtil(this);

        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_DATA_USER_ID) || !intent.hasExtra(EXTRA_DATA_USER_NAME) || !intent.hasExtra(EXTRA_DATA_USER_IMAGE)) {
            finish();
        } else {
            userId = intent.getIntExtra(EXTRA_DATA_USER_ID, -1);
            userName = intent.getStringExtra(EXTRA_DATA_USER_NAME);
            userImage = intent.getStringExtra(EXTRA_DATA_USER_IMAGE);
            initUi();
            setDetails();
            loadDetails();
        }

    }

    private void loadDetails() {
        owhloService.getProfile(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), userId).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    userProfile = response.body();
                    setDetails();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                t.getMessage();
            }
        });
    }

    private void setDetails() {
        Glide.with(this).load(userProfile != null ? userProfile.getImage() : userImage)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(this, 8))).placeholder(R.drawable.ic_person_white_72dp)).into(profileImage);
        profileName.setText(userProfile != null ? userProfile.getName() : userName);
        if (userProfile != null) {
            userPostsCount.setText(String.valueOf(userProfile.getPosts_count()));
            userFollowersCount.setText(String.valueOf(userProfile.getFollowers_count()));
            userFollowingCount.setText(String.valueOf(userProfile.getFollowing_count()));
            floatingActionButton.setImageDrawable(ContextCompat.getDrawable(this, userProfile.getIs_following() == 1 ? R.drawable.ic_done_white_24dp : R.drawable.ic_person_add_white_24dp));
        }
    }

    private void initUi() {
        findViewById(R.id.ll_top).setVisibility(View.GONE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left);
            actionBar.setTitle(userName);
        }
        profileImage = findViewById(R.id.userImage);
        profileName = findViewById(R.id.fullName);
        userPostsCount = findViewById(R.id.userPostsCount);
        userFollowersCount = findViewById(R.id.userFollowersCount);
        userFollowingCount = findViewById(R.id.userFollowingCount);

        progressBar = findViewById(R.id.profileRefreshProgress);
        floatingActionButton = findViewById(R.id.fab_setting);
        UserResponse userResponse = Helper.getLoggedInUser(sharedPreferenceUtil);
        floatingActionButton.setVisibility((userResponse != null ? userResponse.getId() : -1) == userId ? View.GONE : View.VISIBLE);
        floatingActionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_person_add_white_24dp));
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionProfile();
            }
        });
        followerCountContainer = findViewById(R.id.followerCountContainer);
        followingCountContainer = findViewById(R.id.followingCountContainer);
        followerCountContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userProfile != null)
                    startActivity(FollowerFollowingActivity.newInstance(UserProfileDetailActivity.this, userProfile.getId(), "Followers"));
            }
        });
        followingCountContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userProfile != null)
                    startActivity(FollowerFollowingActivity.newInstance(UserProfileDetailActivity.this, userProfile.getId(), "Followings"));
            }
        });
        followerCountContainer.setClickable(userProfile != null && userProfile.getIs_following() == 1);
        followingCountContainer.setClickable(userProfile != null && userProfile.getIs_following() == 1);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab_bookmarks);
        floatingActionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_message_white_24dp));
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userProfile != null)
                    startActivity(MessagesActivity.newIntent(UserProfileDetailActivity.this, null, UserRealm.fromUserResponse(userProfile)));
            }
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.profileFrame, HomeFeedsFragment.newInstance("study", null, userId, false, null), "user_feed")
                .commit();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView empty_view_text = findViewById(R.id.empty_view_text);
                if (empty_view_text != null) {
                    empty_view_text.setText("You need to follow " + userName + " to see posts.");
                }
                TextView empty_view_sub_text = findViewById(R.id.empty_view_sub_text);
                if (empty_view_sub_text != null) {
                    empty_view_sub_text.setVisibility(View.GONE);
                }
            }
        }, 500);
    }

    private void actionProfile() {
        floatingActionButton.setClickable(false);
        progressBar.setVisibility(View.VISIBLE);
        owhloService.profileFollowAction(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), userId).enqueue(new Callback<ProfileFollowResponse>() {
            @Override
            public void onResponse(Call<ProfileFollowResponse> call, Response<ProfileFollowResponse> response) {
                refreshFeeds();
                floatingActionButton.setClickable(true);
                progressBar.setVisibility(View.GONE);
                followerCountContainer.setClickable(response.isSuccessful() && response.body().isFollowed());
                followingCountContainer.setClickable(response.isSuccessful() && response.body().isFollowed());
                if (response.isSuccessful() && response.body().getSuccess() != 0) {
                    floatingActionButton.setImageDrawable(ContextCompat.getDrawable(UserProfileDetailActivity.this, response.body().isFollowed() ? R.drawable.ic_done_white_24dp : R.drawable.ic_person_add_white_24dp));
                }
            }

            @Override
            public void onFailure(Call<ProfileFollowResponse> call, Throwable t) {
                floatingActionButton.setClickable(true);
                progressBar.setVisibility(View.GONE);
                refreshFeeds();
            }
        });
    }

    private void refreshFeeds() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm != null) {
            HomeFeedsFragment homeFeedsFragment = (HomeFeedsFragment) fm.findFragmentByTag("user_feed");
            if (homeFeedsFragment != null) homeFeedsFragment.refresh();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static Intent newInstance(Context context, int userId, String userName, String userImage) {
        Intent intent = new Intent(context, UserProfileDetailActivity.class);
        intent.putExtra(EXTRA_DATA_USER_ID, userId);
        intent.putExtra(EXTRA_DATA_USER_NAME, userName);
        intent.putExtra(EXTRA_DATA_USER_IMAGE, userImage);
        return intent;
    }
}
