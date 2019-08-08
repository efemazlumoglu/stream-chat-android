package io.getstream.chat.example.tommaso;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.getstream.sdk.chat.rest.Message;

import java.util.ArrayList;
import java.util.List;

import io.getstream.chat.example.R;

public class ChannelMessageListAdapter extends RecyclerView.Adapter<ChannelMessageListAdapter.ChannelViewHolder> {
    private final LayoutInflater mInflater;
    private List<Message> mMessages; // Cached copy of messages

    public ChannelMessageListAdapter(Context context) {
        mMessages = new ArrayList<>();
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public ChannelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.tommaso_recyclerview_item, parent, false);
        return new ChannelViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ChannelViewHolder holder, int position) {
        if (mMessages != null) {
            Message current = mMessages.get(position);
            holder.messageItemView.setText(current.getText());
        }
    }

    public void setMessages(List<Message> messages){
        mMessages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mMessages != null)
            return mMessages.size();
        else return 0;
    }

    class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageItemView;

        private ChannelViewHolder(View itemView) {
            super(itemView);
            messageItemView = itemView.findViewById(R.id.textView);
        }
    }
}
