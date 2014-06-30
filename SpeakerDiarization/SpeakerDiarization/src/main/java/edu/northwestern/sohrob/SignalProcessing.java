package edu.northwestern.sohrob;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Created by sohrob on 6/9/14.
 */

public class SignalProcessing {

    private static final int REL_SPEC_WINDOW = 1000;
    private int FRAME_LENGTH;
    private int FFT_LENGTH;
    private int HALF_FRAME_LENGTH;
    private final double epsilon = 0.000000001;

    int no_of_samples;
    int prevIndex;
    int indexx;

    private double[] hamming_window_coefs;
    private double[] norm_spec;
    private FastFourierTransformer fft;

    //spectral entropy variables
    private double[][] prev_sum_spec;
    private double[][] prev_spec;
    double[] unnormed_sum_spec;
    double[] unnormed_mean_spec;
    double[] normed_mean_spec;

    //these are (squared) power spectral values generated from a white gaussian noise with a total energy of 32768 (maximum for the microphone signal)
    //1024 samples:
    //private double[] noise_levels_squared = {8.0458,37.31,6.7785,6.305,20.634,2.5927,62.22,56.687,49.066,36.215,17.641,3.2606,39.864,2.5348,22.042,102.45,52.026,14.203,6.1719,34.022,9.9862,17.883,39.194,25.91,4.7518,25.08,12.747,0.88251,90.856,44.739,60.073,57.284,58.18,3.7939,36.174,5.4619,15.126,168.41,14.419,57.663,6.7696,93.203,16.579,28.314,39.007,17.307,20.848,123.18,0.41098,49.548,16.055,15.547,21.859,18.709,66.37,6.2229,16.524,20.419,24.488,1.2916,7.4758,31,28.15,35.687,9.4885,20.242,28.76,8.6446,13.876,35.254,7.8419,10.518,72.737,45.673,65.011,0.65869,19.778,55.933,23.39,44.119,54.987,8.3633,17.188,74.352,3.7652,70.382,28.455,7.2919,15.353,15.434,39.196,23.855,13.208,28.587,23.058,89.188,104.78,26.952,118.96,28.51,7.8405,147.67,46.582,4.7203,19.989,12.212,287.08,0.78157,89.337,6.0012,21.352,99.23,69.255,11.231,7.295,10.898,48.137,4.2696,7.2117,152.49,47.982,19.36,75.282,131.04,72.483,44.712,8.8636,19.426,31.414,7.1193,6.6818,47.791,74.527,73.194,6.1868,1.2721,85.176,16.352,21.121,30.485,0.31766,51.746,50.571,29.166,1.5477,30.399,13.147,43.214,32.991,67.592,5.9543,9.672,38.32,46.895,53.942,5.5441,30.843,69.444,9.1152,9.2727,6.3801,1.2922,2.5006,27.527,39.078,3.7621,37.328,5.08,53.681,5.3447,24.907,7.241,21.307,17.599,36.747,11.588,5.4869,28.431,20.492,9.9841,11.358,19.816,1.3823,19.988,31.539,2.1755,45.674,9.6541,49.079,11.506,50.898,33.218,3.8273,19.094,41.929,97.757,3.6235,2.4641,20.813,8.8354,75.995,21.42,62.043,5.0849,6.5142,28.103,35.374,27.937,57.789,37.244,24.547,4.3311,3.7909,31.002,8.2975,73.898,37.442,8.8171,32.997,181.62,33.314,17.142,11.5,6.2515,9.0272,4.5075,8.9928,13.291,80.069,10.022,4.0337,31.671,16.065,1.2224,12.697,63.06,116.02,35.465,19.294,4.9773,23.301,30.138,3.7944,5.5647,18.642,6.7299,93.166,15.261,2.6965,1.5652,137.35,57.828,28.299,7.4175,11.993,23.317,14.194,45.253,2.8396,7.34,31.461,41.7,88.804,29.831,74.249,8.9911,30.154,71.199,12.368,8.5545,2.9354,36.367,13.657,32.667,57.736,19.115,24.313,6.3565,88.319,7.1873,88.067,4.8771,2.3467,3.5129,179.5,50.441,7.4627,4.8446,1.7205,50.352,30.157,19.314,44.116,44.597,15.365,27.462,43.687,20.552,111.34,11.279,19.229,55.164,41.341,10.668,22.544,14.328,66.183,37.246,3.2129,10.619,31.583,17.022,79.919,1.7689,21.326,11.113,62.212,3.6555,7.5158,20.408,51.148,14.163,7.5538,0.1926,79.229,0.49898,79.886,15.378,20.234,26.313,30.415,7.5904,2.8904,17.952,24.931,13.987,6.0329,53.992,18.395,8.9712,0.47071,28.191,29.623,22.717,9.1415,78.979,2.2508,0.31346,11.352,16.182,191.48,14.551,14.157,56.522,40.464,53.486,31.872,26.884,98.378,7.825,15.448,26.011,27.061,4.2179,125,2.9728,25.695,2.7415,3.1798,46.781,70.441,80.16,26.736,7.3115,50.979,13.081,30.735,15.668,15.362,30.005,84.855,17.38,32.705,72.809,71.631,7.0261,26.761,11.051,133.41,63.26,18.37,30.002,33.064,15.197,21.372,33.55,109.68,34.218,23.192,5.4424,90.534,6.1044,78.253,5.8096,5.2161,131.13,4.3057,9.7128,29.139,35.367,10.174,35.909,5.393,76.575,107.5,11.667,21.589,1.9487,30.599,31.296,53.354,23.848,37.618,38.652,27.367,34.038,9.9779,66.272,20.815,92.811,64.344,5.8607,29.612,68.61,25.951,2.7395,1.9939,44.806,53.787,55.582,18.111,13.801,30.438,18.597,9.1768,2.2869,69.391,7.5741,14.584,0.13101,12.93,30.227,76.686,7.6271,37.38,32.225,27.373,37.468,4.6995,0.6345,2.5785,1.6884,5.3648,6.7649,9.1098,0.44771,35.944,92.261,54.51,25.087,6.9625,35.865,14.453,9.444,28.647,30.588,9.5377,12.02,4.5218,3.5187,8.3977,61.599,27.55,21.894,3.3086,14.323,18.771,0.57042,1.5516,128.17,17.261,99.562,36.171,29.339,11.701,17.29,94.102,70.349,11.229,50.135,19.302,15.226,21.669,12.759,77.705,2.8747,45.491,19.438,34.132,9.7465,0.23297,2.2984,5.3306,2.2984,0.23297,9.7465,34.132,19.438,45.491,2.8747,77.705,12.759,21.669,15.226,19.302,50.135,11.229,70.349,94.102,17.29,11.701,29.339,36.171,99.562,17.261,128.17,1.5516,0.57042,18.771,14.323,3.3086,21.894,27.55,61.599,8.3977,3.5187,4.5218,12.02,9.5377,30.588,28.647,9.444,14.453,35.865,6.9625,25.087,54.51,92.261,35.944,0.44771,9.1098,6.7649,5.3648,1.6884,2.5785,0.6345,4.6995,37.468,27.373,32.225,37.38,7.6271,76.686,30.227,12.93,0.13101,14.584,7.5741,69.391,2.2869,9.1768,18.597,30.438,13.801,18.111,55.582,53.787,44.806,1.9939,2.7395,25.951,68.61,29.612,5.8607,64.344,92.811,20.815,66.272,9.9779,34.038,27.367,38.652,37.618,23.848,53.354,31.296,30.599,1.9487,21.589,11.667,107.5,76.575,5.393,35.909,10.174,35.367,29.139,9.7128,4.3057,131.13,5.2161,5.8096,78.253,6.1044,90.534,5.4424,23.192,34.218,109.68,33.55,21.372,15.197,33.064,30.002,18.37,63.26,133.41,11.051,26.761,7.0261,71.631,72.809,32.705,17.38,84.855,30.005,15.362,15.668,30.735,13.081,50.979,7.3115,26.736,80.16,70.441,46.781,3.1798,2.7415,25.695,2.9728,125,4.2179,27.061,26.011,15.448,7.825,98.378,26.884,31.872,53.486,40.464,56.522,14.157,14.551,191.48,16.182,11.352,0.31346,2.2508,78.979,9.1415,22.717,29.623,28.191,0.47071,8.9712,18.395,53.992,6.0329,13.987,24.931,17.952,2.8904,7.5904,30.415,26.313,20.234,15.378,79.886,0.49898,79.229,0.1926,7.5538,14.163,51.148,20.408,7.5158,3.6555,62.212,11.113,21.326,1.7689,79.919,17.022,31.583,10.619,3.2129,37.246,66.183,14.328,22.544,10.668,41.341,55.164,19.229,11.279,111.34,20.552,43.687,27.462,15.365,44.597,44.116,19.314,30.157,50.352,1.7205,4.8446,7.4627,50.441,179.5,3.5129,2.3467,4.8771,88.067,7.1873,88.319,6.3565,24.313,19.115,57.736,32.667,13.657,36.367,2.9354,8.5545,12.368,71.199,30.154,8.9911,74.249,29.831,88.804,41.7,31.461,7.34,2.8396,45.253,14.194,23.317,11.993,7.4175,28.299,57.828,137.35,1.5652,2.6965,15.261,93.166,6.7299,18.642,5.5647,3.7944,30.138,23.301,4.9773,19.294,35.465,116.02,63.06,12.697,1.2224,16.065,31.671,4.0337,10.022,80.069,13.291,8.9928,4.5075,9.0272,6.2515,11.5,17.142,33.314,181.62,32.997,8.8171,37.442,73.898,8.2975,31.002,3.7909,4.3311,24.547,37.244,57.789,27.937,35.374,28.103,6.5142,5.0849,62.043,21.42,75.995,8.8354,20.813,2.4641,3.6235,97.757,41.929,19.094,3.8273,33.218,50.898,11.506,49.079,9.6541,45.674,2.1755,31.539,19.988,1.3823,19.816,11.358,9.9841,20.492,28.431,5.4869,11.588,36.747,17.599,21.307,7.241,24.907,5.3447,53.681,5.08,37.328,3.7621,39.078,27.527,2.5006,1.2922,6.3801,9.2727,9.1152,69.444,30.843,5.5441,53.942,46.895,38.32,9.672,5.9543,67.592,32.991,43.214,13.147,30.399,1.5477,29.166,50.571,51.746,0.31766,30.485,21.121,16.352,85.176,1.2721,6.1868,73.194,74.527,47.791,6.6818,7.1193,31.414,19.426,8.8636,44.712,72.483,131.04,75.282,19.36,47.982,152.49,7.2117,4.2696,48.137,10.898,7.295,11.231,69.255,99.23,21.352,6.0012,89.337,0.78157,287.08,12.212,19.989,4.7203,46.582,147.67,7.8405,28.51,118.96,26.952,104.78,89.188,23.058,28.587,13.208,23.855,39.196,15.434,15.353,7.2919,28.455,70.382,3.7652,74.352,17.188,8.3633,54.987,44.119,23.39,55.933,19.778,0.65869,65.011,45.673,72.737,10.518,7.8419,35.254,13.876,8.6446,28.76,20.242,9.4885,35.687,28.15,31,7.4758,1.2916,24.488,20.419,16.524,6.2229,66.37,18.709,21.859,15.547,16.055,49.548,0.41098,123.18,20.848,17.307,39.007,28.314,16.579,93.203,6.7696,57.663,14.419,168.41,15.126,5.4619,36.174,3.7939,58.18,57.284,60.073,44.739,90.856,0.88251,12.747,25.08,4.7518,25.91,39.194,17.883,9.9862,34.022,6.1719,14.203,52.026,102.45,22.042,2.5348,39.864,3.2606,17.641,36.215,49.066,56.687,62.22,2.5927,20.634,6.305,6.7785,37.31};
    //256 samples:
    private double[] noise_levels_squared = {4.3114,9.7273,2.9697,10.853,39.696,102.61,25.653,6.7852,5.8647,2.6363,6.2531,7.0634,67.697,19.73,51.707,29.976,7.7323,65.264,17.159,13.553,16.531,12.81,51.555,3.0661,25.24,25.304,55.479,29.491,3.1041,47.703,33.386,1.444,1.1995,22.029,28.382,49.389,8.0424,18.69,37.646,23.17,8.3329,2.3946,64.612,56.38,9.1553,188.59,5.805,16.439,24.284,93.543,34.753,11.231,2.9781,35.106,23.789,24.465,10.103,97.506,29.217,9.2598,34.287,21.379,89.978,3.0884,36.141,33.617,22.066,53.181,22.947,5.8044,12.407,16.608,17.048,119.9,23.434,11.163,26.703,15.544,0.85987,8.6048,125.49,4.9101,38.166,0.068303,29.124,46.202,50.089,28.551,17.438,111.1,55.576,38.506,14.352,97.103,5.4783,37.656,2.8822,16.696,7.949,29.654,30.139,25.851,12.688,105.37,70.009,62.609,35.482,13.096,36.994,52.06,16.238,1.8282,40.309,16.562,29.425,38.033,42.996,13.749,9.3077,31.914,80.624,9.7076,78.673,32.991,56.209,42.395,45.219,90.91,12.182,90.91,45.219,42.395,56.209,32.991,78.673,9.7076,80.624,31.914,9.3077,13.749,42.996,38.033,29.425,16.562,40.309,1.8282,16.238,52.06,36.994,13.096,35.482,62.609,70.009,105.37,12.688,25.851,30.139,29.654,7.949,16.696,2.8822,37.656,5.4783,97.103,14.352,38.506,55.576,111.1,17.438,28.551,50.089,46.202,29.124,0.068303,38.166,4.9101,125.49,8.6048,0.85987,15.544,26.703,11.163,23.434,119.9,17.048,16.608,12.407,5.8044,22.947,53.181,22.066,33.617,36.141,3.0884,89.978,21.379,34.287,9.2598,29.217,97.506,10.103,24.465,23.789,35.106,2.9781,11.231,34.753,93.543,24.284,16.439,5.805,188.59,9.1553,56.38,64.612,2.3946,8.3329,23.17,37.646,18.69,8.0424,49.389,28.382,22.029,1.1995,1.444,33.386,47.703,3.1041,29.491,55.479,25.304,25.24,3.0661,51.555,12.81,16.531,13.553,17.159,65.264,7.7323,29.976,51.707,19.73,67.697,7.0634,6.2531,2.6363,5.8647,6.7852,25.653,102.61,39.696,10.853,2.9697,9.7273};


