package sjtu.scuc.academic;

import gurobi.GRBEnv;
import gurobi.GRBModel;
import ilog.concert.IloException;
import gurobi.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/9 9:31.
 * E-mail: zsp1197@163.com
 */
public class MIPAlgGurobi extends SCUCAlg implements EconomicDispatchable {
    private GRBEnv env;
    private GRBModel gurobigo;
    /**
     * gen's power output level
     */
    private GRBVar[][] p;
    /**
     * gen's status. 0:off, 1:on
     */
    private GRBVar[][] u;

    /**
     * gen's start up flag. start up:1
     */
    private GRBVar[][] y;
    /**
     * gen's shut down flag. shut down:1
     */
    private GRBVar[][] z;
    private GRBVar f1;
    private GRBVar f2;

    @Override
    public void solve(int[][] gen_status, SCUCData scucData) throws InfeasibleException {
        this.scucData = scucData;
        beforehand_process();
        callSolver("EconmicDispatch_FixedIntVar");

        afterward_process();
    }

    public void solve(SCUCData scucData) {
        this.scucData = scucData;
        addVariables(false);
        addCommonConstraintsAndObj();
        callSolver("EconmicDispatch_FixedIntVar");

        afterward_process();
    }

    @Override
    protected void beforehand_process() {
        addVariables(false);
        addCommonConstraintsAndObj();
    }

    private void addCommonConstraintsAndObj() {
        addGenCostDef();

        addLoadBalanceConstraint();
        addReserveConstraint();
        try {
            addRampConstraint();
        } catch (GRBException e) {
            e.printStackTrace();
        }

        addMinOnTimeConstraint();
        addMinDnTimeConstraint();

//        addUserDefinedLeqConstraint();

        addObjFunction();
    }

