package com.example.generativeai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.View;
import android.speech.tts.TextToSpeech;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final String apiKey = "AIzaSyDxRW22duTLZoaSMAzB2uqUHEoa2-ITiEY"; // API key của bạn
    private EditText editTextQuestion;
    private LinearLayout chatHistoryLayout;
    private TextToSpeech tts;

    private final JSONArray chatHistory = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_chat);

        editTextQuestion = findViewById(R.id.editTextQuestion);
        Button buttonSend = findViewById(R.id.buttonSend);
        chatHistoryLayout = findViewById(R.id.chatHistoryLayout);

        buttonSend.setOnClickListener(v -> {
            String question = editTextQuestion.getText().toString();
            if (!question.isEmpty()) {
                addChatBubble("Bạn", question);
                callGeminiApi(question);
                editTextQuestion.setText("");
            } else {
                addChatBubble("AI", "Vui lòng nhập câu hỏi!");
            }
        });

        // Khởi tạo TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = tts.setLanguage(Locale.getDefault());
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    addChatBubble("AI", "TTS không hỗ trợ ngôn ngữ này!");
                }
            } else {
                addChatBubble("AI", "Không thể khởi tạo TTS!");
            }
        });
    }

    private void addChatBubble(String sender, String message) {
        // Tạo layout ngang chứa tin nhắn + nút TTS
        LinearLayout messageLayout = new LinearLayout(this);
        messageLayout.setOrientation(LinearLayout.HORIZONTAL);
        messageLayout.setPadding(10, 10, 10, 10);

        // TextView hiển thị nội dung
        TextView messageView = new TextView(this);
        messageView.setText(sender + ": " + message);
        messageView.setTextSize(18);
        messageView.setMaxWidth(800);
        messageView.setPadding(20, 10, 20, 10);

        if (sender.equals("Bạn")) {
            messageView.setBackgroundResource(R.drawable.chat_bubble_user);
        } else {
            messageView.setBackgroundResource(R.drawable.chat_bubble_ai);
        }

        messageLayout.addView(messageView);

        // Thêm nút 🔊 nếu là AI
        if (sender.equals("AI")) {
            ImageButton ttsButton = new ImageButton(this);
            ttsButton.setImageResource(R.drawable.text_to_speech);
            ttsButton.setBackgroundResource(R.drawable.tts_button_background);
            ttsButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            LinearLayout.LayoutParams ttsButtonParams = new LinearLayout.LayoutParams(120, 120);
            ttsButtonParams.gravity = Gravity.CENTER_VERTICAL;
            ttsButtonParams.setMargins(10, 0, 0, 0);
            ttsButton.setLayoutParams(ttsButtonParams);
            ttsButton.setPadding(16, 16, 16, 16);

            // 🔊 Đọc lại tin nhắn của AI khi nhấn vào nút TTS
            ttsButton.setOnClickListener(v -> {
                if (tts != null) {
                    tts.stop(); // Ngắt đoạn đang đọc (an toàn)
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                }
            });

            messageLayout.addView(ttsButton);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);
        messageLayout.setLayoutParams(params);

        chatHistoryLayout.addView(messageLayout);
        scrollToBottom();
    }

    private void scrollToBottom() {
        ScrollView scrollView = findViewById(R.id.scrollViewChatHistory);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void callGeminiApi(String question) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        try {
            JSONObject bodyJson = new JSONObject();

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", question);
            parts.put(part);
            userMessage.put("parts", parts);
            chatHistory.put(userMessage);

            bodyJson.put("contents", chatHistory);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, bodyJson.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> addChatBubble("AI", "Lỗi kết nối: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : null;
                    if (response.isSuccessful() && responseBody != null) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            JSONArray candidates = json.getJSONArray("candidates");
                            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String answer = parts.getJSONObject(0).getString("text");
                            String cleanAnswer = answer.replace("*", "");

                            JSONObject aiMessage = new JSONObject();
                            aiMessage.put("role", "model");

                            JSONArray aiParts = new JSONArray();
                            JSONObject aiPart = new JSONObject();
                            aiPart.put("text", answer);
                            aiParts.put(aiPart);

                            aiMessage.put("parts", aiParts);
                            chatHistory.put(aiMessage);

                            runOnUiThread(() -> addChatBubble("AI", cleanAnswer));
                        } catch (Exception e) {
                            runOnUiThread(() -> addChatBubble("AI", "Lỗi xử lý phản hồi!"));
                        }
                    } else {
                        runOnUiThread(() -> addChatBubble("AI", "API lỗi: " + response.message()));
                    }
                }
            });
        } catch (Exception e) {
            addChatBubble("AI", "Lỗi xử lý câu hỏi!");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
