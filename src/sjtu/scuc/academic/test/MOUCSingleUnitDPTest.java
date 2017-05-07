package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.GeneratorWithQuadraticCostCurve;
import sjtu.scuc.academic.MOUCSingleUnitDP;

import java.lang.reflect.Array;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by Zhai Shaopeng on 2017/5/5 19:15.
 * E-mail: zsp1197@163.com
 */
public class MOUCSingleUnitDPTest {
    GeneratorWithQuadraticCostCurve[] gens=new GeneratorWithQuadraticCostCurve[2];
    int no_of_ti=24;
    double[] lambda=new double[no_of_ti];
    double[] mu=new double[no_of_ti];
    private MOUCSingleUnitDP myclass;
    @Before
    public void setUp() throws Exception {
        GeneratorWithQuadraticCostCurve gen=new GeneratorWithQuadraticCostCurve(0,0,0,0,0,0);
        gen.setStartupCost(10);
        gen.setInitialP(0);
        gen.setInitialConditionHour(3);
        gen.setRamp_rate(999);
        gen.setMin_dn_time(2);
        gen.setMin_on_time(2);
        gen.setMinP(10);
        gen.setMaxP(100);
        gens[0]=gen;
        Arrays.fill(lambda,1);
        Arrays.fill(mu,1);
        myclass=new MOUCSingleUnitDP();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void solve() throws Exception {
        myclass.solve(gens,0,lambda,mu,null);
        int[] trueStatus=new int[no_of_ti];
        Arrays.fill(trueStatus,1);
        assertTrue(Arrays.equals(trueStatus,myclass.getGenStatus()));
        double[] trueOut=new double[no_of_ti];
        Arrays.fill(trueOut,gens[0].getMaxP());

        for (int t = 0; t < no_of_ti; t++) {
            assertTrue(Arrays.equals(trueOut,myclass.getGenOut()));
        }
    }

    @Test
    public void getGenOut() throws Exception {
    }

    @Test
    public void getGenStatus() throws Exception {
    }

    @Test
    public void getGenStatusFromDPStatus() throws Exception {
    }

}