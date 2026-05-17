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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    private ApiService apiService;
    private TextView tvProfileName, tvTotalQs, tvCorrectQs, tvIncorrectQs, tvMistakeSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvTotalQs = findViewById(R.id.tvTotalQs);
        tvCorrectQs = findViewById(R.id.tvCorrectQs);
        tvIncorrectQs = findViewById(R.id.tvIncorrectQs);
        tvMistakeSummary = findViewById(R.id.tvMistakeSummary);


        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        Button btnUpgrade = findViewById(R.id.btnUpgrade);
        Button btnShare = findViewById(R.id.btnShare);


        SharedPreferences prefs = getSharedPreferences("HelpHubDatabase", MODE_PRIVATE);
        String username = prefs.getString("SAVED_USERNAME", "Student");
        tvProfileName.setText(username);


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                        .connectTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                        .build())
                .build();
        apiService = retrofit.create(ApiService.class);


        fetchUserStats(username);


        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        btnUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, UpgradeActivity.class);
                startActivity(intent);
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = tvProfileName.getText().toString();
                String total = tvTotalQs.getText().toString();
                String correct = tvCorrectQs.getText().toString();


                if (total.equals("0") && correct.equals("0")) {
                    Toast.makeText(ProfileActivity.this, "Please wait for stats to load!", Toast.LENGTH_SHORT).show();
                    return;
                }


                String shareMessage = "Hey! Check out my HelpHub AI Quiz stats!\n\n" +
                        "🧑‍🎓 User: " + name + "\n" +
                        "🧠 Questions Answered: " + total + "\n" +
                        "✅ Correct Answers: " + correct + "\n\n" +
                        "I am studying hard! Can you beat my score?";


                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My HelpHub AI Stats");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);


                startActivity(Intent.createChooser(shareIntent, "Share your profile via:"));
            }
        });
    }

    private void fetchUserStats(String username) {
        apiService.getHistory(username).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray historyArray = response.body().getAsJsonArray("history");

                        int total = 0;
                        int correct = 0;
                        int incorrect = 0;
                        StringBuilder mistakesBuilder = new StringBuilder(); // To catch the wrong answers

                        for (JsonElement quizElement : historyArray) {
                            JsonArray resultsArray = quizElement.getAsJsonObject().getAsJsonArray("results");
                            total += resultsArray.size();

                            for (JsonElement questionElement : resultsArray) {
                                JsonObject qObj = questionElement.getAsJsonObject();
                                String question = qObj.get("question").getAsString();
                                String userAnswer = qObj.get("user_answer").getAsString();
                                String correctAnswer = qObj.get("correct_answer").getAsString();

                                if (userAnswer.equals(correctAnswer)) {
                                    correct++;
                                } else {
                                    incorrect++;

                                    if (incorrect <= 5) {
                                        mistakesBuilder.append("Missed: ").append(question).append("\n");
                                    }
                                }
                            }
                        }

                        tvTotalQs.setText(String.valueOf(total));
                        tvCorrectQs.setText(String.valueOf(correct));
                        tvIncorrectQs.setText(String.valueOf(incorrect));


                        if (incorrect > 0) {
                            fetchMistakeSummary(mistakesBuilder.toString());
                        } else {
                            tvMistakeSummary.setText("No mistakes! You are doing perfectly.");
                        }

                    } catch (Exception e) {
                        Toast.makeText(ProfileActivity.this, "Error parsing history.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Failed to connect to database.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchMistakeSummary(String mistakes) {
        JsonObject payload = new JsonObject();
        payload.addProperty("mistakes", mistakes);

        apiService.getSummary(payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String summary = response.body().get("summary").getAsString();
                    tvMistakeSummary.setText(summary);
                } else {
                    tvMistakeSummary.setText("Summary temporarily unavailable.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                tvMistakeSummary.setText("Failed to load AI summary.");
            }
        });
    }
}