package com.example.click_me;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.click_me.entity.User;
import com.example.click_me.util.DBUtil;

import java.sql.SQLException;

public class TestActivity extends AppCompatActivity {

    private TextView tv_id;
    private TextView tv_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        tv_id = findViewById(R.id.tv_id);
        tv_name = findViewById(R.id.tv_name);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = handler.obtainMessage();
                try {
                    User user = DBUtil.queryUser("111");
                    message.what = 1;
                    message.obj = user;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                handler.sendMessage(message);
            }
        }).start();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    User user = (User) msg.obj;
                    tv_id.setText(user.getId());
                    tv_name.setText(user.getName());
                    break;
            }
        }

    };
}
