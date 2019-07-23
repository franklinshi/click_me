package com.example.click_me.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.services.weather.LocalDayWeatherForecast;
import com.example.click_me.R;
import com.example.click_me.entity.news_item;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {
    private List<news_item> list = new ArrayList<>();

    public static class VH extends RecyclerView.ViewHolder{
        TextView content;
        ImageView img;
        public VH(@NonNull View v) {
            super(v);
            content = (TextView)v.findViewById(R.id.news_item_tv);
            img = (ImageView)v.findViewById(R.id.news_item_img);
        }
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.news_item, viewGroup, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int position) {
        if(vh instanceof RecyclerView.ViewHolder){
            news_item ni = list.get(position);
            vh.content.setText(ni.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public NewsAdapter(List<news_item> data){
        list = data;
   }
    //更新数据
    public void update(List<news_item> data) {
        this.list = data;
        notifyDataSetChanged();
    }

}
