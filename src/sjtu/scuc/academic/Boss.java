package sjtu.scuc.academic;

import gurobi.*;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Double.MAX_VALUE;

/**
 * Created by Zhai Shaopeng on 2017/5/10 17:11.
 * E-mail: zsp1197@163.com
 */
public class Boss {
    List<SCUCData> systems;

    Tielines tielines;

    Parameters parameters;

    Calresult[] results;

    BossMemory bossMemory;

    ANC anc;

    boolean has_been_called=false;
    public Boss(List<SCUCData> systems) {
        this.bossMemory=new BossMemory();
        this.systems = systems;
        writeidx(systems);
        checkNormalization();
        this.results = new Calresult[systems.size()];
    }
    private void checkNormalization() {
        final int no_of_sys = systems.size();
        double normalization=systems.get(0).getNormalization();
        for (int si = 0; si < no_of_sys; si++) {
            if(normalization!=systems.get(si).getNormalization()){
                throw new java.lang.Error("normalizion error!");
            }
        }
    }

    public void boss_work() {
        if(!has_been_called){
            has_been_called=true;
        }else {
            throw new java.lang.Error("boss_?? has been called! This function must be called firstly");
        }
        parameters.print();
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();

        initializeTieline();
        refine_sysload_with_tieline();
        iwantMOUC();
        bossMemory.add_memory(results,tielines);
        do {
            updateTielines();
            refine_sysload_with_tieline();
            for (int si = 0; si < no_of_sys; si++) {
                SCUCSolver scucSolver = new SCUCSolver();
                MIPGurobi scucAlg = new MIPGurobi();
                scucSolver.setScucAlg(scucAlg);
                results[si] = scucSolver.optimize(systems.get(si));
            }
            bossMemory.add_memory(results,tielines);

        } while (keeponWorking());
    }

    public void boss_ANC(){
        if(!has_been_called){
            has_been_called=true;
        }else {
            throw new java.lang.Error("boss_?? has been called! This function must be called firstly");
        }
        iwantMOUC();
        anc=new ANC(systems,tielines);
        anc.anc_work();
    }

    private boolean keeponWorking() {
        System.out.println("check!");
        double[] a=bossMemory.get_cost_history();
        for (int i = 0; i < a.length; i++) {
            System.out.print(Double.toString(a[i])+" ");
        }
        System.out.println();
        return true;
    }


