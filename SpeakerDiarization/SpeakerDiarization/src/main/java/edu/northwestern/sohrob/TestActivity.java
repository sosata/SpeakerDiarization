package edu.northwestern.sohrob;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
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
    private final int framesize = 256;
    private final int buffersize = 4*framesize;
    private final int fs = 8000;
    private SignalProcessing SP;
    private FastFourierTransformer fft;
    private MFCC melfreq;
    private double[][] MFCC_values_acc;

    private final int delta_depth = 5;
    private final int n_mfcc = 14;

    private onlineCHMM hmm_voice;
    private final double[] mean_unvoiced = {0.22, 13.64, 0.17};
    private final double[] mean_voiced = {0.51, 6.69, 0.31};
    private final double[][] mean_all = {mean_unvoiced, mean_voiced};
    private final double[][] covariance_unvoiced = {{0.0200, -0.0357, 0.0064}, {-0.0357, 164.2143, 0.0494}, {0.0064, 0.0494, 0.0191}};
    private final double[][] covariance_voiced = {{0.0353, -0.0449, 0.0181}, {-0.0449, 27.6196, 0.2227}, {0.0181, 0.2227, 0.0270}};
    private final double[][][] covariance_all = {covariance_unvoiced, covariance_voiced};
    private final double[][] transition = {{0.999, 0.001}, {0.001, 0.999}};
    private final double[] prior = {0.5,0.5};

    private onlineDHMM hmm_speech;
    private final double[][] transition_speech = {{0.99999, 0.00001}, {0.00001, 0.99999}};
    private final double[] prior_speech = {0.5,0.5};
    private final double[][] emission = {{1, 0},{0.25,0.75}};


    Yin yinpitchdetector;



    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up the audio recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, fs, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, buffersize);

        //set up signal processing modules
        SP = new SignalProcessing(framesize);
        fft = new FastFourierTransformer(DftNormalization.STANDARD);
        melfreq = new MFCC(fs, n_mfcc, 4000, 0, framesize);

        //initializing variables
        MFCC_values_acc = new double[n_mfcc][delta_depth];
        for (int i=0; i<n_mfcc; i++)
            for (int j=0; j<delta_depth; j++)
                MFCC_values_acc[i][j] = 0.0;

        //setting up the encog HMM:

        hmm_voice = new onlineCHMM(2,3);
        hmm_voice.setParams(prior, transition, mean_all, covariance_all);

        hmm_speech = new onlineDHMM(2,2);
        hmm_speech.setParams(prior_speech, transition_speech, emission);

        yinpitchdetector = new Yin(fs, framesize);

    }

    @Override
    public void onPause() {

        super.onPause();

        //Button b = (Button)findViewById(R.id.bListen);
        //b.setText("Start Listening");

        TestActivity._continueProcessing = false;

        recorder.stop();
    }

    protected void onResume()
    {
        super.onResume();

        TestActivity._continueProcessing = true;

        recorder.startRecording();

        final TestActivity me = this;
        final TextView features_view = (TextView) findViewById(R.id.features);
        final TextView output_view = (TextView) findViewById(R.id.output);


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

                    for (int k=0; k<buffersize/framesize; k++) {

                        double signal[] = new double[framesize];
                        for (int i = 0; i < framesize; i++)
                            signal[i] = ((double) buffer[i+k*buffersize/framesize]) / 32767.0;

                        //zero mean
                        signal = SP.zero_mean(signal);

                        //hamming window
                        double[] signal_win = SP.window_hamming(signal);

                        //find the fft
                        Complex[] fft_values = fft.transform(signal_win, TransformType.FORWARD);

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

                        //calculate the MFCC coefficients
                        double[] MFCC_values = melfreq.calculateMfcc(power);

                        //push MFCC values into the accumulating matrix
                        for (int i = 0; i < n_mfcc; i++) {
                            MFCC_values_acc[i][delta_depth - 1] = MFCC_values[i];
                            for (int j = 0; j < delta_depth - 1; j++)
                                MFCC_values_acc[i][j] = MFCC_values_acc[i][j + 1];
                        }


                        //computing the pitch (YIN algorithm)
                        double pitch = yinpitchdetector.getPitch(signal).getPitch();

                        //running the encog HMMs
                        /*
                        double[][] seq = {{AC[0],AC[1],rel_spec_entropy}};
                        double[][] dummy = {{0,0,0}};
                        obs = new BasicMLDataSet(seq, dummy);
                        //vb = new ViterbiCalculator(obs, hmm);
                        //int[] state = vb.stateSequence();
                        int[] state = hmm.getStatesForSequence(obs);
                        //hmm.updateProperties();
                        //hmm.setPi(0, 1-state[0]);
                        //hmm.setPi(1, state[0]);

                        //running the second encog HMM
                        double[][] seq2 = {{state[0]}};
                        double[][] dummy2 = {{0}};
                        obs2 = new BasicMLDataSet(seq2, dummy2);
                        //vb2 = new ViterbiCalculator(obs2, hmm2);
                        //int[] state2 = vb2.stateSequence();
                        int[] state2 = hmm2.getStatesForSequence(obs2);
                        //hmm2.updateProperties();
*/


                        //running my voice HMM
                        double[] obs = {AC[0],AC[1],rel_spec_entropy};
                        hmm_voice.updateState(obs);
                        int state_voice = hmm_voice.getState();
                        if ((pitch<40)||(pitch>500)) state_voice = 0;

                        //running my speech HMM
                        hmm_speech.updateState(state_voice);
                        int state_speech = hmm_speech.getState();

                        //compute delta MFCC values
                        double[] deltaMFCC_values = melfreq.calculate_delta(MFCC_values_acc);

                        String voice = "";
                        if (state_voice==1)
                            voice = "VOICE";

                        String speech = "";
                        if (state_speech==1)
                            speech = "SPEECH";

                        final String out = voice+"\n"+speech;

/*                        if ((state[0]==0)&&(state2[0]==0))
                            cls = "";
                        else if ((state[0]==1)&&(state2[0]==1))
                            cls = "VOICE SPEECH";
                        else if ((state[0]==0)&&(state2[0]==1))
                            cls = "SPEECH";
                        else
                            cls = "VOICE";
*/
/*
                        if ((AC[0] > 0.3) && (AC[1] > 0) && (AC[1] <= 9) && (rel_spec_entropy > 0.2))
                            cls = "VOICE";
                        else
                            cls = "";
*/
                        String txt_temp = String.format("PMAX: %.2f\nNP: %.0f\nRSE: %.2f\nPitch: %.2f", AC[0], AC[1], rel_spec_entropy, pitch);
                        for (int i = 1; i < MFCC_values.length; i++)
                            txt_temp += String.format("\nMFCC%d: %.2f - %.2f", i + 1, MFCC_values[i], deltaMFCC_values[i]);

                        //long audioStop = System.currentTimeMillis();

                        final String txt = txt_temp;
                        me.runOnUiThread(new Runnable() {
                            public void run() {
                                features_view.setText(txt);
                                output_view.setText(out);
                            }
                        });

                    }


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
