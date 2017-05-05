package sjtu.scuc.academic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.text.MessageFormat;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
abstract public class EconomicDispatch1Hour implements EconomicDispatchable {
    boolean outputDetail = false;
    double gapTolerance = 0.0001;

    private boolean respectStartupShutdownOutput = false;

    int penaltyCost = 10000000;
    protected double[][] genOut = null;  // output for each generator at each time interval

    /**
     * prepare data for each hour dispatch
     * OR
     * execute economic dispatch for all hours
     *
     * @param gen_status:
     * @param scucData
     * @return objective value or penalty cost
     */
    public void solve(final int[][] gen_status, SCUCData scucData) throws InfeasibleException {
        Generator[] gens = scucData.getGens();
        double[] p_load = scucData.getTotalLoad();
        double[] p_reserve = scucData.getReserve();
        Map<Integer, List<LeqConstraint>> ti_constraints_map = scucData.getTi_constraints_map();

        final int no_of_gen = gens.length;
        final int no_of_ti = p_load.length;
        genOut = new double[no_of_gen][no_of_ti];

        double[] genPreTimeIntervalOut = new double[no_of_gen];
        for (int i = 0; i < no_of_gen; i++) genPreTimeIntervalOut[i] = gens[i].getInitialP();

        for (int t = 0; t < no_of_ti; t++) {
            int[] status = new int[no_of_gen];
            for (int i = 0; i < no_of_gen; i++) status[i] = gen_status[i][t];

            List<LeqConstraint> cons = ti_constraints_map.get(t);

            double cost = 0;
            if (p_reserve != null)
                cost += economicDispatch(gens, status, p_load[t], genPreTimeIntervalOut, p_reserve[t], cons);
            else
                cost += economicDispatch(gens, status, p_load[t], genPreTimeIntervalOut, 0, cons);

            if(cost== penaltyCost) {
                String s = MessageFormat.format("Infeasible economic dispatch at hour {0}", t);
                throw new InfeasibleException(s, null);
            }

            for (int i = 0; i < no_of_gen; i++) {
                genOut[i][t] = gens[i].getP();
                genPreTimeIntervalOut[i] = gens[i].getP();
            }
        }
    }

    /**
     * return the total economic dispatch cost (including penalty cost)
     * and set the corresponding generator's output in gen.getP()
     *
     * @param gen:         generations
     * @param gen_status:  generations' status
     * @param p_load:      total totalLoad level
     * @param genPreOut:   generator's output at previous time interval
     * @param p_reserve:   generator reserve requirement
     * @param constraints: constraints at current time interval
     * @return the total economic dispatch cost (including penalty cost)
     */
    public double economicDispatch(final Generator[] gen, final int[] gen_status, final double p_load, final double[] genPreOut, double p_reserve, List<LeqConstraint> constraints) {

        List<Generator> on_gen_list = new ArrayList<Generator>(gen.length);
        List<Integer> on_gen_idx = new ArrayList<Integer>(gen.length);
        for (int i = 0; i < gen.length; i++) {
            gen[i].setP(0);
            gen[i].updateOperationLimit(genPreOut[i]);

            if (gen_status[i] == 1) {
                on_gen_list.add(gen[i]);
                on_gen_idx.add(i);
            }
        }

        Generator[] on_gens = on_gen_list.toArray(new Generator[on_gen_list.size()]);
        Integer[] on_gens_idx = on_gen_idx.toArray(new Integer[on_gen_idx.size()]);

        double totalMaxOperation = 0, totalMaxP = 0;
        for (Generator on_gen : on_gens) {
            totalMaxOperation += on_gen.getMaxOperationP();
            totalMaxP += on_gen.getMaxP();
        }
        if (totalMaxOperation < p_load || totalMaxP < (p_load + p_reserve)) {
            for (Generator on_gen : on_gens) {
                on_gen.setP(on_gen.getMaxOperationP());
            }
            return penaltyCost;
        }

        return economicDispatch(p_load, on_gens, on_gens_idx, constraints);
    }

    /**
     * economic dispatch for single hour
     *
     * @param p_load:
     * @param on_gens:
     * @param on_gens_idx:
     * @param constraints:
     * @return objective value or penalty cost
     */
    public abstract double economicDispatch(final double p_load, final Generator[] on_gens, Integer[] on_gens_idx, List<LeqConstraint> constraints);

    public double[][] getGenOut() {
        return genOut;
    }


    public double getGapTolerance() {
        return gapTolerance;
    }

    public void setGapTolerance(double gapTolerance) {
        this.gapTolerance = gapTolerance;
    }

    public int getPenaltyCost() {
        return penaltyCost;
    }

    public void setPenaltyCost(int penalty_cost) {
        this.penaltyCost = penalty_cost;
    }

    public boolean isOutputDetail() {
        return outputDetail;
    }

    public void setOutputDetail(boolean output_detail) {
        this.outputDetail = output_detail;
    }

    public boolean isRespectStartupShutdownOutput() {
        return respectStartupShutdownOutput;
    }

    public void setRespectStartupShutdownOutput(boolean respectStartupShutdownOutput) {
        this.respectStartupShutdownOutput = respectStartupShutdownOutput;
    }
}