    public SignalProcessing(int length) {

        Log.e("Test", "note: noise length = " + noise_levels_squared.length);

        FRAME_LENGTH = length;
        FFT_LENGTH = length/2+1;
        HALF_FRAME_LENGTH = length/2;

        //creating the hamming window coefficients for later use
        hamming_window_coefs = new double[FRAME_LENGTH];
        double denom = (double)FRAME_LENGTH-1;
        for (int i = 0; i < FRAME_LENGTH; i++)
            hamming_window_coefs[i] = 0.54 - (0.46 * Math.cos( 2.0 * Math.PI * ((double)i/denom)));

        //creating the FFt object for later use
        fft = new FastFourierTransformer(DftNormalization.STANDARD);

        //initializing parameters for calculating spectral entropy
        no_of_samples = 0;
        indexx = 0;
        norm_spec = new double[FFT_LENGTH];

        prev_sum_spec = new double[REL_SPEC_WINDOW][];
        for (int i=0; i<REL_SPEC_WINDOW; i++) {
            prev_sum_spec[i] = new double[FFT_LENGTH];
            for (int j = 0; j < FFT_LENGTH; j++)
                prev_sum_spec[i][j] = 0.0;
        }

        prev_spec = new double[REL_SPEC_WINDOW][];
        for (int i=0; i<REL_SPEC_WINDOW; i++) {
            prev_spec[i] = new double[FFT_LENGTH];
            for (int j = 0; j < FFT_LENGTH; j++)
                prev_spec[i][j] = 0.0;
        }

        unnormed_sum_spec = new double[FFT_LENGTH];
        for (int i=0; i<FFT_LENGTH; i++) {
            unnormed_sum_spec[i] = 0.0;
        }

        unnormed_mean_spec = new double[FFT_LENGTH];
        normed_mean_spec = new double[FFT_LENGTH];

    }

