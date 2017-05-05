package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class GeneratorFactory {
    final public static String QuadraticCostCurve = "Quadratic";
    final public static String PiecewiseCostCurve = "Piecewise";

    public static Generator creator(final int min_on_time, final int min_dn_time, final int initialConditionHours
            , final String costCurveType, final double aConstant, final double aLinear, final double aQuadratic, final double Gasc, final double Gasb, final double Gasa) {
        assert (costCurveType.equalsIgnoreCase(QuadraticCostCurve));
        assert (min_on_time > 0);
        assert (min_dn_time > 0);

        Generator gen = new GeneratorWithQuadraticCostCurve(aConstant, aLinear, aQuadratic,Gasc,Gasb,Gasa);

        gen.setMin_on_time(min_on_time);
        gen.setMin_dn_time(min_dn_time);
        gen.setInitialConditionHour(initialConditionHours);
        gen.initial();

        return gen;
    }

    public static Generator creator(final int min_on_time, final int min_dn_time, final int initialConditionHours
            , final String costCurveType, final double maxP, final double minP
            , final double[] breakpoints, final double[] slopes, final double leftestPoint_x, final double leftestPoint_y) {
        assert (costCurveType.equalsIgnoreCase(PiecewiseCostCurve));
        assert (min_on_time > 0);
        assert (min_dn_time > 0);

        Generator gen = new GeneratorWithPiecewiseCostCurve(breakpoints, slopes, leftestPoint_x, leftestPoint_y);

        gen.setMin_on_time(min_on_time);
        gen.setMin_dn_time(min_dn_time);
        gen.setInitialConditionHour(initialConditionHours);
        gen.setMaxP(maxP);
        gen.setMinP(minP);
        gen.initial();

        return gen;
    }
}
