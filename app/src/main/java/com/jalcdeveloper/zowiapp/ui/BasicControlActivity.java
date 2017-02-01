package com.jalcdeveloper.zowiapp.ui;

import android.content.Intent;
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
// valor absoluto
import java.lang.Math;

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
    private Button speak;

    // sensores de movimiento
    private SensorManager mSensorManager;
    private Sensor mSensor;
    // vector para guardar los valores devueltos por el sensor de rotación y los previos
    private float[] mRot;
    private float[] prev_mRot;
    // timestamp del último movimiento detectado
    private float timestamp;
    private float EPSILON = 0.1f;
    private int last_move = -1;

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
        speak = (Button) findViewById(R.id.speech_btn);

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
        this.timestamp = 0;

        zowi.setRequestListener(requestListener);
        zowi.programIdRequest();

        speak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent speak_ac = new Intent(getApplicationContext(), MainVoiceActivity.class);
                startActivity(speak_ac);
            }
        });
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
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        batteryTimer.cancel();
        super.onPause();

    }

    // devuelve el máximo elemento entre los tres primeros elementos un array
    private int max(float[] list){
        int ind = -1;
        float max = -9999999;
        for (int i = 0; i < 3; i++) {
            if(list[i] > max) {
                max = list[i];
                ind = i;
            }
        }
        return ind;
    }

    // despresiona y para a zowi cuando se detecta un movimiento brusco
    private void stopZowi() {
        switch (last_move) {
            case 0:
                this.buttonWalkForward.setPressed(false);
                break;
            case 1:
                this.buttonWalkBackward.setPressed(false);
                break;
            case 2:
                this.buttonTurnLeft.setPressed(false);
                break;
            case 3:
                this.buttonTurnRight.setPressed(false);
                break;
        }
        //zowiHelper.stop(zowi);
    }

    // método para escuchar cambios en los valores de los sensores
    @Override
    public void onSensorChanged(SensorEvent event) {

        // detectamos si se ha producido un giro
        if (this.timestamp != 0) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                /*
                   values es un vector de float donde:
                   values[0]: x*sin(theta/2)
                   values[1]: y*sin(theta/2)
                   values[2]: z*sin(theta/2)
                   values[3]: cos(theta/2)
                   values[4]: estimated heading accuracy (in radians) or -1 if unavailable
                */
                this.mRot = event.values.clone();
                // Log.d(TAG, "mRot = " + mRot[0] + " " + mRot[1] + " " + mRot[2]);
                // Log.d(TAG, "prev_mRot = " + prev_mRot[0] + " " + prev_mRot[1] + " " + prev_mRot[2]);
                float[] resta = new float[mRot.length];
                for (int i=0; i<mRot.length; i++) {
                    resta[i] = Math.abs(mRot[i] - prev_mRot[i]);
                }
                //Log.d(TAG, "resta = " + resta[0] + " " + resta[1] + " " + resta[2]);
                int ind = this.max(resta);
                //Log.d(TAG, "Maximo = " + ind);
                if(resta[ind] > EPSILON) {
                    if (last_move != -1) {
                        stopZowi();
                    }
                    switch (ind) {
                        case 0:
                            Log.d(TAG, "Se mueve en el eje 0");
                            if (mRot[0] >= 0) {
                                Log.d(TAG, "Es positivo");
                                last_move = 3;
                            } else {
                                Log.d(TAG, "Es negativo");
                                last_move = 2;
                            }
                            break;
                        case 1:
                            Log.d(TAG, "Se mueve en el eje 1");
                            if (mRot[1] >= 0) {
                                Log.d(TAG, "Es positivo");
                                last_move = 0;
                            } else {
                                Log.d(TAG, "Es negativo");
                                last_move = 1;
                            }
                            break;
                        case 2:
                            Log.d(TAG, "Se mueve en el eje 2");
                            if (mRot[2] >= 0) {
                                Log.d(TAG, "Es positivo");
                                last_move = -1;
                            } else {
                                Log.d(TAG, "Es negativo");
                                last_move = -1;
                            }
                            break;
                    }
                } else {
                    switch (last_move) {
                        case 0:
                            this.buttonWalkForward.setPressed(true);
                            //zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.FORWARD_DIR);
                            break;
                        case 1:
                            this.buttonWalkBackward.setPressed(true);
                            //zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.BACKWARD_DIR);
                            break;
                        case 2:
                            this.buttonTurnLeft.setPressed(true);
                            //zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, zowi.LEFT_DIR);
                            break;
                        case 3:
                            this.buttonTurnRight.setPressed(true);
                            //zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, zowi.RIGHT_DIR);
                            break;
                    }
                }
            }
        }
        this.timestamp = event.timestamp;
        this.prev_mRot = event.values.clone();
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