    public double[] window_hamming(double[] signal) {
        double[] result = new double[signal.length];
        for (int i=0; i<signal.length; i++)
            result[i] = signal[i] * hamming_window_coefs[i];
        return result;
    }

    public double[] zero_mean(double[] signal) {
        double mean = 0.0;
        for (int i=0; i<signal.length; i++)
            mean += signal[i];
        mean /= signal.length;
        for (int i=0; i<signal.length; i++)
            signal[i] -= mean;
        return signal;

    }

    public double[] spectral_magnitude(Complex[] fftvalues) {

        //only using FFT_LENGTH of fourier components here since the other half
        // is a mirror and serves no purpose for calculating the entropy
        double[] signal_out = new double[FFT_LENGTH];
        for (int i=0; i<FFT_LENGTH; i++)
            signal_out[i] = fftvalues[i].abs();
        return signal_out;

    }

    double[] spectral_power(Complex[] fftvalues)  {
        double[] out = new double[fftvalues.length];
        //double power_sum = 0.0;
        for(int j=0; j<fftvalues.length; j++){
            out[j] = fftvalues[j].abs()*fftvalues[j].abs();
            //power_sum += out[j];
        }
        //Log.e("Test", "total power: "+power_sum);
        return out;
    }

    // not in use for now
    double[] AddGaussianNoise(double[] power) {

        double[] out = new double[power.length];
        for(int j=0; j<power.length; j++){
            out[j] = power[j] + 0.02*noise_levels_squared[j];
        }
        return out;
    }

