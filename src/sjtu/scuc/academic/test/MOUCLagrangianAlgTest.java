package sjtu.scuc.academic.test;

import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.*;

import java.util.Arrays;

import static org.junit.Assert.*;
import static sjtu.scuc.academic.Doit.getSCUCData;

/**
 * Created by Zhai Shaopeng on 2017/5/6 16:19.
 * E-mail: zsp1197@163.com
 */
public class MOUCLagrangianAlgTest {
    SCUCData scucData;
    MOUCLagrangianAlg scucAlg;
    SCUCSolver scucSolverMOUC;
    SCUCSolver scucSolverOri;

    @Before
    public void setUp() throws Exception {
        scucData = getSCUCData("UC-context - Full.xml");
        scucSolverMOUC = new SCUCSolver();
        scucSolverOri = new SCUCSolver();
        scucSolverOri.setScucAlg(new LagrangianAlg());

        scucAlg=new MOUCLagrangianAlg();

        int no_of_ti=scucData.getTotalLoad().length;
        double[] lambda=new double[no_of_ti];
        double[] mu=new double[no_of_ti];
        Arrays.fill(lambda,99999999);
        Arrays.fill(mu,99999999);
        scucAlg.setLambda(lambda);
        scucAlg.setMu(mu);

        scucSolverMOUC.setScucAlg(scucAlg);
        double[] totalload = scucData.getTotalLoad();
        double[] reserve = new double[totalload.length];
        for (int t = 0; t < totalload.length; t++) {
            reserve[t] = 1.1 * totalload[t];
        }
        scucData.setReserve(reserve);
    }

    @Test
    public void beforehand_process() throws Exception {
    }

    @Test
    public void callSolver() throws Exception {
        Calresult CalresultOri = scucSolverOri.optimize(scucData);
        Calresult CalresultMOUC = scucSolverMOUC.optimize(scucData);
        System.out.println("done");
    }

}