package edu.northwestern.sohrob;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;


public class TestActivity extends Activity
{
    private boolean doClassification = false;
    private boolean _extractFeatures = true;
    private boolean _doRecording = false;
    private boolean _recordAudio = false;
    private boolean _bufferChanged = false;


    private final int framesize = 256;              // sliding frame size to do signal processing
    private final int buffersize = 32*framesize;    // in samples - audio buffer size cannot be smaller than 640 bytes (320 samples of short)
    //private final int overlap = 128;              // overlap between sliding frames
    private final int fs = 8000;                    // sampling frequency

    private final int delta_depth = 5;
    private final int n_mfcc = 14;

    private Thread _audioReadingThread = null;

    private AudioRecord recorder;
    final private short[] buffer  = new short[buffersize];
    private short[] buffer_temp;

    private SignalProcessing SP;
    private FastFourierTransformer fft;
    private MFCC melfreq;
    private double[][] MFCC_values_acc;

    private onlineCHMM hmm_voice;
    private final double[] mean_unvoiced = {0.22, 24.0/*13.64*/, 0.17};
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

    private File csv_dir;

    final List<String[]> features = new ArrayList<String[]>();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up the audio recorder
        //*** NOTE: buffersize is in bytes!
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2*buffersize);
        buffer_temp = new short[buffersize];

        //AudioFormat.CHANNEL_IN_DEFAULT
        //set up signal processing modules
        SP = new SignalProcessing(framesize);
        fft = new FastFourierTransformer(DftNormalization.STANDARD);
        melfreq = new MFCC(fs, n_mfcc, 4000, 0, framesize);

        // initializing variables
        
        MFCC_values_acc = new double[n_mfcc][delta_depth];
        for (int i=0; i<n_mfcc; i++)
            for (int j=0; j<delta_depth; j++)
                MFCC_values_acc[i][j] = 0.0;

        // setting up the encog hmm:

        hmm_voice = new onlineCHMM(2,3);
        hmm_voice.setParams(prior, transition, mean_all, covariance_all);

        hmm_speech = new onlineDHMM(2,2);
        hmm_speech.setParams(prior_speech, transition_speech, emission);

        yinpitchdetector = new Yin(fs, framesize);

        // setting up the csv writer
        
        csv_dir = this.getBaseContext().getExternalFilesDir(null);

        if (csv_dir==null)
            Log.e("INFO","Cannot access the local CSV directory!" );

        if (csv_dir!=null && !csv_dir.exists()) {
            csv_dir.mkdirs();
            Log.i("INFO","CSV directory did not exist and was created." );
        }



        //int fs_test = 16000;
        //Log.i("INF","min buffer size for "+fs_test+" Hz : "+AudioRecord.getMinBufferSize(fs_test, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT));

    }

    @Override
    public void onPause() {

        super.onPause();

        this._extractFeatures = false;

        this._recordAudio = false;

        recorder.stop();
    }

    protected void onResume()
    {
        super.onResume();

        _extractFeatures = true;

        recorder.startRecording();

        final TestActivity me = this;
        final TextView features_view = (TextView) findViewById(R.id.features);
        final TextView output_view = (TextView) findViewById(R.id.output);

        _recordAudio = true;
        if (this._audioReadingThread == null || !this._audioReadingThread.isAlive())
        {
            // A dead thread cannot be restarted. A new thread has to be created.
            this._audioReadingThread = new Thread(this._audioReadingRunnable);
            this._audioReadingThread.start();
        }

        Runnable r = new Runnable()
        {
            public void run()
            {

                double[][] signal = new double[buffersize/framesize][framesize];

                while (_extractFeatures)
                {

                    //if (recorder.getState() != AudioRecord.STATE_INITIALIZED) recorder.release();
                    while (!_bufferChanged) {
                        try {
                            Thread.sleep(1);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    _bufferChanged = false;

                    synchronized (buffer) {
                        for (int k = 0; k < buffersize / framesize; k++)
                            for (int i = 0; i < framesize; i++)
                                signal[k][i] = ((double) buffer[i + k * buffersize / framesize]) / 32767.0;
                    }

                    // audio feature extraction

                    for (int k=0; k<buffersize/framesize; k++) {

                        //zero mean
                        double[] signal_zm = SP.zero_mean(signal[k]);

                        //hamming window
                        double[] signal_win = SP.window_hamming(signal_zm);

                        //calculating the fft
                        Complex[] fft_values = fft.transform(signal_win, TransformType.FORWARD);
                        //Complex[] fft_values = FFT.fft(signal_win_complex);

                        //find the spectral magnitude for entropy calculation
                        double[] spec_mag = SP.spectral_magnitude(fft_values);

                        //calculating the spectral entropy
                        double rel_spec_entropy = SP.spectral_entropy(spec_mag);

                        //calculating the power spectrum to calculate the AC
                        double[] power = SP.spectral_power(fft_values);

                        //total energy of the frame
                        double energy_total = 0.0;
                        for (int kk=0; kk<power.length; kk++)
                            energy_total += power[kk];

                        //find the AC peak number and max magnitude
                        double[] AC = SP.computeAutoCorrelationPeaks2(power);

                        //find the dominant frequency band
                        //double freq_max = SP.find_dominant_freq(signal, fs);

                        //calculating MFCC coefficients
                        double[] MFCC_values = melfreq.calculateMfcc(power);

                        //pushing new MFCC values into the MFCC matrix
                        for (int i = 0; i < n_mfcc; i++) {
                            for (int j = 0; j < delta_depth - 1; j++)
                                MFCC_values_acc[i][j] = MFCC_values_acc[i][j + 1];
                            MFCC_values_acc[i][delta_depth - 1] = MFCC_values[i];
                        }

                        //computing delta MFCC values
                        double[] deltaMFCC_values = melfreq.calculate_delta(MFCC_values_acc);

                        //computing the pitch (based on YIN algorithm)
                        double pitch = yinpitchdetector.getPitch(signal[k]).getPitch();

                        if (doClassification) {

                            //running the voice HMM (continuous type)
                            double[] obs = {AC[0], AC[1], rel_spec_entropy};
                            hmm_voice.updateState(obs);
                            int state_voice = hmm_voice.getState();
                            if ((pitch < 40) || (pitch > 500)) state_voice = 0;

                            //running the speech HMM (discrete type)
                            hmm_speech.updateState(state_voice);
                            int state_speech = hmm_speech.getState();


                            String voice = "";
                            if (state_voice == 1)
                                voice = "VOICE";

                            String speech = "";
                            if (state_speech == 1)
                                speech = "SPEECH";

                            final String out = voice + "\n" + speech;

                            /*if ((state[0]==0)&&(state2[0]==0))
                                cls = "";
                            else if ((state[0]==1)&&(state2[0]==1))
                                cls = "VOICE SPEECH";
                            else if ((state[0]==0)&&(state2[0]==1))
                                cls = "SPEECH";
                            else
                                cls = "VOICE";*/

                            /*if ((AC[0] > 0.3) && (AC[1] > 0) && (AC[1] <= 9) && (rel_spec_entropy > 0.2))
                                cls = "VOICE";
                            else
                                cls = "";*/

                            final String txt = String.format("PMAX: %.2f\nNP: %.0f\nRSE: %.2f\nPitch: %.2f", AC[0], AC[1], rel_spec_entropy, pitch);

                            //final String txt = txt_temp;
                            me.runOnUiThread(new Runnable() {
                                public void run() {
                                    features_view.setText(txt);
                                    output_view.setText(out);
                                }
                            });

                        }

                        // add new feature values to the features matrix

                        if (me._doRecording) {

                            String[] feature_vector = new String[5+n_mfcc*2];

                            feature_vector[0] = Double.toString(AC[0]);
                            feature_vector[1] = Double.toString(AC[1]);
                            feature_vector[2] = Double.toString(rel_spec_entropy);
                            feature_vector[3] = Double.toString(pitch);
                            feature_vector[4] = Double.toString(energy_total);

                            for (int i=0; i<n_mfcc; i++) {
                                feature_vector[i+5] = Double.toString(MFCC_values[i]);
                                feature_vector[i+5+n_mfcc] = Double.toString(deltaMFCC_values[i]);
                            }

                            synchronized (me.features) {
                                me.features.add(feature_vector);
                            }

                        }


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

    public void onStartRecording(View view) {

        final ImageButton bRecord = (ImageButton) view;
        bRecord.setBackgroundColor(0xFF00FFFF);

        String phonenumber = PhoneNumberUtils.formatNumber("13124513080");
        Log.i("Info", "Phone number: "+phonenumber);

        this._doRecording = true;

    }

    public void onStopRecording(View view) throws InterruptedException {

        String _filename = "features.csv";

        if (!this._doRecording) {
            return;
        }

        this._doRecording = false;

        final ImageButton bRecord = (ImageButton) findViewById(R.id.record);
        bRecord.setBackgroundColor(0x0000FFFF);

        File csvFile;
        csvFile = new File(csv_dir, _filename);

        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(csvFile), ' ');
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (writer==null) {
            Log.e("INFO","Cannot write to file" + csvFile.getAbsolutePath());
        } else {
            synchronized (this.features) {
                writer.writeAll(this.features, false);
                this.features.clear();
            }
            try {
                writer.close();
                Log.i("INFO","CSV file containing feature values written successfully." );
                Log.i("INFO", csvFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    public void onClearRecords(View view) {

        //final ImageButton bClear = (ImageButton) view;

/*
        synchronized (this.features) {
            this.features.clear();
        }
*/
    }

    private Runnable _audioReadingRunnable = new Runnable() {

        @Override
        public void run() {

            while (_recordAudio) {

                //Log.e("INFO", "Audio reading started!");

                long T0 = System.currentTimeMillis();

                recorder.read(buffer_temp, 0, buffersize);

                synchronized (buffer) {
                    System.arraycopy(buffer_temp, 0, buffer, 0, buffersize);
                }

                _bufferChanged = true;

                long deltaT = System.currentTimeMillis() - T0;
                long sleepTime = (long)(buffersize*1000/(double)fs) - deltaT;
                if (sleepTime<0) sleepTime = 0;

                try {
                    Thread.sleep(sleepTime);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    _recordAudio = false;
                }


            }

        }
    };

}
