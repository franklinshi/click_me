package com.example.click_me;


import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.example.click_me.adapter.weatherAdapter;


import java.io.IOException;
import java.util.ArrayList;
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
    @Bind(R.id.msg_item_icon)
    CircleImageView icon;
    @Bind(R.id.msg_item_msg)
    TextView msg;
    @Bind(R.id.msg_item_time)
    TextView msg_item_time;
    @Bind(R.id.msg_item_userName)
    TextView userName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    handler_fg1.sendMessage(msg);  //发送mqtt信息给fragment1(frag_ele)
                    break;

                default:
            }
        }

    };

        private void init() {
            ButterKnife.bind(this);
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
            }, 60*30*1000, 60*30*1000);
            //每3小时更新一次天气的实时和预报
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
            }, 3*60*60*1000, 3*60*60*1000);
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


}


