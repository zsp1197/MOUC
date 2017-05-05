package sjtu.scuc.academic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import util.HToString;

import java.util.Arrays;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class LeqConstraint {

    private static Log log = LogFactory.getLog(LeqConstraint.class);

    private int timeInterval;

    /**
     * corresponding lagrangian multiply
     */
    private double mu = 0;

    /**
     * coefficients of generator output level
     */
    private double[] a = null;

    /**
     * right hand side value
     */
    private double b;
    //    private double mu_plus = 0.02;
    //    private double mu_minus = 0.002;
    private double mu_plus = 0.05;
    private double mu_minus = 0.01;
    private String name;


    public double getDualValueComponent(final double[] genOut) {
        return mu * getConstraintValue(genOut);
    }

    public double getConstraintValue(final double[] genOut) {
        double v = 0;
        for (int i = 0; i < a.length; i++) {
            v += a[i] * genOut[i];
        }
        v -= b;
        return v;
    }

    public void updateMU(final double[] gen_out) {
        final double dq_dmu = getConstraintValue(gen_out);
        if (dq_dmu > 0) mu += mu_plus * dq_dmu;
        else if (dq_dmu < 0) mu += mu_minus * dq_dmu;

        if (mu < 0) mu = 0;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
    }

    public double getMu() {
        return mu;
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    public double[] getA() {
        return a;
    }

    public void setA(double[] a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getMu_plus() {
        return mu_plus;
    }

    public void setMu_plus(double mu_plus) {
        this.mu_plus = mu_plus;
    }

    public double getMu_minus() {
        return mu_minus;
    }

    public void setMu_minus(double mu_minus) {
        this.mu_minus = mu_minus;
    }

    public double getA(int gen_idx) {
        return a[gen_idx];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LeqConstraint that = (LeqConstraint) o;

        if (Double.compare(that.b, b) != 0) return false;
        if (timeInterval != that.timeInterval) return false;
        if (!Arrays.equals(a, that.a)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        result = timeInterval;
        result = 31 * result + Arrays.hashCode(a);
        temp = b != +0.0d ? Double.doubleToLongBits(b) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }


    public String toString() {
        return new StringBuilder().append("LeqConstraint{").append("name='").append(name).append('\'').append(", timeInterval=").append(timeInterval).append(", aa=").append(HToString.convert(a, a.length)).append(", b=").append(b).append('}').toString();
    }
}