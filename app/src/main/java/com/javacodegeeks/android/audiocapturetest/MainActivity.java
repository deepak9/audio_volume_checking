package com.javacodegeeks.android.audiocapturetest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int sampleRate = 11025;
    private static final int bufferSizeFactor = 10;

    private AudioRecord audio;
    private int bufferSize;

    private ProgressBar level;

    private Handler handler = new Handler();

    private int maxVolume = 0;

    private int lastLevel = 0;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView txtSpeechInput;
    private TextView maxVolumeTv;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        level = (ProgressBar) findViewById(R.id.progressbar_level);

        level.setMax(32676);

        ToggleButton record = (ToggleButton) findViewById(R.id.togglebutton_record);
        Button asrTest = (Button) findViewById(R.id.asr_test);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        maxVolumeTv = (TextView) findViewById(R.id.maxVolumeText);

        asrTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub

                if (isChecked) {
                    bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT) * bufferSizeFactor;

                    audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                    audio.startRecording();

                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            readAudioBuffer();
                        }
                    });

                    thread.setPriority(Thread.currentThread().getThreadGroup().getMaxPriority());

                    thread.start();

                    handler.removeCallbacks(update);
                    handler.postDelayed(update, 25);

                } else if (audio != null) {
                    audio.stop();
                    audio.release();
                    audio = null;
                    handler.removeCallbacks(update);
                    maxVolumeTv.setText(Integer.toString(maxVolume));
                }

            }
        });
    }

    private void readAudioBuffer() {

        try {
            short[] buffer = new short[bufferSize];

            int bufferReadResult;

            do {

                bufferReadResult = audio.read(buffer, 0, bufferSize);

                for (int i = 0; i < bufferReadResult; i++){

                    if (buffer[i] > lastLevel) {
                        lastLevel = buffer[i];
                        maxVolume = buffer[i];
                    }

                }

            } while (bufferReadResult > 0 && audio.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);

            if (audio != null) {
                audio.release();
                audio = null;
                handler.removeCallbacks(update);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Runnable update = new Runnable() {

        public void run() {

            MainActivity.this.level.setProgress(lastLevel);

            lastLevel *= .5;

            handler.postAtTime(this, SystemClock.uptimeMillis() + 500);

        }

    };

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.e("results",result.toString());
                    txtSpeechInput.setText(result.get(0));
                }
                break;
            }

        }
    }
}