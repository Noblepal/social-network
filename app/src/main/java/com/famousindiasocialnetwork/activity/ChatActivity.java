package com.famousindiasocialnetwork.activity;

import android.app.Activity;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.mxn.soul.flowingdrawer_core.ElasticDrawer;
import com.mxn.soul.flowingdrawer_core.FlowingDrawer;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.adapter.MenuUsersRecyclerAdapter;
import com.famousindiasocialnetwork.fragment.ConfirmationDialogFragment;
import com.famousindiasocialnetwork.fragment.MyChatsFragment;
import com.famousindiasocialnetwork.fragment.UserSelectDialogFragment;
import com.famousindiasocialnetwork.listener.ContextualModeInteractor;
import com.famousindiasocialnetwork.listener.OnUserGroupItemClick;
import com.famousindiasocialnetwork.listener.UserGroupSelectionDismissListener;
import com.famousindiasocialnetwork.model.Message;
import com.famousindiasocialnetwork.model.UserRealm;
import com.famousindiasocialnetwork.service.FetchMyUsersService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChatActivity extends BaseChatActivity implements OnUserGroupItemClick, ContextualModeInteractor {
    private static final int REQUEST_CODE_CHAT_FORWARD = 99;
    private static String USER_SELECT_TAG = "userselectdialog";
    private static String CONFIRM_TAG = "confirmtag";

    private RecyclerView menuRecyclerView;
    private SwipeRefreshLayout swipeMenuRecyclerView;
    private FlowingDrawer drawerLayout;
    private EditText searchContact;
    private Menu menu;

    private MenuUsersRecyclerAdapter menuUsersRecyclerAdapter;
    private ArrayList<UserRealm> myUsers = new ArrayList<>();
    private ArrayList<Message> messageForwardList = new ArrayList<>();
    private UserSelectDialogFragment userSelectDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initUi();
        setupMenu();
        getSupportFragmentManager().beginTransaction().replace(R.id.chatsFrame, new MyChatsFragment(), "chats").commit();
        fetchContacts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chats, menu);
        this.menu = menu;
        menu.getItem(1).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionNav:
                drawerLayout.openMenu(true);
                return true;
            case R.id.actionDelete:
                confirmDelete();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    void myUsersResult(ArrayList<UserRealm> myUsers) {
        this.myUsers.clear();
        this.myUsers.addAll(myUsers);
        MyChatsFragment myChatsFragment = (MyChatsFragment) getSupportFragmentManager().findFragmentByTag("chats");
        if (myChatsFragment != null) myChatsFragment.notifyMyUsersUpdate(myUsers);
        Collections.sort(myUsers, new Comparator<UserRealm>() {
            @Override
            public int compare(UserRealm user1, UserRealm user2) {
                return user1.getName().compareToIgnoreCase(user2.getName());
            }
        });
        menuUsersRecyclerAdapter.notifyDataSetChanged();
        swipeMenuRecyclerView.setRefreshing(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (ElasticDrawer.STATE_CLOSED != drawerLayout.getDrawerState()) {
            drawerLayout.closeMenu(true);
        } else if (isContextualMode()) {
            disableContextualMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (REQUEST_CODE_CHAT_FORWARD):
                if (resultCode == Activity.RESULT_OK) {
                    //show forward dialog to choose users
                    messageForwardList.clear();
                    ArrayList<Message> temp = data.getParcelableArrayListExtra("FORWARD_LIST");
                    messageForwardList.addAll(temp);
                    userSelectDialogFragment = UserSelectDialogFragment.newInstance(myUsers, new UserGroupSelectionDismissListener() {
                        @Override
                        public void onUserGroupSelectDialogDismiss() {
                            messageForwardList.clear();
                        }

                        @Override
                        public void selectionDismissed() {

                        }
                    });
                    FragmentManager manager = getSupportFragmentManager();
                    Fragment frag = manager.findFragmentByTag(USER_SELECT_TAG);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    userSelectDialogFragment.show(manager, USER_SELECT_TAG);
                }
                break;
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
                        MyChatsFragment myChatsFragment = (MyChatsFragment) getSupportFragmentManager().findFragmentByTag("chats");
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

    private void setupMenu() {
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuUsersRecyclerAdapter = new MenuUsersRecyclerAdapter(this, myUsers);
        menuRecyclerView.setAdapter(menuUsersRecyclerAdapter);
        swipeMenuRecyclerView.setColorSchemeResources(R.color.colorAccent);
        swipeMenuRecyclerView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchContacts();
            }
        });
        searchContact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                menuUsersRecyclerAdapter.getFilter().filter(editable.toString());
            }
        });
    }

    private void fetchContacts() {
        if (!FetchMyUsersService.IN_PROGRESS) {
            if (!swipeMenuRecyclerView.isRefreshing())
                swipeMenuRecyclerView.setRefreshing(true);
            startService(new Intent(this, FetchMyUsersService.class));
        }
    }

    private void initUi() {
        findViewById(R.id.ll_top).setVisibility(View.GONE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this, R.style.MontserratBoldTextAppearance);
        toolbar.setTitle("CHATS");
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left);
        }

        menuRecyclerView = findViewById(R.id.menu_recycler_view);
        swipeMenuRecyclerView = findViewById(R.id.menu_recycler_view_swipe_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        searchContact = findViewById(R.id.searchContact);
        drawerLayout.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL);
        findViewById(R.id.addConversation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openMenu(true);
            }
        });
    }

    public void disableContextualMode() {
        if (menu != null) {
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
        }
        getSupportActionBar().setTitle("CHATS");
        MyChatsFragment myChatsFragment = (MyChatsFragment) getSupportFragmentManager().findFragmentByTag("chats");
        if (myChatsFragment != null) myChatsFragment.disableContextualMode();
    }

    @Override
    public void enableContextualMode() {
        if (menu != null) {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(true);
        }
    }

    @Override
    public boolean isContextualMode() {
        return menu != null && menu.getItem(1).isVisible();
    }

    @Override
    public void updateSelectedCount(int count) {
        if (count > 0) {
            getSupportActionBar().setTitle(String.format("%d selected", count));
        } else {
            disableContextualMode();
        }
    }

    @Override
    public void OnUserClick(UserRealm user, int position, View userImage) {
        if (ElasticDrawer.STATE_CLOSED != drawerLayout.getDrawerState()) {
            drawerLayout.closeMenu(true);
        }

        Intent intent = MessagesActivity.newIntent(this, messageForwardList, user);
        startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD);

        if (userSelectDialogFragment != null)
            userSelectDialogFragment.dismiss();
    }
}
