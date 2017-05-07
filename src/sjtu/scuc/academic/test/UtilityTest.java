package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.GeneratorWithQuadraticCostCurve;
import sjtu.scuc.academic.Utility;

import static org.junit.Assert.*;

/**
 * Created by Zhai Shaopeng on 2017/5/6 10:19.
 * E-mail: zsp1197@163.com
 */
public class UtilityTest {
    GeneratorWithQuadraticCostCurve gen;
    int[] genStatus;
    double[] genOut;
    @Before
    public void setUp() throws Exception {
        gen=new GeneratorWithQuadraticCostCurve(1,1,1,1,1,1);
        gen.setStartupCost(0);
        gen.setInitialP(0);
        gen.setInitialConditionHour(-10);
        gen.setRamp_rate(999);
        gen.setMin_dn_time(2);
        gen.setMin_on_time(2);
        gen.setMinP(10);
        gen.setMaxP(100);

        genStatus= new int[]{0, 1, 1};
        genOut= new double[]{0, 10, 2};
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getGenGasCost() throws Exception {
        assertEquals(118.0,Utility.getGenGasCost(gen,genStatus,genOut));
    }

    @Test
    public void getGenMOUCCost() throws Exception {
        assertEquals(27848.0,Utility.getGenMOUCCost(gen,genStatus,genOut));
    }

}