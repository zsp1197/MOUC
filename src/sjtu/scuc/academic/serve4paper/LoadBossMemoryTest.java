package sjtu.scuc.academic.serve4paper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sjtu.scuc.academic.BossMemory;
import sjtu.scuc.academic.Tools;

import static org.junit.Assert.*;

/**
 * Created by Zhai Shaopeng on 2017/5/23 12:52.
 * E-mail: zsp1197@163.com
 */
public class LoadBossMemoryTest {
    LoadBossMemory lbm;
    @Before
    public void setUp() throws Exception {
        String filepath = "C:\\Users\\zhai\\IdeaProjects\\MOUC\\boss_work.memory";
        lbm = new LoadBossMemory(filepath);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void load() throws Exception {
        String filepath = "C:\\Users\\zhai\\IdeaProjects\\MOUC\\boss_work.memory";
        BossMemory totest = lbm.load(filepath);
        Tools.print_double_array(totest.get_obj_history());
    }

    @Test
    public void get_f_history() throws Exception {
        Tools.print_double_array(lbm.get_f_history("f1_ori"));
        Tools.print_double_array(lbm.get_f_history("f2_ori"));
        Tools.print_double_array(lbm.get_f_history("f"));
    }

    @Test
    public void write2csv() throws Exception {
        String path="D:\\SJTU\\多目标优化项目\\论文\\写论文\\改啊改\\数据\\test\\";
        lbm.write2csv(path);
    }

    @Test
    public void true_f() throws Exception {
        double[] f1_ori=lbm.get_f_history("f1_ori");
        double[] f2_ori=lbm.get_f_history("f2_ori");
        double[] f=lbm.get_f_history("f");
        double[] coefficents=lbm.getBm().getNomalize_coefficentes();
        double[] f_caled=new double[f.length];
        for (int i = 0; i < f.length; i++) {
            f_caled[i]=coefficents[0]*f1_ori[i]*f1_ori[i]+coefficents[1]*f2_ori[i]*f2_ori[i];
        }

        Tools.print_double_array(f_caled);
        Tools.print_double_array(f);
    }
}