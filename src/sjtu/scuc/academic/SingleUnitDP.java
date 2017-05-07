package sjtu.scuc.academic;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class SingleUnitDP {

    // TODO: add respectStartupShutdownOutput ?

    protected static Log log = LogFactory.getLog(SingleUnitDP.class);
    private final int penaltyCost = 10000;

    private double[] genOut = null;
    private int[] genStatus = null;

    public void solve(final Generator[] gens, final int gen_indx, final double[] lambda, double[] mu, final Map<Integer, List<LeqConstraint>> ti_constraints_map) {

        final Generator gen = gens[gen_indx];


        final int no_of_ti = lambda.length;
//        min_on_time + min_dn_time
        final int no_of_status = gen.getNoOfStatus();

        int[][] preStatusIndex = new int[no_of_ti][no_of_status];
        double[][] out = new double[no_of_ti][no_of_status];
        double[][] objVal = new double[no_of_ti][no_of_status];
        for (int t = 0; t < no_of_ti; t++)
            for (int i = 0; i < no_of_status; i++)
                objVal[t][i] = penaltyCost;

        int preStaIndex = gen.getInitialStatusIndex();
        final double maxP = gen.getMaxP();
        for (int statusIndex = 0; statusIndex < no_of_status; statusIndex++) {
            preStatusIndex[0][statusIndex] = preStaIndex;

            if (!gen.isReachable(preStaIndex, statusIndex)) {
                out[0][statusIndex] = 0;
                objVal[0][statusIndex] = penaltyCost;
                continue;
            }

            double startupCost = gen.getStartupCost(preStaIndex, statusIndex);

            double temp_lambda = lambda[0];
            List<LeqConstraint> cons = ti_constraints_map.get(0);
            if (cons != null) {
                for (LeqConstraint con : cons) {
                    if (gen.isON(statusIndex)) {
                        temp_lambda -= con.getMu() * con.getA(gen_indx);
                    }
                }
            }
            double output = gen.getOutput(statusIndex, temp_lambda);


            double genCost = gen.getGenCost(output);
            double transferCost = gen.getTransferCost(preStaIndex, statusIndex);

            // cost with totalLoad balance
            out[0][statusIndex] = output;
            objVal[0][statusIndex] = transferCost;
            if (gen.isON(statusIndex))
                objVal[0][statusIndex] += (startupCost + genCost - lambda[0] * output);

            // cost with reserve
            if (mu != null) {
                if (gen.isON(statusIndex))
                    objVal[0][statusIndex] += -mu[0] * maxP;
            }

            if (cons != null) {
                for (LeqConstraint con : cons) {
                    if (gen.isON(statusIndex)) objVal[0][statusIndex] += output * con.getMu() * con.getA(gen_indx);
                }
            }
        }

        for (int t = 1; t < no_of_ti; t++) {
            for (int statusIndex = 0; statusIndex < no_of_status; statusIndex++) {
                for (preStaIndex = 0; preStaIndex < no_of_status; preStaIndex++) {

                    if (!gen.isReachable(preStaIndex, statusIndex)) continue;

                    double startupCost = gen.getStartupCost(preStaIndex, statusIndex);

                    double temp_lambda = lambda[t];
                    List<LeqConstraint> cons = ti_constraints_map.get(t);
                    if (cons != null) {
                        for (LeqConstraint con : cons) {
                            if (gen.isON(statusIndex)) {
                                temp_lambda -= con.getMu() * con.getA(gen_indx);
                            }
                        }
                    }
                    double output = gen.getOutput(statusIndex, temp_lambda);

                    double genCost = gen.getGenCost(output);
                    double transferCost = gen.getTransferCost(preStaIndex, statusIndex);

                    double obj = objVal[t - 1][preStaIndex];
                    // cost with totalLoad balance
                    if (gen.isON(statusIndex)) obj += (startupCost + genCost - lambda[t] * output);
                    obj += transferCost;

                    // cost with reserve
                    if (mu != null) {
                        if (gen.isON(statusIndex))
                            obj += -mu[t] * maxP;
                    }

                    if (cons != null) {
                        for (LeqConstraint con : cons) {
                            if (gen.isON(statusIndex)) obj += output * con.getMu() * con.getA(gen_indx);
                        }
                    }

                    if (objVal[t][statusIndex] > obj) {
                        preStatusIndex[t][statusIndex] = preStaIndex;
                        out[t][statusIndex] = output;
                        objVal[t][statusIndex] = obj;
                    }
                }
            }
        }

        int finalStatusIndex = 0;
        for (int statusIndex = 1; statusIndex < no_of_status; statusIndex++) {
            if (objVal[no_of_ti - 1][statusIndex] < objVal[no_of_ti - 1][finalStatusIndex]) {
                finalStatusIndex = statusIndex;
            }
        }

        genOut = new double[no_of_ti];
        genStatus = new int[no_of_ti];

        genStatus[no_of_ti - 1] = gen.getStatus(finalStatusIndex);
        genOut[no_of_ti - 1] = out[no_of_ti - 1][finalStatusIndex];
        int nextStatusIndex = finalStatusIndex;
        for (int t = no_of_ti - 2; t >= 0; t--) {
            int statusIndex = preStatusIndex[t + 1][nextStatusIndex];
            genStatus[t] = gen.getStatus(statusIndex);
            genOut[t] = out[t][statusIndex];

            nextStatusIndex = statusIndex;
        }
    }



    public double[] getGenOut() {
        return genOut;
    }

    public int[] getGenStatus() {
        return genStatus;
    }

    public static void getGenStatusFromDPStatus(final int[][] gen_dp_status, int[][] genStatus) {
        final int genNum = gen_dp_status.length;
        final int tiNum = gen_dp_status[0].length;
        for (int i = 0; i < genNum; i++) {
            for (int t = 0; t < tiNum; t++) {
                if (gen_dp_status[i][t] > 0) genStatus[i][t] = 1;
                else genStatus[i][t] = 0;
            }
        }
    }
}
