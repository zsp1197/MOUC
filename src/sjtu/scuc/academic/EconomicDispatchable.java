package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public interface EconomicDispatchable extends Algorithmicable {
    public void solve(final int[][] gen_status, SCUCData scucData) throws InfeasibleException;

    /**
     * [no_of_gen][no_of_ti]
     * @return
     */
    public double[][] getGenOut();

    public int getPenaltyCost();
    public void setPenaltyCost(int penalty_cost);

    public boolean isRespectStartupShutdownOutput();
    public void setRespectStartupShutdownOutput(boolean respectStartupShutdownOutput);
}