    private void updateTielines() {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        double[][] mprices = getMprices();
        double[][] deltaSysLoad = getMaxDelta(mprices);
        try {
            tielines.deltaSetTielines(solveTielines(deltaSysLoad,"update"));
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public double[][] getMprices(){
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        double[][] mprices = new double[no_of_sys][no_of_ti];
        for (int si = 0; si < no_of_sys; si++) {
            SCUCData scucData = systems.get(si);
            if(scucData.getTargetflag()!=3){
                throw new java.lang.Error("targetFlag!=3? are u sure?");
            }
            Differentiation devi = new Differentiation(scucData);
            mprices[si] = devi.getMpriceTs(results[si]);
        }
        return mprices;
    }

    private double[][] getMaxDelta(double[][] mprices) {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        double[][] result = new double[no_of_sys][no_of_ti];
        for (int si = 0; si < no_of_sys; si++) {
            for (int t = 0; t < no_of_ti; t++) {
                result[si][t] = getMaxDeltaAtT(si, t, mprices);
            }
        }
        return result;
    }

    private double getMaxDeltaAtT(int si, int t, double[][] mprices) {
        final int no_of_sys = systems.size();
        double[] maxdeltas = new double[no_of_sys];
        double coefficence = parameters.getCoefficient();
        for (int i = 0; i < no_of_sys; i++) {
            if (i == si) {
//            对角元素不能用
                maxdeltas[i] = 0;
            }else{
                double a=Math.abs(mprices[si][t]);
                double b=Math.abs(mprices[i][t]);
                maxdeltas[i] = coefficence * systems.get(si).getTotalLoad()[t] * (a-b) / Math.max(a,b);
//            maxdeltas[i] = coefficence * systems.get(si).getTotalLoad()[t] * ((mprices[si][t] - mprices[i][t]) / Math.max(mprices[si][t], mprices[i][t]));
                maxdeltas[i] = Math.abs(maxdeltas[i]);
            }
        }
        return get_max_value_4_doubles(maxdeltas);
    }

    public double get_max_value_4_doubles(double[] doubles) {
        List<Double> list = new ArrayList<Double>();
        for (int i = 0; i < doubles.length; i++) {
            list.add(doubles[i]);
        }
        return Collections.max(list);
    }

    public void iwantMOUC() {
        final int no_of_sys = systems.size();
        for (int si = 0; si < no_of_sys; si++) {
            SCUCSolver scucSolver = new SCUCSolver();
            MIPGurobi scucAlg = new MIPGurobi();
            scucSolver.setScucAlg(scucAlg);
            SCUCData scucData = systems.get(si);
            scucData.setTargetflag(1);
            Calresult result1 = scucSolver.optimize(scucData);
            scucData.setTargetflag(2);
            Calresult result2 = scucSolver.optimize(scucData);
            scucData.setResult1(result1);
            scucData.setResult2(result2);
            scucData.setTargetflag(3);
            results[si] = scucSolver.optimize(scucData);
        }
        refineResult12_for_normalize();
    }

    private void refineResult12_for_normalize() {
        double f1=0;
        double f2=0;
        double for_normalize=systems.get(0).getNormalization();
        final int no_of_sys = systems.size();
        for (int si = 0; si < no_of_sys; si++) {
            f1=f1+systems.get(si).getResult1().getBestObjValue();
            f2=f2+systems.get(si).getResult2().getBestObjValue();
        }
        f1=for_normalize*f1/no_of_sys;
        f2=for_normalize*f2/no_of_sys;
        for (int si = 0; si < no_of_sys; si++) {
            systems.get(si).getResult1().setBestObjValue(f1);
            systems.get(si).getResult2().setBestObjValue(f2);
        }
    }
    private void writeidx(List<SCUCData> systems) {
        for (int i = 0; i < systems.size(); i++) {
            systems.get(i).setIndex(i);
        }
    }

    public void setTieMax_with_love(double themax) {
        double[][] tiemax = new double[systems.size()][systems.size()];
        for (int i = 0; i < systems.size(); i++) {
            Arrays.fill(tiemax[i], themax);
            tiemax[i][i] = 0;
        }
        tielines = new Tielines(tiemax);
    }

    public void refine_sysload_with_tieline() {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        for (int si = 0; si < no_of_sys; si++) {
            double[] oriload = systems.get(si).getTotalLoad();
            double[] load = new double[no_of_ti];
            Arrays.fill(load, 0);
            for (int t = 0; t < no_of_ti; t++) {
//                要往外面传功率，等效于本地负荷增大
                load[t] = oriload[t] + tielines.getSysOut(si, t);
            }
            systems.get(si).setTotalLoad(load);
        }
    }

    public void initializeTieline() {
        final int no_of_ti = systems.get(0).getTiNum();
        double totalSyscapability = 0;
        double[] totalSysLoad = new double[no_of_ti];
        double[][] dreamSysLoad = new double[systems.size()][no_of_ti];
        double[][] deltaSysLoad = new double[systems.size()][no_of_ti];
//        get totalSyscapability
        for (int i = 0; i < systems.size(); i++) {
            totalSyscapability = totalSyscapability + systems.get(i).getCapability();
        }
//        get totalSysLoad
        for (int t = 0; t < systems.get(0).getTiNum(); t++) {
            for (int i = 0; i < systems.size(); i++) {
                totalSysLoad[t] = totalSysLoad[t] + systems.get(i).getTotalLoad()[t];
            }
        }
//        get dreamSysLoad
        for (int t = 0; t < no_of_ti; t++) {
            for (int si = 0; si < systems.size(); si++) {
                dreamSysLoad[si][t] = totalSysLoad[t] * (systems.get(si).getCapability() / totalSyscapability);
            }
        }
//        get deltaSysLoad
        for (int t = 0; t < no_of_ti; t++) {
            for (int si = 0; si < systems.size(); si++) {
                deltaSysLoad[si][t] = systems.get(si).getTotalLoad()[t] - dreamSysLoad[si][t];
            }
        }
//        solve to get tieline
        try {
//            tielines.setTielines((solveTielines(deltaSysLoad, "initialize")));
            tielines.setTielines_with_maxRefine((solveTielines(deltaSysLoad, "initialize")));
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public double[][][] solveTielines(double[][] deltaSysLoad, String mode) throws GRBException {
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);
        model.getEnv().set(GRB.IntParam.OutputFlag, 0);
        final int no_of_sys = systems.size();
        final int no_of_ti = systems.get(0).getTiNum();
        GRBVar[][][] x = new GRBVar[no_of_sys][no_of_sys][no_of_ti];
        GRBVar[][] f = new GRBVar[no_of_sys][no_of_ti];
//        1
        for (int i = 0; i < no_of_sys; i++) {
            for (int j = 0; j < no_of_sys; j++) {
                for (int t = 0; t < no_of_ti; t++) {
                    if (i != j) {
                        x[i][j][t] = model.addVar(-tielines.getTieMax()[i][j], tielines.getTieMax()[i][j], 0, GRB.CONTINUOUS, null);
                    } else {
                        x[i][j][t] = model.addVar(0, 0, 0.0, GRB.CONTINUOUS, null);
                    }
                }
            }
        }
//        2
        for (int i = 0; i < no_of_sys; i++) {
            for (int j = 0; j < no_of_sys; j++) {
                for (int t = 0; t < no_of_ti; t++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, x[i][j][t]);
                    expr.addTerm(1.0, x[j][i][t]);
                    model.addConstr(expr, GRB.EQUAL, 0, null);
                }
            }
        }
//        3
        for (int t = 0; t < no_of_ti; t++) {
            for (int i = 0; i < no_of_sys; i++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int j = 0; j < no_of_sys; j++) {
                    expr.addTerm(1.0, x[i][j][t]);
                }
                model.addConstr(expr, GRB.GREATER_EQUAL, -Math.abs(deltaSysLoad[i][t]), null);
                model.addConstr(expr, GRB.LESS_EQUAL, Math.abs(deltaSysLoad[i][t]), null);
            }
        }
        if (mode == "initialize") {
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    f[i][t] = model.addVar(-MAX_VALUE, MAX_VALUE, 0, GRB.CONTINUOUS, null);
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int j = 0; j < no_of_sys; j++) {
                        expr.addTerm(1.0, x[i][j][t]);
                    }
                    expr.addConstant(deltaSysLoad[i][t]);
                    model.addConstr(expr, GRB.EQUAL, f[i][t], null);
                }
            }
            GRBQuadExpr exprq = new GRBQuadExpr();
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    exprq.addTerm(1, f[i][t], f[i][t]);
                }
            }
            model.setObjective(exprq, GRB.MINIMIZE);
            model.update();
            model.optimize();
            double[][][] result = new double[no_of_sys][no_of_sys][no_of_ti];
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    for (int j = 0; j < no_of_sys; j++) {
                        result[i][j][t] = x[i][j][t].get(GRB.DoubleAttr.X);
                    }
                }
            }
            model.dispose();
            env.dispose();
            return result;
        } else if (mode == "update") {
            double[][] mprices=getMprices();
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    f[i][t] = model.addVar(-MAX_VALUE, MAX_VALUE, 0, GRB.CONTINUOUS, null);
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int j = 0; j < no_of_sys; j++) {
                        expr.addTerm(mprices[i][t], x[i][j][t]);
                    }
                    model.addConstr(expr, GRB.EQUAL, f[i][t], null);
                }
            }
            GRBLinExpr expr = new GRBLinExpr();
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    expr.addTerm(1, f[i][t]);
                }
            }
            model.setObjective(expr, GRB.MINIMIZE);
            model.update();
            model.optimize();

            double[][][] result = new double[no_of_sys][no_of_sys][no_of_ti];
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    for (int j = 0; j < no_of_sys; j++) {
                        result[i][j][t] = x[i][j][t].get(GRB.DoubleAttr.X);
                    }
                }
            }
            System.out.println("联络线对应成本增加应该是负的："+Double.toString(model.get(GRB.DoubleAttr.ObjVal)));
            model.dispose();
            env.dispose();
            return result;
        } else {
            model.dispose();
            env.dispose();
            throw new java.lang.Error("mode is wrong!!");
        }
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public List<SCUCData> getSystems() {
        return systems;
    }

    public Tielines getTielines() {
        return tielines;
    }

    public ANC getAnc() {
        return anc;
    }
}
