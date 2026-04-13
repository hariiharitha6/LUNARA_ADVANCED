package com.example.lunara;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatVH> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT  = 1;

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public ChatVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_USER
                ? R.layout.item_chat_bubble_user
                : R.layout.item_chat_bubble_bot;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ChatVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatVH holder, int position) {
        holder.text.setText(messages.get(position).text);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ChatVH extends RecyclerView.ViewHolder {
        TextView text;
        ChatVH(View v) {
            super(v);
            text = v.findViewById(R.id.chatText);
        }
    }
}
