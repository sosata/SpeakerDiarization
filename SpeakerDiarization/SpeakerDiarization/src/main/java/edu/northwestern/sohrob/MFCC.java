package edu.northwestern.sohrob;

/**
 * Created by sohrob on 6/23/14.
 */
public class MFCC {

    private final int mel_Length_MAX = 41;
    private final int FILTER_SIZE = 500;
    private final double FLT_MIN = 1.0e-37;

    int mel_Length;
    int[] centers;
    double[] rNormalize;
    double[] iNormalize;
    double[] inputCopy;
    double[] outputCopy;
    double[] output;
    double[] Xr; double[] Xi;
    double[] window;
    double[] input;
    double[][] filters = new double[mel_Length_MAX][FILTER_SIZE];
    int[] filterStart = new int[FILTER_SIZE];
    int[] filterSize = new int[mel_Length_MAX];
    int inputLength, psLength;

    public MFCC(int sample_rate, int mell, int high_freq, int low_freq, int inputlen) {

        inputLength = inputlen; window = new double[inputLength];
        psLength = inputlen / 2;
        int i;
        double nyquist = sample_rate / 2.0f;
        double highMel = 1127.0 * Math.log(1+high_freq/700.0f);///Math.log(1 + 1000.0f / 700);
        double lowMel = 1127.0 * Math.log(1+low_freq/700.0f);///Math.log(1 + 1000.0f / 700);
        mel_Length = mell;
        centers = new int[mel_Length + 2];
        for (i=0; i < mel_Length + 2; i++) {
            double melCenter = lowMel + i*(highMel-lowMel)/(mel_Length+1);
            centers[i] = (int) (Math.floor(.5 + psLength*700*(Math.exp(melCenter*Math.log(1 + 1000.0 / 700.0)/1000)-1)/nyquist));
        }
        //initialize windows
        for (i = 0; i < inputLength; i++)
            window[i]=.54f-.46f*Math.cos((2 * Math.PI * i) / inputLength);

        // initialize filters
        assert(mel_Length < mel_Length_MAX);
        for (i=0;i<mel_Length;i++) {
            filterStart[i] = centers[i]+1;
            filterSize[i] = (centers[i+2]-centers[i]-1);
            int freq, j;
            for (freq=centers[i]+1, j=0 ; freq<=centers[i+1]; freq++, j++)
            {
                assert(j < FILTER_SIZE);
                filters[i][j] = (freq-centers[i])/ (double)(centers[i+1]-centers[i]);
            }
            for (freq=centers[i+1]+1 ; freq<centers[i+2] ; freq++, j++)
            {
                assert(j < FILTER_SIZE);
                filters[i][j] = (centers[i+2]-freq)/ (double)(centers[i+2]-centers[i+1]);
            }
        }

	    // test ////////////////
/*
        for (i=0; i<centers.length; i++)
            Log.e("Test", String.format("center[%d] = %d", i, centers[i]));
	    for (i=0;i<mel_Length;i++)
		    Log.e("Test", String.format("filterstart[%d] = %d", i, filterStart[i]));
        for (i=0;i<mel_Length;i++)
		    Log.e("Test",String.format("filtersize[%d] = %d",i,filterSize[i]));
*/

        inputCopy  = new double[mel_Length];
        outputCopy = new double[mel_Length];
        output = new double[mel_Length];
        rNormalize = new double[mel_Length];
        iNormalize = new double[mel_Length];
        Xr = new double[mel_Length];
        Xi = new double[mel_Length];
        input = new double [inputLength];
        double sqrt2n = Math.sqrt(2.0f/inputLength);
        for (i=0;i<mel_Length;i++)
        {
            rNormalize[i]=Math.cos(Math.PI*i/(2*mel_Length))*sqrt2n;
            iNormalize[i]=-Math.sin(Math.PI*i/(2*mel_Length))*sqrt2n;
        }
        rNormalize[0] /= Math.sqrt(2.0);


    }

    double[] calculateMfcc(double[] data)
    {

        int i,j;
        for (i = 0; i < inputLength; i ++)
            input[i] = data[i]; // * window[i];
        double[] tmpBuffer1 = new double[mel_Length];
        int nbFilters = mel_Length;
        for (i = 0 ; i < nbFilters ; i++)
        {
            tmpBuffer1[i]=0;
            int filterSizee = filterSize[i];
            int filtStart = filterStart[i];
            for (j=0;j<filterSizee;j++)
            {
                tmpBuffer1[i] += filters[i][j]*input[j+filtStart];
            }
        }


        for (i=0, j=0 ;i<mel_Length ; i+=2, j++){
            inputCopy[j]=Math.log(tmpBuffer1[i] + FLT_MIN);
        }

        for (i = mel_Length-1; i>=0 ; i-=2, j++){
            inputCopy[j]=Math.log(tmpBuffer1[i] + FLT_MIN);
        }
        double wr; double wi; double w; int n, m;
        for (n=0; n<mel_Length; n++) {
            Xr[n]=Xi[n]=0;
            for (m=0; m<mel_Length; m++) {
                w=2*Math.PI*m*n/mel_Length;
                if (true/*forward*/) w=-w;
                wr=Math.cos(w); wi=Math.sin(w);
                Xr[n]=Xr[n]+wr*inputCopy[m];
                Xi[n]=Xi[n]+wi*inputCopy[m];
            }
            Xr[n]=Xr[n]/Math.sqrt((double) mel_Length);
            Xi[n]=Xi[n]/Math.sqrt((double) mel_Length);
        }

        for (i = 0; i < mel_Length; i++)
            if (i <= mel_Length / 2)
                outputCopy[i] = Xr[i];
            else
                outputCopy[i] = Xi[i];

        for (i=1;i<mel_Length/2;i++) {
            output[i]=rNormalize[i]*outputCopy[i] - iNormalize[i]*outputCopy[mel_Length-i];
            output[mel_Length-i]=rNormalize[mel_Length-i]*outputCopy[i] + iNormalize[mel_Length-i]*outputCopy[mel_Length-i];
        }

        output[0]=outputCopy[0]*rNormalize[0];
        output[mel_Length/2] = outputCopy[mel_Length/2]*rNormalize[mel_Length/2];
        return output;
    }

    // delta is calculated along the rows (first dimension which denotes time)
    // all of the samples provided here from each MFCC coefficient are used to calculate a single delta value

    double[] calculate_delta(double[][] data) {
        double[] out = new double[data.length];
        for (int i=0; i<data.length; i++) {
            out[i] = 0;
            for (int j = 0; j < data[0].length/2; j++) {
                //Log.e("Test", String.format("j = %d",j));
                out[i] += (j + 1) * data[i][(int) Math.ceil((double) (data[0].length) / 2.0) + j] - (j + 1) * data[i][data[0].length / 2 - j - 1];
            }
        }
        return out;

    }

}
