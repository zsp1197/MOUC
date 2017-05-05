package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class GeneratorWithPiecewiseCostCurve extends Generator {

    /**
     * the breakpoints of the piecewise linear function
     */
    double[] breakpoints = null;

    /**
     * the slope of each segment (that is, the rate of increase or decrease of the function between two breakpoints)
     */
    double[] slopes = null;

    /**
     * the geometric X coordinates of most left point of the function.
     */
    double leftestPoint_x;

    /**
     * the geometric Y coordinates of most left point of the function.
     */
    double leftestPoint_y;

    public void initial() {
        super.initial();

        breakpoints[0] = minP;
        breakpoints[breakpoints.length - 1] = maxP;
    }

    public GeneratorWithPiecewiseCostCurve(double[] breakpoints, double[] slopes, double leftestPoint_x, double leftestPoint_y) {
        if(breakpoints==null || breakpoints.length==0){
            this.breakpoints = new double[2];
        }else{
            this.breakpoints = new double[breakpoints.length + 2];
            System.arraycopy(breakpoints, 0, this.breakpoints, 1, breakpoints.length);
        }

        this.slopes = new double[slopes.length];
        System.arraycopy(slopes, 0, this.slopes, 0, slopes.length);

        this.leftestPoint_x = leftestPoint_x;
        this.leftestPoint_y = leftestPoint_y;
    }

    public double getGenCost(final double p) {
        double cost = leftestPoint_y;

        double deltaP;
        int i = 0;
        while (p > breakpoints[i]) {
            if (i == (breakpoints.length - 1)) {
                deltaP = p - breakpoints[i];
                cost += deltaP * slopes[i - 1];
                break;
            }

            if (p <= breakpoints[i + 1]) {
                deltaP = p - breakpoints[i];
            } else {
                deltaP = breakpoints[i + 1] - breakpoints[i];
            }
            cost += deltaP * slopes[i];

            i++;
        }
        return cost;
    }

    /**
     * Suppose the generator is ON
     *
     * @param lambda: lagrangian multiplier of totalLoad balance constraint
     * @return output generation level. between maximumP and minimumP
     */
    public double getOutput(final double lambda) {
        int i;
        for (i = 0; i < slopes.length; i++) {
            if (lambda < slopes[i]) break;
        }
        return breakpoints[i];
    }

    /**
     * Suppose the generator is ON
     *
     * @param lambda: lagrangian multiplier of totalLoad balance constraint
     * @return output generation level. between maxOperationP and minOperationP
     */
    public double getOperationOutput(final double lambda) {
        int i;
        for (i = 0; i < slopes.length; i++) {
            if (lambda < slopes[i]) break;
        }
        if (i == 0) return minOperationP;
        else if (i == slopes.length) return maxOperationP;
        else return breakpoints[i];
    }

    public double[] getBreakpoints() {
        return breakpoints;
    }

    public double[] getSlopes() {
        return slopes;
    }

    public double getLeftestPoint_x() {
        return leftestPoint_x;
    }

    public double getLeftestPoint_y() {
        return leftestPoint_y;
    }
}