package sjtu.scuc.academic;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.List;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class EconomicDispatch1HourCplex extends EconomicDispatch1Hour {
    private static Log log = LogFactory.getLog(EconomicDispatch1HourCplex.class);

    public double economicDispatch(final double p_load, final Generator[] on_gens, Integer[] on_gens_idx, List<LeqConstraint> constraints) {
        double ed = 0;

        try {
            IloCplex cplex = new IloCplex();

            //turn off all logging output
            cplex.setOut(null);

            final int no_of_unit = on_gens.length;

            // create power level variable for each generator
            IloNumVar[] p = new IloNumVar[no_of_unit];
            // create cost variable and defination for each generator
            IloNumVar[] cost = new IloNumVar[no_of_unit];
            for (int i = 0; i < no_of_unit; i++) {
                p[i] = cplex.numVar(on_gens[i].getMinOperationP(), on_gens[i].getMaxOperationP(), IloNumVarType.Float, MessageFormat.format("p({0})", i));
                cost[i] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float, MessageFormat.format("c({0})", i));

                if (on_gens[i] instanceof GeneratorWithPiecewiseCostCurve) {
                    final GeneratorWithPiecewiseCostCurve gen = (GeneratorWithPiecewiseCostCurve) on_gens[i];
                    double[] breakpoints = gen.getBreakpoints();
                    IloNumVar[] p_seg = new IloNumVar[breakpoints.length - 1];
                    for (int j = 0; j < p_seg.length; j++) {
                        p_seg[j] = cplex.numVar(0, breakpoints[j + 1] - breakpoints[j], IloNumVarType.Float, MessageFormat.format("p({0},{1})", i, j));
                    }

                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(p[i], 1.0);
                    for (int j = 0; j < p_seg.length; j++) {
                        expr.addTerm(p_seg[j], -1.0);
                    }
                    cplex.addEq(expr, breakpoints[0], MessageFormat.format("def_p_{0}", i));

                    double[] slopes = gen.getSlopes();
                    expr = cplex.linearNumExpr();
                    expr.addTerm(cost[i], -1.0);
                    expr.addTerms(p_seg, slopes);
                    cplex.addEq(expr, -gen.getLeftestPoint_y(), MessageFormat.format("def_c_{0}", i));
                } else if (on_gens[i] instanceof GeneratorWithQuadraticCostCurve) {
                    final GeneratorWithQuadraticCostCurve gen = (GeneratorWithQuadraticCostCurve) on_gens[i];
                    final double aConst = gen.getAConstant();
                    final double aLinear = gen.getALinear();
                    final double aQuadratic = gen.getAQuadratic();

                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(cost[i], -1.0);
                    expr.addTerm(p[i], aLinear);
                    cplex.addLe(cplex.sum(cplex.prod(p[i], p[i], aQuadratic), expr), -aConst
                            , MessageFormat.format("def_c_{0}", i));
                }
            }

            // totalLoad balance equation constraint
            cplex.addEq(cplex.sum(p), p_load, "load_banlance");

            // other leq constraints
            if (constraints != null)
                for (LeqConstraint con : constraints) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < no_of_unit; ++i) {
                        expr.addTerm(p[i], con.getA(on_gens_idx[i]));
                    }
                    cplex.addLe(expr, con.getB());
                }

            // objective funciton
            IloLinearNumExpr expr = cplex.linearNumExpr();
            for (int i = 0; i < no_of_unit; ++i) {
                expr.addTerm(cost[i], 1.);
            }
            cplex.addMinimize(expr, "cost");

            if (isOutputDetail()) cplex.exportModel("economidDispatch1HourCplex.lp");
            if (cplex.solve()) {
                for (int i = 0; i < no_of_unit; ++i) {
                    on_gens[i].setP(cplex.getValue(p[i]));
                }
                ed = cplex.getObjValue();
            } else {
                ed = penaltyCost;
            }
            cplex.end();
        } catch (IloException exc) {
            log.error(exc);
        }
        return ed;
    }
}