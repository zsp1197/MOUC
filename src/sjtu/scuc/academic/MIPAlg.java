package sjtu.scuc.academic;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by Zhai Shaopeng on 2017/5/5.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class MIPAlg extends SCUCAlg implements EconomicDispatchable {

    private IloCplex cplex;
    /**
     * gen's power output level
     */
    private IloNumVar[][] p;
    /**
     * gen's status. 0:off, 1:on
     */
    private IloNumVar[][] u;
    /**
     * gen's cost
     */
    private IloNumVar[][] c;
    /**
     * gen's start up flag. start up:1
     */
    private IloNumVar[][] y;
    /**
     * gen's shut down flag. shut down:1
     */
    private IloNumVar[][] z;

    private List<IloNumVar[]> vList = new ArrayList<IloNumVar[]>();
    private List<IloNumVar[]> qList = new ArrayList<IloNumVar[]>();

    protected void beforehand_process() {
        try {
            addVariables(false);

            addCommonConstraintsAndObj();
        } catch (IloException e) {
            log.error(e);
        }
    }

    private void addCommonConstraintsAndObj() throws IloException {
        addGenCostDef();
        addStartupShutdownFlagDef();

        addLoadBalanceConstraint();
        addReserveConstraint();
        addRampConstraint();

        addMinOnTimeConstraint();
        addMinDnTimeConstraint();

        addUserDefinedLeqConstraint();

        addObjFunction();
    }

    protected void afterward_process() {
        cplex.end();
    }

    protected void addCuts(List<LeqConstraint> cuts) {
        try {
            addLeqConstraints(cuts);
        } catch (IloException e) {
            log.error(e);
        }
    }

    protected Calresult callSolver(final String s) throws InfeasibleException {
        try {
            if (outputDetail) cplex.exportModel(MessageFormat.format("{0}.lp", s));

            final int no_of_gen = scucData.getGenNum();
            final int no_of_ti = scucData.getTiNum();

            genStatus = new int[no_of_gen][no_of_ti];
            genOutput = new double[no_of_gen][no_of_ti];
            if (cplex.solve()) {
                for (int t = 0; t < no_of_ti; t++) {
                    for (int i = 0; i < no_of_gen; ++i) {
                        genStatus[i][t] = cplex.getValue(u[t][i]) > 0.5 ? 1 : 0;
                        genOutput[i][t] = cplex.getValue(p[t][i]);
                    }
                }

                // debug: output volumn
                StringBuilder vString = new StringBuilder().append("Volumn:");
                for (int i = 0; i < vList.size(); i++) {
                    vString.append("\r\n");
                    IloNumVar[] v = vList.get(i);
                    double[] vValue = cplex.getValues(v);
                    for (int j = 0; j < vValue.length; j++) {
                        vString.append(MessageFormat.format("  V_U{0}_T{1}={2, number, #.###}", i, j, vValue[j]));
                    }
                }
                log.debug(vString.toString());

                // debug: output volumn
                StringBuilder qString = new StringBuilder().append("Used water:");
                for (int i = 0; i < qList.size(); i++) {
                    qString.append("\r\n");
                    IloNumVar[] v = qList.get(i);
                    double[] vValue = cplex.getValues(v);
                    for (int j = 0; j < vValue.length; j++) {
                        qString.append(MessageFormat.format("  q_U{0}_T{1}={2, number, #.###}", i, j, vValue[j]));
                    }
                }
                log.debug(qString.toString());

            } else {
                throw new InfeasibleException(s, scucData);
            }
        } catch (IloException e) {
            log.error(e);
        }
        return null;
    }

    protected void addBranchPowerflowConstraints(final int no_of_branch, final Branch[] branchs, final double[][] PTDF, final String constraintName) {
        final List<BusLoad> busLoadList = scucData.getBusLoadList();
        final BusLoad busLoads[] = busLoadList.toArray(new BusLoad[busLoadList.size()]);

        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        for (int t = 0; t < no_of_ti; t++) {
            for (int i = 0; i < no_of_branch; i++) {
                try {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int j = 0; j < no_of_gen; j++) {
                        expr.addTerm(p[t][j], PTDF[i][gens[j].getBusIdx()]);
                    }

                    double loadContribute = 0;
                    for (BusLoad busLoad : busLoads) {
                        final int bus_idx = busLoad.getBusIdx();
                        final double[] load = busLoad.getLoad();
                        loadContribute += PTDF[i][bus_idx] * (-load[t]);
                    }

                    cplex.addLe(expr, branchs[i].getCapacity() - loadContribute, MessageFormat.format("{2}_t{0}_b{1}_ub", t, i, constraintName));
                    cplex.addGe(expr, -branchs[i].getCapacity() - loadContribute, MessageFormat.format("{2}_t{0}_b{1}_lb", t, i, constraintName));
                } catch (IloException e) {
                    log.error(e);
                }
            }
        }
    }

    private void addVariables(boolean isLP) throws IloException {
        cplex = new IloCplex();
        if (!outputDetail) cplex.setOut(null);

        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();

        // create variables
        p = new IloNumVar[no_of_ti][no_of_gen];
        String[][] pName = new String[no_of_ti][no_of_gen];
        u = new IloNumVar[no_of_ti][no_of_gen];
        String[][] uName = new String[no_of_ti][no_of_gen];
        c = new IloNumVar[no_of_ti][no_of_gen];
        String[][] cName = new String[no_of_ti][no_of_gen];
        y = new IloNumVar[no_of_ti][no_of_gen];
        String[][] yName = new String[no_of_ti][no_of_gen];
        z = new IloNumVar[no_of_ti][no_of_gen];
        String[][] zName = new String[no_of_ti][no_of_gen];
        for (int t = 0; t < no_of_ti; t++) {
            for (int i = 0; i < no_of_gen; i++) {
                pName[t][i] = MessageFormat.format("p({0},{1})", t, i);
                uName[t][i] = MessageFormat.format("u({0},{1})", t, i);
                cName[t][i] = MessageFormat.format("c({0},{1})", t, i);
                yName[t][i] = MessageFormat.format("y({0},{1})", t, i);
                zName[t][i] = MessageFormat.format("z({0},{1})", t, i);
            }
            p[t] = cplex.numVarArray(no_of_gen, 0.0, Double.MAX_VALUE, IloNumVarType.Float, pName[t]);
            c[t] = cplex.numVarArray(no_of_gen, 0.0, Double.MAX_VALUE, IloNumVarType.Float, cName[t]);
            if (isLP) {
                u[t] = cplex.numVarArray(no_of_gen, 0.0, 1.0, IloNumVarType.Float, uName[t]);
                y[t] = cplex.numVarArray(no_of_gen, 0.0, 1.0, IloNumVarType.Float, yName[t]);
                z[t] = cplex.numVarArray(no_of_gen, 0.0, 1.0, IloNumVarType.Float, zName[t]);
            } else {
                u[t] = cplex.numVarArray(no_of_gen, 0.0, 1.0, IloNumVarType.Int, uName[t]);
                y[t] = cplex.numVarArray(no_of_gen, 0.0, 1.0, IloNumVarType.Int, yName[t]);
                z[t] = cplex.numVarArray(no_of_gen, 0.0, 1.0, IloNumVarType.Int, zName[t]);
            }
        }

        // set u's value according to the initial condition hours and min_on/dn_time
        // define relation between p and u
        for (int i = 0; i < no_of_gen; i++) {
            final double maxP = gens[i].getMaxP();
            final double minP = gens[i].getMinP();
            for (int t = 0; t < no_of_ti; t++) {
                p[t][i].setLB(0.0);
                p[t][i].setUB(maxP);
                if (gens[i].mustON(t)) {
                    u[t][i].setLB(1.0);
                    u[t][i].setUB(1.0);
                } else if (gens[i].mustOFF(t)) {
                    u[t][i].setLB(0.0);
                    u[t][i].setUB(0.0);
                }

                IloLinearNumExpr expr = cplex.linearNumExpr();
                expr.addTerm(p[t][i], 1.0);
                expr.addTerm(u[t][i], -maxP);
                cplex.addLe(expr, 0.0, MessageFormat.format("pu_ub({0},{1})", t, i));
                expr = cplex.linearNumExpr();
                expr.addTerm(p[t][i], 1.0);
                expr.addTerm(u[t][i], -minP);
                cplex.addGe(expr, 0.0, MessageFormat.format("pu_lb({0},{1})", t, i));
            }
        }
    }

    private void addStartupShutdownFlagDef() throws IloException {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // define start up flag, shut down flag.
        for (int i = 0; i < no_of_gen; ++i) {
            // because Ramping Up/Down Constraints are using startup/shutdown flag,
            // I cannot ignore the defination of these flags.
//            if (!gens[i].needStartupShutdownFlag()) continue;

            // for other time intervals, add constraints
            for (int t = 1; t < no_of_ti; t++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                expr.addTerm(y[t][i], 1.0);
                expr.addTerm(z[t][i], 1.0);
                cplex.addLe(expr, 1, MessageFormat.format("updownflag_{0}_{1}_1", t, i));

                expr = cplex.linearNumExpr();
                expr.addTerm(y[t][i], 1.0);
                expr.addTerm(z[t][i], -1.0);
                expr.addTerm(u[t][i], -1.0);
                expr.addTerm(u[t - 1][i], 1.0);
                cplex.addEq(expr, 0, MessageFormat.format("updownflag_{0}_{1}_2", t, i));
            }

            // for the first time interval, update it's bound according the initial power and ramp rate
            IloLinearNumExpr expr = cplex.linearNumExpr();
            expr.addTerm(y[0][i], 1.0);
            expr.addTerm(z[0][i], 1.0);
            cplex.addLe(expr, 1, MessageFormat.format("updownflag_{0}_{1}_1", 0, i));
            final int initialCondionHour = gens[i].getInitialConditionHour();
            int initialStatus;
            if (initialCondionHour > 0) {
                initialStatus = 1;
            } else {
                initialStatus = 0;
            }
            expr = cplex.linearNumExpr();
            expr.addTerm(y[0][i], 1.0);
            expr.addTerm(z[0][i], -1.0);
            expr.addTerm(u[0][i], -1.0);
            cplex.addEq(expr, -initialStatus, MessageFormat.format("updownflag_{0}_{1}_2", 0, i));
        }
    }

    private void addObjFunction() throws IloException {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // objective funciton
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int i = 0; i < no_of_gen; ++i) {
            final double startupCost = gens[i].getStartupCost();
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(c[t][i], 1.);

                // startup cost
                if (startupCost != 0) expr.addTerm(y[t][i], startupCost);
            }
        }
        cplex.addMinimize(expr, "cost");
    }

    private void addUserDefinedLeqConstraint() throws IloException {
        // other leq constraints
        addLeqConstraints(scucData.getConstraints());
    }

    private void addLeqConstraints(final List<LeqConstraint> leqConstraints) throws IloException {
        final int no_of_gen = scucData.getGenNum();

        if (leqConstraints != null)
            for (LeqConstraint con : leqConstraints) {
                int t = con.getTimeInterval();
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int i = 0; i < no_of_gen; ++i) {
                    expr.addTerm(p[t][i], con.getA(i));
                }
                final String name = con.getName();
                if (name != null && name.length() > 0) cplex.addLe(expr, con.getB(), name);
                else cplex.addLe(expr, con.getB());
            }
    }

    private void addRampConstraint() throws IloException {
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
            if (initialCondionHour > 0) {
                int t = 0;
                if (!isRespectStartupShutdownOutput()) {// p[t - 1][i] = initialP > 0
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], 1.0);
                    cplex.addLe(expr, ramp_up + initialP, MessageFormat.format("ramp_{0}_{1}_up", t, i));

                    expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], -1.0);
                    cplex.addLe(expr, ramp_down - initialP, MessageFormat.format("ramp_{0}_{1}_dn", t, i));
                } else {// p[t - 1][i] = initialP > 0, y[t][i] = 0
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], 1.0);
                    cplex.addLe(expr, ramp_up + initialP, MessageFormat.format("ramp_{0}_{1}_up", t, i));

                    expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], -1.0);
                    expr.addTerm(z[t][i], -max_gen_down + ramp_down);
                    cplex.addLe(expr, ramp_down - initialP, MessageFormat.format("ramp_{0}_{1}_dn", t, i));
                }
            } else { // p[t - 1][i] = 0, z[t][i] = 0
                int t = 0;
                if (!isRespectStartupShutdownOutput()) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], 1.0);
                    cplex.addLe(expr, ramp_up, MessageFormat.format("ramp_{0}_{1}_up", t, i));
                } else {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], 1.0);
                    expr.addTerm(y[t][i], -max_gen_up + ramp_up);
                    cplex.addLe(expr, ramp_up, MessageFormat.format("ramp_{0}_{1}_up", t, i));

                    // After analyzing this constraint, we know that it is redundant,
                    // in order to reduce the number of constraints and increase the solving speed,
                    // it is not added into the model.
