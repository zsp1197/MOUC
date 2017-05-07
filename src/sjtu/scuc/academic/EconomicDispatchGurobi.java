package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/6 16:07.
 * E-mail: zsp1197@163.com
 */
public class EconomicDispatchGurobi implements EconomicDispatchable {
    @Override
    public boolean isOutputDetail() {
        return false;
    }

    @Override
    public void setOutputDetail(boolean outputDetail) {

    }

    @Override
    public double getGapTolerance() {
        return 0;
    }

    @Override
    public void setGapTolerance(double gapTolerance) {

    }

    @Override
    public void solve(int[][] gen_status, SCUCData scucData) throws InfeasibleException {

    }

    @Override
    public double[][] getGenOut() {
        return new double[0][];
    }

    @Override
    public int getPenaltyCost() {
        return 0;
    }

    @Override
    public void setPenaltyCost(int penalty_cost) {

    }

    @Override
    public boolean isRespectStartupShutdownOutput() {
        return false;
    }

    @Override
    public void setRespectStartupShutdownOutput(boolean respectStartupShutdownOutput) {

    }
}
