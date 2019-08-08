package com.getstream.sdk.chat.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.getstream.sdk.chat.ChannelRepository;
import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.rest.Message;
import com.getstream.sdk.chat.rest.User;

import java.util.List;

public class ChannelViewModel extends AndroidViewModel {
    private ChannelRepository channelRepository;

    public ChannelViewModel(Application application, Channel channel) {
        super(application);
        channelRepository = new ChannelRepository(channel);
    }

    public synchronized LiveData<List<Message>> getMessages() {
        return channelRepository.getMessages();
    }

    public synchronized LiveData<List<User>> getTypingUsers() {
        return channelRepository.getTypingUsers();
    }

    public synchronized LiveData<Number> getWatcherCount() {
        return channelRepository.getWatcherCount();
    }
}
