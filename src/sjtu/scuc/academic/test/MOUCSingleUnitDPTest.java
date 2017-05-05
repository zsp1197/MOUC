package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.GeneratorWithQuadraticCostCurve;
import sjtu.scuc.academic.MOUCSingleUnitDP;

import static org.junit.Assert.*;

/**
 * Created by Zhai Shaopeng on 2017/5/5 19:15.
 * E-mail: zsp1197@163.com
 */
public class MOUCSingleUnitDPTest {
    GeneratorWithQuadraticCostCurve[] gens=new GeneratorWithQuadraticCostCurve[2];
    double[] lambda=new double[24];
    double[] mu=new double[24];
    private MOUCSingleUnitDP myclass;
    @Before
    public void setUp() throws Exception {
        GeneratorWithQuadraticCostCurve gen=new GeneratorWithQuadraticCostCurve(1,1,1,1,1,1);
        gen.setStartupCost(0);
        gen.setInitialP(0);
        gen.setInitialConditionHour(-10);
        gen.setRamp_rate(999);
        gen.setMin_dn_time(2);
        gen.setMin_on_time(2);
        gen.setMinP(10);
        gen.setMaxP(100);
        gens[0]=gen;
        for (int t = 0; t < 24; t++) {
            lambda[t]=1;
            mu[t]=1;
        }
        myclass=new MOUCSingleUnitDP();
        System.out.println("setUp done!");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void solve() throws Exception {
        assertEquals(1, myclass.solve(gens,0,lambda,mu,null)[3]);
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