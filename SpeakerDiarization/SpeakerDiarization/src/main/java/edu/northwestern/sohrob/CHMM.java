package edu.northwestern.sohrob;

import org.encog.ml.data.MLSequenceSet;
import org.encog.ml.hmm.HiddenMarkovModel;
import org.encog.ml.hmm.alog.KullbackLeiblerDistanceCalculator;
import org.encog.ml.hmm.alog.MarkovGenerator;
import org.encog.ml.hmm.distributions.ContinousDistribution;
import org.encog.ml.hmm.train.bw.TrainBaumWelch;

/**
 * Created by sohrob on 6/30/14.
 */
public class CHMM {

    static HiddenMarkovModel buildContHMM()
    {
        double [] mean1 = {0.25, -0.25};
        double [][] covariance1 = { {1, 2}, {1, 4} };

        double [] mean2 = {0.5, 0.25};
        double [][] covariance2 = { {4, 2}, {3, 4} };

        HiddenMarkovModel hmm = new HiddenMarkovModel(2);

        hmm.setPi(0, 0.8);
        hmm.setPi(1, 0.2);

        hmm.setStateDistribution(0, new ContinousDistribution(mean1,covariance1));
        hmm.setStateDistribution(1, new ContinousDistribution(mean2,covariance2));

        hmm.setTransitionProbability(0, 1, 0.05);
        hmm.setTransitionProbability(0, 0, 0.95);
        hmm.setTransitionProbability(1, 0, 0.10);
        hmm.setTransitionProbability(1, 1, 0.90);

        return hmm;
    }

    static HiddenMarkovModel buildContInitHMM()
    {
        double [] mean1 = {0.20, -0.20};
        double [][] covariance1 = { {1.3, 2.2}, {1.3, 4.3} };

        double [] mean2 = {0.5, 0.25};
        double [][] covariance2 = { {4.1, 2.1}, {3.2, 4.4} };

        HiddenMarkovModel hmm = new HiddenMarkovModel(2);

        hmm.setPi(0, 0.9);
        hmm.setPi(1, 0.1);

        hmm.setStateDistribution(0, new ContinousDistribution(mean1,covariance1));
        hmm.setStateDistribution(1, new ContinousDistribution(mean2,covariance2));

        hmm.setTransitionProbability(0, 1, 0.10);
        hmm.setTransitionProbability(0, 0, 0.90);
        hmm.setTransitionProbability(1, 0, 0.15);
        hmm.setTransitionProbability(1, 1, 0.85);

        return hmm;
    }

    public static void main(String[] args) {

        HiddenMarkovModel hmm = buildContHMM();
        HiddenMarkovModel learntHmm = buildContInitHMM();

        MarkovGenerator mg = new MarkovGenerator(hmm);
        MLSequenceSet training = mg.generateSequences(200,100);

        TrainBaumWelch bwl = new TrainBaumWelch(learntHmm,training);

        KullbackLeiblerDistanceCalculator klc =
                new KullbackLeiblerDistanceCalculator();

        System.out.println("Training Continuous Hidden Markov Model with Baum Welch");

        for(int i=1;i<=10;i++) {
            double e = klc.distance(learntHmm, hmm);
            System.out.println("Iteration #"+i+": Difference: " + e);
            bwl.iteration();
            learntHmm = (HiddenMarkovModel)bwl.getMethod();
        }
    }
}
