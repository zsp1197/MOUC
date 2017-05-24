package sjtu.scuc.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/12 9:59.
 * E-mail: zsp1197@163.com
 */
public class BossMemory implements Serializable {
    int step;

    List<Calresult[]> results_history;
    List<Tielines> tielines_history;

    public List<double[][]> getMp_history() {
        return mp_history;
    }

    List<double[][]> mp_history;

    private double[] nomalize_coefficentes;

    public double[] getNomalize_coefficentes() {
        return nomalize_coefficentes;
    }

    public void setNomalize_coefficentes(double[] nomalize_coefficentes) {
        this.nomalize_coefficentes = nomalize_coefficentes;
    }
    public BossMemory() {
        step = -1;
        results_history = new ArrayList<Calresult[]>();
        tielines_history = new ArrayList<Tielines>();
        mp_history = new ArrayList<double[][]>();
    }

    public void add_memory(Calresult[] results, Tielines tielines, double[][] mprices) {
        final int no_of_sys = results.length;
        Calresult[] temp_results = new Calresult[no_of_sys];
        for (int i = 0; i < no_of_sys; i++) {
            temp_results[i] = (Calresult) Tools.deepClone(results[i]);
        }
        step = step + 1;
//        results_history.add(results);
        results_history.add(temp_results);
//        tielines_history.add(tielines);
        tielines_history.add((Tielines) Tools.deepClone(tielines));
        mp_history.add(Tools.deepcopy2D_DoubleArray(mprices));
    }

    public double[] get_obj_history() {
        int no_of_history = results_history.size();
        int no_of_sys = results_history.get(0).length;
        double result[] = new double[results_history.size()];
        for (int i = 0; i < no_of_history; i++) {
            Calresult[] temp_results = results_history.get(i);
            for (int si = 0; si < no_of_sys; si++) {
                result[i] = result[i] + temp_results[si].getBestObjValue();
            }
        }
        return result;
    }

    public double[] get_single_cost_history(List<SCUCData> systems, int targetflag) {
        int no_of_history = results_history.size();
        int no_of_sys = results_history.get(0).length;
        double result[] = new double[no_of_history];
        for (int i = 0; i < no_of_history; i++) {
            Calresult[] temp_results = results_history.get(i);
            for (int si = 0; si < no_of_sys; si++) {
                result[i] = result[i] + Tools.getObjValue(temp_results[si], systems.get(si), targetflag);

            }
        }
        return result;
    }
    public double[] get_single_cost_history(int targetflag) {
        int no_of_history = results_history.size();
        int no_of_sys = results_history.get(0).length;
        double result[] = new double[no_of_history];
        for (int i = 0; i < no_of_history; i++) {
            Calresult[] temp_results = results_history.get(i);
            for (int si = 0; si < no_of_sys; si++) {
//                result[i] = result[i] + Tools.getObjValue(temp_results[si], systems.get(si), targetflag);
                if (targetflag==1){
                    result[i] = result[i] + temp_results[si].getF1_gurobi();
                }else if(targetflag==2){
                    result[i] = result[i] + temp_results[si].getF2_gurobi();
                }
                else {
                    throw new java.lang.Error("undefined mode");
                }
            }
        }
        return result;
    }

    public double[] get_normalized_MOUC_cost(List<SCUCData> systems) {
        int no_of_history = results_history.size();
        int no_of_sys = results_history.get(0).length;
        double[] result = new double[results_history.size()];
        double[] true_f1 = get_single_cost_history(1);
//        double[] true_f1 = get_single_cost_history(systems, 1);
        double[] true_f2 = get_single_cost_history(2);
//        double[] true_f2 = get_single_cost_history(systems, 2);
        System.out.println("我算的f1/f2： ");
        Tools.print_double_array(true_f1);
        Tools.print_double_array(true_f2);
        System.out.println("这里的系数：");
        Tools.print_double_array(systems.get(0).getNormalize_coefficentes());
        for (int i = 0; i < no_of_history; i++) {
            result[i] = systems.get(0).getNormalize_coefficentes()[0] * true_f1[i] * true_f1[i] + systems.get(0).getNormalize_coefficentes()[1] * true_f2[i] * true_f2[i];
        }
        return result;
    }

    public List<Calresult[]> getResults_history() {
        return results_history;
    }

    public List<Tielines> getTielines_history() {
        return tielines_history;
    }

}
