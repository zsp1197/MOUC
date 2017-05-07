package sjtu.scuc.academic;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class LagrangianAlg extends SCUCAlg {

    // TODO: add whole period economic dispatch in each iteration

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

    // step to update Lagrangian multipliers for totalLoad balance constraints
    //    private double a_lambda_plus = 0.01;
    //    private double a_lambda_minus = 0.002;
    private double a_lambda_plus = 0.05;
    private double a_lambda_minus = 0.01;
    private double[] lambda = null; //[ti]

    // step to update Lagrangian multipliers for generation reserve constraints
    //    private double a_mu_plus = 0.01;
    //    private double a_mu_minus = 0.002;
    private double a_mu_plus = 0.05;
    private double a_mu_minus = 0.01;
    private double[] mu = null; //[ti]

    private SingleUnitDP dpAlg = new SingleUnitDP();
    private EconomicDispatchable edAlg = new EconomicDispatch1HourCplex();

    private StringBuilder q_str = new StringBuilder().append("q:");
    private StringBuilder j_str = new StringBuilder().append("J:");

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

    protected void afterward_process() {
//        log.debug(q_str.toString());
//        log.debug(j_str.toString());
    }

    protected void addCuts(List<LeqConstraint> cuts) {
        List<LeqConstraint> constraints = scucData.getConstraints();
        Map<Integer, List<LeqConstraint>> ti_constraints_map = scucData.getTi_constraints_map();
        int addedCutsNum = 0;
        for (LeqConstraint con : cuts) {
            int addedCut = Utility.addConstraint(con, constraints, ti_constraints_map);
            if (addedCut == 1) {
                log.debug(MessageFormat.format("Adding cut at Iteration {0}:{1}", cur_iteration, con.toString()));
            }
            addedCutsNum += addedCut;
        }
        if (addedCutsNum == 0) {
            log.warn(MessageFormat.format("No cut added at Iteration {0}", cur_iteration));
        }
    }

    protected Calresult callSolver(final String name) {
//        log.info(name);
        double cur_best_obj = Double.MAX_VALUE;

        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        int[][] tempGenStatus = new int[no_of_gen][no_of_ti];

        Generator[] gens = scucData.getGens();
        // initial cur_gap
        int iteration = 1;
        cur_gap = 1000;
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
            SingleUnitDP.getGenStatusFromDPStatus(gen_dp_status, tempGenStatus);
            try {
                edAlg.solve(tempGenStatus, scucData);
                gen_ed_out = edAlg.getGenOut();
                J_star = Utility.getTotalActualCost(scucData.getGenList(), tempGenStatus, gen_ed_out);
            } catch (InfeasibleException e) {
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

            // output result
//            if (outputDetail) output_iteration_info(iteration, log.isDebugEnabled());

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
        result.setGenOutput(gen_dp_out);
        result.setGenStatus(tempGenStatus);
        result.setLambda(lambda);
        result.setMu(mu);
        return result;
    }

    protected void addBranchPowerflowConstraints(final int no_of_branch, final Branch[] branches, final double[][] PTDF, final String constraintName) {
        final List<BusLoad> busLoadList = scucData.getBusLoadList();
        final BusLoad busLoads[] = busLoadList.toArray(new BusLoad[busLoadList.size()]);

        double[][] PTDF_minus = new double[PTDF.length][PTDF[0].length];
        for (int i = 0; i < PTDF_minus.length; i++)
            for (int j = 0; j < PTDF_minus[i].length; j++)
                PTDF_minus[i][j] = -PTDF[i][j];

        final int no_of_ti = scucData.getTiNum();
        List<LeqConstraint> constraints = scucData.getConstraints();
        Map<Integer, List<LeqConstraint>> ti_constraints_map = scucData.getTi_constraints_map();
        for (int t = 0; t < no_of_ti; t++) {
            for (int i = 0; i < no_of_branch; i++) {

                double loadContribute = 0;
                for (BusLoad busLoad : busLoads) {
                    final int bus_idx = busLoad.getBusIdx();
                    final double[] load = busLoad.getLoad();
                    loadContribute += PTDF[i][bus_idx] * (-load[t]);
                }

                LeqConstraint expr_ub = new LeqConstraint();
                expr_ub.setName(MessageFormat.format("{2}_T{0}_{1}_ub", t, branches[i].getName(), constraintName));
                expr_ub.setTimeInterval(t);
                expr_ub.setA(PTDF[i]);
                expr_ub.setB(branches[i].getCapacity() - loadContribute);
                Utility.addConstraint(expr_ub, constraints, ti_constraints_map);

                LeqConstraint expr_lb = new LeqConstraint();
                expr_lb.setName(MessageFormat.format("{2}_t{0}_b{1}_lb", t, i, constraintName));
                expr_lb.setTimeInterval(t);
                expr_lb.setA(PTDF_minus[i]);
                expr_lb.setB(branches[i].getCapacity() + loadContribute);
                Utility.addConstraint(expr_lb, constraints, ti_constraints_map);
            }
        }
    }

    private void output_iteration_info(final int iteration, boolean showDetail) {
        // cur_iteration #
        StringBuilder sb = new StringBuilder();
        sb.append("iteration ").append(iteration).append(" :");
        sb.append("\t").append(MessageFormat.format("q = {0,number,#.###}", q_star));
        sb.append("\t").append(MessageFormat.format("J = {0,number,#.###}", J_star));
        sb.append("\t").append(MessageFormat.format("gap = {0,number,#.######}", cur_gap));
        log.info(sb.toString());

        if (!showDetail) return;

        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();

        // title
        sb = new StringBuilder();
        sb.append(printStr("Hour", 5));
        sb.append(printStr("lambda", 8));
        sb.append(printStr("L-SigmaP", 9));
        if (mu != null) {
            sb.append(printStr("mu", 8));
            sb.append(printStr("PMax-L-Res", 11));
        }
        for (int i = 0; i < no_of_gen; i++) sb.append(printStr("u" + (i + 1), 3));
        for (int i = 0; i < no_of_gen; i++) sb.append(printStr("P" + (i + 1), 8));
        for (int i = 0; i < no_of_gen; i++) sb.append(printStr("P_ed_" + (i + 1), 8));
        log.info(sb.toString());

        int[][] gen_status = new int[no_of_gen][no_of_ti];
        SingleUnitDP.getGenStatusFromDPStatus(gen_dp_status, gen_status);
        // details
        final double[] p_load = scucData.getTotalLoad();
        final double[] p_reserve = scucData.getReserve();
        for (int t = 0; t < no_of_ti; t++) {
            sb = new StringBuilder();
            sb.append(printStr(MessageFormat.format("{0,number}", t), 5));
            sb.append(printStr(MessageFormat.format("{0,number,#.###}", lambda[t]), 8));
            double sigmaP = 0;
            double sigmaPMax = 0;
            for (int i = 0; i < no_of_gen; i++) {
                sigmaP += gen_dp_out[i][t];
                if (gen_status[i][t] == 1) sigmaPMax += gens[i].getMaxP();
            }
            sb.append(printStr(MessageFormat.format("{0,number,#.##}", p_load[t] - sigmaP), 9));

            if (mu != null) {
                sb.append(printStr(MessageFormat.format("{0,number,#.###}", mu[t]), 8));
                sb.append(printStr(MessageFormat.format("{0,number,#.##}", sigmaPMax - p_load[t] - p_reserve[t]), 11));
            }

            for (int i = 0; i < no_of_gen; i++)
                sb.append(printStr(MessageFormat.format("{0,number}", gen_status[i][t]), 3));

            for (int i = 0; i < no_of_gen; i++)
                sb.append(printStr(MessageFormat.format("{0,number,#.##}", gen_dp_out[i][t]), 8));

            for (int i = 0; i < no_of_gen; i++)
                sb.append(printStr(MessageFormat.format("{0,number,#.##}", gen_ed_out[i][t]), 8));

            log.info(sb.toString());
        }

        // constraints' details
        List<LeqConstraint> constraints = scucData.getConstraints();
        if (constraints.size() > 0) {
            StringBuilder name_str = new StringBuilder().append(printStr("name", 5));
            StringBuilder ti_str = new StringBuilder().append(printStr("hour", 5));
            StringBuilder mu_str = new StringBuilder(printStr("mu", 5));
            for (LeqConstraint con : constraints) {
                final double mu = con.getMu();
                if (mu > 0) {
                    name_str.append(printStr(con.getName(), 20));
                    ti_str.append(printStr(MessageFormat.format("{0,number}", con.getTimeInterval()), 20));
                    mu_str.append(printStr(MessageFormat.format("{0,number,#.###}", mu), 20));
                }
            }
            log.info(name_str.toString());
            log.info(ti_str.toString());
            log.info(mu_str.toString());
        }

        // output hydro unit details
//        for (int i = 0; i < no_of_gen; i++) {
//            if (gens[i] instanceof HydroUnit) log.info(gens[i].toString());
//        }
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

    private double calDualValue() {
        double dual = 0;

        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        final double[] p_load = scucData.getTotalLoad();
        final double[] p_reserve = scucData.getReserve();

        int[][] gen_status = new int[no_of_gen][no_of_ti];
        SingleUnitDP.getGenStatusFromDPStatus(gen_dp_status, gen_status);
        for (int i = 0; i < no_of_gen; i++) {
            final Generator gen = gens[i];
            dual += Utility.getGenCost(gen, gen_status[i], gen_dp_out[i]);


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

        //
        double[][] genOutTimeUnit = new double[no_of_ti][no_of_gen];
        for (int t = 0; t < no_of_ti; t++) {
            for (int i = 0; i < no_of_gen; i++) {
                if (gen_status[i][t] == 1)
                    genOutTimeUnit[t][i] = gen_dp_out[i][t];
                else
                    genOutTimeUnit[t][i] = 0;
            }
        }
        List<LeqConstraint> constraints = scucData.getConstraints();
        for (LeqConstraint con : constraints) {
            final int ti = con.getTimeInterval();
            dual += con.getDualValueComponent(genOutTimeUnit[ti]);
        }

        return dual;
    }


    public double getQ_star() {
        return q_star;
    }

    public double getJ_star() {
        return J_star;
    }

    public double getCur_gap() {
        return cur_gap;
    }

    public double[][] getGen_ed_out() {
        return gen_ed_out;
    }

    public double[][] getGen_dp_out() {
        return gen_dp_out;
    }

    public int[][] getGen_dp_status() {
        return gen_dp_status;
    }

    public double[] getLambda() {
        return lambda;
    }

    public void setLambda(double[] lambda) {
        this.lambda = lambda;
    }

    public void setA_lambda_plus(double a_lambda_plus) {
        this.a_lambda_plus = a_lambda_plus;
    }

    public void setA_lambda_minus(double a_lambda_minus) {
        this.a_lambda_minus = a_lambda_minus;
    }

    public double getA_lambda_plus() {
        return a_lambda_plus;
    }

    public double getA_lambda_minus() {
        return a_lambda_minus;
    }

    public double[] getMu() {
        return mu;
    }

    public void setMu(double[] mu) {
        this.mu = mu;
    }

    public void setA_mu_plus(double a_mu_plus) {
        this.a_mu_plus = a_mu_plus;
    }

    public void setA_mu_minus(double a_mu_minus) {
        this.a_mu_minus = a_mu_minus;
    }

    public double getA_mu_plus() {
        return a_mu_plus;
    }

    public double getA_mu_minus() {
        return a_mu_minus;
    }

    public EconomicDispatchable getEdAlg() {
        return edAlg;
    }

    public void setEdAlg(EconomicDispatchable edAlg) {
        this.edAlg = edAlg;
    }

}