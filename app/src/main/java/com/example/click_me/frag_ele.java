package com.example.click_me;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link frag_ele.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link frag_ele#newInstance} factory method to
 * create an instance of this fragment.
 */
public class frag_ele extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int doorStat = 0x002;
    private static final int liftStat = 0x004;
    private static final int goalStat = 0x005;
    private static final int callStat = 0x006;
    private static final int passStat = 0x007;

    MainActivity mActivity;
    static Bitmap[] bitmaps = new Bitmap[2]; //用来做电梯门开关动画的bitmap数组
    private CountDownTimer t1;   //负责显示电梯到达该楼层所要秒数
    private CountDownTimer t2;   //负责显示textview(welcome,xxx)后计时5秒让其不可见
    private static final int Floor = 1; //该平板所在楼层，固定。
    private int destination = 1;  //电梯所要去的目标楼层
    private boolean called = false; //在该平板所在楼层是否有人呼梯
    private int[] arr = new int[]{3, 2, 3, 1, 8}; //电梯不同状态时距离到达所在楼层需要时间/s

    @Bind(R.id.floor_tv)
    TextView floor_tv;
    @Bind(R.id.arrow)
    ImageView arrow;
    @Bind(R.id.left_img)
    ImageView left_img;
    @Bind(R.id.right_img)
    ImageView right_img;
    @Bind(R.id.cnt_down)
    TextView cnt_down;
    @Bind(R.id.layout_right)
    LinearLayout layout_right;
    @Bind(R.id.welcome)
    TextView welcome;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public frag_ele() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment frag_ele.
     */
    // TODO: Rename and change types and number of parameters
    public static frag_ele newInstance(String param1, String param2) {
        frag_ele fragment = new frag_ele();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inf = inflater.inflate(R.layout.layout_frag_ele, container, false);
        // Inflate the layout for this fragment
        ButterKnife.bind(this,inf);
        Mqtt_lift mqtt = new Mqtt_lift();
        mqtt.addMqttHandler(mqttHandler);
        mqtt.listen_lift();
        return inf;


    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        mActivity = (MainActivity)context;
        mActivity.getHandler_fg1(mqttHandler);
        //这里left和right指电梯门的左右两边门的bitmap
        Bitmap left = BitmapFactory.decodeResource(getResources(),R.drawable.left);
        Bitmap right = BitmapFactory.decodeResource(getResources(),R.drawable.right);
        bitmaps[0] = left;
        bitmaps[1] = right;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private final Handler mqttHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case doorStat:
                    String mDoor = msg.getData().getString("doorStat");
                    int mFloor = msg.getData().getInt("floorStat");
                    floor_tv.setText("floor " + mFloor);
                    arrow.setBackground(null);
                    if (mDoor.equals("opening")) {
                        left_img.animate().cancel();
                        right_img.animate().cancel();
                        open_door();
                    } else if (mDoor.equals("closing")) {
                        left_img.animate().cancel();
                        right_img.animate().cancel();
                        close_door();
                    }else if(mDoor.equals("closed")){
                        if(!called)
                            if(destination == mFloor) {
                                cnt_down.setVisibility(View.INVISIBLE);
                            }
                        if(mFloor == 1)
                            called = false;
                    }
                    if(mFloor == 2){
                        if(t1!=null) {
                            t1.cancel();
                            t1 = null;
                            t1 = new CountDownTimer((solve(mDoor)+1) * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    cnt_down.setText(millisUntilFinished / 1000 + "s");
                                }

                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }else{
                            t1 = new CountDownTimer((solve("opening")+1) * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    cnt_down.setText(millisUntilFinished / 1000 + "s");
                                }

                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }
                    }
                    break;
                case liftStat:
                    if(destination == 1)
                        cnt_down.setVisibility(View.VISIBLE);
                    String direction = msg.getData().getString("direction");
                    if(direction.equals("up")){
                        if(getActivity()!=null)
                        arrow.setBackground(getActivity().getResources().getDrawable(R.drawable.up_arrow));
                        if(t1 != null) {
                            t1.cancel();
                            t1 = null;
                            t1 = new CountDownTimer((solve("opening") + 9) * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    cnt_down.setText(millisUntilFinished / 1000 + "s");
                                }

                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }else{
                            t1 = new CountDownTimer((solve("opening") + 9) * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    cnt_down.setText(millisUntilFinished / 1000 + "s");
                                }

                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }
                    }else if(direction.equals("down")){
                        if(getActivity()!=null)
                        arrow.setBackground(getActivity().getResources().getDrawable(R.drawable.down_arrow));
                        if(t1 != null) {
                            t1.cancel();
                            t1 = null;
                            t1 = new CountDownTimer((9) * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    cnt_down.setText(millisUntilFinished / 1000 + "s");
                                }
                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }else{
                            t1 = new CountDownTimer((9) * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    cnt_down.setText(millisUntilFinished / 1000 + "s");
                                }

                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }
                    }
                    break;

                case goalStat:
                    destination = msg.getData().getInt("destination");
                    break;

                case callStat:
                    Integer fromFloor = msg.getData().getInt("fromfloor");
                    switch (fromFloor){
                        case 1:
                        case 5:
                        case 7:
                        case 9:
                            called = true;
                            cnt_down.setVisibility(View.VISIBLE);
                            break;
                        default:
                    }
                    break;
                case passStat:
                    String firstName = msg.getData().getString("firstname");
                    if(firstName.equals(null))firstName = "";
                    String lastName = msg.getData().getString("lastname");
                    welcome.setText("Welcome, "+firstName+" "+lastName);
                    if(t2!=null){
                        t2.cancel();
                        t2 = null;
                    }
                    t2 = new CountDownTimer(5 * 1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }
                        @Override
                        public void onFinish() {
                            if(getActivity()!=null){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        welcome.setText("");
                                    }
                                });
                            }
                        }
                    }.start();
                    break;

                default:
            }
        }
    };


    //开门动画
    private void open_door() {
        left_img.animate()
                .setDuration(3000)
                .translationX(-bitmaps[1].getWidth())
                .start();
        right_img.animate()
                .setDuration(3000)
                .translationX(bitmaps[1].getWidth())
                .start();
    }
    //关门动画
    private void close_door() {
        if (left_img.getTranslationX() == 0) {
            left_img.setTranslationX(-bitmaps[0].getWidth());
            right_img.setTranslationX(bitmaps[1].getWidth());
        }
        left_img.animate()
                .setDuration(3000)
                .translationX(0)
                .start();
        right_img.animate()
                .setDuration(3000)
                .translationX(0)
                .start();
    }

    //根据门状态获取电梯到达所在楼层的秒数
    private int solve(String status) {
        int ans = 0;
        int st = 0;
        switch (status){
            case "opening":
                st = 0;
                break;
            case "opened":
                st = 1;
                break;
            case "closing":
                st = 2;
                break;
            case "closed":
                st = 3;
                break;
            case "travel":
                st = 4;
                break;
            default:
        }
        for(int i = st; i < arr.length; ++i){
            ans += arr[i];
        }
        return ans;
    }

}
