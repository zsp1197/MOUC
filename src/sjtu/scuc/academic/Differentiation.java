package sjtu.scuc.academic;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/10 9:49.
 * E-mail: zsp1197@163.com
 */
public class Differentiation {
    SCUCData scucData;
    final double penalty = 99999999;
    final double gap=10;

    public Differentiation(SCUCData scucData) {
        this.scucData = scucData;
    }

    public double[] getMpriceTs(Calresult result) {
        int no_of_ti = scucData.getTiNum();
        double[] mprices = new double[no_of_ti];
        for (int t = 0; t < no_of_ti; t++) {
            mprices[t] = getMpriceT(result, t);
        }
        return mprices;
    }

    public double getMpriceT(Calresult calresult, int t) {
        List<Double> mprices = new ArrayList<Double>();
        int no_of_ti = scucData.getTiNum();
        int no_of_gen = scucData.getGenNum();
        for (int i = 0; i < no_of_gen; i++) {
                mprices.add(deviP(calresult, i, t, 1));
        }

        return Collections.min(mprices);
    }

    public double deviP(Calresult calresult, int i, int t, int order) {
        Generator[] gens = scucData.getGens();
        GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
        int[][] genStatus = calresult.getGenStatus();
        int[][] genY = calresult.getGenY();
        double[][] genOutput = calresult.getGenOutput();
        if (miss_Wrong(gen, genStatus, genOutput, i, t)) {
            return penalty;
        }
        DerivativeStructure pi_t = new DerivativeStructure(1, 3, 0, genOutput[i][t]);
//        get f1
        double constant1 = getConstant(deepcopy2Dint(genStatus), genY, genOutput, i, t, 1);
        constant1 = constant1 + gen.getAConstant() + genY[i][t] * gen.getStartupCost();
        DerivativeStructure f1_ = new DerivativeStructure(gen.getAQuadratic(), pi_t.multiply(pi_t), gen.getALinear(), pi_t);
        DerivativeStructure f1 = f1_.add(constant1);

//        get f2
        double constant2 = getConstant(deepcopy2Dint(genStatus), genY, genOutput, i, t, 2);
        constant2 = constant2 + gen.getGasc();
        DerivativeStructure f2_ = new DerivativeStructure(gen.getGasa(), pi_t.multiply(pi_t), gen.getGasb(), pi_t);
        DerivativeStructure f2 = f2_.add(constant2);

//        find f=f1^2+f2^2
        if (calresult.getTargetflag() != scucData.getTargetflag()) {
            throw new java.lang.Error("Targetflag must be the same");
        }
        DerivativeStructure f = null;
        if (calresult.getTargetflag() == 1) {
            f = f1;
        } else if (calresult.getTargetflag() == 2) {
            f = f2;
        } else if ((calresult.getTargetflag() == 3)||(calresult.getTargetflag() == 4)) {
            double result1 = scucData.getResult1().getBestObjValue();
            double result2 = scucData.getResult2().getBestObjValue();
            f = new DerivativeStructure(1 / (result1 * result1), f1.multiply(f1), 1 / (result2 * result2), f2.multiply(f2));
        } else {
            throw new java.lang.Error("Targetflag is not right!");
        }

        return f.getPartialDerivative(order);
    }

    public boolean miss_Wrong(GeneratorWithQuadraticCostCurve gen, int[][] genStatus, double[][] genOutput, int i, int t) {
        if (genStatus[i][t] == 0) {
            return true;
        }
        if (gen.getMaxP() - genOutput[i][t] < gap) {
            return true;
        }
        return false;
    }

    private double getConstant(int[][] fakeStatus, int[][] genY, double[][] genOutput, int i, int t, int targetflag) {
        fakeStatus[i][t] = 0;
        return Tools.getObjValue(fakeStatus, genY, genOutput, scucData, targetflag);
    }


    public static int[][] deepcopy2Dint(int[][] original) {
        if (original == null) {
            return null;
        }

        final int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
            // For Java versions prior to Java 6 use the next:
            // System.arraycopy(original[i], 0, result[i], 0, original[i].length);
        }
        return result;
    }
}
