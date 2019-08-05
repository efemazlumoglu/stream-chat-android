package com.getstream.sdk.chat.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.getstream.sdk.chat.rest.response.ChannelResponse;

public class ChatActivityViewModelFactory implements ViewModelProvider.Factory {
    private ChannelResponse channelResponse;

    public ChatActivityViewModelFactory(ChannelResponse channelResponse){
        this.channelResponse = channelResponse;
    }

    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatActivityViewModel.class)) {
            return (T) new ChatActivityViewModel(channelResponse);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
