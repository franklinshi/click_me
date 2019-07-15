package com.example.click_me.adapter;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.services.weather.LocalDayWeatherForecast;
import com.amap.api.services.weather.LocalWeatherForecast;
import com.example.click_me.MainActivity;
import com.example.click_me.R;
import com.example.click_me.entity.weather_item;

import java.util.ArrayList;
import java.util.List;

//左下显示天气和温度的recyclerView的适配器
public class weatherAdapter extends RecyclerView.Adapter<weatherAdapter.VH>{

    public static class VH extends RecyclerView.ViewHolder{
        TextView day;
        ImageView img;
        TextView temp;
        public VH(View v) {
            super(v);
            day = (TextView) v.findViewById(R.id.day);
            img = (ImageView)v.findViewById(R.id.weather_img);
            temp = (TextView)v.findViewById(R.id.weather_temp);
        }
    }

    private List<LocalDayWeatherForecast> mDatas;
    public weatherAdapter(List<LocalDayWeatherForecast> data) {
        this.mDatas = data;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        if(holder instanceof RecyclerView.ViewHolder) {
            final LocalDayWeatherForecast iwf = mDatas.get(position);
            String week = iwf.getWeek();
            String day = "";
            switch (week){
                case "1":
                    day = "MON";
                    break;
                case "2":
                    day = "TUES";
                    break;
                case "3":
                    day = "WED";
                    break;
                case "4":
                    day = "THU";
                    break;
                case "5":
                    day = "FRI";
                    break;
                case "6":
                    day = "SAT";
                    break;
                case "7":
                    day = "SUN";
                    break;
                default:
            }
            String weather = iwf.getDayWeather();
            int id = 0;
            switch (weather){
                case "多云":
                case "晴间多云":
                case "少云":
                  id = R.drawable.cloudy;
                  break;
                case "晴":
                case "热":
                  id = R.drawable.sunny;
                  break;
                case "和风":
                case "微风":
                case "有风":
                case "清风":
                case "强风":
                case "劲风":
                case "疾风":
                case "大风":
                case "烈风":
                    id = R.drawable.wind;
                    break;
                case "雨":
                case "小雨":
                case "小雨-中雨":
                    id = R.drawable.rain_1;
                    break;
                case "中雨":
                case "中雨-大雨":
                case "阵雨":
                    id = R.drawable.rain2;
                    break;
                case "大雨":
                case "大雨-暴雨":
                case "强阵雨":
                case "暴雨":
                case "大暴雨":
                    id = R.drawable.rain_3;
                    break;
                case "雪":
                case "小雪":
                case "阵雪":
                case "中雪":
                case "大雪":
                case "小雪-中雪":
                case "中雪-大雪":
                    id = R.drawable.snow;
                    break;
                case "雷阵雨":
                case "强雷阵雨":
                    id = R.drawable.thunder_rain;
                    break;
                  default:
                  id = R.drawable.def;
            }
            holder.day.setText(day);
            holder.img.setImageResource(id);
            holder.temp.setText(iwf.getNightTemp()+"℃~"+iwf.getDayTemp()+"℃");
        }
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        //LayoutInflater.from指定写法
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new VH(v);
    }
    //更新数据
    public void update(List<LocalDayWeatherForecast> data) {
        this.mDatas = data;
        notifyDataSetChanged();
    }
}