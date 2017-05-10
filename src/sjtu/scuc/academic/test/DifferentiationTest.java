package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.*;

import java.util.Arrays;

import static org.junit.Assert.*;
import static sjtu.scuc.academic.Doit.getSCUCData;

/**
 * Created by Zhai Shaopeng on 2017/5/10 10:18.
 * E-mail: zsp1197@163.com
 */
public class DifferentiationTest {
    Differentiation devi;
    SCUCData scucData;
    MIPGurobi scucAlg;
    SCUCSolver scucSolver;
    Calresult result;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void deepcopy2Dint() throws Exception {
        int[][] hehe = new int[3][4];
        int[][] deeped = devi.deepcopy2Dint(hehe);
        assertTrue((hehe[1][2] == deeped[1][2]));
        deeped[1][2] = 31;
        assertFalse((hehe[1][2] == deeped[1][2]));
        assertTrue((hehe[1][1] == deeped[1][1]));
    }

    @Test
    public void deviP1() throws Exception {
        prepare4data();
        System.out.println(devi.deviP(result, 3, 2, 1));
    }

    @Test
    public void deviP2() throws Exception {
        prepare4data();
        scucData.setTargetflag(1);
        result.setTargetflag(1);

        int t = 3;
        int i = 0;

        System.out.println(devi.deviP(result, i, t, 1));

        Generator[] gens = scucData.getGens();
        GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) gens[i];
        while (devi.miss_Wrong(gen, result.getGenStatus(), result.getGenOutput(), i, t) == true) {
            i += 1;
//            寻找边际机组
            gen = (GeneratorWithQuadraticCostCurve) gens[i];
        }

        double p = result.getGenOutput()[i][t];
        assertEquals(devi.deviP(result, i, t, 1), (2 * gen.getAQuadratic() * p + gen.getALinear()));
    }

    @Test
    public void getMpriceT() throws Exception {
        prepare4data();
        System.out.println(devi.getMpriceT(result, 3));
    }

    @Test
    public void getMpriceTs() throws Exception {
        prepare4data();
        double[] mprices = devi.getMpriceTs(result);
        for (int i = 0; i < mprices.length; i++) {
            System.out.print(Double.toString(mprices[i]) + " ");
        }
    }

    @Test
    public void getMpriceTs2() throws Exception {
        prepare4data();
        scucData.setTargetflag(1);
        result.setTargetflag(1);
        double[] mprices = devi.getMpriceTs(result);
        for (int i = 0; i < mprices.length; i++) {
            System.out.print(Double.toString(mprices[i]) + " ");
        }
    }

    private void prepare4data() {
        scucData = getSCUCData("UC-context - Full.xml");
        double[] totalload = scucData.getTotalLoad();
        double[] reserve = new double[totalload.length];
        for (int t = 0; t < totalload.length; t++) {
            reserve[t] = 1.05 * totalload[t];
        }
        scucData.setReserve(reserve);
        scucData.setMode("f1");
        scucSolver = new SCUCSolver();
        scucAlg = new MIPGurobi();

        scucSolver.setScucAlg(scucAlg);
        scucData.setTargetflag(1);
        Calresult result1 = scucSolver.optimize(scucData);
        scucData.setTargetflag(2);
        Calresult result2 = scucSolver.optimize(scucData);
        scucData.setResult1(result1);
        scucData.setResult2(result2);
        scucData.setTargetflag(3);
        result = scucSolver.optimize(scucData);

        devi = new Differentiation(scucData);
    }
}