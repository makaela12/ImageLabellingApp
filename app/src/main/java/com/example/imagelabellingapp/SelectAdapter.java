package com.example.imagelabellingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SelectAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private List<String> mMainTextList;
    private List<String> mSubTextList;

    public SelectAdapter(Context context, List<String> mainTextList, List<String> subTextList) {
        super(context, R.layout.list_item_project, mainTextList);
        mContext = context;
        mMainTextList = mainTextList;
        mSubTextList = subTextList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = inflater.inflate(R.layout.list_item_project, parent, false);
        }

        TextView mainTextView = listItemView.findViewById(R.id.projectName);
        TextView subTextView = listItemView.findViewById(R.id.projectDescript);

        mainTextView.setText(mMainTextList.get(position));
        subTextView.setText(mSubTextList.get(position));

        return listItemView;
    }

    @Override
    public String getItem(int position) {
        return mMainTextList.get(position);
    }
    public void updateData(List<String> mainTextList, List<String> subTextList) {
        mMainTextList.clear();
        mMainTextList.addAll(mainTextList);
        mSubTextList.clear();
        mSubTextList.addAll(subTextList);
        notifyDataSetChanged();
    }
    public void addAll(List<String> mainTextList, List<String> subTextList) {
        mMainTextList.addAll(mainTextList);
        mSubTextList.addAll(subTextList);
        notifyDataSetChanged();
    }


}
