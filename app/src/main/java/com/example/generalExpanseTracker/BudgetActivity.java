package com.example.generalExpanseTracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

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

public class BudgetActivity extends AppCompatActivity {

    EditText edtBudget;
    Button btnSave;
    TextView txtBudget, txtSpent, txtRemaining;
    ProgressBar progressBar;

    ApiService apiService;
    String username;

    double totalBudget = 0;
    double totalSpent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        edtBudget = findViewById(R.id.edtBudget);
        btnSave = findViewById(R.id.btnSave);
        txtBudget = findViewById(R.id.txtBudget);
        txtSpent = findViewById(R.id.txtSpent);
        txtRemaining = findViewById(R.id.txtRemaining);
        progressBar = findViewById(R.id.progressBar);

        apiService = ApiClient.getClient().create(ApiService.class);

        username = getSharedPreferences("app", MODE_PRIVATE)
                .getString("username", "");

        Log.d("username", username);

        getBudget();
        getTransactions();

        btnSave.setOnClickListener(v -> saveOrUpdateBudget());
    }

    private void saveOrUpdateBudget() {

        double amount = Double.parseDouble(edtBudget.getText().toString());

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("amount", amount);

        // 🔍 First check if budget exists
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
                    // ✅ UPDATE
                    callUpdate(body);
                } else {
                    // ✅ ADD
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

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);

        apiService.getBudget(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                if (response.body() == null)
                    return;

                Object dataObj = response.body().get("data");

                // 🔍 DEBUG
                Log.d("BUDGET_DATA_TYPE",
                        dataObj != null ? dataObj.getClass().getName() : "null");

                // ✅ HANDLE BOTH CASES (List or Map)
                if (dataObj instanceof Map) {

                    Map<String, Object> data = (Map<String, Object>) dataObj;

                    totalBudget = Double.parseDouble(
                            String.valueOf(data.get("amount")));

                } else if (dataObj instanceof List) {

                    List<?> list = (List<?>) dataObj;

                    if (!list.isEmpty() && list.get(0) instanceof Map) {

                        Map<String, Object> data = (Map<String, Object>) list.get(0);

                        totalBudget = Double.parseDouble(
                                String.valueOf(data.get("amount")));
                    }
                }

                txtBudget.setText("Budget: ₹" + totalBudget);
                updateUI();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("BUDGET_ERROR", t.getMessage());
            }
        });
    }

    private void getTransactions() {

        Map<String, String> body = new HashMap<>();
        body.put("username", username);

        apiService.getTransactions(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                if (response.body() == null)
                    return;

                Object dataObj = response.body().get("data");

                Log.d("FULL_RESPONSE", response.body().toString());
                Log.d("DATA_TYPE", dataObj != null ? dataObj.getClass().getName() : "null");

                totalSpent = 0;

                if (dataObj instanceof List) {

                    List<?> rawList = (List<?>) dataObj;

                    long now = System.currentTimeMillis();
                    Calendar calNow = Calendar.getInstance();
                    calNow.setTimeInMillis(now);

                    for (Object obj : rawList) {

                        if (!(obj instanceof Map))
                            continue;

                        Map<?, ?> txn = (Map<?, ?>) obj;

                        String type = String.valueOf(txn.get("type"));

                        if (!"debit".equals(type))
                            continue;

                        // ✅ SAFE TIME PARSE
                        long time = 0;
                        try {
                            Object timeObj = txn.get("time");
                            if (timeObj != null) {
                                time = (long) Double.parseDouble(String.valueOf(timeObj));
                            }
                        } catch (Exception e) {
                            Log.e("TIME_PARSE_ERROR", e.getMessage());
                            continue;
                        }

                        Calendar calTxn = Calendar.getInstance();
                        calTxn.setTimeInMillis(time);

                        if (calTxn.get(Calendar.MONTH) == calNow.get(Calendar.MONTH) &&
                                calTxn.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {

                            // ✅ SAFE AMOUNT PARSE
                            double amount = 0;
                            try {
                                Object amtObj = txn.get("transactionAmount");
                                if (amtObj != null) {
                                    amount = Double.parseDouble(String.valueOf(amtObj));
                                }
                            } catch (Exception e) {
                                Log.e("AMOUNT_PARSE_ERROR", e.getMessage());
                            }

                            totalSpent += amount;
                        }
                    }

                } else {
                    Log.e("ERROR", "data is NOT a List");
                }

                updateUI();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void updateUI() {

        txtSpent.setText("Spent: ₹" + totalSpent);

        double remaining = totalBudget - totalSpent;
        txtRemaining.setText("Remaining: ₹" + remaining);

        if (totalBudget > 0) {
            int progress = (int) ((totalSpent / totalBudget) * 100);
            progressBar.setProgress(Math.min(progress, 100));
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
}