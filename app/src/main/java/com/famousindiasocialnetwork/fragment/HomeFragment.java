package com.famousindiasocialnetwork.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
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
import com.google.android.material.tabs.TabLayout;

public class HomeFragment extends Fragment {
    private ShowHideViewListener showHideViewListener;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private MainInteractor mListener;

    private ViewPager viewPager;
    private TabLayout tabLayout;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        viewPager = view.findViewById(R.id.activity_profile_view_pager);
        tabLayout = view.findViewById(R.id.frag_profile_tab_layout);
        return view;
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
}
