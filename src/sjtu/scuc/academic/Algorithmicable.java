package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public interface Algorithmicable {
    public boolean isOutputDetail();
    public void setOutputDetail(boolean outputDetail);

    public double getGapTolerance();
    public void setGapTolerance(double gapTolerance);
}
