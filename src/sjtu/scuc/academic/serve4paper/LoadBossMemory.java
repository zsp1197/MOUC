package sjtu.scuc.academic.serve4paper;

import sjtu.scuc.academic.BossMemory;
import sjtu.scuc.academic.Calresult;
import sjtu.scuc.academic.Differentiation;
import sjtu.scuc.academic.SCUCData;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by Zhai Shaopeng on 2017/5/23 12:38.
 * E-mail: zsp1197@163.com
 */
public class LoadBossMemory {
    public BossMemory getBm() {
        return bm;
    }

    private BossMemory bm;

    public LoadBossMemory(String file) {
        this.bm = load(file);
    }

    public BossMemory load(String file) {
        BossMemory result = null;
        ObjectInputStream ois = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            result = (BossMemory) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public double[] get_f_history(String mode) {
//        返回的f1/f2是未考虑归一化的
        final int no_of_steps = bm.getResults_history().size();
        final int no_of_sys = bm.getResults_history().get(0).length;
        double[] result = new double[no_of_steps];
        for (int step = 0; step < no_of_steps; step++) {
            Calresult[] results = bm.getResults_history().get(step);
            for (int si = 0; si < no_of_sys; si++) {
                if (mode == "f1_ori") {
                    result[step] = result[step] + results[si].getF1_gurobi();
                } else if (mode == "f2_ori") {
                    result[step] = result[step] + results[si].getF2_gurobi();
                } else if (mode == "f") {
                    result = bm.get_obj_history();
                } else {
                    throw new java.lang.Error("mode is undefined!");
                }
            }
        }
        return result;
    }

    public double[][][] get_mps() {
        final int no_of_steps = bm.getResults_history().size();
        final int no_of_sys = bm.getResults_history().get(0).length;
        final int no_of_ti = bm.getTielines_history().get(0).getTielines()[0][0].length;
        double[][][] result = new double[no_of_steps][no_of_sys][no_of_ti];
        for (int step = 0; step < no_of_steps; step++) {
            result[step] = bm.getMp_history().get(step);
        }
        return result;
    }

    public void write2csv(String path) {
        FileWriter writer;
//        write f1s/f2s/fs
        try {
            writer = new FileWriter(path + "f1s.csv");
            double[] f1s = get_f_history("f1_ori");
            double[] f2s = get_f_history("f2_ori");
            double[] fs = get_f_history("f");
            doubles2file(writer, f1s);
            writer.close();

            writer = new FileWriter(path + "f2s.csv");
            doubles2file(writer, f2s);
            writer.close();
            writer = new FileWriter(path + "fs.csv");
            doubles2file(writer, fs);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        write mp_#
        final int no_of_steps = bm.getResults_history().size();
        for (int step = 0; step < no_of_steps; step++) {
            try {
                writer = new FileWriter(path + String.format("mp_%d",step)+".csv");
                double[][] mps=bm.getMp_history().get(step);
                for (int si = 0; si < mps.length; si++) {
                    double[] mp_at_t=mps[si];
                    doubles2file(writer, mp_at_t);
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        write coefficents
        try {
            writer = new FileWriter(path + "coefficients.csv");
            doubles2file(writer,bm.getNomalize_coefficentes());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doubles2file(FileWriter writer, double[] target) throws IOException {
        for (int i = 0; i < target.length; i++) {
            writer.append(String.valueOf(target[i]));
            if (i == (target.length - 1)) {
                writer.append("\n");
                break;
            }
            writer.append(",");
        }
    }
}
