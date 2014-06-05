package edu.northwestern.sohrob;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.Date;
import java.io.IOException;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;

public class TestActivity extends Activity
{
    private static boolean _continueProcessing = true;
    private AudioRecord recorder;
    private short[] buffer;
    private int buffersize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        timerTextView = (TextView) findViewById(R.id.fullscreen_content);

        Button b = (Button) findViewById(R.id.bListen);
        b.setText("Start Listening");
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (b.getText().equals("Stop Listening")) {
                    timerHandler.removeCallbacks(timerRunnable);
                    b.setText("Start Listening");
                } else {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    b.setText("Stop Listening");
                }
            }
        });
*/

        buffersize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, buffersize);



    }

    @Override
    public void onPause() {
        super.onPause();
//        timerHandler.removeCallbacks(timerRunnable);
        Button b = (Button)findViewById(R.id.bListen);
        b.setText("Start Listening");

        TestActivity._continueProcessing = false;

        recorder.stop();
    }

    protected void onResume()
    {
        super.onResume();

        TestActivity._continueProcessing = true;

        recorder.startRecording();

/*         final Handler h = new Handler(Looper.getMainLooper());

        final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                h.postDelayed(this, 1000);

                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                timerTextView.setText(String.format("%d:%02d", minutes, seconds));
            }
        };

        h.postDelayed(timerRunnable, 1000);
*/
        final TestActivity me = this;
        final TextView timerTextView = (TextView) findViewById(R.id.fullscreen_content);



        Runnable r = new Runnable()
        {
            public void run()
            {
                while (TestActivity._continueProcessing)
                {

                    // get audio
/*
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
*/
                    buffer = new short[buffersize];
                    recorder.read(buffer, 0, buffersize);



                    //if (recorder.getState() != AudioRecord.STATE_INITIALIZED) recorder.release();


                    // process audio

                    long audioStart = System.currentTimeMillis();

                    double sum = 0.0;
                    for (int i=0; i<buffersize; i++) sum += Math.abs((double)buffer[i])/(double)buffersize;
                    final String txt = ((int)sum) + "";

                    long audioStop = System.currentTimeMillis();



                    me.runOnUiThread(new Runnable()
                    {
                        public void run() {
                            timerTextView.setText(txt);
                        }
                    });


                    // broadcast results

                    Log.e("TEST", "Process time (ms): " + (audioStop-audioStart));


                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

}
