package com.famousindiasocialnetwork.listener;

import com.famousindiasocialnetwork.model.Message;

public interface OnMessageItemClick {
    void OnMessageClick(Message message, int position);

    void OnMessageLongClick(Message message, int position);
}
