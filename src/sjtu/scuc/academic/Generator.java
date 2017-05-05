package sjtu.scuc.academic;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
abstract public class Generator {
    /**
     * bus index: begin from 0
     */
    private int busIdx;
    private String name;

    public double[] getOria() {
        return oria;
    }

    double oria[]={0,0,0};
    public double[] getA() {
        return a;
    }

    public void setA(double[] a) {
        this.a = a;
    }

    private double[] a ={0, 0, 0};


    abstract public double getGenCost(final double p);

    /**
     * Suppose the generator is ON
     *
     * @param lambda: lagrangian multiplier of totalLoad balance constraint
     * @return output generation level. between maximumP and minimumP
     */
    abstract public double getOutput(final double lambda);

    /**
     * Suppose the generator is ON
     *
     * @param lambda: lagrangian multiplier of totalLoad balance constraint
     * @return output generation level. between maxOperationP and minOperationP
     */
    abstract public double getOperationOutput(final double lambda);

    /**
     * minimum output for generation
     */
    double minP;
    /**
     * maximum output for generation
     */
    double maxP;

    /**
     * minimum operation output for generation
     * considering ramp
     */
    double minOperationP = 0;
    /**
     * maximum operation output for generation
     * considering ramp
     */
    double maxOperationP = 99999;

    private int initialConditionHour = -10000; // default unit status is OFF and is ready to startup at any time
    private int initialStatus = 0;

    private double initialP = 0;
    private double ramp_rate = 99999;

    private int min_on_time = 1;
    private int min_dn_time = 1;
    private int no_of_status;

    private double startupCost = 0;

    private int[] statusArray = null;
    private Map<Integer, Integer> status_to_index_map = null;

    private double P;
    private final int penalty_cost = 10000;

    public double getMinP() {
        return minP;
    }

    public double getMaxP() {
        return maxP;
    }

    public void setMinP(double minP) {
        this.minP = minP;
        this.minOperationP = minP;
    }

    public void setMaxP(double maxP) {
        this.maxP = maxP;
        this.maxOperationP = maxP;
    }

    public int getNoOfStatus() {
        return no_of_status;
    }

    public int getInitialConditionHour() {
        return initialConditionHour;
    }

    public void setInitialConditionHour(int initialConditionHour) {
        this.initialConditionHour = initialConditionHour;
    }

    public double getStartupCost(int preStatusIndex, int curStatusIndex) {
        if (startupCost == 0) return 0;

        if (!isON(preStatusIndex) && isON(curStatusIndex)) return startupCost;
        else return 0;
    }

    public double getOutput(int statusIndex, double lambda) {
        if (isON(statusIndex)) return getOutput(lambda);
        else return 0;
    }

    public boolean isONbyStatus(int status) {
        return status > 0;
    }

    public boolean isON(int statusIndex) {
        int status = getStatus(statusIndex);
        return isONbyStatus(status);
    }

    // return status by status index
    public int getStatus(int statusIndex) {
        return statusArray[statusIndex];
    }

    public int getStatusIndex(int status) {
        Object o = status_to_index_map.get(status);
        if (o == null) return -1;
        else return (Integer) o;
    }

    public double getP() {
        return P;
    }

    public void setP(double p) {
        this.P = p;
    }

    public double getStartupCost() {
        return startupCost;
    }

    public void setStartupCost(double startupCost) {
        this.startupCost = startupCost;
    }

    public void initial() {
        no_of_status = min_on_time + min_dn_time;
        statusArray = new int[no_of_status];
        status_to_index_map = new HashMap<Integer, Integer>(no_of_status);

        int statusIndex = 0;
        while (statusIndex < min_dn_time) {
            statusArray[statusIndex] = statusIndex - min_dn_time;
            status_to_index_map.put(statusArray[statusIndex], statusIndex);

            statusIndex++;
        }
        statusIndex = min_dn_time;
        while (statusIndex < min_dn_time + min_on_time) {
            statusArray[statusIndex] = statusIndex - min_dn_time + 1;
            status_to_index_map.put(statusArray[statusIndex], statusIndex);

            statusIndex++;
        }

        initialStatus = getStatusByConditionHour(initialConditionHour);
        if (!isONbyStatus(initialStatus)) initialP = 0;
    }

