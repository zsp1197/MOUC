package sjtu.scuc.academic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class SCUCSolver {

    protected static Log log = LogFactory.getLog(SCUCSolver.class);

    private SCUCAlg scucAlg;

    public Calresult optimize(SCUCData scucData) {
        scucAlg.setScucData(scucData);
        return scucAlg.optimize();
    }

    public SCUCAlg getScucAlg() {
        return scucAlg;
    }

    public void setScucAlg(SCUCAlg scucAlg) {
        this.scucAlg = scucAlg;
    }

    /**
     * print used algorithm description
     */
    public void printInfo() {
        log.info(" ");
        log.info(scucAlg);
    }

}
