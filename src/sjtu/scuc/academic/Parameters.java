package sjtu.scuc.academic;

import java.io.Serializable;

/**
 * Created by Zhai Shaopeng on 2017/2/4.
 */
public class Parameters implements Serializable {
    double min_change_price;

    public double getNormalization() {
        return normalization;
    }

    double normalization;

    public double getMin_change_price() {
        return min_change_price;
    }

    public void setMin_change_price(double min_change_price) {
        this.min_change_price = min_change_price;
    }

    public double getMaxTieline() {
        return maxTieline;
    }

    public void setMaxTieline(double maxTieline) {
        this.maxTieline = maxTieline;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    double maxTieline;
    double coefficient;

    public int getIters() {
        return iters;
    }

    public void setIters(int iters) {
        this.iters = iters;
    }

    int iters;

    public Parameters(double min_change_price, double maxTieline, double coefficient,int iters,double normalization) {
        this.min_change_price = min_change_price;
        this.maxTieline = maxTieline;
        this.coefficient = coefficient;
        this.iters = iters;
        this.normalization = normalization;
    }

    public void print() {
        System.out.println("min_change_price " +min_change_price);
        System.out.println("coefficient " +coefficient);
        System.out.println("iters "+iters);
        System.out.println("maxTieline "+maxTieline);
    }
}
