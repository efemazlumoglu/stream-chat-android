package com.getstream.sdk.chat.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.getstream.sdk.chat.R;
import com.getstream.sdk.chat.databinding.ViewMessageInputBinding;
import com.getstream.sdk.chat.function.SendFileFunction;
import com.getstream.sdk.chat.rest.Message;
import com.getstream.sdk.chat.utils.GridSpacingItemDecoration;
import com.getstream.sdk.chat.utils.Utils;



/**
 * Rich Message Input View component, allows you to:
 * - type messages
 * - run slash commands
 * - emoticons
 * - file uploads
 * - send typing events
 *
 * The view is made reusable by allowing
 * - Customization via attrs/style
 * - Data binding
 */
public class MessageInputView extends RelativeLayout implements View.OnClickListener, TextWatcher, View.OnFocusChangeListener {

    /*
    TODO:
    - Attachment listener...
    - Commands listener
    - Data bindings (rename)
    - Attributes
    - Documentation
     */

    // binding for this view
    private ViewMessageInputBinding binding;

    // listeners
    private SendMessageListener sendMessageListener;
    private TypingListener typingListener;
    private AttachmentsListener attachmentsListener;
    private OnFocusChangeListener onFocusChangeListener;

    // state
    private Message editingMessage;
    private boolean isTyping;

    // TODO Rename
    private SendFileFunction sendFileFunction;



    public MessageInputView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MessageInputView, 0, 0);
        String hintText = a.getString(R.styleable.MessageInputView_inputHint);
        a.recycle();

        // setup the bindings
        this.init(context);

        binding = ViewMessageInputBinding.bind(this);
        binding.setActiveMessageComposer(false);
        binding.setActiveMessageSend(false);

        binding.etMessage.setHint(hintText);

        binding.tvSend.setOnClickListener(this);

        binding.etMessage.setOnFocusChangeListener(this);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_message_input, this);

        this.initAttachmentUI(context);
    }

    private void initAttachmentUI(Context context) {
        // TODO: make the attachment UI into it's own view and allow you to change it.
        binding.rvMedia.setLayoutManager(new GridLayoutManager(context, 4, LinearLayoutManager.VERTICAL, false));
        binding.rvMedia.hasFixedSize();
        binding.rvComposer.setLayoutManager(new GridLayoutManager(context, 1, LinearLayoutManager.HORIZONTAL, false));
        int spanCount = 4;  // 4 columns
        int spacing = 2;    // 1 px
        boolean includeEdge = false;
        binding.rvMedia.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));

        binding.tvOpenAttach.setOnClickListener(v -> sendFileFunction.onClickAttachmentViewOpen(v));
        binding.ivBackAttachment.setOnClickListener(v -> sendFileFunction.onClickAttachmentViewClose(v));
        binding.tvCloseAttach.setOnClickListener(v -> sendFileFunction.onClickAttachmentViewClose(v));
        binding.llMedia.setOnClickListener(v -> sendFileFunction.onClickSelectMediaViewOpen(v, null));
        binding.llCamera.setOnClickListener(v -> {
            Utils.setButtonDelayEnable(v);
            sendFileFunction.onClickAttachmentViewClose(v);
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            Intent chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeVideoIntent});
            // TODO: somehow fix this
            // startActivityForResult(chooserIntent, Constant.CAPTURE_IMAGE_REQUEST_CODE);

        });
        binding.llFile.setOnClickListener(v -> sendFileFunction.onClickSelectFileViewOpen(v, null));
        binding.tvMediaClose.setOnClickListener(v -> sendFileFunction.onClickSelectMediaViewClose(v));
    }

    public boolean IsEditing() {
        return editingMessage != null;
    }

    public void EditMessage(Message message) {
        editingMessage = message;
    }

    public Message GetEditMessage() {
        return editingMessage;
    }

    public void CancelEditMessage() {
        editingMessage = null;
        binding.etMessage.setText("");
        this.clearFocus();
        sendFileFunction.fadeAnimationView(binding.ivBackAttachment, false);
    }

    public void setEnabled(boolean enabled) {
        binding.etMessage.setEnabled(true);
    }

    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        this.onFocusChangeListener = l;
    }

    public void clearFocus() {
        binding.etMessage.clearFocus();
    }

    public boolean requestInputFocus() {
        return binding.etMessage.requestFocus();
    }


    public String getMessageText() {
        return binding.etMessage.getText().toString();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_send) {
            this.onSendMessage(binding.etMessage.getText().toString());
            this.stopTyping();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //noop
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //noop
    }

    @Override
    public void afterTextChanged(Editable s) {
        String messageText = this.getMessageText();
        if (s.length() > 0) {
            this.startTyping();
        } else {
            this.stopTyping();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            this.stopTyping();
        }

        binding.setActiveMessageComposer(hasFocus);
    }


    /**
     * Used for listening to the sendMessage event
     */
    public interface SendMessageListener {
        void onSendMessage(String input);
    }

    private void onSendMessage(String input) {
        if (sendMessageListener != null) {
            sendMessageListener.onSendMessage(input);
        }
    }

    public void setOnSendMessageListener(SendMessageListener l) {
        // TODO: it's not really nice that you can only set 1 listener...
        this.sendMessageListener = l;
    }

    /**
     * This interface is called when you add an attachment
     */
    public interface AttachmentsListener {
        void onAddAttachments();
    }

    /**
     * A simple interface for typing events
     */
    public interface TypingListener {
        void onStartTyping();
        void onStopTyping();
    }

    public void setTypingListener(TypingListener l) {
        // TODO: it's not really nice that you can only set 1 listener...
        this.typingListener = l;
    }

    private void stopTyping() {
        isTyping = false;
        if (typingListener != null) {
            typingListener.onStopTyping();
        }
    }

    private void startTyping() {
        isTyping = true;
        if (typingListener != null) {
            typingListener.onStartTyping();
        }
    }

    public boolean IsTyping() {
        return isTyping;
    }


}