package com.famousindiasocialnetwork.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.adapter.UniversalPagerAdapter;
import com.famousindiasocialnetwork.listener.MainInteractor;
import com.famousindiasocialnetwork.listener.ShowHideViewListener;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {
    private ShowHideViewListener showHideViewListener;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private MainInteractor mListener;

    private ViewPager viewPager;
    private TabLayout tabLayout;

    private static final String TAG = "HomeFragment";

    private View rootView = null;

    private AppUpdateManager appUpdateManager;
    private static final int RC_APP_UPDATE = 11;

    public HomeFragment() {
    }

    @SuppressLint("ValidFragment")
    public HomeFragment(ShowHideViewListener showHideViewListener) {
        this();
        this.showHideViewListener = showHideViewListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceUtil = new SharedPreferenceUtil(getContext());
        Log.e(TAG, "onCreate: On create");
        appUpdateManager = AppUpdateManagerFactory.create(getActivity());
        appUpdateManager.registerListener(installListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        viewPager = rootView.findViewById(R.id.activity_profile_view_pager);
        tabLayout = rootView.findViewById(R.id.frag_profile_tab_layout);

        checkUpdate();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewPager();
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

    private void initViewPager() {
        UniversalPagerAdapter adapter = new UniversalPagerAdapter(getChildFragmentManager());
        adapter.addFrag(HomeFeedsFragment.newInstance("hot", mListener.getBookmarkPosts(), -1, true, showHideViewListener), "Home");
        adapter.addFrag(HomeFeedsFragment.newInstance("feed", mListener.getBookmarkPosts(), -1, false, showHideViewListener), "Private Feeds");
        // adapter.addFrag(FollowersFragment.newInstance(), "Followers");
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        reduceMarginsInTabs(tabLayout, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25f, getResources().getDisplayMetrics()));
    }

    public static void reduceMarginsInTabs(TabLayout tabLayout, int marginOffset) {
        View tabStrip = tabLayout.getChildAt(0);
        if (tabStrip instanceof ViewGroup) {
            ViewGroup tabStripGroup = (ViewGroup) tabStrip;
            for (int i = 0; i < ((ViewGroup) tabStrip).getChildCount(); i++) {
                View tabView = tabStripGroup.getChildAt(i);
                if (tabView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) tabView.getLayoutParams()).leftMargin = marginOffset;
                    ((ViewGroup.MarginLayoutParams) tabView.getLayoutParams()).rightMargin = marginOffset;
                }
            }

            tabLayout.requestLayout();
        }
    }

    public void checkUpdate() {
        Task appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        Log.d(TAG, "checkUpdate: Checking for updates...");

        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo result) {
                if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    // Request the update.
                    Log.d(TAG, "Update available!");

                    try {
                        appUpdateManager.startUpdateFlowForResult(result,
                                AppUpdateType.FLEXIBLE, getActivity(), RC_APP_UPDATE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "No Update available!");
                }
            }
        });
    }

    private InstallStateUpdatedListener installListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(InstallState state) {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Log.d(TAG, "An update has been downloaded");
                showSnackBarForCompleteUpdate();
            } else if (state.installStatus() == InstallStatus.INSTALLED) {
                if (appUpdateManager != null) {
                    appUpdateManager.unregisterListener(installListener);
                }
            }
        }
    };

    private void showSnackBarForCompleteUpdate() {
        Snackbar snackbar = Snackbar.make(rootView.findViewById(R.id.frag_profile_tab_layout),
                "New update is ready!",
                Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("Install", view -> {
            if (appUpdateManager != null) {
                appUpdateManager.completeUpdate();
            }
        });

        snackbar.setActionTextColor(getResources().getColor(R.color.white));
        snackbar.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "onActivityResult: app download failed");
            } else {
                Log.e(TAG, "onActivityResult: app download complete");
            }
        }
    }
}
