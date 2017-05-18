package sjtu.scuc.academic;

import gurobi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Double.MAX_VALUE;

/**
 * Created by Zhai Shaopeng on 2017/5/16 22:05.
 * E-mail: zsp1197@163.com
 */
public class ANC {
    List<SCUCData> systems;
    Tielines tielines;
    SCUCData bigSystem;
    Calresult first_result;
    Calresult[] results;
    double final_result;

    public ANC(List<SCUCData> systems, Tielines tielines) {
        this.systems = systems;
        this.tielines = tielines;
    }

    public void anc_work() {
        final int no_of_ti = systems.get(0).getTiNum();
        final int no_of_sys = systems.size();
        results = new Calresult[no_of_sys];
        bigSystem = integrate_sysmtems(systems);
        SCUCSolver scucSolver = new SCUCSolver();
        MIPGurobi scucAlg = new MIPGurobi();
        scucSolver.setScucAlg(scucAlg);
        bigSystem.setTargetflag(4);
        first_result = scucSolver.optimize(bigSystem);
        tielines.setTielines(solveTieline(first_result, systems, tielines));

        refine_sysload_with_tieline();

        for (int si = 0; si < no_of_sys; si++) {
            scucSolver = new SCUCSolver();
            scucAlg = new MIPGurobi();
            scucSolver.setScucAlg(scucAlg);
            results[si] = scucSolver.optimize(systems.get(si));
        }
    }

    public double get_total_MOUC_cost(){
        final int no_of_sys = systems.size();
        double result=0;
        for (int si = 0; si < no_of_sys; si++) {
            result=result+results[si].getBestObjValue();
        }
        return result;
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

    private double[][][] solveTieline(Calresult first_result, List<SCUCData> systems, Tielines tielines) {
//        获取每个子系统的负荷改变量
        final int no_of_sys = systems.size();
        final int no_of_ti = systems.get(0).getTiNum();
        double[][] deltaGen = new double[no_of_sys][no_of_ti];
        for (int si = 0; si < no_of_sys; si++) {
            for (int t = 0; t < no_of_ti; t++) {
                deltaGen[si][t] = getRealGen(si, t, first_result) - systems.get(si).getTotalLoad()[t];
            }
        }
//        计算合理的联络线分配值
        double[][][] result = new double[no_of_sys][no_of_sys][no_of_ti];
        try {
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);
            model.getEnv().set(GRB.IntParam.OutputFlag, 0);
            GRBVar[][] f = new GRBVar[no_of_sys][no_of_ti];
//            1
            GRBVar[][][] x = new GRBVar[no_of_sys][no_of_sys][no_of_ti];
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
//            2
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
//            add obj
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    f[i][t] = model.addVar(-MAX_VALUE, MAX_VALUE, 0, GRB.CONTINUOUS, null);
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int j = 0; j < no_of_sys; j++) {
                        expr.addTerm(1.0, x[i][j][t]);
                    }
                    expr.addConstant(-deltaGen[i][t]);
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
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    for (int j = 0; j < no_of_sys; j++) {
                        result[i][j][t] = x[i][j][t].get(GRB.DoubleAttr.X);
                    }
                }
            }
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            e.printStackTrace();
        }
        return result;
    }

    private double getRealGen(int si, int t, Calresult first_result) {
        int gen_start_index = 0;
        List<Generator> genList = systems.get(si).getGenList();
        double result = 0;
        for (int i = 0; i < si; i++) {
            gen_start_index = gen_start_index + systems.get(i).getGenNum();
        }
        for (int i = 0; i < genList.size(); i++) {
            result = result + first_result.getGenOutput()[gen_start_index + i][t];
        }
        return result;
    }

    private SCUCData integrate_sysmtems(List<SCUCData> systems) {
        final int no_of_sys = systems.size();
        final int no_of_ti = systems.get(0).getTiNum();
        SCUCData result = new SCUCData();
        double[] totalload = new double[no_of_ti];
        double[] reserve = new double[no_of_ti];
        List<Generator> genList = new ArrayList<Generator>();
        for (SCUCData scucData : systems) {
            for (int t = 0; t < no_of_ti; t++) {
                totalload[t] = totalload[t] + scucData.getTotalLoad()[t];
                reserve[t] = reserve[t] + scucData.getReserve()[t];
            }
            for (Generator gen : scucData.getGenList()) {
                genList.add(gen);
            }
        }
        result.setResult1(systems.get(0).getResult1());
        result.setResult2(systems.get(0).getResult2());
        result.setGenList(genList);
        result.setTotalLoad(totalload);
        result.setReserve(reserve);
        result.setTargetflag(systems.get(0).getTargetflag());
        return result;
    }
}
