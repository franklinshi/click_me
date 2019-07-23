package com.example.click_me;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;

//订阅并接收电梯状态消息，并发送
public class Mqtt_lift {
    private String username="admin";
    private String password="admin";
    private String broker="tcp://47.96.26.134:1883";
    private String clientId="user"+System.currentTimeMillis();
    String content_1to2 = "001141|0557673|";
    String content_2to1 = "001142|0557673|";
    private static final int doorStat=0x002;
    private static final int liftStat =0x004;
    private static final int goalStat=0x005;
    private static final int callStat=0x006;
    private static final int passStat=0x007;

    //获取对应的handler来发送信息，虽然这里只有mainActivtiy一个
    private MqttClient client;
    private ArrayList<Handler> handlers = new ArrayList<>();
    public void addMqttHandler(Handler h){
        handlers.add(h);
    }
    public void removeMqttHandler(Handler h){
        handlers.remove(h);
    }
    public boolean containsMqttHandler(Handler h){
        return handlers.contains(h);
    }

    //只呼梯的操作
    public void call_lift(){
        final String Tcall = "port/rnd/call";
        int qos = 1;
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions conOpts = new MqttConnectOptions();
            conOpts.setUserName(username);
            conOpts.setPassword(password.toCharArray());
            client.connect(conOpts);

            MqttMessage msg;
            msg = new MqttMessage(content_1to2.getBytes()); //表示由1楼至2楼的呼梯字段
            msg.setQos(qos);

            client.publish(Tcall,msg);  //发送相应topic的msg
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
    public void listen_lift(){

        /* publish a call lift message
         * if success: call lift msg published
         * if fail: failed at + loc. reasoncode + msg +cause
         */
        final String Tcall = "port/rnd/call";
        final String Tdoor = "port/rnd/door";
        final String Tlift = "port/rnd/travel";
        final String Tgoal = "port/rnd/destination";
        final String Tpass = "port/rnd/passenger";
        int qos = 1;
        MemoryPersistence persistence = new MemoryPersistence();
        try{
            client=new MqttClient(broker,clientId,persistence);
            MqttConnectOptions conOpts=new MqttConnectOptions();
            conOpts.setUserName(username);
            conOpts.setPassword(password.toCharArray());
            client.connect(conOpts);

            MqttMessage msg;
            msg =new MqttMessage(content_1to2.getBytes());
            msg.setQos(qos);

            //订阅，ps若既subscribe又publish同一个topic可能会出错
            client.subscribe(Tdoor);
            client.subscribe(Tlift);
            client.subscribe(Tgoal);
            client.subscribe(Tcall);
            client.subscribe(Tpass);


            MqttCallback mqttCallback=new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload=new String(message.getPayload());
                    System.out.println(topic);
                    System.out.println(payload);
                    if(topic.equals(Tdoor)){
                        //analyzing payload
                        JSONObject obj= JSON.parseObject(payload);
                        String doorStatus = obj.getString("door");
                        int floorStatus = obj.getIntValue("floor");
                        String direction = obj.getString("direction");
                        System.out.println("received Tdoor: "+doorStatus);
                        Message msg1=new Message();
                        msg1.what=doorStat;
                        Bundle bundle=new Bundle();
                        bundle.putString("doorStat",doorStatus);
                        bundle.putInt("floorStat",floorStatus);
                        bundle.putString("direction",direction);
                        msg1.setData(bundle);
                        for(int i = 0; i < handlers.size();++i) {
                            handlers.get(i).sendMessage(msg1);
                        }
                    }
                    if(topic.equals(Tlift)){
                        JSONObject obj= JSON.parseObject(payload);
                        String direction = obj.getString("direction");
                        Message msg1=new Message();
                        msg1.what=liftStat;
                        Bundle bundle=new Bundle();
                        bundle.putString("direction",direction);
                        msg1.setData(bundle);
                        for(int i = 0; i < handlers.size();++i) {
                            handlers.get(i).sendMessage(msg1);
                        }
                    }
                    if(topic.equals(Tgoal)){
                        JSONObject obj= JSON.parseObject(payload);
                        Integer destination = obj.getInteger("destination");
                        Message msg1=new Message();
                        msg1.what=goalStat;
                        Bundle bundle=new Bundle();
                        bundle.putInt("destination",destination);
                        msg1.setData(bundle);
                        for(int i = 0; i < handlers.size();++i) {
                            handlers.get(i).sendMessage(msg1);
                        }
                    }
                    if(topic.equals(Tcall)){
                        Integer fromFloor = Integer.parseInt(payload.substring(5,6));
                        Message msg1=new Message();
                        msg1.what=callStat;
                        Bundle bundle=new Bundle();
                        bundle.putInt("fromfloor",fromFloor);
                        msg1.setData(bundle);
                        for(int i = 0; i < handlers.size();++i) {
                            handlers.get(i).sendMessage(msg1);
                        }
                    }
                    if(topic.equals(Tpass)){
                        JSONObject obj= JSON.parseObject(payload);
                        String firstName = obj.getString("firstName");
                        String lastName = obj.getString("lastName");
                        Message msg1=new Message();
                        msg1.what=passStat;
                        Bundle bundle=new Bundle();
                        bundle.putString("firstname",firstName);
                        bundle.putString("lastname",lastName);
                        msg1.setData(bundle);
                        for(int i = 0; i < handlers.size();++i) {
                            handlers.get(i).sendMessage(msg1);
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            };
            client.setCallback(mqttCallback);

        }catch (MqttException e){
           System.out.println("failed at"+e.getLocalizedMessage()+"."+e.getReasonCode()+e.getMessage()+e.getCause());
        }
    }
    public void stop(){
        try{
            client.disconnect();
            client.close();
            System.out.println("mqtt service stopped");
        }catch(MqttException e){
            e.printStackTrace();
        }
    }
    }

