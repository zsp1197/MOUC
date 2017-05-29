package sjtu.scuc.academic;

import gurobi.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.doubleToLongBits;

/**
 * Created by Zhai Shaopeng on 2017/5/10 17:11.
 * E-mail: zsp1197@163.com
 */
public class Boss implements Serializable {
    List<SCUCData> systems;

    Tielines tielines;

    Parameters parameters;

    Calresult[] results;

    BossMemory bossMemory;

    ANC anc;

    boolean has_been_called = false;

    public Boss(List<SCUCData> systems) {
        this.bossMemory = new BossMemory();
        this.systems = systems;
        writeidx(systems);
        checkNormalization();
        this.results = new Calresult[systems.size()];
    }

    private void checkNormalization() {
        final int no_of_sys = systems.size();
        double normalization = systems.get(0).getNormalization();
        for (int si = 0; si < no_of_sys; si++) {
            if (normalization != systems.get(si).getNormalization()) {
                throw new java.lang.Error("normalizion error!");
            }
        }
    }

    public void boss_work(Tielines gived_tielines) {
        int step = 0;
        parameters.print();
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        iwantMOUC();
        bossMemory.setNomalize_coefficentes(systems.get(0).getNormalize_coefficentes());
        if (gived_tielines == null) {
            if (!has_been_called) {
                has_been_called = true;
            } else {
                throw new java.lang.Error("boss_?? has been called! This function must be called firstly");
            }
            initializeTieline();
        } else {
            this.tielines = gived_tielines;
        }
        refine_sysload_with_tieline();
//        主循环
        do {
            for (int si = 0; si < no_of_sys; si++) {
                SCUCSolver scucSolver = new SCUCSolver();
                MIPGurobi scucAlg = new MIPGurobi();
                scucSolver.setScucAlg(scucAlg);
                results[si] = scucSolver.optimize(systems.get(si));
            }
            bossMemory.add_memory(results, tielines, getMprices());
            updateTielines();
            refine_sysload_with_tieline();
            step++;
        } while (keeponWorking(step));
    }

    public void boss_ANC() {
        if (!has_been_called) {
            has_been_called = true;
        } else {
            throw new java.lang.Error("boss_?? has been called! This function must be called firstly");
        }
        iwantMOUC();
        anc = new ANC(systems, tielines);
        anc.anc_work();
    }

    private boolean keeponWorking(int step) {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        final int from = 0;
        final int to = 1;
        System.out.println("结果变化：");
        double[] a = bossMemory.get_obj_history();
        double[] b = bossMemory.get_normalized_MOUC_cost(systems);
        Tools.print_double_array(a);
        Tools.print_double_array(b);
        if (step >= parameters.getIters())
            return false;
        return true;
    }

    private void print_Mprices(int from, int to, String mode) {
        final int no_of_ti = systems.get(0).getTiNum();
        System.out.println("mps");
        double[][] mps = getMprices();
        if (mode == "value") {
            for (int t = 0; t < no_of_ti; t++) {
                System.out.print(String.format("%.2f", mps[from][t]) + " ");
            }
            System.out.println();
            for (int t = 0; t < no_of_ti; t++) {
                System.out.print(String.format("%.2f", mps[to][t]) + " ");
            }
            System.out.println();
        } else if (mode == "relationship") {
            for (int t = 0; t < no_of_ti; t++) {
                if (mps[from][t] - mps[to][t] >= 0) {
                    System.out.print("+" + " ");
                } else {
                    System.out.print("-" + " ");
                }
            }
            System.out.println();
        } else {
            throw new java.lang.Error("undefined mode");
        }
    }

    private void print_loads(int si) {
        final int no_of_ti = systems.get(0).getTiNum();
        final double[] loads = systems.get(si).getTotalLoad();
        System.out.println("系统" + String.format("%d", si) + "负荷");
        for (int t = 0; t < no_of_ti; t++) {
            System.out.print(String.format("%.2f", loads[t]) + " ");
        }
        System.out.println();
    }

    private void updateTielines() {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        double[][] mprices = getMprices();
        double[][] deltaSysLoad = getMaxDelta(mprices);
        try {
            tielines.deltaSetTielines(solveTielines(deltaSysLoad, "update"));
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public double[][] getMprices() {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        double[][] mprices = new double[no_of_sys][no_of_ti];
        for (int si = 0; si < no_of_sys; si++) {
            SCUCData scucData = systems.get(si);
            if (scucData.getTargetflag() != 3) {
                throw new java.lang.Error("targetFlag!=3? are u sure?");
            }
            MarginPriceCal devi = new MarginPriceCal(systems,results,parameters);
            mprices[si] = devi.getMpriceTs(si);
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
            } else {
                double a = Math.abs(mprices[si][t]);
                double b = Math.abs(mprices[i][t]);
                maxdeltas[i] = coefficence * systems.get(si).getTotalLoad()[t] * (a - b) / Math.max(a, b);
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
        double total_f1 = 0;
        double total_f2 = 0;
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
            total_f1 = total_f1 + result1.getBestObjValue();
            total_f2 = total_f2 + result2.getBestObjValue();
        }
        double[] normalization = new double[2];
        normalization[0] = parameters.getNormalization() / (total_f1 * total_f1);
        normalization[1] = parameters.getNormalization() / (total_f2 * total_f2);
//        改变归一化系数
        for (int si = 0; si < no_of_sys; si++) {
            systems.get(si).setNomalize_coefficentes(normalization);
            systems.get(si).setTargetflag(3);
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
            double[] oriload = systems.get(si).getOriTotalLoad();
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
//        4
        for (int i = 0; i < no_of_sys; i++) {
            double[] meetReserve = new double[no_of_ti];
            if (mode == "update") {
                int size = bossMemory.getResults_history().size();
                Calresult temp_result = bossMemory.getResults_history().get(size - 1)[i];
                meetReserve = systems.get(i).get_max_delta_load(temp_result);
            } else if (mode == "initialize") {
                for (int t = 0; t < no_of_ti; t++) {
                    SCUCData temp_system = systems.get(i);
                    meetReserve[t] = temp_system.getCapability() - temp_system.getTotalLoad()[t] - temp_system.getReserve()[t];
                }
            }
            for (int t = 0; t < no_of_ti; t++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int j = 0; j < no_of_sys; j++) {
                    expr.addTerm(1.0, x[i][j][t]);
                }
                model.addConstr(expr, GRB.LESS_EQUAL, meetReserve[t], null);
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
            double[][] mprices = getMprices();
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
            System.out.println("联络线对应成本增加应该是负的：" + Double.toString(model.get(GRB.DoubleAttr.ObjVal)));
//            System.out.println("联络线：");
//            Tools.print_double_array(this.tielines.getTielines()[0][1]);
//            System.out.println("联络线升级：");
//            Tools.print_double_array(result[0][1]);
//            System.out.println("系统边际电价");
//            Tools.print_double_array(mprices[0]);
//            Tools.print_double_array(mprices[1]);
            model.dispose();
            env.dispose();
            return result;
        } else {
            model.dispose();
            env.dispose();
            throw new java.lang.Error("mode is undefined!");
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

    public BossMemory getBossMemory() {
        return bossMemory;
    }
}
