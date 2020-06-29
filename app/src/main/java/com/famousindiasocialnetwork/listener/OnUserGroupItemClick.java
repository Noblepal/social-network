package com.famousindiasocialnetwork.listener;

import android.view.View;

import com.famousindiasocialnetwork.model.UserRealm;

public interface OnUserGroupItemClick {
    void OnUserClick(UserRealm user, int position, View userImage);
}
