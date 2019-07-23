package com.example.click_me;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.services.weather.LocalDayWeatherForecast;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.example.click_me.adapter.AutoPollAdapter;
import com.example.click_me.adapter.NewsAdapter;
import com.example.click_me.adapter.weatherAdapter;
import com.example.click_me.entity.news_item;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends FragmentActivity implements WeatherSearch.OnWeatherSearchListener , frag_ele.OnFragmentInteractionListener ,frag_video.OnFragmentInteractionListener{

    //handler状态码
    private static final int Updated = 0x003; //需要更新左半边的pm2.5信息等
    private static final int doorStat = 0x002; //门的状态变化会收到
    private static final int liftStat = 0x004; //电梯开始上/下行时收到
    private static final int goalStat = 0x005; //在0x004接收后收到，可获取最终目的楼层
    private static final int callStat= 0x006; //类似passStat，接收呼梯的mqtt
    private static final int passStat= 0x007; //乘客刷卡时接收到的mqtt(不刷卡直接按楼层无法接收)
    private static final int nextMsg = 0x008; //收到时切换到下一条消息
    //特定时间发送的广播字串，根据时间经过切换显示内容
    public static final String ALARM_GET_FOOD = "alarm_get_food";
    public static final String ALARM_GET_NEWS = "alarm_get_news";

    private AlarmManager am;
    private ArrayList<news_item> newsList;

    //添加fragment
    Handler handler_fg1;
    frag_ele frag1;
    frag_video frag2;
    FragmentManager frag_manager;

    Mqtt_lift mqtt_lift;

    //高德api获取天气等信息
    private List<LocalDayWeatherForecast> list = new ArrayList<>();
    WeatherSearchQuery mquery;
    WeatherSearch mweathersearch;
    LocalWeatherLive weatherlive;
    weatherAdapter wAdapter;

    //获取pm2.5建的client
    private OkHttpClient client = new OkHttpClient();
    private Request request;
    private Callback callback;

    @Bind(R.id.layout_left)
    LinearLayout layout_left;
    @Bind(R.id.weather_list)
    RecyclerView weather_list;
    @Bind(R.id.time)
    TextClock time;
    @Bind(R.id.temp)
    TextView temp;
    @Bind(R.id.air)
    TextView air;
    @Bind(R.id.tv_pm25_val)
    TextView tv_pm25_val;
    @Bind(R.id.card_ele)
    CardView card_ele;
    @Bind(R.id.main_news_recyclerView)
    AutoPollRecyclerView news_recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取消顶部标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        init();

    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final FragmentTransaction frag_transaction = frag_manager.beginTransaction();
            switch (msg.what){
                case Updated:
                    //更新pm2.5的数值，一般不会超200所以就设了三色
                    Integer i = msg.arg1;
                    tv_pm25_val.setText(i.toString());
                    if (i <= 50) {
                        tv_pm25_val.setTextColor(getApplicationContext().getResources().getColor(R.color.green));
                    } else if (i > 50 && i <= 100) {
                        tv_pm25_val.setTextColor(getApplicationContext().getResources().getColor(R.color.yellow));
                    } else if (i > 100 && i <= 200) {
                        tv_pm25_val.setTextColor(getApplicationContext().getResources().getColor(R.color.orange));
                    }
                    break;

                case liftStat:
                case callStat:
                case doorStat:
                case goalStat:
                case passStat:
                    Message new_msg = new Message();
                    new_msg.what = msg.what;
                    new_msg.setData(msg.getData());
                    handler_fg1.sendMessage(new_msg);  //发送mqtt信息给fragment1(frag_ele)
                    break;

                default:
            }
        }

    };

        private void init() {
            ButterKnife.bind(this);
            newsList = new ArrayList<>();
            for(int i = 0 ; i < 4; ++i){
                news_item ni = new news_item();
                ni.setContent("现代摩比斯研驾驶员状态预警系统，探测驾驶员是否粗心驾驶");
                newsList.add(ni);
            }
            /*
            NewsAdapter newsAdapter = new NewsAdapter(newsList);
            LinearLayoutManager manager_news = new LinearLayoutManager(this);
            manager_news.setOrientation(LinearLayoutManager.VERTICAL);
            news_recyclerView.setLayoutManager(manager_news);
            news_recyclerView.setAdapter(newsAdapter);*/
            AutoPollAdapter adapter = new AutoPollAdapter(newsList);
            news_recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
            news_recyclerView.setAdapter(adapter);
            news_recyclerView.start();


            //设置定时切换
            am = (AlarmManager)getSystemService(ALARM_SERVICE);
            //11点触发广播
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 13);
            calendar.set(Calendar.MINUTE, 20);
            calendar.set(Calendar.SECOND, 0);
            System.out.println(calendar.getTime());
            Intent intent_food = new Intent(ALARM_GET_FOOD);
            PendingIntent pi_food = PendingIntent.getBroadcast(MainActivity.this,0,intent_food,0);
            long intervalMillis  = 60 * 1000 * 60 * 24; // 闹钟间隔
            am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), intervalMillis, pi_food);

            frag_manager = getSupportFragmentManager();
            FragmentTransaction frag_transaction = frag_manager.beginTransaction();
            frag1 = new frag_ele();
            frag2 = new frag_video();
            frag_transaction.add(R.id.card_ele, frag1).commit();

            //开始接收电梯发来的mqtt
            mqtt_lift = new Mqtt_lift();
            mqtt_lift.addMqttHandler(handler);
            mqtt_lift.listen_lift();

            //获取pm2.5指数
            request = new Request.Builder()
                    .url("https://api.waqi.info/feed/shanghai/?token=5741866f4c9307e09e3dcd24a8ad0f10ae868921")
                    .build();
            callback = new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.out.println("request to get pm2.5 failed");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    String payload = new String(response.body().string());
                    System.out.println(payload);
                        JSONObject jsb = JSON.parseObject(payload);

                            int pm25 = jsb.getJSONObject("data").getJSONObject("iaqi").getJSONObject("pm25").getIntValue("v");
                            System.out.println(pm25);
                            Message msg = new Message();
                            msg.what = Updated;
                            msg.arg1 = pm25;
                            handler.sendMessage(msg);
                        }
            };
            client.newCall(request).enqueue(callback);


            //检索参数为城市和天气类型，实况天气为WEATHER_TYPE_LIVE、天气预报为WEATHER_TYPE_FORECAST
            mquery = new WeatherSearchQuery("上海", WeatherSearchQuery.WEATHER_TYPE_LIVE);
            mweathersearch=new WeatherSearch(this);
            mweathersearch.setOnWeatherSearchListener(this);
            mweathersearch.setQuery(mquery);
            mweathersearch.searchWeatherAsyn(); //异步搜索

            //预报未来几天
            mquery = new WeatherSearchQuery("上海", WeatherSearchQuery.WEATHER_TYPE_FORECAST);
            mweathersearch = new WeatherSearch(this);
            mweathersearch.setOnWeatherSearchListener(this);
            mweathersearch.setQuery(mquery);
            //异步搜索
            mweathersearch.searchWeatherAsyn();

            wAdapter = new weatherAdapter(list);
            LinearLayoutManager manager = new LinearLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.HORIZONTAL);
            weather_list.setLayoutManager(manager);
            weather_list.setAdapter(wAdapter);

            //每一小时更新一次pm2.5值
            Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    client.newCall(request).enqueue(callback);
                }
            }, 3*60*30*1000, 3*60*30*1000);
            //每5时更新一次天气的实时和预报
            Timer t2 = new Timer();
            t2.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mquery = new WeatherSearchQuery("上海", WeatherSearchQuery.WEATHER_TYPE_LIVE);
                    mweathersearch=new WeatherSearch(MainActivity.this);
                    mweathersearch.setOnWeatherSearchListener(MainActivity.this);
                    mweathersearch.setQuery(mquery);
                    mweathersearch.searchWeatherAsyn(); //异步搜索

                    //预报未来几天
                    mquery = new WeatherSearchQuery("上海", WeatherSearchQuery.WEATHER_TYPE_FORECAST);
                    mweathersearch = new WeatherSearch(MainActivity.this);
                    mweathersearch.setOnWeatherSearchListener(MainActivity.this);
                    mweathersearch.setQuery(mquery);
                    //异步搜索
                    mweathersearch.searchWeatherAsyn();

                }
            }, 5*60*60*1000, 5*60*60*1000);

        }




    /**
     * 实时天气查询回调
     */
    @Override
    public void onWeatherLiveSearched(LocalWeatherLiveResult weatherLiveResult ,int rCode) {
        if (rCode == 1000) {
            if (weatherLiveResult != null&&weatherLiveResult.getLiveResult() != null) {
                weatherlive = weatherLiveResult.getLiveResult();
                air.setText(weatherlive.getWeather());
                temp.setText(weatherlive.getTemperature()+"°");
            }else {
                System.out.println("onWeatherLiveSearched: NO result");
            }
        }else {
            System.out.println("onWeatherLiveSeached: error");
        }
    }
    /**
     * 天气预报
     * @param localWeatherForecastResult
     * @param i
     */
    @Override
    public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {
        list = localWeatherForecastResult.getForecastResult().getWeatherForecast();
        wAdapter.update(list);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //做想要做的数据操作和通信，如给其他Fragment发送数据
    }

    //获取第一个碎片，显示电梯状态的碎片的handler
    public void getHandler_fg1(Handler handler){
        this.handler_fg1 = handler;
    }

    private class AlarmReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println(action);
            switch (action){
                case "alarm_get_food":
                    System.out.println("alarm_get_food received");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FragmentTransaction ft = frag_manager.beginTransaction();
                            ft.replace(R.id.card_ele,frag2).commit();
                        }
                    });
                    break;
                case "alarm_get_news":
                    System.out.println("alarm_get_news received");
                    break;
                default:
            }

        }
    }
}


