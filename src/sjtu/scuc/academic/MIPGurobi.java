package sjtu.scuc.academic;

import gurobi.GRBEnv;
import gurobi.GRBModel;
import ilog.concert.IloException;
import gurobi.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/9 16:54.
 * E-mail: zsp1197@163.com
 */
public class MIPGurobi extends SCUCAlg implements EconomicDispatchable {

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
     * gen's cost
     */
    private GRBVar[][] c;
    /**
     * gen's gas cost
     */
    private GRBVar[][] cg;
    /**
     * gen's start up flag. start up:1
     */
    private GRBVar[][] y;
    /**
     * gen's shut down flag. shut down:1
     */
    private GRBVar[][] z;
    //    private GRBVar[][] D;
    private GRBVar f1;
    private GRBVar f2;

    private List<GRBVar[]> vList = new ArrayList<GRBVar[]>();
    private List<GRBVar[]> qList = new ArrayList<GRBVar[]>();

    //    SCUCData scucData;
    protected void beforehand_process(SCUCData scucData) {
        this.scucData = scucData;
        try {
//            把p,c变为字符串
            addVariables(false);
            try {
                addCommonConstraintsAndObj();
            } catch (GRBException e) {
                e.printStackTrace();
            }
        } catch (IloException e) {
            log.error(e);
        }
    }

    private void addCommonConstraintsAndObj() throws IloException, GRBException {

//        addGenCostDef();
        System.out.println("targetflag is " + scucData.getTargetflag());
        if (scucData.getTargetflag() == 1) {
//            addGenCostDef2();
            addGenCostDef();
        } else if (scucData.getTargetflag() == 2) {
            addGasCostDef();
        } else {
            addGenCostDef();
            addGasCostDef();
        }
//        addGenCostDef2();
//        addGenGasCostDef();
        addStartupShutdownFlagDef();

        addLoadBalanceConstraint();
        addReserveConstraint();
        if (scucData.isRamp() == true) {
            addRampConstraint();
            System.out.println("get ramp!!");
        }


        addMinOnTimeConstraint();
        addMinDnTimeConstraint();

//        addUserDefinedLeqConstraint();
        if ((scucData.getTargetflag() == 1) || (scucData.getTargetflag() == 2)) {
            addObjFunction();
        } else if (scucData.getTargetflag() == 3) {
            addMObj();
//            addObjFunctionNBI();
        } else if (scucData.getTargetflag() == 4) {
//            意味着，这个多目标是为ANC而设，归一化部分改变策略！
            addMObjANC();
        } else {

        }
//        addObjFunctionwithsimplePlus();
//        addObjFunctionPlus();
    }

    private void addMObjANC() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // objective funciton
        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < no_of_gen; ++i) {
            double startupCost = gens[i].getStartupCost();
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(1., c[t][i]);

                if (startupCost != 0) expr.addTerm(startupCost, y[t][i]);
            }
        }
        try {
            gurobigo.addConstr(expr, GRB.LESS_EQUAL, f1, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }

        expr = new GRBLinExpr();
        for (int i = 0; i < no_of_gen; ++i) {
            double startupCost = gens[i].getStartupCost();
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(1., cg[t][i]);
            }
        }
        try {
            gurobigo.addConstr(expr, GRB.LESS_EQUAL, f2, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }
//        add onj
        GRBQuadExpr exprq = new GRBQuadExpr();
//        保证统一的归一化
//        if((result1==0)||(result2==0)){
//            throw new java.lang.Error("multi-obj haven't been configured!");
//        }
//        double result1=Tools.getObjValue(scucData.getResult1(),scucData,1);
//        double result2=Tools.getObjValue(scucData.getResult2(),scucData,2);
        exprq.addTerm(scucData.getNormalize_coefficentes()[0], f1, f1);
        exprq.addTerm(scucData.getNormalize_coefficentes()[1], f2, f2);
        try {
            gurobigo.setObjective(exprq, GRB.MINIMIZE);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addMObj() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // objective funciton
        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < no_of_gen; ++i) {
            double startupCost = gens[i].getStartupCost();
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(1., c[t][i]);

                if (startupCost != 0) expr.addTerm(startupCost, y[t][i]);
            }
        }
        try {
            gurobigo.addConstr(expr, GRB.EQUAL, f1, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }

        expr = new GRBLinExpr();
        for (int i = 0; i < no_of_gen; ++i) {
            double startupCost = gens[i].getStartupCost();
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(1., cg[t][i]);
            }
        }
        try {
            gurobigo.addConstr(expr, GRB.EQUAL, f2, null);
        } catch (GRBException e) {
            e.printStackTrace();
        }
