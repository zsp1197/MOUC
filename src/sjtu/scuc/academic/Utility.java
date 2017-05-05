package sjtu.scuc.academic;
import Jama.Matrix;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class Utility {
    public static double getGenCost(final Generator gen, final int[] genStatus, final double[] genOut) {
        double genCost = 0;

        if (genStatus[0] == 1) {
            genCost += gen.getGenCost(genOut[0]);
            if (gen.getInitialConditionHour() <= 0) genCost += gen.getStartupCost();
        }
        for (int t = 1; t < genStatus.length; t++) {
            if (genStatus[t] == 1) {
                genCost += gen.getGenCost(genOut[t]);
                if (genStatus[t - 1] == 0) genCost += gen.getStartupCost();
            }
        }
        return genCost;
    }

    public static double getGenStartupCost(final Generator gen, final int[] genStatus) {
        if (gen.getStartupCost() == 0) return 0;

        double startupCost = 0;

        int preStatusIndex = gen.getInitialStatusIndex();
        if (genStatus[0] == 1 && !gen.isON(preStatusIndex)) startupCost += gen.getStartupCost();

        for (int t = 1; t < genStatus.length; t++) {
            if (genStatus[t] == 1 && genStatus[t - 1] == 0) startupCost += gen.getStartupCost();
        }

        return startupCost;
    }

    public static double getTotalActualCost(final List<Generator> gens, final int[][] gen_status, final double[][] gen_out) {
        double genCost = 0;

        int no_of_gen = gens.size();
        int no_of_ti = gen_status[0].length;
        int[] genStatus = new int[no_of_ti];
        double[] genOut = new double[no_of_ti];
        for (int i = 0; i < no_of_gen; i++) {
            Generator generator = gens.get(i);
            for (int t = 0; t < no_of_ti; t++) {
                genStatus[t] = gen_status[i][t];
                genOut[t] = gen_out[i][t];
            }
            genCost += getGenCost(generator, genStatus, genOut);
        }

        return genCost;
    }

    /**
     * @param gen_out: [gen][ti]
     * @return gen_out_t_g: [ti][gen]
     */
    public static double[][] getGenOutTimeGen(double[][] gen_out) {
        final int no_of_gen = gen_out.length;
        final int no_of_ti = gen_out[0].length;
        double gen_out_t_g[][] = new double[no_of_ti][no_of_gen];
        for (int i = 0; i < no_of_gen; i++)
            for (int t = 0; t < no_of_ti; t++)
                gen_out_t_g[t][i] = gen_out[i][t];
        return gen_out_t_g;
    }

    /**
     * @param gen_out: [ti][gen]
     * @return gen_out_g_t: [gen][ti]
     */
    public static double[][] getGenOutGenTime(double[][] gen_out) {
        final int no_of_ti = gen_out.length;
        final int no_of_gen = gen_out[0].length;
        double gen_out_g_t[][] = new double[no_of_gen][no_of_ti];
        for (int i = 0; i < no_of_gen; i++)
            for (int t = 0; t < no_of_ti; t++)
                gen_out_g_t[i][t] = gen_out[t][i];
        return gen_out_g_t;
    }

    public static void copy(int[][] src, int[][] des) {
        for (int i = 0; i < src.length; i++)
            System.arraycopy(src[i], 0, des[i], 0, src[i].length);
    }

    public static void copy(double[][] src, double[][] des) {
        for (int i = 0; i < src.length; i++)
            System.arraycopy(src[i], 0, des[i], 0, src[i].length);
    }

    /**
     * @param branchs:
     * @param no_of_bus
     * @param refBusIdx: @return PTDF[no_of_branch][no_of_bus]
     */
    public static double[][] calculatePTDF(final Branch[] branchs, final int no_of_bus, final int refBusIdx) {

        // TODO: sime B_prime matrix is symmetrical, we can improve the following code
        // get B_prime matrix
        double[][] B_prime = new double[no_of_bus][no_of_bus];
        for (Branch branch : branchs) {
            final int fromBus_idx = branch.getFromBusIdx();
            final int toBus_idx = branch.getToBusIdx();
            final double v = 1. / branch.getX();
            B_prime[fromBus_idx][toBus_idx] -= v;
            B_prime[toBus_idx][fromBus_idx] -= v;
            B_prime[fromBus_idx][fromBus_idx] += v;
            B_prime[toBus_idx][toBus_idx] += v;
        }

        int[] row_idx = new int[no_of_bus - 1];
        {
            int j = 0;
            for (int i = 0; i < no_of_bus; i++) {
                if (i == refBusIdx) continue;
                row_idx[j++] = i;
            }
        }
        int[] col_idx = new int[no_of_bus - 1];
        System.arraycopy(row_idx, 0, col_idx, 0, row_idx.length);

        // LU decomposition B_prime matrix
        // B_prime = L * U
        Matrix B_prime_fullmatrix = new Matrix(B_prime);
        Matrix B_prime_matrix = B_prime_fullmatrix.getMatrix(row_idx, col_idx);

        final int no_of_branch = branchs.length;
        double[][] PTDF = new double[no_of_branch][no_of_bus];
        // solve delta_theta = inv(B) * delta_P
        for (int i = 0; i < no_of_bus; i++) {
            if (i == refBusIdx) continue;

            double[] delta_P = new double[no_of_bus];
            delta_P[i] = 1.0;
            double[] delta_P_without_refBus = new double[no_of_bus - 1];
            System.arraycopy(delta_P, 0, delta_P_without_refBus, 0, refBusIdx);
            System.arraycopy(delta_P, refBusIdx + 1, delta_P_without_refBus, refBusIdx, no_of_bus - refBusIdx - 1);

            Matrix x = B_prime_matrix.solve(new Matrix(delta_P_without_refBus, delta_P_without_refBus.length));

            double[] delta_theta = new double[no_of_bus];
            for (int j = 0; j < refBusIdx; j++) delta_theta[j] = x.get(j, 0);
            for (int j = refBusIdx + 1; j < no_of_bus; j++) delta_theta[j] = x.get(j - 1, 0);

            for (int j = 0; j < no_of_branch; j++) {
                // calculate PTDF[][]
                final Branch branch = branchs[j];
                final int fromBus_idx = branch.getFromBusIdx();
                final int toBus_idx = branch.getToBusIdx();
                PTDF[j][i] = (delta_theta[fromBus_idx] - delta_theta[toBus_idx]) / branch.getX();
            }
        }

        return PTDF;
    }

    public static void solveLUXB(final Matrix L, final Matrix U, final int[] pivot, final double[] b1, double[] x1) {

        double[] b = new double[b1.length];
        for (int i = 0; i < b.length; i++) b[pivot[i]] = b1[i];

        final int rowNum_of_L = L.getRowDimension();

        // solve L*temp = b
        double[] temp = new double[L.getColumnDimension()];
        for (int k = 0; k < rowNum_of_L; k++) {
            temp[k] = 0.0;
            for (int l = 0; l < k; l++) {
                temp[k] += L.get(k, l) * temp[l];
            }
            temp[k] = (b[k] - temp[k]) / L.get(k, k);
        }

        // solve U*delta_theta = temp
        double[] x = new double[x1.length];
        final int rowNum_of_U = U.getRowDimension();
        for (int k = rowNum_of_U - 1; k >= 0; k--) {
            x[k] = 0.0;
            for (int l = rowNum_of_U - 1; l > k; l--) {
                x[k] += U.get(k, l) * x[l];
            }
            x[k] = (temp[k] - x[k]) / U.get(k, k);
        }
        for (int i = 0; i < x.length; i++) x1[i] = x[pivot[i]];
    }

    public static int addConstraint(LeqConstraint con, List<LeqConstraint> constraints, Map<Integer, List<LeqConstraint>> ti_constraints_map) {
        String name = con.getName();
        if (name == null || name.length() == 0)
            con.setName(MessageFormat.format("constraint_auto_{0}", constraints.size()));

        if( constraints.contains(con) ) return 0;

        constraints.add(con);
        final Integer ti = con.getTimeInterval();
        List<LeqConstraint> ti_cons = ti_constraints_map.get(ti);
        if (ti_cons == null) {
            ti_cons = new ArrayList<LeqConstraint>();
            ti_constraints_map.put(ti, ti_cons);
        }
        ti_cons.add(con);

        return 1;
    }
}