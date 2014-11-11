package com.javacodegeeks.android.audiocapturetest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private static final int sampleRate = 11025;
    private static final int bufferSizeFactor = 10;

    private AudioRecord audio;
    private int bufferSize;

    private ProgressBar level;

    private Handler handler = new Handler();

    private int maxVolume = 0;

    private int lastLevel = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        level = (ProgressBar) findViewById(R.id.progressbar_level);

        level.setMax(32676);

        ToggleButton record = (ToggleButton) findViewById(R.id.togglebutton_record);

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
                    Log.e("Max audio",Integer.toString(maxVolume));
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
}