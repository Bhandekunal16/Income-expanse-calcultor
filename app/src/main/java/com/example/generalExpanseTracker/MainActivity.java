package com.example.generalExpanseTracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.*;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private TextView tvBalance;
    private Button btnAddExpense, btnViewTransactions, btnBudget, btnVpa;
    private BarChart barChart;
    private ApiService apiService;
    private String username, mobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBalance = findViewById(R.id.tvBalance);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnViewTransactions = findViewById(R.id.btnViewTransactions);
        btnBudget = findViewById(R.id.btnBudget);
        btnVpa = findViewById(R.id.btnVpa);
        barChart = findViewById(R.id.BarChart);

        apiService = ApiClient.getClient().create(ApiService.class);
        mobile = getIntent().getStringExtra("mobile");
        username = getSharedPreferences("app", MODE_PRIVATE).getString("username", "");

        refreshDashboard();

        btnAddExpense.setOnClickListener(v -> startActivity(new Intent(this, AddTransactionActivity.class)));
        btnViewTransactions.setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
        btnBudget.setOnClickListener(v -> startActivity(new Intent(this, BudgetActivity.class)));
        // btnVpa.setOnClickListener(v -> startActivity(new Intent(this, VpaActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    private void refreshDashboard() {
        if (mobile != null && !mobile.isEmpty()) {
            loadBalance(mobile);
        }
        if (username != null && !username.isEmpty()) {
            loadStatistics(username);
        }
    }

    private void loadBalance(String mobile) {
        Map<String, String> body = new HashMap<>();
        body.put("mobile", mobile);

        apiService.loginUser(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> res = response.body();
                        Boolean status = (Boolean) res.get("status");
                        if (status != null && status) {
                            Map<String, Object> data = (Map<String, Object>) res.get("data");
                            List<Map<String, Object>> accounts = (List<Map<String, Object>>) data.get("accounts");

                            if (accounts != null && !accounts.isEmpty()) {
                                Map<String, Object> acc = accounts.get(0);
                                double balance = safeDouble(acc.get("balance"));
                                updateBalanceUI(balance);
                            }
                        }
                    } catch (Exception e) {
                        showToast("Balance error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void loadStatistics(String username) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        apiService.getStatistics(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    try {
                        Map<String, Object> res = response.body();
                        Map<String, Object> data = (Map<String, Object>) res.get("data");
                        if (data == null)
                            return;
                        List<String> dates = (List<String>) data.get("date");
                        Map<String, Object> dateWise = (Map<String, Object>) data.get("dateWise");
                        List<BarEntry> creditEntries = new ArrayList<>();
                        List<BarEntry> debitEntries = new ArrayList<>();
                        int index = 0;

                        for (String date : dates) {
                            List<Map<String, Object>> txns = (List<Map<String, Object>>) dateWise.get(date);
                            float creditSum = 0f;
                            float debitSum = 0f;

                            if (txns != null) {
                                for (Map<String, Object> txn : txns) {
                                    String type = safeString(txn.get("type"));
                                    double amount = safeDouble(txn.get("transactionAmount"));
                                    if ("credit".equalsIgnoreCase(type)) {
                                        creditSum += amount;
                                    } else {
                                        debitSum += amount;
                                    }
                                }
                            }

                            creditEntries.add(new BarEntry(index, creditSum));
                            debitEntries.add(new BarEntry(index, debitSum));
                            index++;
                        }
                        renderChart(dates, creditEntries, debitEntries);
                    } catch (Exception e) {
                        showToast("Parse error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showToast("Graph error: " + t.getMessage());
            }
        });
    }

    private void updateBalanceUI(double balance) {
        DecimalFormat df = new DecimalFormat("₹0.00");
        tvBalance.setText(df.format(balance));

        if (balance >= 0) {
            tvBalance.setTextColor(Color.parseColor("#22C55E"));
        } else {
            tvBalance.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void renderChart(List<String> labels, List<BarEntry> creditEntries, List<BarEntry> debitEntries) {
        BarDataSet creditSet = new BarDataSet(creditEntries, "Credit");
        creditSet.setColor(Color.parseColor("#22C55E"));
        BarDataSet debitSet = new BarDataSet(debitEntries, "Debit");
        debitSet.setColor(Color.parseColor("#EF4444"));

        BarData data = new BarData(creditSet, debitSet);
        data.setBarWidth(0.35f);
        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(labels.size());
        barChart.groupBars(0f, 0.2f, 0.05f);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getDescription().setText("");
        barChart.animateY(800);
        barChart.invalidate();
    }

    private String safeString(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }

    private double safeDouble(Object obj) {
        try {
            return obj != null ? Double.parseDouble(String.valueOf(obj)) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}