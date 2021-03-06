/*
 *  Copyright 2016 Zoraida Callejas, Michael McTear and David Griol
 *
 *  This file is part of the Conversandroid Toolkit, from the book:
 *  The Conversational Interface, Michael McTear, Zoraida Callejas and David Griol
 *  Springer 2016 <https://github.com/zoraidacallejas/ConversationalInterface/>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jalcdeveloper.zowiapp.ui;

/**
 * Example activity with speech input and output that implements the
 * speech management methods in VoiceActivity.
 * When the button is pressed, the user is asked to say something and
 * the system synthesizes it back.
 *
 * @author Zoraida Callejas, Michael McTear, David Griol
 * @version 3.0, 05/13/16
 */

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import com.jalcdeveloper.zowiapp.ZowiApp;
import com.jalcdeveloper.zowiapp.io.Zowi;
import com.jalcdeveloper.zowiapp.io.ZowiHelper;
import com.jalcdeveloper.zowiapp.io.ZowiProtocol;

import com.jalcdeveloper.zowiapp.R;

public class MainVoiceActivity extends VoiceActivity {

    private static final String LOGTAG = "TALKBACK";
    private static Integer ID_PROMPT_QUERY = 0;
    private static Integer ID_PROMPT_INFO = 1;

    private long startListeningTime = 0; // To skip errors (see processAsrError method)

    private Zowi zowi;
    private ZowiHelper zowiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set layout
        setContentView(R.layout.activity_voice);

        //Initialize the speech recognizer and synthesizer
        initSpeechInputOutput(this);

        //Set up the speech button
        setSpeakButton();
        // and the go back button
        setGoBackButton();

