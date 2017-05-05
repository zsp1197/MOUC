package sjtu.scuc.academic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class EconomicDispatch1HourPiecewise extends EconomicDispatch1Hour {

    public double economicDispatch(final double p_load, final Generator[] on_gens, Integer[] on_gens_idx, List<LeqConstraint> constraints) {
        GeneratorWithPiecewiseCostCurve[] gens = new GeneratorWithPiecewiseCostCurve[on_gens.length];
        System.arraycopy(on_gens, 0, gens, 0, on_gens.length);

        // pick up the segments between operation min and operation max
        List<LambdaAndGen> lambdaAndGens = new ArrayList<LambdaAndGen>();
        for (GeneratorWithPiecewiseCostCurve gen : gens) {
            final double[] slopes = gen.getSlopes();
            final double[] breakPoints = gen.getBreakpoints();
            final double minOperateP = gen.getMinOperationP();
            final double maxOperateP = gen.getMaxOperationP();
            for (int i = 0; i < slopes.length; i++) {
                if (minOperateP >= breakPoints[i + 1]) continue;
                if (maxOperateP <= breakPoints[i]) break;
                LambdaAndGen lag = new LambdaAndGen();
                lag.setLambda(slopes[i]);
                lag.setGen(gen);
                lag.setSlopeIndex(i);

                lambdaAndGens.add(lag);
            }
        }

        // sort the generator's segments by ascending lambda
        Collections.sort(lambdaAndGens);

        double totalP = 0;
        for (GeneratorWithPiecewiseCostCurve gen : gens) {
            final double p = gen.getMinOperationP();
            gen.setP(p);
            totalP += p;
        }
        if (totalP < p_load) {
            for (LambdaAndGen lambdaAndGen : lambdaAndGens) {
                GeneratorWithPiecewiseCostCurve gen = lambdaAndGen.getGen();
                final double[] breakpoints = gen.getBreakpoints();
                final int index = lambdaAndGen.getSlopeIndex();
                final double deltaP = Math.min(breakpoints[index + 1], gen.getMaxOperationP()) - gen.getP();

                if ((totalP + deltaP) < p_load) {
                    gen.setP(gen.getP() + deltaP);
                    totalP = totalP + deltaP;
                } else {
                    gen.setP(gen.getP() + p_load - totalP);
                    break;
                }
            }
        }

        double ed = 0;
        for (Generator on_gen1 : on_gens) {
            ed += on_gen1.getGenCost(on_gen1.getP());
        }
        return ed;
    }

    class LambdaAndGen implements Comparable {
        double lambda;
        GeneratorWithPiecewiseCostCurve gen;
        int slopeIndex;

        public int compareTo(Object o) {
            LambdaAndGen n = (LambdaAndGen) o;

            if (this.getLambda() < n.getLambda()) return -1;
            else if (this.getLambda() > n.getLambda()) return 1;
            else return 0;
        }

        public double getLambda() {
            return lambda;
        }

        public void setLambda(double lambda) {
            this.lambda = lambda;
        }

        public GeneratorWithPiecewiseCostCurve getGen() {
            return gen;
        }

        public void setGen(GeneratorWithPiecewiseCostCurve gen) {
            this.gen = gen;
        }

        public int getSlopeIndex() {
            return slopeIndex;
        }

        public void setSlopeIndex(int slopeIndex) {
            this.slopeIndex = slopeIndex;
        }
    }

}