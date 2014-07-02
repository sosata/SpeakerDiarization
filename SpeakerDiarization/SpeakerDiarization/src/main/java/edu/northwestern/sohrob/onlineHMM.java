package edu.northwestern.sohrob;

import android.util.Log;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Arrays;

/**
 * Created by sohrob on 7/1/14.
 * Online Continuous HMM
 */

public class onlineHMM {

    private int state;          //current state
    private double[] logPState;    //probability of each state
    private int nState;         //number of states
    private int dimObs;         //dimension of observation space
    private double[][] logT;       //transition matrix
    private double[][] m;       //means of Gaussians
    private double[][][] S;     //covariance matrices of Gaussians
    private double[][][] invS;  //inverse covariance matrices
    private double[] logDenom;     //denominator for calculating likelihoods
    private double[] Pi;        //prior

    public onlineHMM(int NumberOfStates, int ObservationDimension) {

        nState = NumberOfStates;
        dimObs = ObservationDimension;
        logPState = new double[nState];
        logT = new double[nState][nState];
        m = new double[nState][dimObs];
        S = new double[nState][dimObs][dimObs];
        invS = new double[nState][dimObs][dimObs];
        logDenom = new double[nState];
        Pi = new double[nState];

    }

    public void setParams(double[] prior, double[][] transition, double[][] mean, double[][][] covariance) {

        for (int i=0; i<nState; i++) {
            Pi[i] = prior[i];
            for (int j=0; j<nState; j++)
                logT[i][j] = Math.log(transition[i][j]);
            for (int j=0; j<dimObs; j++) {
                m[i][j] = mean[i][j];
                for (int k=0; k<dimObs; k++) {
                    S[i][j][k] = covariance[i][j][k];
                }
            }
        }

        //computing the inverse covariance matrices
        RealMatrix X;
        for (int i=0; i<nState; i++) {
            X = MatrixUtils.createRealMatrix(S[i]);
            RealMatrix invX = new LUDecomposition(X).getSolver().getInverse();
            invS[i] = invX.getData();
            double det = new LUDecomposition(X).getDeterminant();
            logDenom[i] = 0.5 * Math.log(Math.pow(2*Math.PI,dimObs)*det);
        }
        //Log.e("test", "invS = "+ Arrays.deepToString(invS));
        //Log.e("test", "logdenom = "+ Arrays.toString(logdenom));

        //setting the initial state
        state = generateState2i(Pi);
        for (int i=0; i<nState; i++)
            logPState[i] = Math.log(Pi[i]);

    }

    //calculate the log likelihood of the observation
    private double[] getLikelihood(double[] x) {

        double[] loglik  = new double[nState];
        for (int i=0; i<nState; i++) {
            double exponent = 0;
            for (int j=0; j<dimObs; j++)
                for (int k=0; k<dimObs; k++)
                    exponent += (x[j]-m[i][j])*invS[i][j][k]*(x[k]-m[i][k]);
            loglik[i] = -logDenom[i] -0.5*exponent;
        }
        return loglik;
    }

    //update the state using observation x
    public void updateState(double[] x) {

        double[] loglik = getLikelihood(x);
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

        Log.e("test", "voice: "+Arrays.toString(PState));

        state = generateState2i(PState);

    }

    //only for two states!!
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
