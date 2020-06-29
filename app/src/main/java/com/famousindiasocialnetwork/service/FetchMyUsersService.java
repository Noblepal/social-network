package com.famousindiasocialnetwork.service;

import android.app.IntentService;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.famousindiasocialnetwork.model.UserRealm;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.BaseListModel;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

public class FetchMyUsersService extends IntentService {
    private SharedPreferenceUtil sharedPreferenceUtil;
    private UserResponse userMe;
    private ArrayList<UserRealm> myUsers;
    public static boolean IN_PROGRESS = false;

    public FetchMyUsersService() {
        super("FetchMyUsersService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        IN_PROGRESS = true;
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        userMe = Helper.getLoggedInUser(sharedPreferenceUtil);
        if (userMe != null) {
            myUsers = new ArrayList<>();
            fetchMyUsers(1);
            broadcastMyUsers();
        }
        IN_PROGRESS = false;
    }

    private void fetchMyUsers(int page) {
        Call<BaseListModel<UserResponse>> myUsersCall = ApiUtils.getClient().create(DrService.class).getFollowings(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), userMe.getId(), page);
        try {
            Response<BaseListModel<UserResponse>> response = myUsersCall.execute();
            if (response != null && response.isSuccessful()) {
                BaseListModel<UserResponse> body = response.body();
                if (body != null) {
                    for (UserResponse userResponse : body.getData())
                        if (!userResponse.getId().equals(userMe.getId()) && userResponse.getIs_following() == 1)
                            myUsers.add(UserRealm.fromUserResponse(userResponse));
                    if (body.getData() != null && !body.getData().isEmpty()) {
                        fetchMyUsers(body.getCurrent_page() + 1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMyUsers() {
        if (this.myUsers != null) {
            Intent intent = new Intent(Constants.BROADCAST_MY_USERS);
            intent.putParcelableArrayListExtra("data", this.myUsers);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(intent);
        }
    }
}
