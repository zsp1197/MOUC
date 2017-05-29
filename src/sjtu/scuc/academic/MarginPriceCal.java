package sjtu.scuc.academic;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/24 14:33.
 * E-mail: zsp1197@163.com
 */
public class MarginPriceCal {
    final List<SCUCData> systems;
    final Calresult[] results;
    final Parameters parameters;
    final double gap = 10;

    public MarginPriceCal(List<SCUCData> systems, Calresult[] results, Parameters parameters) {
        this.systems = systems;
        this.results = results;
        this.parameters = parameters;
    }

    public double[] getMpriceTs(int sysIndex) {
        final int no_of_ti = systems.get(0).getTiNum();
        double[] mprices = new double[no_of_ti];
        for (int t = 0; t < no_of_ti; t++) {
            mprices[t] = getMpriceT(sysIndex, t);
        }
        return mprices;
    }

    private double getMpriceT(int sysIndex, int t) {
        List<Double> mprices = new ArrayList<Double>();
        SCUCData scucData = systems.get(sysIndex);
        final int no_of_ti = scucData.getTiNum();
        final int no_of_gen = scucData.getGenNum();
        for (int i = 0; i < no_of_gen; i++) {
            mprices.add(deviP(sysIndex, i, t, 1));
        }
        return Collections.min(mprices);
    }

    private double deviP(int sysIndex, int genIndex, int t, int order) {
        final int no_of_sys = this.systems.size();
        final int no_of_ti = systems.get(0).getTiNum();
        GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) systems.get(sysIndex).getGens()[genIndex];
        int[][] genStatus = this.results[sysIndex].getGenStatus();
        int[][] genY = this.results[sysIndex].getGenY();
        double[][] genOutput = this.results[sysIndex].getGenOutput();
        if (miss_Wrong(gen, genStatus, genOutput, genIndex, t)) {
            return this.parameters.getPenalty();
        }
        DerivativeStructure pi_t = new DerivativeStructure(1, 3, 0, genOutput[genIndex][t]);
//        get f1
        double constant1 = getConstant(sysIndex, genIndex, t, 1);
        constant1 = constant1 + gen.getAConstant() + genY[genIndex][t] * gen.getStartupCost();
        DerivativeStructure f1_ = new DerivativeStructure(gen.getAQuadratic(), pi_t.multiply(pi_t), gen.getALinear(), pi_t);
        DerivativeStructure f1 = f1_.add(constant1);

//        get f2
        double constant2 = getConstant(sysIndex, genIndex, t, 2);
        constant2 = constant2 + gen.getGasc();
        DerivativeStructure f2_ = new DerivativeStructure(gen.getGasa(), pi_t.multiply(pi_t), gen.getGasb(), pi_t);
        DerivativeStructure f2 = f2_.add(constant2);

//        find f=f1^2+f2^2
        if (results[0].getTargetflag() != systems.get(0).getTargetflag()) {
            throw new java.lang.Error("Targetflag must be the same");
        }
        DerivativeStructure f = null;
        if (results[0].getTargetflag() == 1) {
            f = f1;
        } else if (results[0].getTargetflag() == 2) {
            f = f2;
        } else if ((results[0].getTargetflag() == 3) || (results[0].getTargetflag() == 4)) {
            f = new DerivativeStructure(systems.get(0).getNormalize_coefficentes()[0], f1.multiply(f1), systems.get(0).getNormalize_coefficentes()[1], f2.multiply(f2));
        } else {
            throw new java.lang.Error("Targetflag is not right!");
        }

        return f.getPartialDerivative(order);
    }

    private boolean miss_Wrong(GeneratorWithQuadraticCostCurve gen, int[][] genStatus, double[][] genOutput, int genIndex, int t) {
        if (genStatus[genIndex][t] == 0) {
            return true;
        }
        if (gen.getMaxP() - genOutput[genIndex][t] < gap) {
            return true;
        }
        return false;
    }

    private double getConstant(int sysIndex, int genIndex, int t, int targetflag) {
        final int no_of_sys = this.systems.size();
        final int no_of_ti = systems.get(0).getTiNum();
        double result = 0;
        for (int si = 0; si < no_of_sys; si++) {
            SCUCData scucData = systems.get(si);
            Calresult temp_result = results[si];
            if (si == sysIndex) {
                int[][] fake_genStatus = Tools.deepcopy2D_IntArray(temp_result.getGenStatus());
                int[][] genY = temp_result.getGenY();
                double[][] genOutput = temp_result.getGenOutput();
                fake_genStatus[genIndex][t] = 0;
                result = result + Tools.getObjValue(fake_genStatus, genY, genOutput, scucData, targetflag);
            } else {
                result = result + Tools.getObjValue(temp_result, scucData, targetflag);
            }
        }
        return result;
    }
}
