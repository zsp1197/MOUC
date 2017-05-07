package sjtu.scuc.academic;

import java.util.List;
import java.util.Map;

import gurobi.*;

/**
 * Created by Zhai Shaopeng on 2017/5/5 20:57.
 * E-mail: zsp1197@163.com
 */
public class MOUCLagrangianAlg extends SCUCAlg {
    /**
     * dual value
     */
    private double q_star;

    /**
     * primal value
     */
    private double J_star;

    /**
     * final cur_gap
     */
    private double cur_gap;

    /**
     * [gens][ti]
     */
    private double[][] gen_ed_out = null;

    /**
     * [gens][ti]
     */
    private double[][] gen_dp_out = null;

    /**
     * [gens][ti]
     */
    private int[][] gen_dp_status = null;
    private double[] lambda = null;

    private double f1Opt;

    public double getF1Opt() {
        return f1Opt;
    }

    public void setF1Opt(double f1Opt) {
        this.f1Opt = f1Opt;
    }

    public double getF2Opt() {
        return f2Opt;
    }

    public void setF2Opt(double f2Opt) {
        this.f2Opt = f2Opt;
    }

    private double f2Opt;

    public double[] getLambda() {
        return lambda;
    }

    public void setLambda(double[] lambda) {
        this.lambda = lambda;
    }

    public double[] getMu() {
        return mu;
    }

    public void setMu(double[] mu) {
        this.mu = mu;
    }

    private double[] mu = null;

    private double a_lambda_plus = 0.05;
    private double a_lambda_minus = 0.01;
    private double a_mu_plus = 0.05;
    private double a_mu_minus = 0.01;

    private MOUCSingleUnitDP dpAlg = new MOUCSingleUnitDP();
//    private EconomicDispatchable edAlg = new EconomicDispatchGurobi();
    private EconomicDispatchable edAlg = new EconomicDispatch1HourCplex();

    private StringBuilder q_str = new StringBuilder().append("q:");
    private StringBuilder j_str = new StringBuilder().append("J:");

    @Override
    protected void beforehand_process() {
        // initial lambda for each time interval
        final int no_of_ti = scucData.getTiNum();
        if (lambda == null || lambda.length != no_of_ti) lambda = new double[no_of_ti];

        final double[] p_reserve = scucData.getReserve();
        if (p_reserve == null) {
            mu = null;
        } else if (p_reserve.length != no_of_ti) {
            log.error("The number of reserve is not equal to the number of time interval.");
            return;
        } else {
            if (mu == null || mu.length != no_of_ti) mu = new double[no_of_ti];
        }

        // temperary variables for generator output and status
        final int no_of_gen = scucData.getGenNum();
        gen_dp_out = new double[no_of_gen][no_of_ti];
        gen_dp_status = new int[no_of_gen][no_of_ti];

        // variables to save optimal solution in the iterations
        genOutput = new double[no_of_gen][no_of_ti];
        genStatus = new int[no_of_gen][no_of_ti];
    }

    @Override
    protected void afterward_process() {

    }

    @Override
    protected void addCuts(List<LeqConstraint> cuts) {

    }

    @Override
    protected Calresult callSolver(String s) throws InfeasibleException {
        double cur_best_obj = Double.MAX_VALUE;
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        int[][] tempGenStatus = new int[no_of_gen][no_of_ti];
        int iteration = 1;
        Generator[] gens = scucData.getGens();
        List<LeqConstraint> constraints = scucData.getConstraints();
        Map<Integer, List<LeqConstraint>> ti_constraints_map = scucData.getTi_constraints_map();
        do {
            // for each generator, based on lambda, build dynamic program to get its status and output
            for (int i = 0; i < no_of_gen; i++) {
                dpAlg.solve(gens, i, lambda, mu, ti_constraints_map);

                int status[] = dpAlg.getGenStatus();
                System.arraycopy(status, 0, gen_dp_status[i], 0, no_of_ti);

                double out[] = dpAlg.getGenOut();
                System.arraycopy(out, 0, gen_dp_out[i], 0, no_of_ti);
            }
            // solve for the dual value
            q_star = calDualValue();
            q_str.append(" ").append(q_star);

            // calculate the primal value
            MOUCSingleUnitDP.getGenStatusFromDPStatus(gen_dp_status, tempGenStatus);
            try {
                edAlg.solve(tempGenStatus, scucData);
                gen_ed_out = edAlg.getGenOut();
                J_star = Utility.getTotalActualCost(scucData.getGenList(), tempGenStatus, gen_ed_out);
            } catch (InfeasibleException e) {
                e.printStackTrace();
                gen_ed_out = edAlg.getGenOut();
                J_star = edAlg.getPenaltyCost();
            }
            j_str.append(" ").append(J_star);

            // save the best solution
            if (J_star < cur_best_obj) {
                cur_best_obj = J_star;
                Utility.copy(tempGenStatus, genStatus);
                Utility.copy(gen_ed_out, genOutput);
            }
            // calculate cur_gap
            double pre_gap = cur_gap;
            cur_gap = Math.abs(J_star - q_star);
            if (Math.abs(q_star) > ZERO) cur_gap = cur_gap / q_star;

            if (Math.abs(cur_gap - pre_gap) < 0.0005) break;

            if (Math.abs(cur_gap) > gapTolerance) {
                updateLambda();
                if (scucData.getReserve() != null) updateMu();

                double[][] genOutTimeGen = Utility.getGenOutTimeGen(gen_dp_out);
                for (LeqConstraint con : constraints) {
                    final int ti = con.getTimeInterval();
                    con.updateMU(genOutTimeGen[ti]);
                }
            }

            if (iteration++ >= getMaxIteration()) break;
//            if (iteration++ >= maxIteration) break;
        } while (Math.abs(cur_gap) > getGapTolerance());
        Calresult result =new Calresult();
        result.setGenOutput(genOutput);
        result.setGenStatus(genStatus);
        result.setLambda(lambda);
        result.setMu(mu);
        return result;
    }
    private void updateLambda() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        int[][] gen_status = new int[no_of_gen][no_of_ti];
        SingleUnitDP.getGenStatusFromDPStatus(gen_dp_status, gen_status);