    private void addObjFunction() {
        GRBQuadExpr expr = new GRBQuadExpr();
        if (scucData.getMode() == "f1") {
            expr.addTerm(1.0, f1);
        } else if (scucData.getMode() == "f2") {
            expr.addTerm(1.0, f2);
        } else {
            expr.addTerm(1.0, f1, f1);
            expr.addTerm(1.0, f2, f2);
        }
        try {
            gurobigo.setObjective(expr, GRB.MINIMIZE);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addMinDnTimeConstraint() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        //  min_dn_time_0_0:  u(0,0) + u(1,0) + u(2,0) + 3 z(0,0) <= 3
        for (int i = 0; i < no_of_gen; ++i) {
            final int min_dn_time = gens[i].getMin_dn_time();
            if (min_dn_time <= 1) continue;
            for (int t = 0; t < no_of_ti; t++) {
                final int end_ti = t + min_dn_time;
                if (end_ti > no_of_ti) break;

                GRBLinExpr expr = new GRBLinExpr();
                for (int j = t; j < end_ti; j++) {
                    expr.addTerm(1.0, u[j][i]);
                }
                expr.addTerm(min_dn_time, z[t][i]);
                try {
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, min_dn_time, null);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addMinOnTimeConstraint() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // min_on_time_0_0:  u(0,0) + u(1,0) + u(2,0) - 3 y(0,0) >= 0
        for (int i = 0; i < no_of_gen; ++i) {
            final int min_on_time = gens[i].getMin_on_time();
            if (min_on_time <= 1) continue;
            for (int t = 0; t < no_of_ti; t++) {
                final int end_ti = t + min_on_time;
                if (end_ti > no_of_ti) break;

                GRBLinExpr expr = new GRBLinExpr();
                for (int j = t; j < end_ti; j++) {
                    expr.addTerm(1.0, u[j][i]);
                }
                expr.addTerm(-min_on_time, y[t][i]);
                try {
                    gurobigo.addConstr(expr, GRB.GREATER_EQUAL, 0, null);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addRampConstraint() throws GRBException {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();

        // ramp
        for (int i = 0; i < no_of_gen; ++i) {
            final double ramp = gens[i].getRamp_rate();
            if (!isRespectStartupShutdownOutput() && (ramp >= gens[i].getMaxP())) continue;

            final double ramp_up = ramp;
            final double ramp_down = ramp;

            final double minP = gens[i].getMinP();
            final double max_gen_up = minP;
            final double max_gen_down = minP;
            // for the first time interval, update it's bound according the initial power and ramp rate
            final double initialP = gens[i].getInitialP();
            final int initialCondionHour = gens[i].getInitialConditionHour();
//            if (false) {
            if (initialCondionHour > 0) {

                int t = 0;
                if (!isRespectStartupShutdownOutput()) {// p[t - 1][i] = initialP > 0,已经运行够了最小启动时间，始终进入
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_up + initialP, null);
                    expr = new GRBLinExpr();
                    expr.addTerm(-1.0, p[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_down - initialP, null);
                } else {// p[t - 1][i] = initialP > 0, y[t][i] = 0
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_up + initialP, null);

                    expr = new GRBLinExpr();
                    expr.addTerm(-1.0, p[t][i]);
                    expr.addTerm(-max_gen_down + ramp_down, z[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_down - initialP, null);
                }
            } else { // p[t - 1][i] = 0, z[t][i] = 0
                int t = 0;
                if (!isRespectStartupShutdownOutput()) {
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_up, null);
                } else {
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    expr.addTerm(-max_gen_up + ramp_up, y[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_up, null);
                    // After analyzing this constraint, we know that it is redundant,
                    // in order to reduce the number of constraints and increase the solving speed,
                    // it is not added into the model.
                }
            }

            // for other time intervals, add constraints
            for (int t = 1; t < no_of_ti; t++) {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t][i]);
                expr.addTerm(-1.0, p[t - 1][i]);
                if (isRespectStartupShutdownOutput()) expr.addTerm(-max_gen_up + ramp_up, y[t][i]);
                gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_up, null);

                expr = new GRBLinExpr();
                expr.addTerm(1.0, p[t - 1][i]);
                expr.addTerm(-1.0, p[t][i]);
                if (isRespectStartupShutdownOutput()) expr.addTerm(-max_gen_down + ramp_down, z[t][i]);
                gurobigo.addConstr(expr, GRB.LESS_EQUAL, ramp_down, null);
            }
        }
    }

    private void addReserveConstraint() {
        final int no_of_ti = scucData.getTiNum();
        // reserve constraint
        final double[] load = scucData.getTotalLoad();
        final double[] reserve = scucData.getReserve();
        if (reserve != null && reserve.length == no_of_ti) {
            final int no_of_gen = scucData.getGenNum();
            Generator[] gens = scucData.getGens();

            for (int t = 0; t < no_of_ti; t++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int i = 0; i < no_of_gen; i++) {
                    expr.addTerm(gens[i].getMaxP(), u[t][i]);
                }
                try {
                    gurobigo.addConstr(expr, GRB.GREATER_EQUAL, reserve[t] + load[t], null);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("zhai: no reserve!");
        }
        try {
            gurobigo.update();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addLoadBalanceConstraint() {
        final int no_of_ti = scucData.getTiNum();
        final int no_of_gen = scucData.getGenNum();
        // totalLoad balance equation constraint
        final double[] load = scucData.getTotalLoad();
        for (int t = 0; t < no_of_ti; t++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int i = 0; i < no_of_gen; i++) {
                expr.addTerm(1, p[t][i]);
            }
            try {
                gurobigo.addConstr(expr, GRB.EQUAL, load[t], String.format("load_balace_%d", t));
            } catch (GRBException e) {
                e.printStackTrace();
            }
        }
    }

    private void addGenCostDef() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // relationship between cost and power level
        GRBQuadExpr expr = new GRBQuadExpr();
        for (int i = 0; i < no_of_gen; i++) {
            GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(gen.getAQuadratic(), p[t][i], p[t][i]);
                expr.addTerm(gen.getALinear(), p[t][i]);
                expr.addTerm(gen.getAConstant(), u[t][i]);
                expr.addTerm(gen.getStartupCost(), y[t][i]);
            }
        }
        try {
            gurobigo.addQConstr(expr, GRB.LESS_EQUAL, f1, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }
//        f2
        expr = new GRBQuadExpr();
        for (int i = 0; i < no_of_gen; i++) {
            GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(gen.getGasa(), p[t][i], p[t][i]);
                expr.addTerm(gen.getGasb(), p[t][i]);
                expr.addTerm(gen.getGasc(), u[t][i]);
            }
        }
        try {
            gurobigo.addQConstr(expr, GRB.LESS_EQUAL, f2, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addVariables(boolean isLP) {

        try {
            env = new GRBEnv();
            gurobigo = new GRBModel(env);

            final int no_of_gen = scucData.getGenNum();
            final int no_of_ti = scucData.getTiNum();
            Generator[] gens = scucData.getGens();

            // create variables    GRBVar为0/1变量
            p = new GRBVar[no_of_ti][no_of_gen];
            u = new GRBVar[no_of_ti][no_of_gen];
            y = new GRBVar[no_of_ti][no_of_gen];
            z = new GRBVar[no_of_ti][no_of_gen];
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_gen; i++) {
//                    p[t][i] = gurobigo.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, String.format("p%d_%d", t, i));
                    u[t][i] = gurobigo.addVar(0.0, 1, 0.0, GRB.BINARY, String.format("u%d_%d", t, i));
                    y[t][i] = gurobigo.addVar(0.0, 1, 0.0, GRB.BINARY, String.format("y%d_%d", t, i));
                    z[t][i] = gurobigo.addVar(0.0, 1, 0.0, GRB.BINARY, String.format("z%d_%d", t, i));
                }
            }
            f1 = gurobigo.addVar(0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "f1");
            f2 = gurobigo.addVar(0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "f2");
            // set u's value according to the initial condition hours and min_on/dn_time
            // define relation between p and u
            for (int i = 0; i < no_of_gen; i++) {
                double maxP = gens[i].getMaxP();
                double minP = gens[i].getMinP();
                for (int t = 0; t < no_of_ti; t++) {
                    p[t][i] = gurobigo.addVar(0.0, maxP, 0.0, GRB.CONTINUOUS, String.format("p%d_%d", t, i));
//                    没看懂
//                    if (gens[i].mustON(t)) {
//                        u[t][i] = gurobigo.addVar(1, 1, 0.0, GRB.BINARY, String.format("u%d_%d",t,i));
//                    } else if (gens[i].mustOFF(t)) {
//                        u[t][i] = gurobigo.addVar(0, 0, 0.0, GRB.BINARY, String.format("u%d_%d",t,i));
//                    }
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    expr.addTerm(-maxP, u[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, 0, null);
                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    expr.addTerm(-minP, u[t][i]);
                    gurobigo.addConstr(expr, GRB.GREATER_EQUAL, 0, null);
                }
            }

            // define start up flag, shut down flag.
            for (int i = 0; i < no_of_gen; ++i) {
                // because Ramping Up/Down Constraints are using startup/shutdown flag,
                // I cannot ignore the defination of these flags.
//            if (!gens[i].needStartupShutdownFlag()) continue;

                // for other time intervals, add constraints
                for (int t = 1; t < no_of_ti; t++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, y[t][i]);
                    expr.addTerm(1.0, z[t][i]);

                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, 1, null);


                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, y[t][i]);
                    expr.addTerm(-1.0, z[t][i]);
                    expr.addTerm(-1.0, u[t][i]);
                    expr.addTerm(1.0, u[t - 1][i]);
                    gurobigo.addConstr(expr, GRB.EQUAL, 0, null);
                }

                // for the first time interval, update it's bound according the initial power and ramp rate
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1.0, y[0][i]);
                expr.addTerm(1.0, z[0][i]);
                gurobigo.addConstr(expr, GRB.LESS_EQUAL, 1, null);
                final int initialCondionHour = gens[i].getInitialConditionHour();
                int initialStatus;
                if (initialCondionHour > 0) {
                    initialStatus = 1;
                } else {
                    initialStatus = 0;
                }
                expr = new GRBLinExpr();
                expr.addTerm(1.0, y[0][i]);
                expr.addTerm(-1.0, z[0][i]);
                expr.addTerm(-1.0, u[0][i]);
                gurobigo.addConstr(expr, GRB.EQUAL, -initialStatus, null);
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double[][] getGenOut() {
        return genOutput;
    }

    @Override
    public int getPenaltyCost() {
        return 0;
    }

    @Override
    public void setPenaltyCost(int penalty_cost) {

    }

    @Override
    protected void afterward_process() {
        gurobigo.dispose();
        try {
            env.dispose();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void addCuts(List<LeqConstraint> cuts) {

    }

    @Override
    protected Calresult callSolver(String s) {
        final int no_of_gen = this.scucData.getGenNum();
        final int no_of_ti = this.scucData.getTiNum();
        int[][] genY = new int[no_of_gen][no_of_ti];
        try {
            double[] tempsum = new double[no_of_ti];
            double temp = 0;
            genStatus = new int[no_of_gen][no_of_ti];
            genOutput = new double[no_of_gen][no_of_ti];

            gurobigo.update();
            gurobigo.optimize();
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_gen; ++i) {
                    genStatus[i][t] = (int) u[t][i].get(GRB.DoubleAttr.X);
                    genOutput[i][t] = p[t][i].get(GRB.DoubleAttr.X);
                    genY[i][t] = (int) y[t][i].get(GRB.DoubleAttr.X);

                    temp = temp + genOutput[i][t];
                }
                tempsum[t] = temp;
                temp = 0;
            }
            System.out.println("最终输出的结果是：" + gurobigo.get(GRB.DoubleAttr.ObjVal));

        } catch (GRBException e) {
            e.printStackTrace();
        }
        Calresult calresult = null;
        try {
            calresult = new Calresult(this.scucData.getMode(), genStatus, genOutput, gurobigo.get(GRB.DoubleAttr.ObjVal), genY);
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return calresult;
    }

    @Override
    protected void addBranchPowerflowConstraints(int no_of_branch, Branch[] branchs, double[][] PTDF, String constraintName) {

    }
}