//        add obj
        GRBQuadExpr exprq = new GRBQuadExpr();
//        保证统一的归一化
//        double result1=scucData.getResult1().getBestObjValue();
//        double result2=scucData.getResult2().getBestObjValue();
//        if((result1==0)||(result2==0)){
//            throw new java.lang.Error("multi-obj haven't been configured!");
//        }
        exprq.addTerm(scucData.getNormalize_coefficentes()[0], f1, f1);
        exprq.addTerm(scucData.getNormalize_coefficentes()[1], f2, f2);
        try {
            gurobigo.setObjective(exprq, GRB.MINIMIZE);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addObjFunctionNBI() throws IloException {

    }

    private void addNBIcons() throws IloException, GRBException {

    }

    private void addGasCostDef() throws IloException, GRBException {

//        int k=100;
        int k = scucData.getKsplit();
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // relationship between cost and power level
        for (int i = 0; i < no_of_gen; i++) {
            final GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
            final double aConst = gen.getGasc();
            final double aLinear = gen.getGasb();
            final double aQuadratic = gen.getGasa();
            final double minP = gen.getMinP();
            final double maxP = gen.getMaxP();
            final double step = (gen.getMaxP() - gen.getMinP()) / k;
            double start = minP;
            double diff;
            double b;
            double y;
            GRBLinExpr expr = new GRBLinExpr();
            for (int t = 0; t < no_of_ti; t++) {
//                    expr = cplex.linearNumExpr();
//                    expr.addTerm(c[t][i], -1.0);
//                    expr.addTerm(u[t][i], aConst);
//                    expr.addTerm(p[t][i], aLinear);
//                    cplex.addLe(cplex.sum(cplex.prod(p[t][i], p[t][i], aQuadratic), expr), 0
//                            , MessageFormat.format("def_c_{0}_{1}", t, i));
                start = minP;
                for (double j = 0; j < k; j++) {
                    start = minP + j * step;
                    y = aQuadratic * start * start + aLinear * start + aConst;
                    diff = 2 * aQuadratic * start + aLinear;
                    b = y - diff * start;
                    expr = new GRBLinExpr();
                    if ((scucData.getTargetflag() == 1) || (scucData.getTargetflag() == 2)) {
                        expr.addTerm(-1.0, c[t][i]);
                    } else {
                        expr.addTerm(-1.0, cg[t][i]);
                    }
                    expr.addTerm(diff, p[t][i]);
                    expr.addTerm(b, u[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, 0, null);
                }
            }
        }
//        gurobigo.update();
    }


    @Override
    protected void beforehand_process() {
        try {
//            把p,c变为字符串
            addVariables(false);
            try {
                addCommonConstraintsAndObj();
            } catch (GRBException e) {
                e.printStackTrace();
            }
        } catch (IloException e) {
            log.error(e);
        }
    }

    protected void afterward_process() {
        gurobigo.dispose();
        try {
            env.dispose();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    protected void addCuts(List<LeqConstraint> cuts) {
        try {
            addLeqConstraints(cuts);
        } catch (IloException e) {
            log.error(e);
        }
    }

    @Override
    protected Calresult callSolver(String s) throws InfeasibleException {
        return callSolver(s, scucData);
    }

    protected Calresult callSolver(final String s, SCUCData scucData) throws InfeasibleException {
        this.scucData = scucData;
        final int no_of_gen = this.scucData.getGenNum();
        final int no_of_ti = this.scucData.getTiNum();
        double[] tempsum = new double[no_of_ti];
        double temp = 0;
        genStatus = new int[no_of_gen][no_of_ti];
        int[][] genY = new int[no_of_gen][no_of_ti];
        try {
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
            calresult = new Calresult(Tools.deepcopy2D_IntArray(genStatus), Tools.deepcopy2D_DoubleArray(genOutput), gurobigo.get(GRB.DoubleAttr.ObjVal), genY);
        } catch (GRBException e) {
            e.printStackTrace();
        }
        calresult.setTargetflag(scucData.getTargetflag());
        if((scucData.getTargetflag()!=1)||(scucData.getTargetflag()!=2)){
            try {
                calresult.setF1_gurobi(f1.get(GRB.DoubleAttr.X));
                calresult.setF2_gurobi(f2.get(GRB.DoubleAttr.X));
            } catch (GRBException e) {
                e.printStackTrace();
            }
        }
        gurobigo.dispose();
        try {
            env.dispose();
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return calresult;
    }

    protected void addBranchPowerflowConstraints(final int no_of_branch, final Branch[] branchs, final double[][] PTDF, final String constraintName) {
//        final List<BusLoad> busLoadList = scucData.getBusLoadList();
//        final BusLoad busLoads[] = busLoadList.toArray(new BusLoad[busLoadList.size()]);
//
//        final int no_of_gen = scucData.getGenNum();
//        final int no_of_ti = scucData.getTiNum();
//        Generator[] gens = scucData.getGens();
//        for (int t = 0; t < no_of_ti; t++) {
//            for (int i = 0; i < no_of_branch; i++) {//check
//                try {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    for (int j = 0; j < no_of_gen; j++) {
//                        expr.addTerm(p[t][j], PTDF[i][gens[j].getBusIdx()]);
//                    }
//
//                    double loadContribute = 0;
//                    for (BusLoad busLoad : busLoads) {
//                        final int bus_idx = busLoad.getBusIdx();
//                        final double[] load = busLoad.getLoad();
//                        loadContribute += PTDF[i][bus_idx] * (-load[t]);
//                    }
//
//                    cplex.addLe(expr, branchs[i].getCapacity() - loadContribute, MessageFormat.format("{2}_t{0}_b{1}_ub", t, i, constraintName));
//                    cplex.addGe(expr, -branchs[i].getCapacity() - loadContribute, MessageFormat.format("{2}_t{0}_b{1}_lb", t, i, constraintName));
//                } catch (IloException e) {
//                    log.error(e);
//                }
//            }
//        }
    }

    private void addVariables(boolean isLP) throws IloException {

        try {
            env = new GRBEnv();
            gurobigo = new GRBModel(env);
            gurobigo.getEnv().set(GRB.IntParam.OutputFlag, 0);
            f1 = gurobigo.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "f1");
            f2 = gurobigo.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "f2");
//        cplex.setParam(IloCplex.DouParam.MIP.tolerances.EpGap, gapTolerance);
//        cplex.setParam(IloCplex.DoubleParam.EpGap, 0.03);
            final int no_of_gen = scucData.getGenNum();
            final int no_of_ti = scucData.getTiNum();
            Generator[] gens = scucData.getGens();

            // create variables    GRBVar为0/1变量
            p = new GRBVar[no_of_ti][no_of_gen];
            String[][] pName = new String[no_of_ti][no_of_gen];
            u = new GRBVar[no_of_ti][no_of_gen];
            String[][] uName = new String[no_of_ti][no_of_gen];
            c = new GRBVar[no_of_ti][no_of_gen];
            String[][] cName = new String[no_of_ti][no_of_gen];
            cg = new GRBVar[no_of_ti][no_of_gen];
            String[][] cgName = new String[no_of_ti][no_of_gen];
            y = new GRBVar[no_of_ti][no_of_gen];
            String[][] yName = new String[no_of_ti][no_of_gen];
            z = new GRBVar[no_of_ti][no_of_gen];
            String[][] zName = new String[no_of_ti][no_of_gen];
//            D = new GRBVar[1][1];
//            String[][] DName = new String[1][1];
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_gen; i++) {
                    pName[t][i] = MessageFormat.format("p({0},{1})", t, i);
                    uName[t][i] = MessageFormat.format("u({0},{1})", t, i);
                    cName[t][i] = MessageFormat.format("c({0},{1})", t, i);
                    cgName[t][i] = MessageFormat.format("cg({0},{1})", t, i);
                    yName[t][i] = MessageFormat.format("y({0},{1})", t, i);
                    zName[t][i] = MessageFormat.format("z({0},{1})", t, i);
//                    DName[0][0] = MessageFormat.format("D({0},{1})", 0, 0);

                    p[t][i] = gurobigo.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, null);
                    u[t][i] = gurobigo.addVar(0.0, 1, 0.0, GRB.BINARY, null);
                    c[t][i] = gurobigo.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, null);
                    cg[t][i] = gurobigo.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, null);
                    y[t][i] = gurobigo.addVar(0.0, 1, 0.0, GRB.BINARY, null);
                    z[t][i] = gurobigo.addVar(0.0, 1, 0.0, GRB.BINARY, null);
                }
//                D[0][0] = gurobigo.addVar(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, null);
            }
            // set u's value according to the initial condition hours and min_on/dn_time
            // define relation between p and u
            for (int i = 0; i < no_of_gen; i++) {
                final double maxP = gens[i].getMaxP();
                final double minP = gens[i].getMinP();
                for (int t = 0; t < no_of_ti; t++) {
//                    p[t][i] = gurobigo.addVar(0.0, maxP, 0.0, GRB.CONTINUOUS, null);
//                    下面这段注释完全不知道他想干嘛
//                    if (gens[i].mustON(t)) {
//                        u[t][i] = gurobigo.addVar(1, 1, 0.0, GRB.BINARY, null);
//                    } else if (gens[i].mustOFF(t)) {
//                        u[t][i] = gurobigo.addVar(0, 0, 0.0, GRB.BINARY, null);
//                    }
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    expr.addTerm(-maxP, u[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, 0, null);
                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, p[t][i]);
                    expr.addTerm(-minP, u[t][i]);
                    gurobigo.addConstr(expr, GRB.GREATER_EQUAL, 0, null);
                    gurobigo.update();
                }
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addStartupShutdownFlagDef() throws IloException {
        try {
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
//            gurobigo.update();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addObjFunction() {
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // objective funciton
        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < no_of_gen; ++i) {
            double startupCost = gens[i].getStartupCost();
            for (int t = 0; t < no_of_ti; t++) {
                expr.addTerm(1., c[t][i]);
                if (scucData.getTargetflag() == 1)
                    if (startupCost != 0) expr.addTerm(startupCost, y[t][i]);
            }
        }
//        try {
//            gurobigo.addConstr(expr,GRB.LESS_EQUAL,f1,null);
//        } catch (GRBException e) {
//            e.printStackTrace();
//        }
//        GRBQuadExpr exprq = new GRBQuadExpr();
//        exprq.addTerm(1,f1,f1);
//        try {
//            gurobigo.setObjective(exprq, GRB.MINIMIZE);
//        } catch (GRBException e) {
//            e.printStackTrace();
//        }
        try {
            gurobigo.setObjective(expr, GRB.MINIMIZE);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }


    private void addLeqConstraints(final List<LeqConstraint> leqConstraints) throws IloException {
        final int no_of_gen = scucData.getGenNum();
        try {
            if (leqConstraints != null)
                for (LeqConstraint con : leqConstraints) {
                    int t = con.getTimeInterval();
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int i = 0; i < no_of_gen; ++i) {
                        expr.addTerm(con.getA(i), p[t][i]);
                    }
                    final String name = con.getName();
                    if (name != null && name.length() > 0) gurobigo.addConstr(expr, GRB.LESS_EQUAL, con.getB(), null);
                    else gurobigo.addConstr(expr, GRB.LESS_EQUAL, con.getB(), null);
                }
//            gurobigo.update();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addRampConstraint() throws IloException, GRBException {
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
//                expr = cplex.linearNumExpr();
//                expr.addTerm(p[t][i], -1.0);
//                cplex.addLe(expr, ramp_down, MessageFormat.format("ramp_{0}_{1}_dn", t, i));
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
//        gurobigo.update();
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
                GRBLinExpr expr = new GRBLinExpr();
                for (int i = 0; i < no_of_gen; i++) {
                    expr.addTerm(gens[i].getMaxP(), u[t][i]);
                }
                try {
                    gurobigo.addConstr(expr, GRB.GREATER_EQUAL, reserve[t] +load[t], null);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
//                cplex.addGe(expr, load[t] + reserve[t], MessageFormat.format("reserve_{0}", t));
            }
        }
        try {
            gurobigo.update();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    private void addLoadBalanceConstraint() throws IloException, GRBException {
        final int no_of_ti = scucData.getTiNum();
        final int no_of_gen = scucData.getGenNum();
        // totalLoad balance equation constraint
        final double[] load = scucData.getTotalLoad();
        GRBLinExpr expr = new GRBLinExpr();
        for (int t = 0; t < no_of_ti; t++) {
            expr = new GRBLinExpr();
            for (int i = 0; i < no_of_gen; i++) {
                expr.addTerm(1, p[t][i]);
            }
            gurobigo.addConstr(expr, GRB.EQUAL, load[t], null);
        }
//        gurobigo.update();
    }

    private void addGenCostDef() throws IloException, GRBException {
        int k = scucData.getKsplit();
        final int no_of_gen = scucData.getGenNum();
        final int no_of_ti = scucData.getTiNum();
        Generator[] gens = scucData.getGens();
        // relationship between cost and power level
        for (int i = 0; i < no_of_gen; i++) {
            final GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
            final double aConst = gen.getAConstant();
            final double aLinear = gen.getALinear();
            final double aQuadratic = gen.getAQuadratic();
            final double minP = gen.getMinP();
            final double maxP = gen.getMaxP();
            final double step = (gen.getMaxP() - gen.getMinP()) / k;
            double start = minP;
            double diff;
            double b;
            double y_notflag = 0;
            GRBLinExpr expr = new GRBLinExpr();
            for (int t = 0; t < no_of_ti; t++) {
                start = minP;
                double startupCost = gens[i].getStartupCost();
                if (startupCost != 0) expr.addTerm(startupCost, y[t][i]);
                for (double j = 0; j < k; j++) {
                    start = minP + j * step;
                    y_notflag = aQuadratic * start * start + aLinear * start + aConst;
                    diff = 2 * aQuadratic * start + aLinear;
                    b = y_notflag - diff * start;
                    expr = new GRBLinExpr();
                    expr.addTerm(-1.0, c[t][i]);
                    expr.addTerm(diff, p[t][i]);
                    expr.addTerm(b, u[t][i]);
                    gurobigo.addConstr(expr, GRB.LESS_EQUAL, 0, null);
//                    gurobigo.update();

                }
            }
        }
    }

    private void addMinDnTimeConstraint() throws IloException, GRBException {
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
                gurobigo.addConstr(expr, GRB.LESS_EQUAL, min_dn_time, null);
            }
        }
//        gurobigo.update();
    }

    private void addMinOnTimeConstraint() throws IloException, GRBException {
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
                gurobigo.addConstr(expr, GRB.GREATER_EQUAL, 0, null);
            }
        }