        final double[] p_load = scucData.getTotalLoad();
        for (int t = 0; t < no_of_ti; t++) {
            double dq_dlambda = p_load[t];
            for (int i = 0; i < no_of_gen; i++) {
                if (gen_status[i][t] == 1) dq_dlambda -= gen_dp_out[i][t];
            }
//dq_dlambda=\sigma{loadDemand}-\sigma{gen_out}
            if (dq_dlambda > 0) lambda[t] += a_lambda_plus * dq_dlambda;//if load balance is not met
            else lambda[t] += a_lambda_minus * dq_dlambda;
        }

    }

    private void updateMu() {
        final double[] p_load = scucData.getTotalLoad();
        final double[] p_reserve = scucData.getReserve();
        final Generator[] gens = scucData.getGens();
        for (int t = 0; t < p_load.length; t++) {
            double dq_dmu = p_load[t] + p_reserve[t];
            for (int i = 0; i < gens.length; i++) {
                if (gens[i].isONbyStatus(gen_dp_status[i][t])) dq_dmu -= gens[i].getMaxP();
            }
//if the reserve constraint could never met
            if (dq_dmu > 0) mu[t] += a_mu_plus * dq_dmu;
            else mu[t] += a_mu_minus * dq_dmu;
//the lagrangian multiplier corresponding to non-equal constraint must always be positive
            if (mu[t] < 0) mu[t] = 0;
        }
    }
    private void updateLambdaMu() {

    }

    private double calDualValue() {
        double dual = 0;

        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        final double[] p_load = scucData.getTotalLoad();
        final double[] p_reserve = scucData.getReserve();

        int[][] gen_status = new int[no_of_gen][no_of_ti];
        MOUCSingleUnitDP.getGenStatusFromDPStatus(gen_dp_status, gen_status);
        for (int i = 0; i < no_of_gen; i++) {
            final Generator gen = gens[i];
//            f1^2+f2^2
            dual += Utility.getGenMOUCCost(gen, gen_status[i], gen_dp_out[i]);
        }

        for (int t = 0; t < no_of_ti; t++) {
            double temp = p_load[t];
            for (int i = 0; i < no_of_gen; i++) {
                if (gen_status[i][t] == 1) temp -= gen_dp_out[i][t];
            }
            dual += lambda[t] * temp;
        }

        if (p_reserve != null && mu != null) {
            for (int t = 0; t < no_of_ti; t++) {
                double temp = p_load[t] + p_reserve[t];
                for (int i = 0; i < no_of_gen; i++) {
                    if (gen_status[i][t] == 1) temp -= gens[i].getMaxP();
                }
                dual += mu[t] * temp;
            }
        }

        //以下内容我完全不知道是在干嘛
//        double[][] genOutTimeUnit = new double[no_of_ti][no_of_gen];
//        for (int t = 0; t < no_of_ti; t++) {
//            for (int i = 0; i < no_of_gen; i++) {
//                if (gen_status[i][t] == 1)
//                    genOutTimeUnit[t][i] = gen_dp_out[i][t];
//                else
//                    genOutTimeUnit[t][i] = 0;
//            }
//        }
//        List<LeqConstraint> constraints = scucData.getConstraints();
//        for (LeqConstraint con : constraints) {
//            final int ti = con.getTimeInterval();
//            dual += con.getDualValueComponent(genOutTimeUnit[ti]);
//        }

        return dual;
    }

    @Override
    protected void addBranchPowerflowConstraints(int no_of_branch, Branch[] branchs, double[][] PTDF, String constraintName) {

    }
}