    public double spectral_entropy(double[] spec) {

        //sum spec samples for normalization later
        double sum_spec = 0;
        for (int i = 0; i<FFT_LENGTH; i++)
            sum_spec += spec[i];

        double spectral_entropy = 0;
        double divider_spec;

        if (no_of_samples <= REL_SPEC_WINDOW) {
            no_of_samples++;
            divider_spec = no_of_samples;
        } else // the value will fix at "REL_SPEC_WINDOW+1"
            divider_spec = REL_SPEC_WINDOW;

        if (indexx!=0)
            prevIndex = indexx-1;
        else
            prevIndex = REL_SPEC_WINDOW - 1; //go back to the last index in REL_SPEC_WINDOW

        //spectral entropy and its moving average
        for (int i = 0; i<FFT_LENGTH; i++){

            norm_spec[i] = spec[i]/(sum_spec + epsilon); //normalize to make a density function

            //no initialization because initially it will not be used
            //before (no_of_samples > REL_SPEC_WINDOW) is true
            prev_sum_spec[indexx][i] =  prev_sum_spec[prevIndex][i]  + spec[i] - prev_spec[indexx][i];
            prev_spec[indexx][i] = spec[i];

            unnormed_sum_spec[i] = prev_sum_spec[indexx][i];

            //spectral entropy
            if(norm_spec[i] != 0)
            {
                spectral_entropy = spectral_entropy - norm_spec[i]*Math.log(norm_spec[i]);
            }

        }

        //adding current results one index ahead so that it can be found in the next iteration
        indexx = (indexx+1)%REL_SPEC_WINDOW;

        //normalize mean spectral entropy
        sum_spec = 0;
        for(int i=0; i<FFT_LENGTH; i++){
            unnormed_mean_spec[i] = unnormed_sum_spec[i]/divider_spec;
            sum_spec += unnormed_mean_spec[i];
        }

        //relative spectral entropy
        double rel_spectral_entropy = 0;
        for (int i=0; i<FFT_LENGTH; i++){
            normed_mean_spec[i] = unnormed_mean_spec[i]/(sum_spec + epsilon);
            if(normed_mean_spec[i] < epsilon)
                normed_mean_spec[i] = epsilon;

            if (norm_spec[i] != 0)
                rel_spectral_entropy += norm_spec[i] * (Math.log(norm_spec[i])-Math.log(normed_mean_spec[i]));
        }

        return (float)rel_spectral_entropy;

    }

