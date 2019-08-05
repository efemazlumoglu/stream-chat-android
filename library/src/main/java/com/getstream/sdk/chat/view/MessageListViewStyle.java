package com.getstream.sdk.chat.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.getstream.sdk.chat.R;

public class MessageListViewStyle {
    private int messageTextColorMine;
    private int messageTextColorOther;
    private int messageBubbleColorMine;
    private int messageBubbleColorOther;
    private Context context;


    public MessageListViewStyle(Context context, AttributeSet attrs) {
        // parse the attributes
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MessageListView, 0, 0);
        messageTextColorMine = a.getColor(R.styleable.MessageListView_messageTextColorMine, -1);
        messageTextColorOther = a.getColor(R.styleable.MessageListView_messageTextColorOther, -1);
        messageBubbleColorMine = a.getColor(R.styleable.MessageListView_messageBubbleColorMine, -1);
        messageBubbleColorOther = a.getColor(R.styleable.MessageListView_messageBubbleColorOther, -1);
        a.recycle();
    }

    @ColorInt
    public int getMessageTextColorMine() {
        return messageTextColorMine;
    }

    public void setMessageTextColorMine(int messageTextColorMine) {
        this.messageTextColorMine = messageTextColorMine;
    }

    @ColorInt
    public int getMessageTextColorOther() {
        return messageTextColorOther;
    }

    public void setMessageTextColorOther(int messageTextColorOther) {
        this.messageTextColorOther = messageTextColorOther;
    }

    @ColorInt
    public int getMessageBubbleColorMine() {
        return messageBubbleColorMine;
    }

    public void setMessageBubbleColorMine(int messageBubbleColorMine) {
        this.messageBubbleColorMine = messageBubbleColorMine;
    }

    @ColorInt
    public int getMessageBubbleColorOther() {
        return messageBubbleColorOther;
    }

    public void setMessageBubbleColorOther(int messageBubbleColorOther) {
        this.messageBubbleColorOther = messageBubbleColorOther;
    }

    protected Drawable getVectorDrawable(@DrawableRes int drawable) {
        return ContextCompat.getDrawable(context, drawable);
    }

    private Drawable createMessageBubble(@ColorInt int c, @DrawableRes int shape) {

        // for backwards compat we always need to wrap
        Drawable drawable = DrawableCompat.wrap(getVectorDrawable(shape)).mutate();

        DrawableCompat.setTintList(
                drawable,
                new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.bubble_main},
                                new int[]{android.R.attr.state_pressed},
                                new int[]{-android.R.attr.state_pressed, -android.R.attr.state_selected}
                        },
                        new int[]{selectedColor, pressedColor, normalColor}
                ));
        return drawable;
    }
}