    public int getStatusByConditionHour(int conditionHour) {
        int status;
        if (conditionHour > min_on_time) status = min_on_time;
        else if (conditionHour > 0) status = conditionHour;
        else if (conditionHour > (-min_dn_time)) status = conditionHour;
        else status = -min_dn_time;
        return status;
    }

    public int getInitialStatusIndex() {
        return getStatusIndex(initialStatus);
    }

    /**
     * if it is reachable from preStatusIndex to curStatusIndex, return 0
     * else return 10,000
     *
     * @param preStatusIndex: status index at previous time interval
     * @param curStatusIndex: status index at current time interval
     * @return transfer cost
     */
    public double getTransferCost(int preStatusIndex, int curStatusIndex) {
        if (isReachable(preStatusIndex, curStatusIndex)) return 0;
        else return penalty_cost;
    }

    public boolean isReachable(int preStatusIndex, int curStatusIndex) {
        boolean reachable;
        if (preStatusIndex == 0) {
            reachable = curStatusIndex == preStatusIndex || curStatusIndex == min_dn_time;
        } else if (preStatusIndex == (no_of_status - 1)) {
            reachable = curStatusIndex == preStatusIndex || curStatusIndex == (preStatusIndex - min_on_time);
        } else if (isON(preStatusIndex)) {
            reachable = curStatusIndex == (preStatusIndex + 1);
        } else { // !isON(preStatusIndex)
            reachable = curStatusIndex == (preStatusIndex - 1);
        }
        return reachable;
    }

    public double getInitialP() {
        return initialP;
    }

    public void setInitialP(double initialP) {
        this.initialP = initialP;
    }

    public double getRamp_rate() {
        return ramp_rate;
    }

    public void setRamp_rate(double ramp_rate) {
        this.ramp_rate = ramp_rate;
    }

    /**
     * update generator's operation maximun output
     * operation max = min(maxP,  preP+ramp)
     * operation min = max(minP,  preP-ramp)
     *
     * @param preP: output at previous time interval
     */
    public void updateOperationLimit(double preP) {
        setMaxOperationP(Math.min(getMaxP(), preP + getRamp_rate()));
        setMinOperationP(Math.max(getMinP(), preP - getRamp_rate()));
    }

    public double getMinOperationP() {
        return minOperationP;
    }

    public void setMinOperationP(double minOperationP) {
        this.minOperationP = minOperationP;
    }

    public double getMaxOperationP() {
        return maxOperationP;
    }

    public void setMaxOperationP(double maxOperationP) {
        this.maxOperationP = maxOperationP;
    }

    public int getMin_on_time() {
        return min_on_time;
    }

    public void setMin_on_time(int min_on_time) {
        this.min_on_time = min_on_time;
    }

    public int getMin_dn_time() {
        return min_dn_time;
    }

    public void setMin_dn_time(int min_dn_time) {
        this.min_dn_time = min_dn_time;
    }

    public boolean mustON(int t) {
        if (initialConditionHour <= 0) return false;
        if (initialConditionHour >= min_on_time) return false;
        if ((initialConditionHour + t + 1) > min_on_time) return false;

        return true;
    }

    public boolean mustOFF(int t) {
        if (initialConditionHour > 0) return false;
        if (initialConditionHour <= -min_dn_time) return false;
        if ((initialConditionHour - t - 1) < -min_dn_time) return false;

        return true;
    }

    public void setBusIdx(int busIdx) {
        this.busIdx = busIdx;
    }

    public int getBusIdx() {
        return busIdx;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Generator generator = (Generator) o;

        if (!name.equals(generator.name)) return false;

        return true;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean needStartupShutdownFlag() {
        if (min_on_time > 1) return true;
        if (min_dn_time > 1) return true;
        if (startupCost > 0) return true;
        return false;
    }
}