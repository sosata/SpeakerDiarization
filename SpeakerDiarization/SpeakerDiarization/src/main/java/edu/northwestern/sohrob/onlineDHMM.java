package edu.northwestern.sohrob;

/**
 * Created by Sohrob on 7/2/14.
 * Discrete Hidden Markov Model
 */

public class onlineDHMM {

    private int state;          //current state
    private double[] logPState;    //probability of each state
    private int nState;         //number of states
    private int dimObs;         //dimension of observation space
    private double[][] logT;       //transition matrix
    private double[][] logE;    //emission matrix
    private double[] Pi;        //prior

    public onlineDHMM(int NumberOfStates, int ObservationDimension) {

        nState = NumberOfStates;
        dimObs = ObservationDimension;
        logPState = new double[nState];
        logT = new double[nState][nState];
        logE = new double[nState][dimObs];
        Pi = new double[nState];

    }

    //setting the HMM parameters
    public void setParams(double[] prior, double[][] transition, double[][] emission) {

        for (int i=0; i<nState; i++) {
            Pi[i] = prior[i];
            for (int j=0; j<nState; j++)
                logT[i][j] = Math.log(transition[i][j]);
            for (int j=0; j<dimObs; j++)
                logE[i][j] = Math.log(emission[i][j]);
        }

        //setting the initial state
        state = generateState2i(Pi);
        for (int i=0; i<nState; i++)
            logPState[i] = Math.log(Pi[i]);

    }

    //calculate the log likelihood of the observation 'x' for each state
    private double[] getLogLikelihood(int x) {

        double[] loglik  = new double[nState];
        for (int i=0; i<nState; i++) {
            loglik[i] = logE[i][x];
        }
        return loglik;
    }

    //update the state using observation 'x'
    public void updateState(int x) {

        double[] loglik = getLogLikelihood(x);
        double[] PState = new double[nState];
        double[] max = new double[2];
        double temp;
        for (int i=0; i<nState; i++) {
            max[i] = logPState[0] + logT[0][i];
            for (int j = 1; j < nState; j++) {
                temp = logPState[j] + logT[j][i];
                if (temp>max[i])
                    max[i] = temp;
            }
        }

        double sum_PState = 0;
        for (int i=0; i<nState; i++) {
            logPState[i] = max[i] + loglik[i];
            PState[i] = Math.exp(logPState[i]);
            sum_PState += PState[i];
        }

        for (int i=0; i<nState; i++) {
            PState[i] /= sum_PState;
            logPState[i] = Math.log(PState[i]);
        }

        //Log.e("test", "speech: "+Arrays.toString(PState));

        state = generateState2i(PState);

    }

    //only for two states (binomial)
    private int generateState2i(double[] pState) {

        if (Math.random()<pState[0])
            return 0;
        else
            return 1;

    }

    public int getState() {
        return state;
    }

}
