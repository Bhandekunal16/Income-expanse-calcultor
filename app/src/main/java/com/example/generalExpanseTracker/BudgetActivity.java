package com.example.generalExpanseTracker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Calendar;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.example.generalExpanseTracker.NotificationHelper;

public class BudgetActivity extends BaseActivity {

    EditText edtBudget;
    Button btnSave;
    TextView txtBudget, txtSpent, txtRemaining;
    ProgressBar progressBar;

    ApiService apiService;
    String username;

    double totalBudget = 0;
    double totalSpent = 0;

    boolean isBudgetLoaded = false;
    boolean isTransactionsLoaded = false;
    private int lastNotifiedLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        enableBackButton();

        edtBudget = findViewById(R.id.edtBudget);
        btnSave = findViewById(R.id.btnSave);
        txtBudget = findViewById(R.id.txtBudget);
        txtSpent = findViewById(R.id.txtSpent);
        txtRemaining = findViewById(R.id.txtRemaining);
        progressBar = findViewById(R.id.progressBar);

        NotificationHelper.createChannel(this);
        requestNotificationPermission(); 

        apiService = ApiClient.getClient().create(ApiService.class);

        username = getSharedPreferences("app", MODE_PRIVATE)
                .getString("username", "");

        Log.d("USERNAME", username);

        getBudget();
        getTransactions();

        btnSave.setOnClickListener(v -> saveOrUpdateBudget());
    }

    private void saveOrUpdateBudget() {

        String input = edtBudget.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Enter budget", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(input);

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("amount", amount);

        Map<String, Object> checkBody = new HashMap<>();
        checkBody.put("username", username);

        apiService.getBudget(checkBody).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                boolean hasBudget = false;

                if (response.body() != null) {
                    Object data = response.body().get("data");

                    if (data instanceof Map) {
                        hasBudget = true;
                    } else if (data instanceof List) {
                        hasBudget = !((List<?>) data).isEmpty();
                    }
                }

                if (hasBudget) {
                    callUpdate(body);
                } else {
                    callAdd(body);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(BudgetActivity.this, "Check failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getBudget() {

        isBudgetLoaded = false;

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);

        apiService.getBudget(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                if (response.body() == null)
                    return;

                Object dataObj = response.body().get("data");

                totalBudget = 0;

                if (dataObj instanceof Map) {

                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    totalBudget = parseDoubleSafe(data.get("amount"));

                } else if (dataObj instanceof List) {

                    List<?> list = (List<?>) dataObj;

                    if (!list.isEmpty() && list.get(0) instanceof Map) {

                        Map<String, Object> data = (Map<String, Object>) list.get(0);
                        totalBudget = parseDoubleSafe(data.get("amount"));
                    }
                }

                txtBudget.setText("Budget: ₹" + totalBudget);

                isBudgetLoaded = true;
                updateUI();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("BUDGET_ERROR", t.getMessage());
            }
        });
    }

    private void getTransactions() {

        isTransactionsLoaded = false;

        Map<String, String> body = new HashMap<>();
        body.put("username", username);

        apiService.getTransactions(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                if (response.body() == null)
                    return;

                Object dataObj = response.body().get("data");

                totalSpent = 0;

                if (dataObj instanceof List) {

                    List<?> rawList = (List<?>) dataObj;

                    Calendar now = Calendar.getInstance();

                    for (Object obj : rawList) {

                        if (!(obj instanceof Map))
                            continue;

                        Map<?, ?> txn = (Map<?, ?>) obj;

                        String type = String.valueOf(txn.get("type"));
                        if (!"debit".equals(type))
                            continue;

                        long time = parseLongSafe(txn.get("time"));

                        Calendar txnCal = Calendar.getInstance();
                        txnCal.setTimeInMillis(time);

                        if (txnCal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                txnCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {

                            double amount = parseDoubleSafe(txn.get("transactionAmount"));
                            totalSpent += amount;
                        }
                    }
                }

                isTransactionsLoaded = true;
                updateUI();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void updateUI() {

        if (!isBudgetLoaded || !isTransactionsLoaded)
            return;

        Log.d("PROGRESS_CHECK",
                "Spent=" + totalSpent + " Budget=" + totalBudget);

        txtSpent.setText("Spent: ₹" + totalSpent);

        double remaining = totalBudget - totalSpent;
        txtRemaining.setText("Remaining: ₹" + remaining);

        if (totalBudget > 0) {
            int progress = (int) ((totalSpent / totalBudget) * 100);
            progressBar.setProgress(Math.min(progress, 100));
            String message = "Used ₹" + totalSpent + " of ₹" + totalBudget + " (" + progress + "%)";

            // ✅ App launch notification
            NotificationHelper.showNotification(
                    this,
                    "Budget Summary",
                    message);

            // ✅ Threshold notifications
            checkThreshold(progress, message);
        } else {
            progressBar.setProgress(0);
        }

    }

    private void callAdd(Map<String, Object> body) {

        apiService.addBudget(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                Toast.makeText(BudgetActivity.this, "Budget Added", Toast.LENGTH_SHORT).show();
                getBudget();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(BudgetActivity.this, "Add Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callUpdate(Map<String, Object> body) {

        apiService.updateBudget(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                Toast.makeText(BudgetActivity.this, "Budget Updated", Toast.LENGTH_SHORT).show();
                getBudget();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(BudgetActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDoubleSafe(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLongSafe(Object value) {
        try {
            return (long) Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void checkThreshold(int progress, String message) {

        if (progress >= 100 && lastNotifiedLevel < 100) {
            notifyUser("Budget Exhausted 🚨", message);
            lastNotifiedLevel = 100;

        } else if (progress >= 75 && lastNotifiedLevel < 75) {
            notifyUser("Warning ⚠️ (75%)", message);
            lastNotifiedLevel = 75;

        } else if (progress >= 50 && lastNotifiedLevel < 50) {
            notifyUser("Half Budget Used (50%)", message);
            lastNotifiedLevel = 50;

        } else if (progress >= 25 && lastNotifiedLevel < 25) {
            notifyUser("25% Budget Used", message);
            lastNotifiedLevel = 25;
        }
    }

    private void notifyUser(String title, String message) {
        NotificationHelper.showNotification(this, title, message);
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        101);
            }
        }
    }
}