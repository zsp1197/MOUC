package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class GeneratorWithQuadraticCostCurve extends Generator {

    double b[]={0,0,0};

    public double[] getOria() {
        return oria;
    }


    public double getGasa(){
        return b[2];
    }
    public double getGasb(){
        return b[1];
    }
    public double getGasc(){
        return b[0];
    }
    public GeneratorWithQuadraticCostCurve(double aConstant, double aLinear, double aQuadratic,double Gasc,double Gasb,double Gasa) {
        oria[0] = aConstant;
        oria[1] = aLinear;
        oria[2] = aQuadratic;
        b[0]=Gasc;
        b[1]=Gasb;
        b[2]=Gasa;
        this.setA(oria);
    }

    public double getAConstant(){
        return this.getA()[0];
    }
    public double getALinear(){
        return this.getA()[1];
    }
    public double getAQuadratic(){
        return this.getA()[2];
    }



    public double getGenCost(final double p) {
        double cost = this.getA()[this.getA().length - 1];
        for (int i = this.getA().length - 2; i >= 0; i--) {
            cost = this.getA()[i] + p * cost;
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
        double p = lambda - this.getA()[1];
        if (Math.abs(this.getA()[2]) > 0.001) p = p / (2 * this.getA()[2]);
        if (p < minP) p = minP;
        else if (p > maxP) p = maxP;
        return p;
    }

    /**
     * Suppose the generator is ON
     *
     * @param lambda: lagrangian multiplier of totalLoad balance constraint
     * @return output generation level. between maxOperationP and minOperationP
     */
    public double getOperationOutput(final double lambda) {
        double p = lambda - this.getA()[1];
        if (Math.abs(this.getA()[2]) > 0.001) p = p / (2 * this.getA()[2]);
        if (p < minOperationP) p = minOperationP;
        else if (p > maxOperationP) p = maxOperationP;
        return p;
    }
}