    double[] computeAutoCorrelationPeaks2(double[] power) {

        double[] power_noisy = AddGaussianNoise(power);

        Complex[] ac_complex = fft.transform(power_noisy, TransformType.INVERSE);

        //convert to real
        double[] ac = new double[ac_complex.length];
        for (int i=0; i<ac.length; i++) {
            ac[i] = ac_complex[i].getReal();
            /*if (ac_complex[i].getImaginary() != 0)
                Log.e("Test", "complex value found for AC! Real: " + ac[i] + " - Imag: " + ac_complex[i].getImaginary());*/
        }

        //normalizing the autocorrelation
        double[] ac_normalized = new double[HALF_FRAME_LENGTH];
        for(int i=0; i<HALF_FRAME_LENGTH; i++){
            ac_normalized[i] = ac[i] / ac[0];
        }

        //find peaks using autocorrelation values
        double[] out = findPeaks(ac_normalized);
        return out;

    }

    double[] findPeaks(double[] in) {

        double[] out = new double[2];

        double maxPeak = 0;
        //int maxPeakIdx = 0;

        //double nonInitialMax = 0;
        //int maxIdx = 0;
        //double gMin = 0;

        double lastVal;

        int pastFirstZeroCrossing = 0;

        int tn = 0;

        // start with (and thus skip) 0 lag
        lastVal = in[0];

        double localMaxPeakValue = 0;
        //int localMaxPeakIndex = 0;

        for(int i=1; i<in.length; i++) {


            //the underlying logic of finding peaks
            //First peak is ignored always, because it will always be one.
            //
            // First intuition is, auto correlation peaks will go down and come back up
            //
            //First if, we are in positive region (i.e., lastVal >= 0 && in[i] >=0) then we will update the peak values repeatedly. And we also update the max ac and peaks.
            //Second if, we are going out into a negative region, we will not definitely see any peak or leaving local peak or a positive region. In other words, we are leaving a peak. So, we update local peaklag and peakval
            //Finally the final if means, if we going into a positive region we have a chance to find a peak. So, we start comparing/finding again.
            //

            if (pastFirstZeroCrossing!=0){
                // are we in a peak?
                if(lastVal >= 0 && in[i] >=0){
                    // then check for new max

                    if(in[i] > localMaxPeakValue){


                        localMaxPeakValue = in[i];
                        //localMaxPeakIndex = i; //commented out temporarily


                        if(in[i] > maxPeak){
                            maxPeak = in[i];
                            //maxPeakIdx = i; //commented out temporarily
                        }
                    }

                    // did we just leave a peak?
                }else if(lastVal >=0 && in[i] < 0 && maxPeak > 0){

                    // count the last peak
                    //autoCorPeakVal[tn] = (jfloat)localMaxPeakValue; //commented out temporarily
                    //autoCorPeakLg[tn] = (jshort)localMaxPeakIndex;  //commented out temporarily
                    tn++;

                }else if(lastVal < 0 && in[i] >= 0){
                    //set the local ac max to zero
                    localMaxPeakValue = in[i];
                    //localMaxPeakIndex = i; //commented out temporarily

                    // then check for new max
                    if(in[i] > maxPeak){
                        maxPeak = in[i]; //it does only need non-initial maxpeak, so it not resetting the peak value every time
                        //maxPeakIdx = i; //commented out temporarily
                    }
                }
            }else{
                if(in[i] <= 0){
                    pastFirstZeroCrossing = 1; //zero crossing is for initial peak (value always one)
                }
            }

            lastVal = in[i];

        }

        out[0] = maxPeak;
        out[1] = tn;

        return out;
    }
}
