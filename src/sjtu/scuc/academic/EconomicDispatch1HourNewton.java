package sjtu.scuc.academic;
import java.util.List;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class EconomicDispatch1HourNewton extends EconomicDispatch1Hour {

    public double economicDispatch(final double p_load, final Generator[] on_gens, Integer[] on_gens_idx, List<LeqConstraint> constraints) {
        double error = 0, lambda = 0;
        double error1 = 0, lambda1 = 0;
        double error2 = 0, lambda2 = 0;

        // find initial solutions
        // lambda1 is the point whose error1 is less than 0
        // lambda2 is the point whose error2 is larger than 0
        int step = 1;
        while (!(error1 < 0 && error2 > 0)) {
            double p = 0;
            for (Generator on_gen : on_gens) p += on_gen.getOperationOutput(lambda);

            error = p - p_load;

            if (Math.abs(error) < gapTolerance) {
                break;
            } else if (error < 0) {
                error1 = error;
                lambda1 = lambda;
            } else {
                error2 = error;
                lambda2 = lambda;
            }

            lambda += step;
            step *= 2;
        }

        // then use Newton method to find the lambda to make error equal to 0
        while (Math.abs(error) > gapTolerance) {
            lambda = lambda1 + (lambda2 - lambda1) * (0 - error1) / (error2 - error1);

            double p = 0;
            for (Generator on_gen : on_gens) p += on_gen.getOperationOutput(lambda);

            error = p - p_load;

            if (error < 0) {
                error1 = error;
                lambda1 = lambda;
            } else {
                error2 = error;
                lambda2 = lambda;
            }
        }

        double ed = 0;
        for (Generator on_gen1 : on_gens) {
            final double output = on_gen1.getOperationOutput(lambda);
            on_gen1.setP(output);
            ed += on_gen1.getGenCost(output);
        }
        return ed;
    }

}