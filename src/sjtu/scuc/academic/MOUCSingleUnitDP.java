package sjtu.scuc.academic;

import gurobi.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by Zhai Shaopeng on 2017/5/5.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class MOUCSingleUnitDP {

    protected static Log log = LogFactory.getLog(SingleUnitDP.class);
//    private final int penaltyCost = 10000;

    private double[] genOut = null;
    private int[] genStatus = null;
//    private int[] genY = null;

    private GRBEnv env;
    private GRBModel gurobimodel;
    private GRBVar f1;
    private GRBVar f2;
    private GRBVar[] u;
    private GRBVar[] p;
    private GRBVar[] y;
    private GRBVar[] z;

    public int[] solve(Generator[] gens, int gen_indx, double[] lambda, double[] mu, Map<Integer, List<LeqConstraint>> ti_constraints_map) {
        GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[gen_indx];
        final int no_of_ti = lambda.length;
        genOut=new double[no_of_ti];
        genStatus=new int[no_of_ti];
        addVars(no_of_ti, gen, gen_indx);
        addObj(no_of_ti, lambda, mu, gen);
        addRampConstraint(no_of_ti, gen);
        addMinOnDnTimeConstraint(no_of_ti, gen);
        addCostDef(no_of_ti, gen);
        try {
            gurobimodel.update();
            gurobimodel.optimize();
            getResult(no_of_ti);
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return genStatus;
    }

    private void getResult(int no_of_ti) throws GRBException {
        for (int t = 0; t < no_of_ti; t++) {
            genStatus[t] = (int) u[t].get(GRB.DoubleAttr.X);
            genOut[t] = p[t].get(GRB.DoubleAttr.X);
//            genY[t] = (int) y[t].get(GRB.DoubleAttr.X);
        }
        System.out.println("最终输出的结果是：" + gurobimodel.get(GRB.DoubleAttr.ObjVal));
    }

    private void addCostDef(int no_of_ti, GeneratorWithQuadraticCostCurve gen) {
        final double StattCost = gen.getStartupCost();
        GRBQuadExpr expr = new GRBQuadExpr();
        for (int t = 0; t < no_of_ti; t++) {
//            leave alone the copnstant term, may cause confusion in caculating gap or the obj value
            expr.addTerm(gen.getAQuadratic(), p[t], p[t]);
            expr.addTerm(gen.getALinear(), p[t]);
            expr.addTerm(gen.getAConstant(), u[t]);
            expr.addTerm(gen.getStartupCost(), y[t]);
        }
        try {
            gurobimodel.addQConstr(expr, GRB.LESS_EQUAL, f1, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }

        expr = new GRBQuadExpr();
        for (int t = 0; t < no_of_ti; t++) {
//            leave alone the copnstant term, may cause confusion in caculating gap or the obj value
            expr.addTerm(gen.getGasa(), p[t], p[t]);
            expr.addTerm(gen.getGasb(), p[t]);
            expr.addTerm(gen.getGasc(), u[t]);
        }
        try {
            gurobimodel.addQConstr(expr, GRB.LESS_EQUAL, f2, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }

    }

    private void addMinOnDnTimeConstraint(int no_of_ti, GeneratorWithQuadraticCostCurve gen) {
//        min on time
        final int min_on_time = gen.getMin_on_time();
        if (min_on_time > 1) {
            for (int t = 0; t < no_of_ti; t++) {
                final int end_ti = t + min_on_time;
                if (end_ti > no_of_ti) break;

                GRBLinExpr expr = new GRBLinExpr();
                for (int j = t; j < end_ti; j++) {
                    expr.addTerm(1.0, u[j]);
                }
                expr.addTerm(-min_on_time, y[t]);
                try {
                    gurobimodel.addConstr(expr, GRB.GREATER_EQUAL, 0, null);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
            }
        }
//        min dn time
        final int min_dn_time = gen.getMin_dn_time();
        if (min_dn_time > 1) {
            for (int t = 0; t < no_of_ti; t++) {
                final int end_ti = t + min_dn_time;
                if (end_ti > no_of_ti) break;

                GRBLinExpr expr = new GRBLinExpr();
                for (int j = t; j < end_ti; j++) {
                    expr.addTerm(1.0, u[j]);
                }
                expr.addTerm(min_dn_time, z[t]);
                try {
                    gurobimodel.addConstr(expr, GRB.LESS_EQUAL, min_dn_time, null);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addRampConstraint(int no_of_ti, GeneratorWithQuadraticCostCurve gen) {
        if (gen.getInitialConditionHour() > 0) {
            int t = 0;
            GRBLinExpr expr = new GRBLinExpr();
            try {
                expr.addTerm(1.0, p[t]);
                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, gen.getRamp_rate() + gen.getInitialP(), null);
                expr = new GRBLinExpr();
                expr.addTerm(-1.0, p[t]);
//                ramp_down=ramp_up
                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, gen.getRamp_rate() - gen.getInitialP(), null);
            } catch (GRBException e) {
                e.printStackTrace();
            }

        } else {
            int t = 0;
            try {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t]);
                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, gen.getRamp_rate(), null);
            } catch (GRBException e) {
                e.printStackTrace();
            }
        }
        // for other time intervals, add constraints
        for (int t = 1; t < no_of_ti; t++) {
//            if (isRespectStartupShutdownOutput()) expr.addTerm(-max_gen_down + ramp_down, z[t][i]);
            try {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t]);
                expr.addTerm(-1.0, p[t - 1]);
//            if (isRespectStartupShutdownOutput()) expr.addTerm(-max_gen_up + ramp_up, y[t][i]);
                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, gen.getRamp_rate(), null);

                expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t - 1]);
                expr.addTerm(-1.0, p[t]);
                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, gen.getRamp_rate(), null);
            } catch (GRBException e) {
                e.printStackTrace();
            }
        }
    }

    private void addObj(int no_of_ti, double[] lambda, double[] mu, GeneratorWithQuadraticCostCurve gen) {
        GRBQuadExpr expr = new GRBQuadExpr();
        expr.addTerm(1.0, f1, f1);
        expr.addTerm(1.0, f2, f2);
        for (int t = 0; t < no_of_ti; t++) {
            expr.addTerm(-lambda[t], p[t]);
            expr.addTerm(-mu[t] * gen.getMaxP(), u[t]);
        }
        try {
            gurobimodel.setObjective(expr, GRB.MINIMIZE);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addVars(int no_of_ti, GeneratorWithQuadraticCostCurve gen, int gen_indx) {
        try {
            env = new GRBEnv("MOUC.log");
            gurobimodel = new GRBModel(env);
            p = new GRBVar[no_of_ti];
            u = new GRBVar[no_of_ti];
            y = new GRBVar[no_of_ti];
            z = new GRBVar[no_of_ti];
            f1 = gurobimodel.addVar(0, Double.MAX_VALUE, 0, GRB.CONTINUOUS, String.format("f%d_1", gen_indx));
            f2 = gurobimodel.addVar(0, Double.MAX_VALUE, 0, GRB.CONTINUOUS, String.format("f%d_2", gen_indx));
            for (int t = 0; t < no_of_ti; t++) {
                p[t] = gurobimodel.addVar(0, gen.getMaxP(), 0, GRB.CONTINUOUS, String.format("p%d_%d", gen_indx, t));
                u[t] = gurobimodel.addVar(0, 1, 0, GRB.BINARY, String.format("u%d_%d", gen_indx, t));
                y[t] = gurobimodel.addVar(0, 1, 0, GRB.BINARY, String.format("y%d_%d", gen_indx, t));
                z[t] = gurobimodel.addVar(0, 1, 0, GRB.BINARY, String.format("z%d_%d", gen_indx, t));
            }
//            relationship among t,z,u
            for (int t = 1; t < no_of_ti; t++) {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1.0, y[t]);
                expr.addTerm(1.0, z[t]);

                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, 1, null);


                expr = new GRBLinExpr();
                expr.addTerm(1.0, y[t]);
                expr.addTerm(-1.0, z[t]);
                expr.addTerm(-1.0, u[t]);
                expr.addTerm(1.0, u[t - 1]);
                gurobimodel.addConstr(expr, GRB.EQUAL, 0, null);
            }

            // for the first time interval, update it's bound according the initial power and ramp rate
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0, y[0]);
            expr.addTerm(1.0, z[0]);
            gurobimodel.addConstr(expr, GRB.LESS_EQUAL, 1, null);
            final int initialCondionHour = gen.getInitialConditionHour();
            int initialStatus;
            if (initialCondionHour > 0) {
                initialStatus = 1;
            } else {
                initialStatus = 0;
            }
            expr = new GRBLinExpr();
            expr.addTerm(1.0, y[0]);
            expr.addTerm(-1.0, z[0]);
            expr.addTerm(-1.0, u[0]);
            gurobimodel.addConstr(expr, GRB.EQUAL, -initialStatus, null);

            for (int t = 0; t < no_of_ti; t++) {
                expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t]);
                expr.addTerm(-gen.getMaxP(), u[t]);
                gurobimodel.addConstr(expr, GRB.LESS_EQUAL, 0, null);
                expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t]);
                expr.addTerm(-gen.getMinP(), u[t]);
                gurobimodel.addConstr(expr, GRB.GREATER_EQUAL, 0, null);
            }
        } catch (GRBException e) {
            e.printStackTrace();
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
