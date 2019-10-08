package io.getstream.chat.example;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.getstream.sdk.chat.StreamChat;
import com.getstream.sdk.chat.enums.FilterObject;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.core.Client;
import com.getstream.sdk.chat.viewmodel.ChannelListViewModel;

import java.util.HashMap;

import io.getstream.chat.example.databinding.FragmentChatBinding;

import static com.getstream.sdk.chat.enums.Filters.and;
import static com.getstream.sdk.chat.enums.Filters.eq;


public class ChatFragment extends Fragment implements View.OnTouchListener, View.OnClickListener {

    public static final String EXTRA_CHANNEL_TYPE = "com.example.chattutorial.CHANNEL_TYPE";
    public static final String EXTRA_CHANNEL_ID = "com.example.chattutorial.CHANNEL_ID";

    private static final String TAG = "ChatFragment";
    private RelativeLayout mTextHolder;
    private ImageView mInfo1;
    private ImageView mInfo2;
    private ImageView mDotTrip, mDotChat;
    //    private RecyclerView mRecycler;
    private View mView;
    private int mEnableChatId;
    private OnTripSelected mCallback;

    private int mUserId = 0;

    private FragmentChatBinding mBinding;

    public static ChatFragment newInstance() {
        Bundle args = new Bundle();
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        mCallback = (OnTripSelected) context;
    }

    public interface OnTripSelected {
        void onTripSelected();

        boolean haveNewTrips();

        boolean chatFromNotification();

        int userIdFromChat();

        void resetNotifyChat();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);
        mView = mBinding.getRoot();
        View goToChat = mView.findViewById(R.id.rlChatsTripHolder);
        goToChat.setOnTouchListener(this);
        goToChat.setOnClickListener(this);
        mTextHolder = mView.findViewById(R.id.rlChatsEmptyChatsHolder);
        mInfo1 = mView.findViewById(R.id.ivChatsInfo1);
        mInfo2 = mView.findViewById(R.id.ivChatsInfo2);
        mDotChat = mView.findViewById(R.id.ivChatsChatFragmentDot);
        mDotTrip = mView.findViewById(R.id.ivChatsTripFragmentDot);

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        String mUserId = "bender";
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoiYmVuZGVyIn0.3KYJIoYvSPgTURznP8nWvsA2Yj2-vLqrm-ubqAeOlcQ";

        Client client = StreamChat.getInstance(getActivity().getApplication());
        HashMap<String, Object> extraData = new HashMap<>();
        // Images gathered from the server have some ending like "?ixlib=rb-3.1.0&w=150&h=150&fit=crop&crop=faces" that library would not accept as an image, therefore we are doing an adjustments
        User currentUser = new User(String.valueOf(mUserId), extraData);
        client.setUser(currentUser, token);

        // most the business logic for chat is handled in the ChannelListViewModel view model
        ChannelListViewModel viewModel = ViewModelProviders.of(this).get(ChannelListViewModel.class);
        mBinding.setViewModel(viewModel);
        mBinding.channelList.setViewModel(viewModel, this);

        // query all channels of type messaging
        FilterObject filter = and(eq("type", "messaging"));//, in("members", String.valueOf(mUserId)));
        viewModel.setChannelFilter(filter);

        viewModel.getChannels().observe(this, channels -> {
            if (channels.isEmpty()) {
                showEmpty();
            } else {
                mTextHolder.setVisibility(View.GONE);
                mInfo1.setVisibility(View.GONE);
                mInfo2.setVisibility(View.GONE);
            }
        });

        // click handlers for clicking a user avatar or channel
        mBinding.channelList.setOnChannelClickListener(channel -> {
//            Intent intent = new Intent(getActivity(), MessagingActivity.class);
//            intent.putExtra(EXTRA_CHANNEL_TYPE, channel.getType());
//            intent.putExtra(EXTRA_CHANNEL_ID, channel.getId());
//            startActivity(intent);
        });
        mBinding.channelList.setOnUserClickListener(user -> {
//            Intent profileIntent = new Intent(getActivity(), ProfileActivity.class);
//            profileIntent.putExtra("profile_id", Integer.parseInt(user.getId()));
//            startActivity(profileIntent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    void showEmpty() {
///        mRecycler.setVisibility(View.GONE);
        mTextHolder.setVisibility(View.VISIBLE);
        mInfo1.setVisibility(View.VISIBLE);
        mInfo2.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
//        mAdapter = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rlChatsTripHolder: {
                openTrip();
                break;
            }
        }

    }

    private void openTrip() {
        try {
            mCallback.onTripSelected();
        } catch (NullPointerException | ClassCastException cce) {
            cce.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

}
