package com.example.generativeai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button chatButton;
    private Button imageGeneratorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        chatButton = findViewById(R.id.chat);
        imageGeneratorButton = findViewById(R.id.taoanh);

        // Chuyển đến ChatActivity khi nhấn nút "Go to Chat"
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        // Chuyển đến ImageGeneratorActivity khi nhấn nút "Go to AI Image Generator"
        imageGeneratorButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageGenerator.class);
            startActivity(intent);
        });
    }
}
