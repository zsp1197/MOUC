package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.Boss;
import sjtu.scuc.academic.Parameters;
import sjtu.scuc.academic.SCUCData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static sjtu.scuc.academic.Doit.getSCUCData;

/**
 * Created by Zhai Shaopeng on 2017/5/10 21:39.
 * E-mail: zsp1197@163.com
 */
public class BossTest {
    List<SCUCData> systems;
    Boss boss;
    int no_of_sys;
    int no_of_ti;

    @Before
    public void setUp() throws Exception {
        systems = new ArrayList<SCUCData>();
        SCUCData scucData10 = getSCUCData("UC-context10.xml");
        SCUCData scucData36 = getSCUCData("UC-context36.xml");
        systems.add(scucData10);
        systems.add(scucData36);
        for (int i = 0; i < 2; i++) {
            systems.get(i).setNormalization(1e-6);
        }
        setReserves();
        boss = new Boss(systems);
        Parameters parameters = new Parameters(10, 100, 0.01, 100);
        boss.setParameters(parameters);
        boss.setTieMax_with_love(parameters.getMaxTieline());
        no_of_sys = systems.size();
        no_of_ti = systems.get(0).getTiNum();
    }

    private void setReserves() {

        for (int si = 0; si < no_of_sys; si++) {
            SCUCData scucData = systems.get(si);
            double[] totalload = scucData.getTotalLoad();
            double[] reserve = new double[totalload.length];
            for (int t = 0; t < totalload.length; t++) {
                reserve[t] = 0.05 * totalload[t];
            }
            scucData.setReserve(reserve);
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void initializeTieline() throws Exception {
        boss.initializeTieline();
    }

    @Test
    public void refine_sysload_with_tieline() throws Exception {
        boss.initializeTieline();
        boss.refine_sysload_with_tieline();
        double[][][] hehe = boss.getTielines().getTielines();
        System.out.println();
    }

    @Test
    public void get_max_value_4_doubles() throws Exception {
        double[] a = new double[]{32, 32, 543, 654, 465};
        double max = boss.get_max_value_4_doubles(a);
        assertEquals(max, 654, 0);
    }

    @Test
    public void boss_work() throws Exception {
        boss.boss_work();
    }

    @Test
    public void solveTielines() throws Exception {
        double[][] deltaSysLoad = new double[no_of_sys][no_of_ti];
        for (int i = 0; i < no_of_sys; i++) {
            Arrays.fill(deltaSysLoad[i], 20);
        }

        boss.initializeTieline();
        boss.refine_sysload_with_tieline();
        boss.iwantMOUC();

        double[][][] a=boss.solveTielines(deltaSysLoad,"update");

        System.out.println("实际的结果应该是："+Double.toString(wtobj(a)));
    }

    private double wtobj(double[][][] x) {
        final int no_of_sys = boss.getSystems().size();
        final int no_of_ti = boss.getSystems().get(0).getTiNum();
        double[][] mprices = boss.getMprices();
        double result = 0;
        for (int si = 0; si < no_of_sys; si++) {
            for (int t = 0; t < no_of_ti; t++) {
                for (int i = 0; i < no_of_sys; i++) {
                    result = result + mprices[si][t] * x[si][i][t];
                }
            }
        }
        return result;
    }
}