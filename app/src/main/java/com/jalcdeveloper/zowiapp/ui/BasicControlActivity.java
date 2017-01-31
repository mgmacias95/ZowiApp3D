package com.jalcdeveloper.zowiapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jalcdeveloper.zowiapp.R;
import com.jalcdeveloper.zowiapp.ZowiApp;
import com.jalcdeveloper.zowiapp.io.Zowi;
import com.jalcdeveloper.zowiapp.io.ZowiHelper;
import com.jalcdeveloper.zowiapp.io.ZowiProtocol;

import java.util.Timer;
import java.util.TimerTask;

// Sensores de movimiento
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.content.Context;
// clases para poder captar cambios en los sensores
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class BasicControlActivity extends ImmersiveActivity implements SensorEventListener {

    private static final String TAG = BasicControlActivity.class.getSimpleName();

    private ImageButton buttonWalkForward;
    private ImageButton buttonWalkBackward;
    private ImageButton buttonTurnLeft;
    private ImageButton buttonTurnRight;
    private ImageButton buttonJump;
    private ImageButton buttonMoonwalkerRight;
    private ImageButton buttonMoonwalkerLeft;
    private ImageButton buttonSwing;
    private ImageButton buttonCrusaitoRight;
    private ImageButton buttonCrusaitoLeft;
    private TextView textBattery;
    
    // sensores de movimiento
    private SensorManager mSensorManager;
    private Sensor mSensor;
    // vector para guardar los valores devueltos por el sensor de rotación
    private float[] mRot;

    private Zowi zowi;
    private ZowiHelper zowiHelper;
    private Timer batteryTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_control);

        zowi = ((ZowiApp) getApplication()).zowi;
        zowiHelper = new ZowiHelper(zowi);

        buttonWalkForward = (ImageButton) findViewById(R.id.button_walk_forward);
        buttonWalkBackward = (ImageButton) findViewById(R.id.button_walk_backward);
        buttonTurnLeft = (ImageButton) findViewById(R.id.button_turn_left);
        buttonTurnRight = (ImageButton) findViewById(R.id.button_turn_right);
        buttonJump = (ImageButton) findViewById(R.id.button_jump);
        buttonMoonwalkerLeft = (ImageButton) findViewById(R.id.button_moonwalker_left);
        buttonMoonwalkerRight = (ImageButton) findViewById(R.id.button_moonwalker_right);
        buttonSwing = (ImageButton) findViewById(R.id.button_swing);
        buttonCrusaitoLeft = (ImageButton) findViewById(R.id.button_crusaito_left);
        buttonCrusaitoRight = (ImageButton) findViewById(R.id.button_crusaito_right);
        textBattery = (TextView) findViewById(R.id.text_battery);

        buttonWalkForward.setOnTouchListener(walkForwardOnTouchListener);
        buttonWalkBackward.setOnTouchListener(walkBackwardOnTouchListener);
        buttonTurnLeft.setOnTouchListener(turnLeftOnTouchListener);
        buttonTurnRight.setOnTouchListener(turnRightOnTouchListener);
        buttonJump.setOnTouchListener(jumpOnTouchListener);
        buttonMoonwalkerLeft.setOnTouchListener(moonwalkerLeftOnTouchListener);
        buttonMoonwalkerRight.setOnTouchListener(moonwalkerRightOnTouchListener);
        buttonSwing.setOnTouchListener(swingOnTouchListener);
        buttonCrusaitoLeft.setOnTouchListener(crusaitoLeftOnTouchListener);
        buttonCrusaitoRight.setOnTouchListener(crusaitoRightOnTouchListener);
        
        // sensores de movimiento
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        zowi.setRequestListener(requestListener);
        zowi.programIdRequest();

    }

    @Override
    protected void onResume() {
        /** Timmer Refresh battery Zowi level **/
        batteryTimer = new Timer();
        batteryTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                zowi.batteryRequest();
            }
        }, 0, 5000);
        super.onResume();
    }

    @Override
    protected void onPause() {
        batteryTimer.cancel();
        super.onPause();

    }

    // método para escuchar cambios en los valores de los sensores
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "DENTRO DEL onSensorChanged");
        // detectamos si se ha producido un giro
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            mRot = event.values.clone();
            Log.d(TAG, "ROTACION: " + mRot);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     *
     **/
    private View.OnTouchListener walkForwardOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.FORWARD_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener walkBackwardOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){

                case MotionEvent.ACTION_DOWN:
                    zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.BACKWARD_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener turnLeftOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener turnRightOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener jumpOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.jump(zowi, Zowi.FAST_SPEED);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener moonwalkerLeftOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.moonWalker(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener moonwalkerRightOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.moonWalker(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener swingOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.swing(zowi, Zowi.NORMAL_SPEED);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener crusaitoLeftOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.crusaito(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener crusaitoRightOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.crusaito(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     * Listener of request Zowi
     */
    Zowi.RequestListener requestListener = new Zowi.RequestListener() {
        @Override
        public void onResponse(char command, final String data) {
            Log.d(TAG, "Commannd Response: " + command + " - " + data);

            switch (command){
                case ZowiProtocol.BATTERY_COMMAND:

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textBattery.setText("Battery: " + data);
                        }
                    });
                    break;

            }
        }
    };
}
