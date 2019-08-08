package com.getstream.sdk.chat.rest.core;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.getstream.sdk.chat.component.Component;
import com.getstream.sdk.chat.interfaces.ChannelListEventHandler;
import com.getstream.sdk.chat.interfaces.ClientConnectionCallback;
import com.getstream.sdk.chat.interfaces.TokenProvider;
import com.getstream.sdk.chat.interfaces.WSResponseHandler;
import com.getstream.sdk.chat.model.Config;
import com.getstream.sdk.chat.model.Event;
import com.getstream.sdk.chat.model.TokenService;
import com.getstream.sdk.chat.rest.Message;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.enums.Token;
import com.getstream.sdk.chat.rest.BaseURL;
import com.getstream.sdk.chat.rest.WebSocketService;
import com.getstream.sdk.chat.rest.controller.APIService;
import com.getstream.sdk.chat.rest.controller.RetrofitClient;
import com.getstream.sdk.chat.rest.interfaces.DeviceCallback;
import com.getstream.sdk.chat.rest.interfaces.EventCallback;
import com.getstream.sdk.chat.rest.interfaces.GetDevicesCallback;
import com.getstream.sdk.chat.rest.interfaces.GetRepliesCallback;
import com.getstream.sdk.chat.rest.interfaces.QueryUserListCallback;
import com.getstream.sdk.chat.rest.interfaces.QueryChannelCallback;
import com.getstream.sdk.chat.rest.interfaces.QueryChannelListCallback;
import com.getstream.sdk.chat.rest.interfaces.SendFileCallback;
import com.getstream.sdk.chat.rest.interfaces.MessageCallback;
import com.getstream.sdk.chat.rest.request.AddDeviceRequest;
import com.getstream.sdk.chat.rest.request.MarkReadRequest;
import com.getstream.sdk.chat.rest.request.ReactionRequest;
import com.getstream.sdk.chat.rest.request.SendActionRequest;
import com.getstream.sdk.chat.rest.request.SendEventRequest;
import com.getstream.sdk.chat.rest.request.SendMessageRequest;
import com.getstream.sdk.chat.rest.request.UpdateMessageRequest;
import com.getstream.sdk.chat.rest.response.ChannelState;
import com.getstream.sdk.chat.rest.response.DevicesResponse;
import com.getstream.sdk.chat.rest.response.EventResponse;
import com.getstream.sdk.chat.rest.response.FileSendResponse;
import com.getstream.sdk.chat.rest.response.QueryChannelsResponse;
import com.getstream.sdk.chat.rest.response.GetDevicesResponse;
import com.getstream.sdk.chat.rest.response.GetRepliesResponse;
import com.getstream.sdk.chat.rest.response.QueryUserListResponse;
import com.getstream.sdk.chat.rest.response.MessageResponse;
import com.getstream.sdk.chat.utils.Global;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Client implements WSResponseHandler {

    private static final String TAG = Client.class.getSimpleName();

    public String getApiKey() {
        return apiKey;
    }

    // Main Params
    private String apiKey;

    public User getUser() {
        return user;
    }

    private User user;
    private String userToken;

    public String getUserId() {
        return user.getId();
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String connectionId;

    private Component component;
    // Client params
    public List<Channel> activeChannels = new ArrayList<>();
    public List<User> users = new ArrayList<>();
    public Map<String, List<Message>> ephemeralMessage = new HashMap<>(); // Key: Channel ID, Value: ephemeralMessages

    public APIService getApiService() {
        return mService;
    }

    public boolean isConnected() {
        return connected;
    }

    private boolean connected;
    private List<ClientConnectionCallback> connectionWaiters;

    private APIService mService;

    private List<ChatEventHandler> eventSubscribers;
    private Map<Number, ChatEventHandler> eventSubscribersBy;
    private int subscribersSeq;

    private Map<String, Config> channelTypeConfigs;

    private WebSocketService WSConn;
    private ApiClientOptions options;

    // Interfaces
    private ChannelListEventHandler channelListEventHandler;

    public void setChannelListEventHandler(ChannelListEventHandler channelListEventHandler) {
        this.channelListEventHandler = channelListEventHandler;
    }

    public Client(String apiKey) {
        this(apiKey, new ApiClientOptions());
    }

    public Client(String apiKey, ApiClientOptions options) {
        this.apiKey = apiKey;
        eventSubscribers = new ArrayList<>();
        eventSubscribersBy = new HashMap<>();
        connectionWaiters = new ArrayList<>();
        channelTypeConfigs = new HashMap<>();
        this.options = options;
    }

    public Component getComponent() {
        if (this.component == null) {
            this.component = new Component();
            Global.component = this.component;
        }
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
        Global.component = component;
    }

    // Server-side Token
    public void setUser(User user, final TokenProvider provider) {
        try {
            this.user = user;
            provider.onResult((String token) -> {
                userToken = token;
                connect();
            });
        } catch (Exception e) {
            provider.onError(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    // Dev, Guest Token
    public void setUser(User user, Token token) throws Exception {
        this.user = user;
        switch (token) {
            case DEVELOPMENT:
                this.userToken = TokenService.devToken(user.getId());
                break;
            case HARDCODED:
                this.userToken = token.getToken();
                break;
            case GUEST:
                this.userToken = TokenService.createGuestToken(user.getId());
                break;
            default:
                break;
        }
        Log.d(TAG, "TOKEN: " + this.userToken);
        if (!TextUtils.isEmpty(this.userToken)) {
            connect();
        }
    }

    // Hardcoded Code token
    public void setUser(User user, @NonNull String token) {
        this.user = user;
        this.userToken = token;
        connect();
    }

    // endregion

    public final synchronized int addEventHandler(ChatEventHandler handler) {
        int id = ++subscribersSeq;
        eventSubscribers.add(handler);
        eventSubscribersBy.put(id, handler);
        return id;
    }

    public final synchronized void removeEventHandler(Number handlerId) {
        ChatEventHandler handler = eventSubscribersBy.remove(handlerId);
        eventSubscribers.remove(handler);
    }

    public void waitForConnection(ClientConnectionCallback callback){
        if (connected) {
            callback.onSuccess();
        } else {
            connectionWaiters.add(callback);
        }
    }

    private JSONObject buildUserDetailJSON() {
        HashMap<String, Object> jsonParameter = new HashMap<>();
        HashMap<String, Object> userDetails = new HashMap<>(user.getExtraData());

        userDetails.put("id", this.user.getId());
        userDetails.put("name", this.user.getName());
        userDetails.put("image", this.user.getImage());

        jsonParameter.put("user_details", userDetails);
        jsonParameter.put("user_id", this.user.getId());
        jsonParameter.put("user_token", this.userToken);
        jsonParameter.put("server_determines_connection_id", true);
        return new JSONObject(jsonParameter);
    }

    private synchronized void connect() {
        BaseURL baseURL = new BaseURL(options.getLocation());

        JSONObject json = buildUserDetailJSON();
        String wsURL = baseURL.url(BaseURL.Scheme.webSocket) + "connect?json=" + json + "&api_key="
                + apiKey + "&authorization=" + userToken + "&stream-auth-type=" + "jwt";
        Log.d(TAG, "WebSocket URL : " + wsURL);

        setupEventHandling();

        mService = RetrofitClient.getAuthorizedClient(baseURL.url(BaseURL.Scheme.https), userToken).create(APIService.class);
        WSConn = new WebSocketService(wsURL, user.getId(), this);
        WSConn.connect();
    }

    public Channel channel(String type, String id){
        return new Channel(this, type, id);
    }

    public Channel channel(String type, String id, HashMap<String, Object> extraData){
        return new Channel(this, type, id, extraData);
    }

    @Override
    public void connectionResolved(Event event){
        connectionId = event.getConnectionId();
        if (event.getMe() != null)
            user = event.getMe();

        connected = true;
        for (ClientConnectionCallback waiter: connectionWaiters) {
            waiter.onSuccess();
        }
        connectionWaiters.clear();
    }

    @Override
    public void onWSEvent(Event event) {
        for (ChatEventHandler handler: eventSubscribers) {
            handler.dispatchEvent(event);
        }

        if (event.isChannelEvent()){
            Channel channel = getChannelByCid(event.getCid());
            if (channel != null) {
                channel.handleChannelEvent(event);
            }
        }
    }

    @Override
    public void connectionRecovered() {
        connected = true;
        //TODO: add reconnection magic here
    }

    public synchronized void addChannelConfig(String channelType, Config config) {
        channelTypeConfigs.put(channelType, config);
    }

    public synchronized Config getChannelConfig(String channelType) {
        return channelTypeConfigs.get(channelType);
    }

    public synchronized void addToActiveChannels(Channel channel){
        Log.d(TAG, "adding channel cid to active channels: " + channel.getCid());

        if (getChannelByCid(channel.getCid()) == null){
            activeChannels.add(channel);
        }
    }

    public Channel getChannelByCid(String cid) {
        Log.d(TAG, "getChannelByCid: " + cid);
        for (Channel channel : activeChannels) {
            if (cid.equals(channel.getCid())) {
                return channel;
            }
        }
        return null;
    }

    // endregion

    // region Channel
    public void queryChannels(JSONObject payload, QueryChannelListCallback callback) {
        mService.queryChannels(apiKey, user.getId(), connectionId, payload).enqueue(new Callback<QueryChannelsResponse>() {
            @Override
            public void onResponse(Call<QueryChannelsResponse> call, Response<QueryChannelsResponse> response) {
                if (response.body().getChannels() == null || response.body().getChannels().isEmpty())
                    // TODO: this is expected, how is this an error? should just return an empty list
                    callback.onError("There is no any active Channel(s)!", -1);
                else {
                    for (int i = 0; i < response.body().getChannels().size(); i++) {
                        ChannelState channelState = response.body().getChannels().get(i);
                        Channel channelData = channelState.getChannel();
                        Channel channel = channel(channelData.getType(), channelData.getId(), channelData.getExtraData());
                        checkEphemeralMessages(channelState);
                        channel.setChannelState(channelState);
                        addToActiveChannels(channel);
                    }
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<QueryChannelsResponse> call, Throwable t) {
                Log.e(TAG, "shit hit the fan");
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    private void checkEphemeralMessages(ChannelState response) {
        if (response == null) return;
        List<Message> ephemeralMainMessages = Global.getEphemeralMessages(response.getChannel().getId(), null);
        if (ephemeralMainMessages != null && !ephemeralMainMessages.isEmpty()) {
            for (int i = 0; i < ephemeralMainMessages.size(); i++) {
                Message message = ephemeralMainMessages.get(i);
                if (response.getMessages().contains(message)) continue;
                response.getMessages().add(message);
            }
        }
    }

    /**
     * deleteChannel - Delete the given channel
     *
     * @param channelId the Channel id needs to be specified
     * @return {object} Response that includes the channel
     */
    public void deleteChannel(@NonNull String channelId, QueryChannelCallback callback) {

        mService.deleteChannel(channelId, apiKey, user.getId(), connectionId).enqueue(new Callback<ChannelState>() {
            @Override
            public void onResponse(Call<ChannelState> call, Response<ChannelState> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // region Message

    /**
     * sendMessage - Send a message to this channel
     *
     * @param {object} message The Message object
     * @return {object} The Server Response
     */
    public void sendMessage(@NonNull String channelId,
                            @NonNull SendMessageRequest sendMessageRequest,
                            MessageCallback callback) {

        mService.sendMessage(channelId, apiKey, user.getId(), connectionId, sendMessageRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * updateMessage - Update the given message
     *
     * @param {object} message object, id needs to be specified
     * @return {object} Response that includes the message
     */
    public void updateMessage(@NonNull String messageId,
                              @NonNull UpdateMessageRequest request,
                              MessageCallback callback) {

        mService.updateMessage(messageId,
                apiKey,
                user.getId(),
                connectionId,
                request).enqueue(new Callback<MessageResponse>() {

            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    public void getMessage(@NonNull String messageId,
                           MessageCallback callback) {

        mService.getMessage(messageId, apiKey, user.getId(), connectionId).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * deleteMessage - Delete the given message
     *
     * @param {string} messageID the message id needs to be specified
     * @return {object} Response that includes the message
     */
    public void deleteMessage(@NonNull String messageId,
                              MessageCallback callback) {

        mService.deleteMessage(messageId, apiKey, user.getId(), connectionId).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * markRead - Send the mark read event for this user, only works if the `read_events` setting is enabled
     *
     * @return {Promise} Description
     */
    public void markRead(@NonNull String channelId,
                         MarkReadRequest readRequest,
                         EventCallback callback) {

        mService.markRead(channelId, apiKey, user.getId(), connectionId, readRequest).enqueue(new Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, Response<EventResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<EventResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * markAllRead - marks all channels for this user as read
     *
     * @return {Promise} Description
     */
    public void markAllRead(MarkReadRequest readRequest,
                            EventCallback callback) {

        mService.markAllRead(apiKey, user.getId(), connectionId, readRequest).enqueue(new Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, Response<EventResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<EventResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }
    // endregion

    // region Thread

    /**
     * getReplies - List the message replies for a parent message
     *
     * @param {type} parent_id The message parent id, ie the top of the thread
     * @param {type} options   Pagination params, ie {limit:10, idlte: 10}
     * @return {type} A channelResponse with a list of messages
     */
    public void getReplies(@NonNull String parentId,
                           String limit,
                           String firstId,
                           GetRepliesCallback callback) {

        if (TextUtils.isEmpty(firstId)) {
            mService.getReplies(parentId, apiKey, user.getId(), connectionId, limit).enqueue(new Callback<GetRepliesResponse>() {
                @Override
                public void onResponse(Call<GetRepliesResponse> call, Response<GetRepliesResponse> response) {
                    callback.onSuccess(response.body());
                }

                @Override
                public void onFailure(Call<GetRepliesResponse> call, Throwable t) {
                    callback.onError(t.getLocalizedMessage(), -1);
                }
            });
        } else {
            mService.getRepliesMore(parentId, apiKey, user.getId(), connectionId, limit, firstId).enqueue(new Callback<GetRepliesResponse>() {
                @Override
                public void onResponse(Call<GetRepliesResponse> call, Response<GetRepliesResponse> response) {
                    callback.onSuccess(response.body());
                }

                @Override
                public void onFailure(Call<GetRepliesResponse> call, Throwable t) {
                    callback.onError(t.getLocalizedMessage(), -1);
                }
            });
        }

    }
    // endregion

    // region Reaction

    /**
     * sendReaction - Send a reaction about a message
     *
     * @param {string} messageID the message id
     * @param {object} reaction the reaction object for instance {type: 'love'}
     * @param {string} user_id the id of the user (used only for server side request) default null
     * @return {object} The Server Response
     */
    public void sendReaction(@NonNull String messageId,
                             @NonNull ReactionRequest reactionRequest,
                             MessageCallback callback) {

        mService.sendReaction(messageId, apiKey, user.getId(), connectionId, reactionRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * deleteReaction - Delete a reaction by user and type
     *
     * @param {string} messageID the id of the message from which te remove the reaction
     * @param {string} reactionType the type of reaction that should be removed
     * @param {string} user_id the id of the user (used only for server side request) default null
     * @return {object} The Server Response
     */
    public void deleteReaction(@NonNull String messageId,
                               @NonNull String reactionType,
                               MessageCallback callback) {

        mService.deleteReaction(messageId, reactionType, apiKey, user.getId(), connectionId).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // endregion

    // region Event

    /**
     * sendEvent - Send an event on this channel
     *
     * @param {object} event for example {type: 'message.read'}
     * @return {object} The Server Response
     */
    public void sendEvent(@NonNull String channelId,
                          @NonNull SendEventRequest eventRequest,
                          EventCallback callback) {

        mService.sendEvent(channelId, apiKey, user.getId(), connectionId, eventRequest).enqueue(new Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, Response<EventResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<EventResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // endregion

    // region File
    public void sendImage(@NonNull String channelId,
                          MultipartBody.Part part,
                          SendFileCallback callback) {

        mService.sendImage(channelId, part, apiKey, user.getId(), connectionId).enqueue(new Callback<FileSendResponse>() {
            @Override
            public void onResponse(Call<FileSendResponse> call, Response<FileSendResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    public void sendFile(@NonNull String channelId,
                         MultipartBody.Part part,
                         SendFileCallback callback) {

        mService.sendFile(channelId, part, apiKey, user.getId(), connectionId).enqueue(new Callback<FileSendResponse>() {
            @Override
            public void onResponse(Call<FileSendResponse> call, Response<FileSendResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // endregion
    public void sendAction(@NonNull String messageId,
                           @NonNull SendActionRequest request,
                           MessageCallback callback) {

        mService.sendAction(messageId, apiKey, user.getId(), connectionId, request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // region User

    /**
     * queryUsers - Query users and watch user presence
     *
     * @param {object} filterConditions MongoDB style filter conditions
     * @param {object} sort             Sort options, for instance {last_active: -1}
     * @param {object} options          Option object, {presence: true}
     * @return {object} User Query Response
     */
    public void queryUsers(@NonNull JSONObject payload,
                           QueryUserListCallback callback) {

        mService.queryUsers(apiKey, user.getId(), connectionId, payload).enqueue(new Callback<QueryUserListResponse>() {
            @Override
            public void onResponse(Call<QueryUserListResponse> call, Response<QueryUserListResponse> response) {
                for (int i = 0; i < response.body().getUsers().size(); i++)
                    if (!response.body().getUsers().get(i).isMe())
                        users.add(response.body().getUsers().get(i));

                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<QueryUserListResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // endregion
    public void setupEventHandling(){
        addEventHandler(new ChatEventHandler(){
            private void notifyChannelsSubscribers() {
                // TODO: kill this
                if (channelListEventHandler != null)
                    channelListEventHandler.updateChannels();
            }
            private void updateChannelMessage(Event event) {
                Channel channel = getChannelByCid(event.getCid());
                channel.handleMessageUpdatedOrDeleted(event);
                notifyChannelsSubscribers();
            }
            @Override
            public void onTypingStart(Event event) {
            }
            @Override
            public void onTypingStop(Event event) {
            }
            @Override
            public void onMessageNew(Event event) {
                Channel channel = getChannelByCid(event.getCid());
                channel.handleNewMessage(event);
                activeChannels.remove(channel);
                activeChannels.add(0, channel);
                notifyChannelsSubscribers();
            }
            @Override
            public void onMessageUpdated(Event event) {
                this.updateChannelMessage(event);
            }
            @Override
            public void onMessageDeleted(Event event) {
                this.updateChannelMessage(event);
            }
            @Override
            public void onMessageRead(Event event) {
                Channel channel = getChannelByCid(event.getCid());
                channel.handleReadEvent(event);
            }
            @Override
            public void onReactionNew(Event event) {
                this.updateChannelMessage(event);
            }
            @Override
            public void onReactionDeleted(Event event) {
                this.updateChannelMessage(event);
            }
            @Override
            public void onChannelUpdated(Event event){
                Channel channel = getChannelByCid(event.getCid());
                channel.handleNewMessage(event);
                activeChannels.remove(channel);
                activeChannels.add(0, channel);
                notifyChannelsSubscribers();
            }
            @Override
            public void onChannelDeleted(Event event) {
                //TODO: remove channel from client activeChannels
            }
            @Override
            public void onConnectionChanged(Event event) {
                if (!event.getOnline()) {
                    connected = false;
                }
            }
        });
    }

    // region Device

    /**
     * addDevice - Adds a push device for a user.
     *
     * @param {string} id the device id
     * @param {string} push_provider the push provider (apn or firebase)
     * @param {string} [userID] the user id (defaults to current user)
     */
    public void addDevice(@NonNull String deviceId,
                          DeviceCallback callback) {

        AddDeviceRequest request = new AddDeviceRequest(deviceId);
        mService.addDevices(apiKey, user.getId(), connectionId, request).enqueue(new Callback<DevicesResponse>() {
            @Override
            public void onResponse(Call<DevicesResponse> call, Response<DevicesResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<DevicesResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * getDevices - Returns the devices associated with a current user
     *
     * @param {string} [userID] User ID. Only works on serversidex
     * @return {devices} Array of devices
     */
    public void getDevices(@NonNull Map<String, String> payload,
                           GetDevicesCallback callback) {

        mService.getDevices(apiKey, user.getId(), connectionId, payload).enqueue(new Callback<GetDevicesResponse>() {
            @Override
            public void onResponse(Call<GetDevicesResponse> call, Response<GetDevicesResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<GetDevicesResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    /**
     * removeDevice - Removes the device with the given id. Clientside users can only delete their own devices
     *
     * @param {string} id The device id
     * @param {string} [userID] The user id. Only specify this for serverside requests
     */
    public void removeDevice(@NonNull String deviceId,
                             DeviceCallback callback) {

        mService.deleteDevice(deviceId, apiKey, user.getId(), connectionId).enqueue(new Callback<DevicesResponse>() {
            @Override
            public void onResponse(Call<DevicesResponse> call, Response<DevicesResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<DevicesResponse> call, Throwable t) {
                callback.onError(t.getLocalizedMessage(), -1);
            }
        });
    }

    // endregion

    public void disconnect() {
    }

    public void setAnonymousUser() {
    }

    /**
     * setGuestUser - Setup a temporary guest user
     *
     * @param {object} user Data about this user. IE {name: "john"}
     * @return {promise} Returns a promise that resolves when the connection is setup
     */
    public void setGuestUser() {
    }

    public void on() {
    }

    public void off() {

    }


    public void muteUser() {
    }

    public void unmuteUser() {
    }

    public void flagMessage() {
    }

    public void unflagMessage() {
    }
}