//                expr = cplex.linearNumExpr();
//                expr.addTerm(p[t][i], -1.0);
//                cplex.addLe(expr, ramp_down, MessageFormat.format("ramp_{0}_{1}_dn", t, i));
                }
            }

            // for other time intervals, add constraints
            for (int t = 1; t < no_of_ti; t++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                expr.addTerm(p[t][i], 1.0);
                expr.addTerm(p[t - 1][i], -1.0);
                if (isRespectStartupShutdownOutput()) expr.addTerm(y[t][i], -max_gen_up + ramp_up);
                cplex.addLe(expr, ramp_up, MessageFormat.format("ramp_{0}_{1}_up", t, i));

                expr = cplex.linearNumExpr();
                expr.addTerm(p[t - 1][i], 1.0);
                expr.addTerm(p[t][i], -1.0);
                if (isRespectStartupShutdownOutput()) expr.addTerm(z[t][i], -max_gen_down + ramp_down);
                cplex.addLe(expr, ramp_down, MessageFormat.format("ramp_{0}_{1}_dn", t, i));
            }
        }

    }

    private void addReserveConstraint() throws IloException {
        final int no_of_ti = scucData.getTiNum();
        // reserve constraint
        final double[] load = scucData.getTotalLoad();
        final double[] reserve = scucData.getReserve();
        if (reserve != null && reserve.length == no_of_ti) {
            final int no_of_gen = scucData.getGenNum();
            Generator[] gens = scucData.getGens();

            for (int t = 0; t < no_of_ti; t++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int i = 0; i < no_of_gen; i++) {
                    expr.addTerm(u[t][i], gens[i].getMaxP());
                }
                cplex.addGe(expr, load[t] + reserve[t], MessageFormat.format("reserve_{0}", t));
            }
        }
    }

    private void addLoadBalanceConstraint() throws IloException {
        final int no_of_ti = scucData.getTiNum();
        // totalLoad balance equation constraint
        final double[] load = scucData.getTotalLoad();
        for (int t = 0; t < no_of_ti; t++) {
            cplex.addEq(cplex.sum(p[t]), load[t], MessageFormat.format("load_banlance_{0}", t));
        }
    }

    private void addGenCostDef() throws IloException {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // relationship between cost and power level
        for (int i = 0; i < no_of_gen; i++) {
            if (gens[i] instanceof GeneratorWithPiecewiseCostCurve) {
                final GeneratorWithPiecewiseCostCurve gen = (GeneratorWithPiecewiseCostCurve) gens[i];
                final double[] breakpoints = gen.getBreakpoints();
                final double[] slopes = gen.getSlopes();

                for (int t = 0; t < no_of_ti; t++) {
                    IloNumVar[] p_seg = new IloNumVar[breakpoints.length - 1];
                    for (int j = 0; j < p_seg.length; j++) {
                        p_seg[j] = cplex.numVar(0, breakpoints[j + 1] - breakpoints[j], IloNumVarType.Float, MessageFormat.format("p({0},{1},{2})", t, i, j));
                    }

                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(p[t][i], 1.0);
                    for (int j = 0; j < p_seg.length; j++) {
                        expr.addTerm(p_seg[j], -1.0);
                    }
                    expr.addTerm(u[t][i], -breakpoints[0]);
                    cplex.addEq(expr, 0.0, MessageFormat.format("def_p_{0}_{1}", t, i));

                    expr = cplex.linearNumExpr();
                    expr.addTerm(c[t][i], -1.0);
                    expr.addTerms(p_seg, slopes);
                    expr.addTerm(u[t][i], gen.getLeftestPoint_y());
                    cplex.addEq(expr, 0.0, MessageFormat.format("def_c_{0}_{1}", t, i));
                }
            } else if (gens[i] instanceof GeneratorWithQuadraticCostCurve) {
                final GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
                final double aConst = gen.getAConstant();
                final double aLinear = gen.getALinear();
                final double aQuadratic = gen.getAQuadratic();

                for (int t = 0; t < no_of_ti; t++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(c[t][i], -1.0);
                    expr.addTerm(u[t][i], aConst);
                    expr.addTerm(p[t][i], aLinear);
                    cplex.addLe(cplex.sum(cplex.prod(p[t][i], p[t][i], aQuadratic), expr), 0
                            , MessageFormat.format("def_c_{0}_{1}", t, i));
                }
            }
        }
    }

    private void addMinDnTimeConstraint() throws IloException {
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

                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int j = t; j < end_ti; j++) {
                    expr.addTerm(u[j][i], 1.0);
                }
                expr.addTerm(z[t][i], min_dn_time);
                cplex.addLe(expr, min_dn_time, MessageFormat.format("min_dn_time_{0}_{1}", t, i));
            }
        }
    }

    private void addMinOnTimeConstraint() throws IloException {
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

                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int j = t; j < end_ti; j++) {
                    expr.addTerm(u[j][i], 1.0);
                }
                expr.addTerm(y[t][i], -min_on_time);
                cplex.addGe(expr, 0, MessageFormat.format("min_on_time_{0}_{1}", t, i));
            }
        }
    }


    private int penaltyCost;

    private void converIntegerVar(final int[][] gen_status) {
        final int no_of_ti = u.length;
        final int no_of_gen = u[0].length;

        try {
            final Generator[] gens = scucData.getGens();
            for (int i = 0; i < no_of_gen; i++) {
                int t = 0;
                fixIntegerVar(i, t, gen_status[i][t] == 1, gens[i].getInitialConditionHour() > 0);

                for (t = 1; t < no_of_ti; t++) {
                    fixIntegerVar(i, t, gen_status[i][t] == 1, gen_status[i][t - 1] == 1);
                }
            }
        } catch (IloException e) {
            log.error(e);
        }
    }

    private void fixIntegerVar(final int i, final int t, final boolean curIsON, final boolean preIsON) throws IloException {
        if (curIsON) {
            u[t][i].setLB(1.0);
            u[t][i].setUB(1.0);
            if (preIsON) {
                y[t][i].setLB(0.0);
                y[t][i].setUB(0.0);
                z[t][i].setLB(0.0);
                z[t][i].setUB(0.0);
            } else {
                y[t][i].setLB(1.0);
                y[t][i].setUB(1.0);
                z[t][i].setLB(0.0);
                z[t][i].setUB(0.0);
            }
        } else {
            u[t][i].setLB(0.0);
            u[t][i].setUB(0.0);
            if (preIsON) {
                y[t][i].setLB(0.0);
                y[t][i].setUB(0.0);
                z[t][i].setLB(1.0);
                z[t][i].setUB(1.0);
            } else {
                y[t][i].setLB(0.0);
                y[t][i].setUB(0.0);
                z[t][i].setLB(0.0);
                z[t][i].setUB(0.0);
            }
        }
    }

    public void solve(final int[][] gen_status, SCUCData scucData) throws InfeasibleException {
        this.scucData = scucData;

        //do the same thing as in beforehand_process();
        try {
            addVariables(true);
            addCommonConstraintsAndObj();
        } catch (IloException e) {
            log.error(e);
        }

        addNetworkConstraints();
        addContingencyConstraints();

        // ed special
        converIntegerVar(gen_status);
        try {
            addHydroUnit4ED(gen_status);
        } catch (IloException e) {
            log.error(e);
        }

        callSolver("EconmicDispatch_FixedIntVar");

        afterward_process();
    }

    private void addHydroUnit4ED(int[][] gen_status) throws IloException {
        final int no_of_ti = u.length;
        final int no_of_gen = u[0].length;
        final Generator[] gens = scucData.getGens();

        for (int i = 0; i < no_of_gen; i++) {
            final Generator gen = gens[i];


        }
    }

    public double[][] getGenOut() {
        return genOutput;
    }

    public int getPenaltyCost() {
        return penaltyCost;
    }

    public void setPenaltyCost(int penalty_cost) {
        this.penaltyCost = penalty_cost;
    }
}