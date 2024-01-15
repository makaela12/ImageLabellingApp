package com.example.imagelabellingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter<S> extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<String> data;

    public FileAdapter(List<String> data) {
        this.data = data;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position) {
        String item = data.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {

        TextView textViewFileName;

        public FileViewHolder(View itemView) {
            super(itemView);
            textViewFileName = itemView.findViewById(android.R.id.text1);
        }

        public void bind(String fileName) {
            textViewFileName.setText(fileName);
        }
    }

    public void addAll(List<String> items) {
        this.data.addAll(items);
        notifyDataSetChanged();
    }
}