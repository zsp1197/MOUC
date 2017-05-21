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

    public BossMemory() {
        step=-1;
        results_history=new ArrayList<Calresult[]>();
        tielines_history=new ArrayList<Tielines>();
    }

    public void add_memory(Calresult[] results,Tielines tielines){
        final int no_of_sys=results.length;
        Calresult[] temp_results=new Calresult[no_of_sys];
        for (int i = 0; i < no_of_sys; i++) {
            temp_results[i]=(Calresult) Tools.deepClone(results[i]);
        }
        step=step+1;
//        results_history.add(results);
        results_history.add(temp_results);
//        tielines_history.add(tielines);
        tielines_history.add((Tielines) Tools.deepClone(tielines));
    }

    public double[] get_obj_history(){
        int no_of_history=results_history.size();
        int no_of_sys=results_history.get(0).length;
        double result[]=new double[results_history.size()];
        for (int i = 0; i < no_of_history; i++) {
            Calresult[] temp_results=results_history.get(i);
            for (int si = 0; si < no_of_sys; si++) {
                result[i]=result[i]+temp_results[si].getBestObjValue();
            }
        }
        return result;
    }

    public double[] get_single_cost_history(List<SCUCData> systems,int targetflag){
        int no_of_history=results_history.size();
        int no_of_sys=results_history.get(0).length;
        double result[]=new double[no_of_history];
        for (int i = 0; i < no_of_history; i++) {
            Calresult[] temp_results=results_history.get(i);
            for (int si = 0; si < no_of_sys; si++) {
                result[i]=result[i]+Tools.getObjValue(temp_results[si],systems.get(si),targetflag);
            }
        }
        return result;
    }

    public double[] get_normalized_MOUC_cost(List<SCUCData> systems){
        int no_of_history=results_history.size();
        int no_of_sys=results_history.get(0).length;
        double[] result=new double[results_history.size()];
        double[] true_f1= get_single_cost_history(systems,1);
        double[] true_f2= get_single_cost_history(systems,2);
        double result1=systems.get(0).getResult1().getBestObjValue();
        double result2=systems.get(0).getResult2().getBestObjValue();

        double norm1=1/(result1*result1);
        double norm2=1/(result2*result2);
        for (int i = 0; i < no_of_history; i++) {
            result[i]=norm1*true_f1[i]*true_f1[i]+norm2*true_f2[i]*true_f2[i];
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
