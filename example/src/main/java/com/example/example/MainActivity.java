package com.example.example;
import android.os.Bundle;
import android.util.Log;

import com.getstream.sdk.chat.component.Component;
import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.core.StreamChat;
import com.getstream.sdk.chat.view.activity.ChatActivity;

import java.util.HashMap;

public class MainActivity extends ChatActivity {

    private final String API_KEY = "u4nadyhunvhz";
    private final String USER_NAME = "Mute dawn";
    private final String USER_ID = "mute-dawn-0";
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "start it here");
        setContentView(R.layout.activity_main);

        StreamChat streamChat = new StreamChat(API_KEY);

        HashMap<String, Object> extraData = new HashMap<>();
        extraData.put("name", USER_NAME);
        extraData.put("image", "https://stepupandlive.files.wordpress.com/2014/09/3d-animated-frog-image.jpg");

        User user = new User(USER_ID, extraData);

        try {
            streamChat.setUser(user, "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoibXV0ZS1kYXduLTAifQ.Ws-J8PU_bU02txq_Owar-dmLII-XkPHoM_XF1SMB49g");
        } catch (Exception e){
            e.printStackTrace();
        }

        Log.i(TAG, "Made it here");

        Channel channel = new Channel("message", "general", null);
        streamChat.setChannel(channel);



        super.onCreate(savedInstanceState);
    }
}