package com.example.sit708_61d_llm_app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HistoryActivity extends AppCompatActivity {

    private ApiService apiService;
    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyContainer = findViewById(R.id.historyContainer);


        SharedPreferences prefs = getSharedPreferences("HelpHubDatabase", MODE_PRIVATE);
        String username = prefs.getString("SAVED_USERNAME", "Student");


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
        apiService = retrofit.create(ApiService.class);


        fetchDetailedHistory(username);
    }

    private void fetchDetailedHistory(String username) {

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading past quizzes...");
        loadingText.setTextColor(Color.WHITE);
        loadingText.setTextSize(18f);
        historyContainer.addView(loadingText);

        apiService.getHistory(username).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                historyContainer.removeAllViews();

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray historyArray = response.body().getAsJsonArray("history");

                        if (historyArray.size() == 0) {
                            TextView emptyText = new TextView(HistoryActivity.this);
                            emptyText.setText("No history found. Take a quiz first!");
                            historyContainer.addView(emptyText);
                            return;
                        }

                        int questionCounter = 1;


                        for (JsonElement quizElement : historyArray) {
                            JsonArray resultsArray = quizElement.getAsJsonObject().getAsJsonArray("results");


                            for (JsonElement questionElement : resultsArray) {
                                JsonObject qObj = questionElement.getAsJsonObject();
                                String question = qObj.get("question").getAsString();
                                String userAnswer = qObj.get("user_answer").getAsString();
                                String correctAnswer = qObj.get("correct_answer").getAsString();


                                createQuestionCard(questionCounter, question, userAnswer, correctAnswer);
                                questionCounter++;
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(HistoryActivity.this, "Error parsing history.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                historyContainer.removeAllViews();
                Toast.makeText(HistoryActivity.this, "Failed to connect to database.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void createQuestionCard(int number, String question, String userAnswer, String correctAnswer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#0288D1")); // Darker blue card
        card.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24); // Add space below each card
        card.setLayoutParams(params);

        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(number + ". " + question);
        tvQuestion.setTextColor(Color.WHITE);
        tvQuestion.setTextSize(18f);
        tvQuestion.setTypeface(null, android.graphics.Typeface.BOLD);
        tvQuestion.setPadding(0, 0, 0, 16);

        TextView tvAnswer = new TextView(this);
        tvAnswer.setText("Your Answer: " + userAnswer + "\nCorrect Answer: " + correctAnswer);


        if (userAnswer.equals(correctAnswer)) {
            tvAnswer.setTextColor(Color.parseColor("#00FF00")); // Green
        } else {
            tvAnswer.setTextColor(Color.parseColor("#FF5252")); // Red
        }
        tvAnswer.setTextSize(16f);


        card.addView(tvQuestion);
        card.addView(tvAnswer);
        historyContainer.addView(card);
    }
}