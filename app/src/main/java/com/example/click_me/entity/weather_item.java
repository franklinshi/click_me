package com.example.click_me.entity;


public class weather_item {
    private String day;  //星期几
    private int weather;  //天气
    private String temp;  //温度

    public weather_item(){
        super();
    }
    public weather_item(String day,int weather,String temp){
        super();
        this.day = day;
        this.weather = weather;
        this.temp = temp;
    }
    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public int getWeather() {
        return weather;
    }

    public void setWeather(int weather) {
        this.weather = weather;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }
}