//        gurobigo.update();
    }


    private int penaltyCost;

    private void converIntegerVar(final int[][] gen_status) {
//        final int no_of_ti = u.length;
//        final int no_of_gen = u[0].length;
//
//        try {
//            final Generator[] gens = scucData.getGens();
//            for (int i = 0; i < no_of_gen; i++) {
//                int t = 0;
//                fixIntegerVar(i, t, gen_status[i][t] == 1, gens[i].getInitialConditionHour() > 0);
//
//                for (t = 1; t < no_of_ti; t++) {
//                    fixIntegerVar(i, t, gen_status[i][t] == 1, gen_status[i][t - 1] == 1);
//                }
//            }
//        } catch (IloException e) {
//            log.error(e);
//        }
    }

//    private void fixIntegerVar(final int i, final int t, final boolean curIsON, final boolean preIsON) throws IloException {
//        if (curIsON) {
//            u[t][i].setLB(1.0);
//            u[t][i].setUB(1.0);
//            if (preIsON) {
//                y[t][i].setLB(0.0);
//                y[t][i].setUB(0.0);
//                z[t][i].setLB(0.0);
//                z[t][i].setUB(0.0);
//            } else {
//                y[t][i].setLB(1.0);
//                y[t][i].setUB(1.0);
//                z[t][i].setLB(0.0);
//                z[t][i].setUB(0.0);
//            }
//        } else {
//            u[t][i].setLB(0.0);
//            u[t][i].setUB(0.0);
//            if (preIsON) {
//                y[t][i].setLB(0.0);
//                y[t][i].setUB(0.0);
//                z[t][i].setLB(1.0);
//                z[t][i].setUB(1.0);
//            } else {
//                y[t][i].setLB(0.0);
//                y[t][i].setUB(0.0);
//                z[t][i].setLB(0.0);
//                z[t][i].setUB(0.0);
//            }
//        }
//    }

    public void solve(final int[][] gen_status, SCUCData scucData) throws InfeasibleException {
        this.scucData = scucData;

        //do the same thing as in beforehand_process();

        try {
            addVariables(true);
            addCommonConstraintsAndObj();
        } catch (IloException e) {
            e.printStackTrace();
        } catch (GRBException e) {
            e.printStackTrace();
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

        callSolver("EconmicDispatch_FixedIntVar", scucData);

        afterward_process();
    }

    private void addHydroUnit4ED(int[][] gen_status) throws IloException {
//        final int no_of_ti = u.length;
//        final int no_of_gen = u[0].length;
//        final Generator[] gens = scucData.getGens();
//
//        for (int i = 0; i < no_of_gen; i++) {
//            final Generator gen = gens[i];
//
//            if (gen instanceof HydroUnit) {
//                final HydroUnit hydroUnit = (HydroUnit) gen;
//                String[] vName = new String[no_of_ti];
//                String[] qName = new String[no_of_ti];
//                for (int t = 0; t < no_of_ti; t++) {
//                    vName[t] = MessageFormat.format("V({0},{1})", t, i);
//                    qName[t] = MessageFormat.format("q({0},{1})", t, i);
//                }
//                GRBVar[] V = cplex.numVarArray(no_of_ti, hydroUnit.getMinV(), hydroUnit.getMaxV(), GRBVarType.Float, vName);
//                GRBVar[] q = cplex.numVarArray(no_of_ti, 0.0, hydroUnit.getMaxQ(), GRBVarType.Float, qName);
//                //
//                vList.add(V);
//                qList.add(q);
//
//                // add relation between p and q
//                final double aConst = hydroUnit.getP_Q_Constant();
//                final double aLinear = hydroUnit.getP_Q_Linear();
//                final double aQuadratic = hydroUnit.getP_Q_Quadritic();
//                for (int t = 0; t < no_of_ti; t++) {
//                    c[t][i].setLB(0.0);
//                    c[t][i].setUB(0.0);
//                    if (gen_status[i][t] == 0) {
//                        p[t][i].setLB(0.0);
//                        p[t][i].setUB(0.0);
//                        q[t].setLB(0.0);
//                        q[t].setUB(0.0);
//                    } else {
//                        q[t].setLB(hydroUnit.getMinQ());
//                        q[t].setUB(hydroUnit.getMaxQ());
//                        IloLinearNumExpr expr = cplex.linearNumExpr();
//                        expr.addTerm(p[t][i], -1.0);
//                        expr.addTerm(q[t], aLinear);
//                        cplex.addGe(cplex.sum(cplex.prod(q[t], q[t], aQuadratic), expr), -aConst
//                                , MessageFormat.format("def_c_{0}_{1}", t, i));
//                    }
//                }
//
//                // add relation between V and q
//                final double r = hydroUnit.getInflow();
//                {
//                    final double V0 = hydroUnit.getV0();
//                    int t = 0;
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(V[t], 1.0);
//                    expr.addTerm(q[t], 1);
//                    cplex.addEq(expr, r + V0, MessageFormat.format("V({0},{1})", t, i));
//
//                    final double VT = hydroUnit.getVT();
//                    if (VT > 0) {
//                        V[no_of_ti - 1].setLB(VT);
//                        V[no_of_ti - 1].setUB(VT);
//                    }
//                }
//                for (int t = 1; t < no_of_ti; t++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(V[t], 1.0);
//                    expr.addTerm(V[t - 1], -1);
//                    expr.addTerm(q[t], 1);
//                    cplex.addEq(expr, r, MessageFormat.format("V({0},{1})", t, i));
//                }
//
//            }
//        }
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
