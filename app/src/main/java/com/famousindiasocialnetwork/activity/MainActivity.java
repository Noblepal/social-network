package com.famousindiasocialnetwork.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

//import com.anupcowkur.reservoir.Reservoir;
import com.famousindiasocialnetwork.adapter.UniversalPagerAdapter;
import com.famousindiasocialnetwork.fragment.ConfirmationDialogFragment;
import com.famousindiasocialnetwork.fragment.MyChatsFragment;
import com.famousindiasocialnetwork.fragment.PostTypeFragment;
import com.famousindiasocialnetwork.fragment.CommentsFragment;
import com.famousindiasocialnetwork.fragment.HomeFragment;
import com.famousindiasocialnetwork.fragment.NotificationFragment;
import com.famousindiasocialnetwork.fragment.PostFragment;
import com.famousindiasocialnetwork.fragment.ProfileFragment;
import com.famousindiasocialnetwork.fragment.SearchUserFragment;
import com.famousindiasocialnetwork.listener.ContextualModeInteractor;
import com.famousindiasocialnetwork.listener.MainInteractor;
import com.famousindiasocialnetwork.listener.OnFragmentStateChangeListener;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.listener.OnUserGroupItemClick;
import com.famousindiasocialnetwork.listener.ShowHideViewListener;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.model.UserRealm;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.famousindiasocialnetwork.util.SpringAnimationHelper;
import com.famousindiasocialnetwork.view.NonSwipeableViewPager;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;

public class MainActivity extends BaseChatActivity implements MainInteractor, View.OnClickListener, OnUserGroupItemClick, ContextualModeInteractor {
    public static final int REQUEST_CODE_DETAIL_ACTIVITY = 0;
    public static final int REQUEST_CODE_ADAPTER = 1;
    final String FRAG_TAG_SEARCH_USER = "fragSearchUser";
    private static final int REQUEST_CODE_CHAT_FORWARD = 99;
    private static String USER_SELECT_TAG = "userselectdialog";
    private static String CONFIRM_TAG = "confirmtag";

    LinearLayout bottomBar;
    NonSwipeableViewPager viewPager;
    Toolbar toolbar;
    TextView tvTitle;
    LinearLayout homeTitleContainer;
    ImageView homeTitleLogo;
    LinearLayout[] bottomImageViews = new LinearLayout[5];

    private SharedPreferenceUtil sharedPreferenceUtil;
    private UniversalPagerAdapter adapter;
    private MenuItem menuSearch, menuDelete;

    private SearchUserFragment searchUserFragment;
    private ArrayList<Post> bookmarks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomBar = findViewById(R.id.bottom_bar);
        viewPager = findViewById(R.id.main_activity_view_pager);
        tvTitle = findViewById(R.id.tv_title);
        homeTitleContainer = findViewById(R.id.ll_top);
        homeTitleLogo = findViewById(R.id.toolbarLogo);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this, R.style.MontserratBoldTextAppearance);

        bottomImageViews[0] = findViewById(R.id.bottom_bar_tab1);
        bottomImageViews[1] = findViewById(R.id.bottom_bar_tab2);
        bottomImageViews[2] = findViewById(R.id.bottom_bar_tab3);
        bottomImageViews[3] = findViewById(R.id.bottom_bar_tab4);
        bottomImageViews[4] = findViewById(R.id.bottom_bar_tab5);
        for (LinearLayout linearLayout : bottomImageViews)
            linearLayout.setOnClickListener(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        tvTitle.setText(getString(R.string.app_name).toUpperCase());
        //tvTitle.setTypeface(Helper.getMontserratBold(this));

        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        bookmarks = Helper.getBookmarkedPosts(sharedPreferenceUtil);

        initialiseAdMob();

        adapter = new UniversalPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new HomeFragment(showHideViewListener), getString(R.string.app_name).toUpperCase());
        adapter.addFrag(new MyChatsFragment(), getString(R.string.chats).toUpperCase());
        adapter.addFrag(new NotificationFragment(), getString(R.string.notification).toUpperCase());
        adapter.addFrag(new ProfileFragment(), getString(R.string.profile).toUpperCase());

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);

        selectTabIndex(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menuSearch = menu.getItem(0);
        this.menuDelete = menu.getItem(1);

        SearchView searchView = (SearchView) this.menuSearch.getActionView();
        searchView.setQueryHint("Search users..");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (getSupportFragmentManager().findFragmentByTag(FRAG_TAG_SEARCH_USER) == null) {
                    searchUserFragment = SearchUserFragment.newInstance(query);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down, R.anim.bottom_up, R.anim.bottom_down)
                            .add(R.id.frameLayout, searchUserFragment, FRAG_TAG_SEARCH_USER)
                            .addToBackStack(null)
                            .commit();
                } else {
                    searchUserFragment.newQuery(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        menuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                if (getSupportFragmentManager().findFragmentByTag(FRAG_TAG_SEARCH_USER) != null)
                    getSupportFragmentManager().popBackStackImmediate();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionDelete:
                confirmDelete();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void confirmDelete() {
        FragmentManager manager = getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag(CONFIRM_TAG);
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }

        ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance("Delete chat",
                "Continue deleting selected chats?",
                "Yes",
                "No",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MyChatsFragment myChatsFragment = (MyChatsFragment) adapter.getItem(1);
                        if (myChatsFragment != null) myChatsFragment.deleteSelectedChats();
                        disableContextualMode();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        disableContextualMode();
                    }
                });
        confirmationDialogFragment.show(manager, CONFIRM_TAG);
    }

    ShowHideViewListener showHideViewListener = new ShowHideViewListener() {
        @Override
        public void showView() {
            showBottomBar();
        }

        @Override
        public void hideView() {
            hideBottomBar();
        }
    };

    private void initialiseAdMob() {
        MobileAds.initialize(this, "admob");
    }

    /**
     * Highlights the view and shows the discription to the user. It is used to tell the user about the features
     */
