package com.example.generalExpanseTracker.Adapter;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.generalExpanseTracker.R;
import com.example.generalExpanseTracker.Model.TransactionModel;

import java.text.SimpleDateFormat;
import java.util.*;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    List<TransactionModel> list;

    public TransactionAdapter(List<TransactionModel> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDesc, tvAmount, tvDate, tvCategory;

        public ViewHolder(View view) {
            super(view);
            tvDesc = view.findViewById(R.id.tvDesc);
            tvAmount = view.findViewById(R.id.tvAmount);
            tvDate = view.findViewById(R.id.tvDate);
            tvCategory = view.findViewById(R.id.tvCategory);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        TransactionModel txn = list.get(position);

        holder.tvDesc.setText(txn.desc);
        holder.tvCategory.setText(txn.category);

        // Date format
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(txn.time)));

        // Amount + color
        if ("credit".equalsIgnoreCase(txn.type)) {
            holder.tvAmount.setText("+ ₹" + txn.amount);
            holder.tvAmount.setTextColor(Color.GREEN);
        } else {
            holder.tvAmount.setText("- ₹" + txn.amount);
            holder.tvAmount.setTextColor(Color.RED);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}