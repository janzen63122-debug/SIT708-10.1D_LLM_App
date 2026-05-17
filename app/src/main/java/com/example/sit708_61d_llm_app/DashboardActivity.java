package com.example.sit708_61d_llm_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnStartQuiz = findViewById(R.id.btnStartQuiz);
        Button btnProfile = findViewById(R.id.btnProfile);

        SharedPreferences prefs = getSharedPreferences("HelpHubDatabase", MODE_PRIVATE);
        String savedName = prefs.getString("SAVED_NAME", "Student");


        if (tvWelcome != null) {
            tvWelcome.setText("Hello " + savedName + "!");
        }

        if (btnStartQuiz != null) {
            btnStartQuiz.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, TaskActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}