package sjtu.scuc.academic;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class BenderDCCut {
    protected static Log log = LogFactory.getLog(BenderDCCut.class);

    private String name;

    protected boolean outputDetail = false;

    /**
     * Node-branch incidence matrix
     */
    private double[][] K = null;

    /**
     * Node-unit incidence matrix
     */
    private int[][] A = null;

    /**
     * Node-totalLoad incidence matrix
     */
    private int[][] B = null;

    /**
     * unit output
     */
    private double[] p = null;

    /**
     * totalLoad
     */
    private double[] l = null;

    /**
     * reactance of branch
     */
    private double[] x = null;

    /**
     * bus index
     */
    private int[] fromBus = null;
    /**
     * bus index
     */
    private int[] toBus = null;

    /**
     * max power flow through branch
     */
    private double[] flowLimit = null;

    private double objValue;

    // output data
    private double[] coef = null;
    private double rh;

    public void calculateCuts() {
        final int no_of_bus = K.length;
        final int no_of_branch = K[0].length;
        final int no_of_gen = A[0].length;
        final int no_of_load = B[0].length;

        try {
            IloCplex cplex = new IloCplex();

            // in order to get the dual variables
            IloRange row[] = new IloRange[no_of_bus];

            // power flow through branches
            String[] pl_name = new String[no_of_branch];
            for (int i = 0; i < pl_name.length; i++) pl_name[i] = MessageFormat.format("pl{0}", i);
            IloNumVar[] pl = cplex.numVarArray(no_of_branch, -Double.MAX_VALUE, Double.MAX_VALUE, pl_name);
            String[] theta_name = new String[no_of_bus];
            for (int i = 0; i < theta_name.length; i++) theta_name[i] = MessageFormat.format("theta{0}", i);
            IloNumVar[] theta = cplex.numVarArray(no_of_bus, -Double.MAX_VALUE, Double.MAX_VALUE, theta_name);
            theta[0].setLB(0.0);
            theta[0].setUB(0.0);
            String[] s_name = new String[2 * no_of_branch];
            for (int i = 0; i < s_name.length; i++) s_name[i] = MessageFormat.format("s{0}", i);
            IloNumVar[] s = cplex.numVarArray(2 * no_of_branch, 0.0, Double.MAX_VALUE, s_name);

            //
            for (int i = 0; i < no_of_bus; i++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                expr.addTerms(pl, K[i]);

                double righthand = 0.;
                for (int j = 0; j < no_of_gen; j++) righthand += A[i][j] * p[j];
                for (int j = 0; j < no_of_load; j++) righthand -= B[i][j] * l[j];

                row[i] = cplex.addEq(expr, righthand);
                row[i].setName(MessageFormat.format("mu{0}", i));
            }

            //
            for (int i = 0; i < no_of_branch; i++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                expr.addTerm(pl[i], x[i]);
                expr.addTerm(theta[fromBus[i]], -1.0);
                expr.addTerm(theta[toBus[i]], 1.0);
                cplex.addEq(expr, 0);

                expr = cplex.linearNumExpr();
                expr.addTerm(pl[i], 1.0);
                expr.addTerm(s[2 * i], -1.0);
                cplex.addLe(expr, flowLimit[i]);

                expr = cplex.linearNumExpr();
                expr.addTerm(pl[i], 1.0);
                expr.addTerm(s[2 * i + 1], 1.0);
                cplex.addGe(expr, -flowLimit[i]);
            }

            IloLinearNumExpr expr = cplex.linearNumExpr();
            for (int i = 0; i < s.length; i++) expr.addTerm(s[i], 1.0);
            cplex.addMinimize(expr);

            if (outputDetail) {
                if (name != null && name.length() > 0) cplex.exportModel(MessageFormat.format("{0}.lp", name));
                else cplex.exportModel("BenderDCCut.lp");
            } else {
                cplex.setOut(null);
            }

            if (cplex.solve()) {
                objValue = cplex.getObjValue();

                if (hasViolations()) {
                    coef = new double[no_of_gen];
                    final double[] duals = cplex.getDuals(row);
                    for (int i = 0; i < no_of_gen; i++) {
                        for (int j = 0; j < no_of_bus; j++) {
                            coef[i] += duals[j] * A[j][i];
                        }
                    }

                    rh = -objValue;
                    for (int i = 0; i < no_of_gen; i++) rh += coef[i] * p[i];
                }
            }
            cplex.end();
        }
        catch (IloException e) {
            log.error(e);
        }
    }

    public void setK(double[][] k) {
        K = k;
    }

    public void setA(int[][] a) {
        A = a;
    }

    public void setB(int[][] b) {
        B = b;
    }

    public void setP(double[] p) {
        this.p = p;
    }

    public void setL(double[] l) {
        this.l = l;
    }

    public void setX(double[] x) {
        this.x = x;
    }

    public void setFromBus(int[] fromBus) {
        this.fromBus = fromBus;
    }

    public void setToBus(int[] toBus) {
        this.toBus = toBus;
    }

    public void setFlowLimit(double[] flowLimit) {
        this.flowLimit = flowLimit;
    }

    public double[] getCoef() {
        return coef;
    }

    public double getRh() {
        return rh;
    }

    public boolean hasViolations() {
        return objValue > 0;
    }

    public boolean isOutputDetail() {
        return outputDetail;
    }

    public void setOutputDetail(boolean outputDetail) {
        this.outputDetail = outputDetail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
