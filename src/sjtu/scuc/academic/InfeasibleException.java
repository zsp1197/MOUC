package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class InfeasibleException extends Exception {
    public InfeasibleException(String s, SCUCData scucData) {
        super(s, scucData);
    }
}
