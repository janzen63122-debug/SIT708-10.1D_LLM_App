package com.example.sit708_61d_llm_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ResultsActivity extends AppCompatActivity {

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        TextView tvResQ1 = findViewById(R.id.tvResQ1); TextView tvResAns1 = findViewById(R.id.tvResAns1); TextView tvResExp1 = findViewById(R.id.tvResExp1);
        TextView tvResQ2 = findViewById(R.id.tvResQ2); TextView tvResAns2 = findViewById(R.id.tvResAns2); TextView tvResExp2 = findViewById(R.id.tvResExp2);
        TextView tvResQ3 = findViewById(R.id.tvResQ3); TextView tvResAns3 = findViewById(R.id.tvResAns3); TextView tvResExp3 = findViewById(R.id.tvResExp3);
        Button btnContinue = findViewById(R.id.btnContinue);

        String q1 = getIntent().getStringExtra("Q1"); String a1 = getIntent().getStringExtra("A1"); String c1 = getIntent().getStringExtra("C1");
        String q2 = getIntent().getStringExtra("Q2"); String a2 = getIntent().getStringExtra("A2"); String c2 = getIntent().getStringExtra("C2");
        String q3 = getIntent().getStringExtra("Q3"); String a3 = getIntent().getStringExtra("A3"); String c3 = getIntent().getStringExtra("C3");

        if (q1 != null) {
            tvResQ1.setText(q1);
            tvResAns1.setText("Your Answer: " + a1 + "\nCorrect Answer: " + c1);
            tvResQ2.setText(q2);
            tvResAns2.setText("Your Answer: " + a2 + "\nCorrect Answer: " + c2);
            tvResQ3.setText(q3);
            tvResAns3.setText("Your Answer: " + a3 + "\nCorrect Answer: " + c3);
        } else {
            tvResQ1.setText("Error: Missing data.");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().readTimeout(10, java.util.concurrent.TimeUnit.MINUTES).build())
                .build();
        apiService = retrofit.create(ApiService.class);

        if (q1 != null) {
            getEvaluationFromAI(q1, a1, tvResExp1);
            getEvaluationFromAI(q2, a2, tvResExp2);
            getEvaluationFromAI(q3, a3, tvResExp3);


            saveQuizToDatabase(q1, a1, c1, q2, a2, c2, q3, a3, c3);
        }

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        Button btnContinueDashboard = findViewById(R.id.btnContinueDashboard);
        btnContinueDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultsActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void saveQuizToDatabase(String q1, String a1, String c1, String q2, String a2, String c2, String q3, String a3, String c3) {

        SharedPreferences prefs = getSharedPreferences("HelpHubDatabase", MODE_PRIVATE);
        String username = prefs.getString("SAVED_USERNAME", "UnknownUser");


        JsonArray resultsArray = new JsonArray();

        JsonObject res1 = new JsonObject(); res1.addProperty("question", q1); res1.addProperty("user_answer", a1); res1.addProperty("correct_answer", c1);
        JsonObject res2 = new JsonObject(); res2.addProperty("question", q2); res2.addProperty("user_answer", a2); res2.addProperty("correct_answer", c2);
        JsonObject res3 = new JsonObject(); res3.addProperty("question", q3); res3.addProperty("user_answer", a3); res3.addProperty("correct_answer", c3);

        resultsArray.add(res1); resultsArray.add(res2); resultsArray.add(res3);


        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.add("results", resultsArray);


        apiService.saveHistory(payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ResultsActivity.this, "Quiz saved to Cloud History!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }

    private void getEvaluationFromAI(String question, String answer, TextView explanationView) {

        explanationView.setText("Prompt sent: \"Evaluate the answer: " + answer + "\"\n\nResponse: AI is thinking...");
        explanationView.setTextColor(android.graphics.Color.LTGRAY);

        apiService.getExplanation(question, answer).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String explanation = response.body().get("explanation").getAsString();
                        explanationView.setText("Prompt sent: \"Evaluate the answer: " + answer + "\"\n\nAI Response: " + explanation);
                        explanationView.setTextColor(android.graphics.Color.WHITE);
                    } catch (Exception e) {
                        explanationView.setText("Prompt sent: \"Evaluate the answer: " + answer + "\"\n\nAI Response: Error reading AI explanation.");
                        explanationView.setTextColor(android.graphics.Color.RED);
                    }
                } else {
                    explanationView.setText("Prompt sent: \"Evaluate the answer: " + answer + "\"\n\nAI Response: Server Error " + response.code());
                    explanationView.setTextColor(android.graphics.Color.RED);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                explanationView.setText("Prompt sent: \"Evaluate the answer: " + answer + "\"\n\nAI Response: FAILED TO CONNECT.");
                explanationView.setTextColor(android.graphics.Color.RED);
            }
        });
    }
}