package com.example.generativeai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageGenerator extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final String apiKey = "AIzaSyDxRW22duTLZoaSMAzB2uqUHEoa2-ITiEY";
    private EditText editTextPrompt;
    private ImageView imageResult;
    private TextView textStatus;
    private static final String TAG = "ImageGenerator";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_image_generator);

        editTextPrompt = findViewById(R.id.editTextPrompt);
        Button buttonGenerate = findViewById(R.id.buttonGenerate);
        imageResult = findViewById(R.id.imageResult);
        textStatus = findViewById(R.id.textStatus);
        Button backToHomeButton = findViewById(R.id.backhome);

        buttonGenerate.setOnClickListener(v -> {
            String prompt = editTextPrompt.getText().toString().trim();
            if (!prompt.isEmpty()) {
                generateImage(prompt);
            } else {
                textStatus.setText("❗ Vui lòng nhập prompt để tạo ảnh.");
            }
        });

        backToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ImageGenerator.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void generateImage(String prompt) {
        textStatus.setText("🛠️ Đang tạo ảnh...");
        imageResult.setImageBitmap(null);

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp-image-generation:generateContent?key=" + apiKey;

            // Tạo phần nội dung prompt
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(part);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", partsArray);

            JSONArray contents = new JSONArray();
            contents.put(content);

            // Thêm generationConfig để yêu cầu trả về ảnh (MIME type image/png)
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("response_mime_type", "image/png");

            // Tạo JSON body cuối cùng
            JSONObject finalBody = new JSONObject();
            finalBody.put("contents", contents);
            finalBody.put("generationConfig", generationConfig);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, finalBody.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            textStatus.setText("❌ Lỗi mạng: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.body() == null) return;

                        String responseBody = response.body().string();
                        Log.d(TAG, "RESPONSE: " + responseBody);

                        JSONObject responseJson = new JSONObject(responseBody);
                        JSONArray candidates = responseJson.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentObject = firstCandidate.getJSONObject("content");
                        JSONArray parts = contentObject.getJSONArray("parts");

                        JSONObject partItem = parts.getJSONObject(0);

                        // Kiểm tra xem có dữ liệu ảnh không
                        if (partItem.has("inlineData")) {
                            JSONObject inlineData = partItem.getJSONObject("inlineData");
                            String imageData = inlineData.getString("data");

                            // Decode base64 thành Bitmap
                            byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (bitmap != null) {
                                    imageResult.setImageBitmap(bitmap);
                                    textStatus.setText("✅ Ảnh đã được tạo!");
                                } else {
                                    textStatus.setText("⚠️ Không thể hiển thị ảnh.");
                                }
                            });
                        } else {
                            String fallbackText = partItem.optString("text", "❌ Không có ảnh được tạo.");
                            new Handler(Looper.getMainLooper()).post(() -> textStatus.setText(fallbackText));
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "JSON ERROR: ", e);
                        new Handler(Looper.getMainLooper()).post(() ->
                                textStatus.setText("❌ Lỗi xử lý dữ liệu: " + e.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            textStatus.setText("❌ Lỗi tạo yêu cầu: " + e.getMessage());
        }
    }

}
