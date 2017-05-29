package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
    Parameters parameters;
    @Before
    public void setUp() throws Exception {
        systems = new ArrayList<SCUCData>();
        SCUCData scucData10 = getSCUCData("UC-context10.xml");
        SCUCData scucData36 = getSCUCData("UC-context36.xml");
        systems.add(scucData10);
        systems.add(scucData36);
        refineGascoefficents(systems);
        setReserves();
        final int no_of_sys = systems.size();
        final int no_of_ti = systems.get(0).getTiNum();
        for (int si = 0; si < no_of_sys; si++) {
            systems.get(si).setOriTotalLoad(Tools.deepcopyDoubleArray(systems.get(si).getTotalLoad()));
        }
        boss = new Boss(systems);
        Parameters parameters = new Parameters(10, 100, 0.006, 1, 1e6);
        this.parameters=parameters;
        boss.setParameters(parameters);
        boss.setTieMax_with_love(parameters.getMaxTieline());
    }

    private void refineGascoefficents(List<SCUCData> systems) {
        for (SCUCData scucData : systems) {
            for (Generator temp : scucData.getGenList()) {
                GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) temp;
                gen.setGasb(Math.abs(gen.getGasb()));
            }
        }
    }

    private void setReserves() {
        int no_of_sys = systems.size();
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
    public void boss_work_with_best_tieline() {
        try {
            boss.boss_ANC();
            System.out.println("ANC最终结果： " + Double.toString(boss.getAnc().get_total_MOUC_cost()));
            boss.boss_work(boss.getTielines());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("退出！");
        }
        serialize_bossMemory(boss.getBossMemory(),String.valueOf(parameters.getCoefficient())+"_boss_work_with_best_tieline.memory");
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
        long startTime = System.currentTimeMillis();   //获取开始时间
        try {
            boss.boss_work(null);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("退出！");
        }
        serialize_bossMemory(boss.getBossMemory(),String.valueOf(parameters.getCoefficient())+"_boss_work.memory");
        long endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： " + (endTime - startTime) + "ms");
    }


    @Test
    public void boss_ANC() throws Exception {
        long startTime = System.currentTimeMillis();   //获取开始时间
        boss.boss_ANC();
        System.out.println("ANC最终结果： " + Double.toString(boss.getAnc().get_total_MOUC_cost()));
        long endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： " + (endTime - startTime) + "ms");
    }


//    @Test
//    public void solveTielines() throws Exception {
//        double[][] deltaSysLoad = new double[no_of_sys][no_of_ti];
//        for (int i = 0; i < no_of_sys; i++) {
//            Arrays.fill(deltaSysLoad[i], 20);
//        }
//
//        boss.initializeTieline();
//        boss.refine_sysload_with_tieline();
//        boss.iwantMOUC();
//
//        double[][][] a=boss.solveTielines(deltaSysLoad,"update");
//
//        System.out.println("实际的结果应该是："+Double.toString(wtobj(a)));
//    }

    private void serialize_bossMemory(BossMemory memories, String name) {
        try {
            FileOutputStream fileOut =
                    new FileOutputStream(name);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(memories);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in " + name);
        } catch (IOException i) {
            i.printStackTrace();
        }
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