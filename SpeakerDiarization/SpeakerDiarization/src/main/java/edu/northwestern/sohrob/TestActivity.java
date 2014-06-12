package edu.northwestern.sohrob;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;


public class TestActivity extends Activity
{
    private static boolean _continueProcessing = true;
    private AudioRecord recorder;
    private short[] buffer;
    private int buffersize = 1024;
    private int fs = 8000;
    private SignalProcessing SP;
    private FastFourierTransformer fft;



    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //buffersize = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, fs, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, buffersize);

        SP = new SignalProcessing(buffersize);
        fft = new FastFourierTransformer(DftNormalization.STANDARD);

    }

    @Override
    public void onPause() {

        super.onPause();

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

                    //long audioStart = System.currentTimeMillis();

                    double signal[] = new double[buffersize];
                    for (int i=0; i<buffersize; i++) signal[i] = ((double)buffer[i])/32767.0;

                    //zero mean
                    signal = SP.zero_mean(signal);

                    //hamming window
                    signal = SP.window_hamming(signal);

                    //find the fft
                    Complex[] fft_values = fft.transform(signal, TransformType.FORWARD);

                    //find the spectral magnitude for entropy calculation
                    double[] spec_mag = SP.spectral_magnitude(fft_values);

                    //find the spectral entropy
                    double rel_spec_entropy = SP.spectral_entropy(spec_mag);

                    //find the power spectrum to calculate the AC
                    double[] power = SP.spectral_power(fft_values);

                    //find the AC peak number and max magnitude
                    double[] AC = SP.computeAutoCorrelationPeaks2(power);

                    //find dominant frequency
                    //double freq_max = SP.find_dominant_freq(signal, fs);

                    final String txt = String.format("PMAX: %.2f\nNP: %.0f\nRSE: %.2f", AC[0], AC[1], rel_spec_entropy);
                    //long audioStop = System.currentTimeMillis();

                    me.runOnUiThread(new Runnable()
                    {
                        public void run() {
                            timerTextView.setText(txt);
                        }
                    });


                    // broadcast results

                    //Log.e("TEST", "Process time (ms): " + (audioStop-audioStart));
                    //Log.e("TEST", "FFT length: " + fftvalues.length);


                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

}
