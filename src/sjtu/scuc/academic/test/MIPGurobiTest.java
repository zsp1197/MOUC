package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.*;

import static sjtu.scuc.academic.Doit.getSCUCData;

/**
 * Created by Zhai Shaopeng on 2017/5/9 17:00.
 * E-mail: zsp1197@163.com
 */
public class MIPGurobiTest {
    SCUCData scucData;
    MIPGurobi scucAlg;
    SCUCSolver scucSolver;
    Calresult result;
    @Before
    public void setUp() throws Exception {
        scucData = getSCUCData("UC-context10.xml");
//        气体系数全变正
        for (Generator temp:scucData.getGenList()){
            GeneratorWithQuadraticCostCurve gen= (GeneratorWithQuadraticCostCurve) temp;
            gen.setGasb(Math.abs(gen.getGasb()));
        }
        double[] totalload = scucData.getTotalLoad();
        double[] reserve = new double[totalload.length];
        for (int t = 0; t < totalload.length; t++) {
            reserve[t] = 0.05 * totalload[t];
        }
        scucData.setReserve(reserve);
        scucData.setTargetflag(1);
        scucSolver = new SCUCSolver();
        scucAlg=new MIPGurobi();

        scucSolver.setScucAlg(scucAlg);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println(result.getTargetflag());
        System.out.println("煤耗 "+Tools.getObjValue(result,scucData,1));
        System.out.println("排放 "+Tools.getObjValue(result,scucData,2));
    }

    @Test
    public void f1() throws Exception {
        scucData.setTargetflag(1);
        result=scucSolver.optimize(scucData);
    }

    @Test
    public void f2() throws Exception {
        scucData.setTargetflag(2);
        result=scucSolver.optimize(scucData);

    }

    @Test
    public void mouc() throws Exception {
        scucData.setTargetflag(1);
        Calresult result1=scucSolver.optimize(scucData);
        scucData.setTargetflag(2);
        Calresult result2=scucSolver.optimize(scucData);
        scucData.setNomalize_coefficentes(new double[]{1,1});
        scucData.setTargetflag(3);
        result=scucSolver.optimize(scucData);
    }
}