//    private void showTapTargetView() {
//        if (sharedPreferenceUtil.getBooleanPreference(Constants.KEY_SHOW_TAP_TARGET_VIEW, true)) {
//            new TapTargetSequence(this)
//                    .targets(
//                            TapTarget
//                                    .forView(bottomImageViews[4], "Your Profile", "Your profile contains your total score, your posts, your activity logs etc.")
//                                    .cancelable(false)
//                                    .tintTarget(false)
//                                    .descriptionTextColor(android.R.color.black),
//                            TapTarget
//                                    .forView(bottomImageViews[0], "Post Anonymously!!", "Yes, you can compressAndUpload textual post, picture and video without revealing your identity")
//                                    .cancelable(false)
//                                    .tintTarget(false)
//                                    .descriptionTextColor(android.R.color.black))
//                    .start();
//            sharedPreferenceUtil.setBooleanPreference(Constants.KEY_SHOW_TAP_TARGET_VIEW, false);
//        }
//    }
    public void selectTabIndex(int index) {
        for (int i = 0; i < bottomImageViews.length; i++) {
            if (i == index) {
                SpringAnimationHelper.performAnimation(bottomImageViews[i]);
                int currentItem = i;
                if (currentItem > 2)
                    currentItem--;
                final int finalCurrentItem = currentItem;

                viewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        viewPager.setCurrentItem(finalCurrentItem);
                    }
                });
                homeTitleLogo.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
                tvTitle.setText(adapter.getPageTitle(currentItem));
