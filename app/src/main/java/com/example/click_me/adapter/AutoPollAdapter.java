package com.example.click_me.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.click_me.R;
import com.example.click_me.entity.news_item;

import java.util.List;

public class AutoPollAdapter extends RecyclerView.Adapter<AutoPollAdapter.BaseViewHolder> {
    private final List<news_item> mData;

    public AutoPollAdapter(List<news_item> list) {
        this.mData = list;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_item, parent, false);
        BaseViewHolder holder = new BaseViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        news_item data = mData.get(position % mData.size());
        holder.content.setText(data.getContent());
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    class BaseViewHolder extends RecyclerView.ViewHolder{
        TextView content;
        ImageView img;
        public BaseViewHolder(@NonNull View v) {
            super(v);
            content = (TextView)v.findViewById(R.id.news_item_tv);
            img = (ImageView)v.findViewById(R.id.news_item_img);
        }
    }
}
