package com.famousindiasocialnetwork.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.famousindiasocialnetwork.activity.BookmarksActivity;
import com.famousindiasocialnetwork.activity.FollowerFollowingActivity;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.activity.SettingsActivity;
import com.famousindiasocialnetwork.listener.MainInteractor;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.famousindiasocialnetwork.network.ApiUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private ImageView userImage;
    private TextView userPostsCount, userFollowersCount, userFollowingCount;
    private TextView userName;
    private ProgressBar profileRefreshProgress;

    private DrService foxyService;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private ProfileResponse profileMe;
    private UserResponse userMe;
    private MainInteractor mListener;
    private FloatingActionButton fab_setting, fab_bookmarks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceUtil = new SharedPreferenceUtil(getContext());
        foxyService = ApiUtils.getClient().create(DrService.class);
        userMe = Helper.getLoggedInUser(sharedPreferenceUtil);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainInteractor) {
            mListener = (MainInteractor) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement MainInteractor");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_profile, container, false);
        userImage = view.findViewById(R.id.userImage);
        userName = view.findViewById(R.id.fullName);
        userPostsCount = view.findViewById(R.id.userPostsCount);
        userFollowersCount = view.findViewById(R.id.userFollowersCount);
        userFollowingCount = view.findViewById(R.id.userFollowingCount);
        profileRefreshProgress = view.findViewById(R.id.profileRefreshProgress);
        view.findViewById(R.id.followerCountContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userMe != null)
                    startActivity(FollowerFollowingActivity.newInstance(getContext(), userMe.getId(), "Followers"));
            }
        });
        view.findViewById(R.id.followingCountContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userMe != null)
                    startActivity(FollowerFollowingActivity.newInstance(getContext(), userMe.getId(), "Followings"));
            }
        });

        if (userMe != null)
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.profileFrame, HomeFeedsFragment.newInstance("study", mListener.getBookmarkPosts(), userMe.getId(), false, null), "my_feed")
                    .commit();

        fab_setting = view.findViewById(R.id.fab_setting);
        fab_bookmarks = view.findViewById(R.id.fab_bookmarks);
        fab_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SettingsActivity.class));
            }
        });
        fab_bookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), BookmarksActivity.class));
            }
        });


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileMe = Helper.getProfileMe(sharedPreferenceUtil);
        if (profileMe != null) {
            setDetails();
        }
        if (userMe != null) {
            refreshProfile();
        }
    }

    private void refreshProfile() {
        profileRefreshProgress.setVisibility(View.VISIBLE);
        foxyService.getProfile(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), userMe.getId()).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                profileRefreshProgress.setVisibility(View.INVISIBLE);
                if (response.isSuccessful()) {
                    profileMe = response.body();
                    Helper.saveProfileMe(sharedPreferenceUtil, profileMe);
                    setDetails();
//                    if (TextUtils.isEmpty(profileMe.getName())) {
//                        startActivity(EditProfileActivityActivity.newInstance(getContext(), profileMe, true));
//                    }
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                profileRefreshProgress.setVisibility(View.INVISIBLE);
                t.getMessage();
            }
        });
    }

    private void setDetails() {
        Glide.with(getContext()).load(profileMe.getImage())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(getContext(), 8))).placeholder(R.drawable.ic_person_white_72dp)).into(userImage);
        userName.setText(profileMe.getName());
        userPostsCount.setText(String.valueOf(profileMe.getPosts_count()));
        userFollowersCount.setText(String.valueOf(profileMe.getFollowers_count()));
        userFollowingCount.setText(String.valueOf(profileMe.getFollowing_count()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userMe != null) {
            refreshProfile();

            if (sharedPreferenceUtil.getBooleanPreference(Constants.KEY_UPDATED, false)) {
                sharedPreferenceUtil.setBooleanPreference(Constants.KEY_UPDATED, false);
                profileMe = Helper.getProfileMe(sharedPreferenceUtil);

                Glide.with(getContext()).load(sharedPreferenceUtil.getStringPreference(userMe.getImage(), profileMe.getImage()))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(getContext(), 8))).placeholder(R.drawable.ic_person_white_72dp)).into(userImage);
                userName.setText(sharedPreferenceUtil.getStringPreference(userMe.getName(), profileMe.getName()));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Constants.PROFILE_CHANGE_EVENT));
                    }
                }, 200);
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && userMe != null) {
            refreshProfile();
        }
    }
}
