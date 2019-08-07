package com.getstream.sdk.chat.view;


import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.rest.Client;

import org.json.JSONObject;

public interface StreamView {
    void setClient(Client client);
    void setChannel(Channel client);
}