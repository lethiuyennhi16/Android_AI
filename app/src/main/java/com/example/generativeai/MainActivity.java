package com.example.generativeai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    //private final OkHttpClient client = new OkHttpClient();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final String apiKey = "AIzaSyDxRW22duTLZoaSMAzB2uqUHEoa2-ITiEY"; // Thay bằng API key thật
    private EditText editTextQuestion;
    private TextView textViewAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextQuestion = findViewById(R.id.editTextQuestion);
        Button buttonSend = findViewById(R.id.buttonSend);
        textViewAnswer = findViewById(R.id.textViewAnswer);

        if (buttonSend == null) {
            textViewAnswer.setText("Lỗi: Không tìm thấy nút Gửi");
            return;
        }

        editTextQuestion.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                String question = editTextQuestion.getText().toString();
                if (!question.isEmpty()) {
                    callGeminiApi(question);
                } else {
                    textViewAnswer.setText("Vui lòng nhập câu hỏi!");
                }
                return true;
            }
            return false;
        });

        buttonSend.setOnClickListener(v -> {
            String question = editTextQuestion.getText().toString();
            if (!question.isEmpty()) {
                callGeminiApi(question);
            } else {
                textViewAnswer.setText("Vui lòng nhập câu hỏi!");
            }
        });
    }

    private void callGeminiApi(String question) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro-exp-03-25:generateContent?key=" + apiKey;


        try {
            System.out.println("Câu hỏi gửi: " + question);
            JSONObject bodyJson = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", "Trả lời ngắn gọn và dễ hiểu bằng tiếng Việt\n" + question);
            parts.put(part);
            userMessage.put("parts", parts);
            contents.put(userMessage);
            bodyJson.put("contents", contents);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, bodyJson.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    new Handler(Looper.getMainLooper()).post(() -> textViewAnswer.setText("Lỗi mạng: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : null;
                    if (response.isSuccessful() && responseBody != null) {
                        try {
                            System.out.println("Phản hồi API: " + responseBody);
                            JSONObject json = new JSONObject(responseBody);
                            JSONArray candidates = json.getJSONArray("candidates");
                            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String answer = parts.getJSONObject(0).getString("text");

                            new Handler(Looper.getMainLooper()).post(() -> textViewAnswer.setText(answer));
                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> textViewAnswer.setText("Lỗi xử lý dữ liệu: " + e.getMessage()));
                        }
                    } else {
                        String errorMessage = "Lỗi: Mã " + response.code();
                        if (responseBody != null) {
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                String errorDetail = errorJson.getJSONObject("error").getString("message");
                                errorMessage += " - " + errorDetail;
                            } catch (Exception e) {
                                errorMessage += " - Không phân tích được chi tiết lỗi";
                            }
                        }
                        System.out.println("Lỗi API: " + errorMessage);
                        String finalErrorMessage = errorMessage;
                        new Handler(Looper.getMainLooper()).post(() -> textViewAnswer.setText(finalErrorMessage));
                    }
                }
            });
        } catch (Exception e) {
            textViewAnswer.setText("Lỗi tạo JSON: " + e.getMessage());
        }
    }
}