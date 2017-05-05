package sjtu.scuc.academic;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Created by zhai on 2016/8/8.
 */
public class Tools {
    static double normalization(double f, double fmin, double fmax) {
        return (f - fmin) / (fmax - fmin);
    }

    static double horisplice(double f1min, double f2max, double f1max, double f2min) {
//        (f1min,f2max,f1max,f2min)
//        return abs((f1max-f1min)/(f2max-f2min));
        return abs((f2max - f2min) / (f1max - f1min));
//        return abs((c-aa)/(b-d));
    }

    static double getObjValue(Calresult calresult, SCUCData scucData, int targetflag) {
        if (calresult.getTargetflag() != targetflag) {
//            System.out.println("get fmax");
        }
        double aConst = 0;
        double aLinear = 0;
        double aQuadratic = 0;
        int no_of_gen = scucData.getGenNum();
        int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        int[][] genStatus = calresult.getGenStatus();
        int[][] genY = calresult.getGenY();
        double[][] genOutput = calresult.getGenOutput();
        double result = 0;
        double p = 0;
        for (int i = 0; i < no_of_gen; i++) {
            GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
            if(targetflag==2){
                aConst = gen.getGasc();
                aLinear = gen.getGasb();
                aQuadratic = gen.getGasa();
            }
            else if(targetflag==1){
                aConst = gen.getAConstant();
                aLinear = gen.getALinear();
                aQuadratic = gen.getAQuadratic();
            }
            double startCost = gen.getStartupCost();
            if (targetflag == 1) {
//            gen cost
                for (int t = 0; t < no_of_ti; t++) {
                    p = genOutput[i][t];
//                    result=result+genStatus[i][t]*(aQuadratic*p*p+aLinear*p+aConst);
//                    result=result+(aQuadratic*p*p+aLinear*p+genStatus[i][t]*aConst)+startCost*genY[i][t];
                    result = result + genStatus[i][t] * (aQuadratic * p * p + aLinear * p + aConst) + startCost * genY[i][t];
                }
            }
            //            gas cost
            else if (targetflag==2){
                for (int t = 0; t < no_of_ti; t++) {
                    p=genOutput[i][t];
//                    result=result+(aQuadratic*p*p+aLinear*p+genStatus[i][t]*aConst);
                    result=result+genStatus[i][t]*(aQuadratic*p*p+aLinear*p+aConst);
                }
            }
        }
        return result;
    }


    public static double[][] getpoints(double f1min, double f2min, double f1max, double f2max, int k) {
        double[][] points = new double[k][2];
        double xstep = (f1max - f1min) / (k - 1);
        double ystep = (f2max - f2min) / (k - 1);
        for (int i = 0; i < k; i++) {
            points[i][0] = f1min + i * xstep;
            points[i][1] = f2max - i * ystep;
        }
        return points;
    }
}
