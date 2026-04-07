package com.example.generalExpanseTracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private BarChart barChart;
    private ApiService apiService;
    private String username, mobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBalance = findViewById(R.id.tvBalance);
        barChart = findViewById(R.id.BarChart);

        CardView cardAdd = findViewById(R.id.cardAdd);
        CardView cardTransactions = findViewById(R.id.cardTransactions);
        CardView cardBudget = findViewById(R.id.cardBudget);

        apiService = ApiClient.getClient().create(ApiService.class);
        mobile = getIntent().getStringExtra("mobile");
        username = getSharedPreferences("app", MODE_PRIVATE).getString("username", "");

        refreshDashboard();

        if (cardAdd != null) {
            cardAdd.setOnClickListener(v -> startActivity(new Intent(this, AddTransactionActivity.class)));
        }

        if (cardTransactions != null) {
            cardTransactions.setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
        }

        if (cardBudget != null) {
            cardBudget.setOnClickListener(v -> startActivity(new Intent(this, BudgetActivity.class)));
        }

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
                                double balance = safeDouble(accounts.get(0).get("balance"));
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
                        Map<String, Object> data = (Map<String, Object>) response.body().get("data");

                        if (data == null)
                            return;

                        List<String> dates = (List<String>) data.get("date");
                        Map<String, Object> dateWise = (Map<String, Object>) data.get("dateWise");

                        List<BarEntry> credit = new ArrayList<>();
                        List<BarEntry> debit = new ArrayList<>();

                        int i = 0;
                        for (String d : dates) {
                            List<Map<String, Object>> txns = (List<Map<String, Object>>) dateWise.get(d);

                            float c = 0, de = 0;

                            if (txns != null) {
                                for (Map<String, Object> txn : txns) {
                                    String type = safeString(txn.get("type"));
                                    double amt = safeDouble(txn.get("transactionAmount"));

                                    if ("credit".equalsIgnoreCase(type))
                                        c += amt;
                                    else
                                        de += amt;
                                }
                            }

                            credit.add(new BarEntry(i, c));
                            debit.add(new BarEntry(i, de));
                            i++;
                        }

                        renderChart(dates, credit, debit);

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
        tvBalance.setText(new DecimalFormat("₹0.00").format(balance));
        tvBalance.setTextColor(
                balance >= 0 ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));
    }

    private void renderChart(List<String> labels,
            List<BarEntry> credit,
            List<BarEntry> debit) {

        BarDataSet cSet = new BarDataSet(credit, "Credit");
        cSet.setColor(Color.parseColor("#22C55E"));

        BarDataSet dSet = new BarDataSet(debit, "Debit");
        dSet.setColor(Color.parseColor("#EF4444"));

        BarData data = new BarData(cSet, dSet);
        data.setBarWidth(0.35f);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(labels.size());
        barChart.groupBars(0f, 0.2f, 0.05f);

        XAxis x = barChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setGranularity(1f);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.getDescription().setText("");
        barChart.animateY(800);
        barChart.invalidate();
    }

    private String safeString(Object o) {
        return o != null ? String.valueOf(o) : "";
    }

    private double safeDouble(Object o) {
        try {
            return o != null ? Double.parseDouble(String.valueOf(o)) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}