//                getSupportActionBar().setDisplayShowTitleEnabled(index != 0);
//                getSupportActionBar().setTitle(adapter.getPageTitle(currentItem));
                //bottomImageViews[i].setBackgroundResource(R.drawable.top_border_primary_dark);
                bottomImageViews[i].setAlpha(1f);
                if (menuSearch != null && menuDelete != null) {
                    menuSearch.setVisible(currentItem == 0);
                    menuDelete.setVisible(currentItem == 1 && isContextualMode());
                }
            } else if (i != 2) {
                //bottomImageViews[i].setBackgroundResource(0);
                bottomImageViews[i].setAlpha(0.4f);
            }
        }
    }

    public void onHomeTabClicked() {
        selectTabIndex(0);
    }

    public void onBookmarksTabClicked() {
        selectTabIndex(1);
    }


    /**
     * Displays Fragment to post icon_text, icon_picture or video
     */
    public void onAddTabClicked() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        if (supportFragmentManager.findFragmentByTag(PostTypeFragment.class.getName()) == null) {
            PostTypeFragment postTypeFragment = PostTypeFragment.newInstance(new OnFragmentStateChangeListener() {
                @Override
                public void onDetach() {
                }

                @Override
                public void onPause() {
                    setAddNewView(true);
                }

                @Override
                public void onOther(String postType) {
                    openPostFragment(postType);
                    selectTabIndex(0);
                }
            });
            supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down, R.anim.bottom_up, R.anim.bottom_down)
                    .add(R.id.frameLayout, postTypeFragment, PostTypeFragment.class.getName())
                    .addToBackStack(null)
                    .commit();

            setAddNewView(false);

        } else {
            supportFragmentManager.popBackStackImmediate();
        }

    }

    public void openPostFragment(String type) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PostTypeFragment.class.getName());
        if (fragment != null) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        if (getSupportFragmentManager().findFragmentByTag(PostFragment.class.getName()) == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.frameLayout, PostFragment.newInstance(type), PostFragment.class.getName())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setAddNewView(boolean invisible) {
        int currentItem = viewPager.getCurrentItem();
        for (int i = 0; i < bottomImageViews.length; i++) {
            if (i == 2) {
                bottomImageViews[i].animate().setDuration(200).rotationBy(invisible ? -45 : 45).start();
            } else {
                bottomImageViews[i].setClickable(invisible);
                bottomImageViews[i].setFocusable(invisible);
                if (i == currentItem) {
                    //bottomImageViews[i].setBackgroundResource(invisible ? R.drawable.top_border_primary_dark : 0);
                    bottomImageViews[i].setAlpha(invisible ? 1f : 0.4f);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = Helper.getCurrentFragment(this);
        if (fragment instanceof CommentsFragment) {
            getSupportFragmentManager().popBackStackImmediate();
            return;
        } else if (isContextualMode()) {
            disableContextualMode();
        } else if (viewPager.getCurrentItem() != 0) {
            onHomeTabClicked();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (getSupportFragmentManager().findFragmentByTag(PostFragment.class.getName()) != null) {
            getSupportFragmentManager().findFragmentByTag(PostFragment.class.getName()).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    void myUsersResult(ArrayList<UserRealm> myUsers) {
        MyChatsFragment myChatsFragment = (MyChatsFragment) adapter.getItem(1);
        if (myChatsFragment != null) myChatsFragment.notifyMyUsersUpdate(myUsers);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(bookmarkPostEventReceiver, new IntentFilter(Constants.BOOKMARK_EVENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bookmarkPostEventReceiver);
    }

    private BroadcastReceiver bookmarkPostEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Constants.BOOKMARK_EVENT)) {
                Post post = intent.getParcelableExtra("post");
                if (post != null) {
                    int pos = bookmarks.indexOf(post);
                    if (pos != -1) {
                        bookmarks.remove(pos);
                    } else {
                        bookmarks.add(post);
                    }
                    Helper.setBookmarkedPosts(sharedPreferenceUtil, bookmarks);
                }
            }
        }
    };

    public void hideBottomBar() {
        Animation slide_down = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        slide_down.setFillAfter(true);
        slide_down.setDuration(200);
        slide_down.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bottomBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bottomBar.startAnimation(slide_down);
    }

    public void showBottomBar() {
        Animation slide_up = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slide_up.setFillAfter(true);
        slide_up.setDuration(200);
        bottomBar.setVisibility(View.VISIBLE);
        bottomBar.startAnimation(slide_up);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_bar_tab3:
                onAddTabClicked();
                break;
            case R.id.bottom_bar_tab2:
                selectTabIndex(1);
                break;
            case R.id.bottom_bar_tab1:
                selectTabIndex(0);
                break;
            case R.id.bottom_bar_tab5:
                selectTabIndex(4);
                break;
            case R.id.bottom_bar_tab4:
                selectTabIndex(3);
                break;
        }
    }

    @Override
    public void enableContextualMode() {
        if (menuDelete != null) menuDelete.setVisible(true);
    }

    @Override
    public boolean isContextualMode() {
        return menuDelete != null && menuDelete.isVisible();
    }

    @Override
    public void updateSelectedCount(int count) {
        if (count > 0) {
            tvTitle.setText(String.format("%d selected", count));
        } else {
            disableContextualMode();
        }
    }

    @Override
    public void OnUserClick(UserRealm user, int position, View userImage) {
        Intent intent = MessagesActivity.newIntent(this, null, user);
        startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD);
    }

    public void disableContextualMode() {
        if (menuDelete != null) menuDelete.setVisible(false);
        tvTitle.setText(adapter.getPageTitle(viewPager.getCurrentItem()));
        MyChatsFragment myChatsFragment = (MyChatsFragment) adapter.getItem(1);
        if (myChatsFragment != null) myChatsFragment.disableContextualMode();
    }

    @Override
    public ArrayList<Post> getBookmarkPosts() {
        return bookmarks;
    }
}