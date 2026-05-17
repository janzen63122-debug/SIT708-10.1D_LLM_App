package com.example.sit708_61d_llm_app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpgradeActivity extends AppCompatActivity {

    private static final String TAG = "UpgradeActivity_DEBUG";
    private ApiService apiService;
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;
    private boolean isLaunchingPaymentSheet = false;


    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51TVQmLHv1LjUfcrTgDJpTvQhE1esawVclJOlN0scc3xH2P7NlZCOvtx8GFQm2LgHEZUIOqJuEJbc6sN59eVwfMJV00sSypoFBs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        Log.d(TAG, "========================================");
        Log.d(TAG, "onCreate() START");
        Log.d(TAG, "========================================");


        try {
            Log.d(TAG, "1️⃣ Attempting PaymentConfiguration.init()");
            PaymentConfiguration.init(getApplicationContext(), STRIPE_PUBLISHABLE_KEY);
            Log.d(TAG, "✅ PaymentConfiguration.init() SUCCESS");
        } catch (Exception e) {
            Log.e(TAG, "❌ PaymentConfiguration.init() FAILED: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Stripe config failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }


        try {
            Log.d(TAG, "2️⃣ Attempting PaymentSheet creation");
            paymentSheet = new PaymentSheet (this, this::onPaymentSheetResult);
            Log.d(TAG, "✅ PaymentSheet created");
        } catch (Exception e) {
            Log.e(TAG, "❌ PaymentSheet creation FAILED: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "PaymentSheet failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }


        try {
            Log.d(TAG, "3️⃣ Attempting Retrofit setup");
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:5000/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "✅ Retrofit setup SUCCESS");
        } catch (Exception e) {
            Log.e(TAG, "❌ Retrofit setup FAILED: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Retrofit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }


        try {
            Log.d(TAG, "4️⃣ Setting up buttons");
            setupButtons();
            Log.d(TAG, "✅ Buttons setup SUCCESS");
        } catch (Exception e) {
            Log.e(TAG, "❌ Button setup FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, "onCreate() COMPLETE - Ready for payment");
        Log.d(TAG, "========================================");
    }

    private void setupButtons() {
        Button btnStarter = findViewById(R.id.btnPurchaseStarter);
        Button btnIntermediate = findViewById(R.id.btnPurchaseIntermediate);
        Button btnAdvanced = findViewById(R.id.btnPurchaseAdvanced);

        Log.d(TAG, "Button IDs found - Starter: " + (btnStarter != null) + ", Intermediate: " + (btnIntermediate != null) + ", Advanced: " + (btnAdvanced != null));

        if (btnStarter != null) {
            btnStarter.setOnClickListener(v -> {
                Log.d(TAG, "🔘 STARTER button clicked");
                fetchPaymentIntent(499, "Starter");
            });
        }

        if (btnIntermediate != null) {
            btnIntermediate.setOnClickListener(v -> {
                Log.d(TAG, "🔘 INTERMEDIATE button clicked");
                fetchPaymentIntent(999, "Intermediate");
            });
        }

        if (btnAdvanced != null) {
            btnAdvanced.setOnClickListener(v -> {
                Log.d(TAG, "🔘 ADVANCED button clicked");
                fetchPaymentIntent(1999, "Advanced");
            });
        }
    }

    private void fetchPaymentIntent(int amountInCents, String tierName) {

        if (isLaunchingPaymentSheet) {
            Log.d(TAG, "Ignoring second click - already launching");
            return;
        }
        isLaunchingPaymentSheet = true;
        Log.d(TAG, "========================================");
        Log.d(TAG, "fetchPaymentIntent() START - Amount: " + amountInCents + ", Tier: " + tierName);
        Log.d(TAG, "========================================");

        Toast.makeText(this, "Connecting to secure checkout...", Toast.LENGTH_SHORT).show();

        JsonObject payload = new JsonObject();
        payload.addProperty("amount", amountInCents);

        Log.d(TAG, "📤 Sending request to /create-payment-intent");
        Log.d(TAG, "   Payload: " + payload.toString());

        apiService.createPaymentIntent(payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "========================================");
                Log.d(TAG, "📡 onResponse() received");
                Log.d(TAG, "   Status Code: " + response.code());
                Log.d(TAG, "   Is Successful: " + response.isSuccessful());
                Log.d(TAG, "========================================");


                if (response.code() != 200) {
                    isLaunchingPaymentSheet = false;
                    Log.e(TAG, "❌ HTTP Status NOT 200, it's: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "   Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "   Could not read error body: " + e.getMessage());
                    }
                    Toast.makeText(UpgradeActivity.this, "Server error: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }


                if (response.body() == null) {
                    isLaunchingPaymentSheet = false;
                    Log.e(TAG, "❌ response.body() is NULL");
                    Toast.makeText(UpgradeActivity.this, "Null response body", Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    Log.d(TAG, "✅ Response body received");
                    Log.d(TAG, "   Full response: " + responseBody.toString());


                    if (!responseBody.has("clientSecret")) {
                        isLaunchingPaymentSheet = false;
                        Log.e(TAG, "❌ 'clientSecret' key NOT in response");
                        Log.e(TAG, "   Available keys: " + responseBody.keySet().toString());
                        Toast.makeText(UpgradeActivity.this, "Missing clientSecret in response", Toast.LENGTH_LONG).show();
                        return;
                    }

                    paymentIntentClientSecret = responseBody.get("clientSecret").getAsString();
                    Log.d(TAG, "✅ clientSecret extracted: " + paymentIntentClientSecret.substring(0, Math.min(30, paymentIntentClientSecret.length())) + "...");


                    if (paymentIntentClientSecret == null || paymentIntentClientSecret.isEmpty()) {
                        isLaunchingPaymentSheet = false;
                        Log.e(TAG, "❌ clientSecret is null or empty");
                        Toast.makeText(UpgradeActivity.this, "Invalid clientSecret", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d(TAG, "========================================");
                    Log.d(TAG, "🟢 Attempting to launch PaymentSheet");
                    Log.d(TAG, "   clientSecret length: " + paymentIntentClientSecret.length());
                    Log.d(TAG, "========================================");

                    PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder(
                            "HelpHub AI - " + tierName
                    ).build();

                    Log.d(TAG, "Calling paymentSheet.presentWithPaymentIntent()");
                    paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
                    Log.d(TAG, "✅ presentWithPaymentIntent() called");

                } catch (NullPointerException e) {
                    isLaunchingPaymentSheet = false;
                    Log.e(TAG, "❌ NullPointerException: " + e.getMessage());
                    Log.e(TAG, "   Stack trace:");
                    e.printStackTrace();
                    Toast.makeText(UpgradeActivity.this, "NullPointerException: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    isLaunchingPaymentSheet = false;
                    Log.e(TAG, "❌ Exception: " + e.getClass().getName() + ": " + e.getMessage());
                    Log.e(TAG, "   Stack trace:");
                    e.printStackTrace();
                    Toast.makeText(UpgradeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLaunchingPaymentSheet = false;
                Log.e(TAG, "========================================");
                Log.e(TAG, "❌ onFailure() - Network error");
                Log.e(TAG, "   Error type: " + t.getClass().getName());
                Log.e(TAG, "   Error message: " + t.getMessage());
                Log.e(TAG, "========================================");
                t.printStackTrace();
                Toast.makeText(UpgradeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {

        isLaunchingPaymentSheet = false;

        Log.d(TAG, "========================================");
        Log.d(TAG, "onPaymentSheetResult() - Type: " + paymentSheetResult.getClass().getSimpleName());
        Log.d(TAG, "========================================");

        if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {


            Log.d(TAG, "User canceled payment");
            Toast.makeText(this, "Payment Canceled", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {



            PaymentSheetResult.Failed failed = (PaymentSheetResult.Failed) paymentSheetResult;
            Log.e(TAG, "❌ Payment failed: " + failed.getError().getMessage());
            Toast.makeText(this, "Payment Failed: " + failed.getError().getMessage(), Toast.LENGTH_LONG).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Completed) {



            Log.d(TAG, "✅✅✅ PAYMENT SUCCESSFUL ✅✅✅");
            Toast.makeText(this, "✅ Payment Successful! Account Upgraded.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}