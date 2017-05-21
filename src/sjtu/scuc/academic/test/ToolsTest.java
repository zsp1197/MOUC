package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.*;

import static org.junit.Assert.*;
import static sjtu.scuc.academic.Doit.getSCUCData;

/**
 * Created by Zhai Shaopeng on 2017/5/10 10:53.
 * E-mail: zsp1197@163.com
 */
public class ToolsTest {
    SCUCData scucData;
    MIPGurobi scucAlg;
    SCUCSolver scucSolver;
    Calresult result;

    @Before
    public void setUp() throws Exception {
        scucData = getSCUCData("UC-context - Full.xml");
        double[] totalload = scucData.getTotalLoad();
        double[] reserve = new double[totalload.length];
        for (int t = 0; t < totalload.length; t++) {
            reserve[t] = 1.05 * totalload[t];
        }
        scucData.setReserve(reserve);
        scucData.setMode("f1");
        scucData.setTargetflag(1);
        scucSolver = new SCUCSolver();
        scucAlg = new MIPGurobi();

        scucSolver.setScucAlg(scucAlg);
        scucData.setTargetflag(1);
        result = scucSolver.optimize(scucData);
    }


    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getObjValue() throws Exception {
        Assert.assertEquals(Tools.getObjValue(result, scucData, 1), Tools.getObjValue(result.getGenStatus(), result.getGenY(), result.getGenOutput(), scucData, 1));
    }

    @Test
    public void deepcopy2D_DoubleArray() throws Exception {
        double[][] target = new double[3][4];
        double[][] result = Tools.deepcopy2D_DoubleArray(target);
        target[1][2] = 3;
        for (int i = 0; i < target.length; i++) {
            for (int j = 0; j < target[0].length; j++) {
                assertEquals(target[i][j],result[i][j],0);
            }
        }
    }

}