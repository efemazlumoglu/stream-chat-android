package com.getstream.sdk.chat.function;

import android.app.Activity;

import com.getstream.sdk.chat.adapter.CommandListItemAdapter;
import com.getstream.sdk.chat.databinding.MyCustomBinding;
import com.getstream.sdk.chat.model.Command;
import com.getstream.sdk.chat.model.Channel;

import java.util.List;

public class CommandFunction {
    private static final String TAG = SendFileFunction.class.getSimpleName();

    CommandListItemAdapter adapter = null;

    List<Command> commands = null;
    MyCustomBinding binding;

    Activity activity;
    Channel channel;

    public CommandFunction(Activity activity, MyCustomBinding binding, Channel channel) {
        this.activity = activity;
        this.binding = binding;
        this.channel = channel;
    }
}
