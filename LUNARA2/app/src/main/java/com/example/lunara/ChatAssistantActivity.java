package com.example.lunara;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAssistantActivity extends BaseDrawerActivity {

    RecyclerView chatRecyclerView;
    EditText chatInput;
    Button sendBtn;
    ChatAdapter adapter;
    List<ChatMessage> messages;
    Map<String, String> faqMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_assistant);

        setupDrawer();

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput        = findViewById(R.id.chatInput);
        sendBtn          = findViewById(R.id.sendBtn);

        messages = new ArrayList<>();
        adapter  = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        buildFAQ();

        addBotMessage(getString(R.string.chat_welcome));

        sendBtn.setOnClickListener(v -> {
            String userText = chatInput.getText().toString().trim();
            if (userText.isEmpty()) return;

            addUserMessage(userText);
            chatInput.setText("");

            String response = findResponse(userText.toLowerCase());
            addBotMessage(response);
        });
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
    }

    private void addBotMessage(String text) {
        messages.add(new ChatMessage(text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
    }

    private void buildFAQ() {
        faqMap = new HashMap<>();
        faqMap.put("diet", getString(R.string.chat_diet));
        faqMap.put("food", getString(R.string.chat_food));
        faqMap.put("nutrition", getString(R.string.chat_nutrition));
        faqMap.put("exercise", getString(R.string.chat_exercise));
        faqMap.put("warning", getString(R.string.chat_warning));
        faqMap.put("danger", getString(R.string.chat_danger));
        faqMap.put("checkup", getString(R.string.chat_checkup));
        faqMap.put("visit", getString(R.string.chat_visit));
        faqMap.put("medicine", getString(R.string.chat_medicine));
        faqMap.put("vaccine", getString(R.string.chat_vaccine));
        faqMap.put("baby", getString(R.string.chat_baby));
        faqMap.put("kick", getString(R.string.chat_kick));
        faqMap.put("mental", getString(R.string.chat_mental));
        faqMap.put("depression", getString(R.string.chat_depression));
        faqMap.put("sleep", getString(R.string.chat_sleep));
        faqMap.put("bp", getString(R.string.chat_bp));
        faqMap.put("sugar", getString(R.string.chat_sugar));
        faqMap.put("weight", getString(R.string.chat_weight));
        faqMap.put("labor", getString(R.string.chat_labor));
        faqMap.put("trimester", getString(R.string.chat_trimester));
        faqMap.put("hello", getString(R.string.chat_hello));
        faqMap.put("thank", getString(R.string.chat_thank));
        faqMap.put("help", getString(R.string.chat_help));
    }

    private String findResponse(String query) {
        for (Map.Entry<String, String> entry : faqMap.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return getString(R.string.chat_default_response);
    }
}