        // create zowi :D
        zowi = ((ZowiApp) getApplication()).zowi;
        zowiHelper = new ZowiHelper(zowi);
    }

    /**
     * Initializes the search button and its listener. When the button is pressed, a feedback is shown to the user
     * and the recognition starts
     */
    private void setSpeakButton() {
        // gain reference to speak button
        Button speak = (Button) findViewById(R.id.speech_btn);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak
                try {
                    speak(getResources().getString(R.string.initial_prompt), ID_PROMPT_QUERY);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS no accessible");
                }
            }
        });
    }
    private void setGoBackButton() {
        // gain reference to speak button
        Button speak = (Button) findViewById(R.id.go_back_button);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Explain to the user why we need their permission to record audio on the device
     * See the checkASRPermission in the VoiceActivity class
     */
    public void showRecordPermissionExplanation(){
        Toast.makeText(getApplicationContext(), "Talback necesita acceder al micrófono para poder grabar audio", Toast.LENGTH_SHORT).show();
    }

    /**
     * If the user does not grant permission to record audio on the device, a message is shown and the app finishes
     */
    public void onRecordAudioPermissionDenied(){
        Toast.makeText(getApplicationContext(), "Talkback no puede trabajar sin acceder al micrófono", Toast.LENGTH_SHORT).show();
        System.exit(0);
    }

    /**
     * Starts listening for any user input.
     * When it recognizes something, the <code>processAsrResult</code> method is invoked.
     * If there is any error, the <code>onAsrError</code> method is invoked.
     */
    private void startListening(){

        if(deviceConnectedToInternet()){
            try {

				/*Start listening, with the following default parameters:
					* Language = English
					* Recognition model = Free form,
					* Number of results = 1 (we will use the best result to perform the search)
					*/
                startListeningTime = System.currentTimeMillis();
                listen(Locale.getDefault(), RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
            } catch (Exception e) {
                this.runOnUiThread(new Runnable() {  //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(),"ASR no se pudo iniciar", Toast.LENGTH_SHORT).show();
                        changeButtonAppearanceToDefault();
                    }
                });

                Log.e(LOGTAG,"ASR no se pudo iniciar");
                try { speak("No se ha podido iniciar el reconocimiento del habla",
                        ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS no accessible"); }

            }
        } else {

            this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                public void run() {
                    Toast.makeText(getApplicationContext(),"Comprueba tu conexión a internet",
                            Toast.LENGTH_SHORT).show();
                    changeButtonAppearanceToDefault();
                }
            });
            try { speak("Comprueba tu conexión a internet", ID_PROMPT_INFO); }
            catch (Exception ex) { Log.e(LOGTAG, "TTS no accessible"); }
            Log.e(LOGTAG, "Dispositivo no conectado a internet");

        }
    }

    /**
     * Invoked when the ASR is ready to start listening. Provides feedback to the user to show that the app is listening:
     * 		* It changes the color and the message of the speech button
     */
    @Override
    public void processAsrReadyForSpeech() {
        changeButtonAppearanceToListening();
    }

    /**
     * Provides feedback to the user to show that the app is listening:
     * 		* It changes the color and the message of the speech button
     */
    private void changeButtonAppearanceToListening(){
        Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
        button.setText(getResources().getString(R.string.speechbtn_listening)); //Changes the button's message to the text obtained from the resources folder
        button.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.speechbtn_listening),PorterDuff.Mode.MULTIPLY);  //Changes the button's background to the color obtained from the resources folder
    }

    /**
     * Provides feedback to the user to show that the app is idle:
     * 		* It changes the color and the message of the speech button
     */
    private void changeButtonAppearanceToDefault(){
        Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
        button.setText(getResources().getString(R.string.speechbtn_default)); //Changes the button's message to the text obtained from the resources folder
        button.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.speechbtn_default),PorterDuff.Mode.MULTIPLY); 	//Changes the button's background to the color obtained from the resources folder
    }

    /**
     * Provides feedback to the user (by means of a Toast and a synthesized message) when the ASR encounters an error
     */
    @Override
    public void processAsrError(int errorCode) {

        changeButtonAppearanceToDefault();

        //Possible bug in Android SpeechRecognizer: NO_MATCH errors even before the the ASR
        // has even tried to recognized. We have adopted the solution proposed in:
        // http://stackoverflow.com/questions/31071650/speechrecognizer-throws-onerror-on-the-first-listening
        long duration = System.currentTimeMillis() - startListeningTime;
        if (duration < 500 && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.e(LOGTAG, "El sistema no parece haber escuchado. Duración = " + duration + "ms. Ignorando el error");
            stopListening();
        }
        else {
            String errorMsg = "";
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMsg = "Error al grabar el audio";
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMsg = "Error desconocido en el cliente";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMsg = "Permisos insuficientes";
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMsg = "Error en la red";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMsg = "Agotado el tiempo de espera";
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = "No se ha encontrado ningún resultado";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMsg = "RecognitionService ocupado";
                case SpeechRecognizer.ERROR_SERVER:
                    errorMsg = "Error en el servidor";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = "No hay audio de entrada";
                default:
                    errorMsg = ""; //Another frequent error that is not really due to the ASR, we will ignore it
            }
            if (errorMsg != "") {
                this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error en el reconocimeinto del habla", Toast.LENGTH_LONG).show();
                    }
                });

                Log.e(LOGTAG, "Error al intentar escuchar: " + errorMsg);
                try { speak(errorMsg, ID_PROMPT_INFO); } catch (Exception e) { Log.e(LOGTAG, "TTS no accessible"); }
            }
        }


    }



    /**
     * Synthesizes the best recognition result
     */
    @Override
    public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {

        if(nBestList!=null){

            Log.d(LOGTAG, "ASR encontrados " + nBestList.size() + " resultados");

            if(nBestList.size()>0){
                String bestResult = nBestList.get(0).toLowerCase(); //We will use the best result
                /*
                    Buscamos una de las posibles ordenes que podemos darle a Zowi en el bestResult:
                        * Hacer el moonwalk hacia la izquierda
                        * Hacer el moonwalk hacia la derecha
                        * Hacer un swing
                        * Hacer un crusaito
                        * Saltar
                */
                if (bestResult.contains("para")) {
                    Log.e(LOGTAG, "He reconocido la orden de parar");
                    zowiHelper.stop(zowi);
                } else if (bestResult.contains("moon") || bestResult.contains("jackson")) {
                    Log.e(LOGTAG, "He reconocido la orden de jackson");
                    if (bestResult.contains("izquierda") && !bestResult.contains("derecha")) {
                        zowiHelper.moonWalker(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    } else if (bestResult.contains("derecha") && !bestResult.contains("izquierda")) {
                        zowiHelper.moonWalker(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    } else {
                        try {
                            speak("Zowi sólo puede hacer el moonwalk en una dirección", ID_PROMPT_INFO);
                        } catch (Exception e) { Log.e(LOGTAG, "TTS no accesible"); }
                    }
                } else if (bestResult.contains("swing")) {
                    Log.e(LOGTAG, "He reconocido la orden de swing");
                    zowiHelper.swing(zowi, Zowi.NORMAL_SPEED);
                } else if (bestResult.contains("crusaito")) {
                    if (bestResult.contains("izquierda") && !bestResult.contains("derecha")) {
                        zowiHelper.crusaito(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    } else if (bestResult.contains("derecha") && !bestResult.contains("izquierda")) {
                        zowiHelper.crusaito(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    } else {
                        try {
                            speak("Zowi sólo puede hacer el crusaito en una dirección", ID_PROMPT_INFO);
                        } catch (Exception e) { Log.e(LOGTAG, "TTS no accesible"); }
                    }
                } else if (bestResult.contains("salt")) {
                    Log.e(LOGTAG, "He reconocido la orden de saltar");
                    zowiHelper.jump(zowi, Zowi.NORMAL_SPEED);
                } else if (bestResult.contains("ayuda") || bestResult.contains("perdid")) {
                    Log.e(LOGTAG, "He reconocido la orden de ayuda");
                    try {
                        speak("Zowi puede hacer el moonwalk y el crusaito a la izquierda y a la derecha " +
                                "También puede saltar y hacer el swing. Cuando quieras que pare," +
                                "sólo tienes que decírselo.", ID_PROMPT_INFO);
                    } catch (Exception e) { Log.e(LOGTAG, "TTS no accessible"); }
                }
                Log.e(LOGTAG, bestResult);
                changeButtonAppearanceToDefault();
            }
        }
    }

    /**
     * Checks whether the device is connected to Internet (returns true) or not (returns false)
     * From: http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
     */
    public boolean deviceConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    /**
     * Shuts down the TTS engine when finished
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    /**
     * Invoked when the TTS has finished synthesizing.
     *
     * In this case, it starts recognizing if the message that has just been synthesized corresponds to a question (its id is ID_PROMPT_QUERY),
     * and does nothing otherwise.
     *
     * According to the documentation the speech recognizer must be invoked from the main thread. onTTSDone callback from TTS engine and thus
     * is not in the main thread. To solve the problem, we use Androids native function for forcing running code on the UI thread
     * (runOnUiThread).

     *
     * @param uttId identifier of the prompt that has just been synthesized (the id is indicated in the speak method when the text is sent
     * to the TTS engine)
     */

    @Override
    public void onTTSDone(String uttId) {
        if(uttId.equals(ID_PROMPT_QUERY.toString())) {
            runOnUiThread(new Runnable() {
                public void run() {
                    startListening();
                }
            });
        }
    }

    /**
     * Invoked when the TTS encounters an error.
     *
     * In this case it just writes in the log.
     */
    @Override
    public void onTTSError(String uttId) {
        Log.e(LOGTAG, "TTS error");
    }

    /**
     * Invoked when the TTS starts synthesizing
     *
     * In this case it just writes in the log.
     */
    @Override
    public void onTTSStart(String uttId) {
        Log.e(LOGTAG, "TTS empieza a hablar");
    }
}
