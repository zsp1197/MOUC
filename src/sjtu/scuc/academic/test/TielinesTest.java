package sjtu.scuc.academic.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.Tielines;

import static org.junit.Assert.*;

/**
 * Created by Zhai Shaopeng on 2017/5/10 14:44.
 * E-mail: zsp1197@163.com
 */
public class TielinesTest {
    Tielines tielines;
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getTieMax() throws Exception {
        double[][] tiemax=new double[5][5];
        tielines=new Tielines(tiemax);

    }

    @Test
    public void setTieMax() throws Exception {
    }

    @Test
    public void getTielines() throws Exception {
    }

    @Test
    public void setTielines() throws Exception {
        double[][] tiemax=new double[5][5];
        double[][][] tieline=new double[5][5][24];
        tielines=new Tielines(tiemax);
        tielines.setTielines(tieline);
    }

}