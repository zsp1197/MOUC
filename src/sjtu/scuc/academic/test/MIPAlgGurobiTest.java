package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.MIPAlgGurobi;
import sjtu.scuc.academic.SCUCData;
import sjtu.scuc.academic.SCUCSolver;

import static sjtu.scuc.academic.Doit.getSCUCData;

/**
 * Created by Zhai Shaopeng on 2017/5/9 15:41.
 * E-mail: zsp1197@163.com
 */
public class MIPAlgGurobiTest {
    SCUCData scucData;
    MIPAlgGurobi scucAlg;
    SCUCSolver scucSolver;
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

        scucSolver = new SCUCSolver();
        scucAlg=new MIPAlgGurobi();

        scucSolver.setScucAlg(scucAlg);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void solve() throws Exception {
        scucSolver.optimize(scucData);
    }

}