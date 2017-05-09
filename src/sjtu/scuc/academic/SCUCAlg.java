package sjtu.scuc.academic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public abstract class SCUCAlg implements Algorithmicable {

    // TODO: add shutdown cost for generators
    // TODO: add Spinning Reserve and Operating Reserve constraints
    // TODO: test large system
    // TODO: add hydro unit, cascaded hydro unit, combined-cycle unit, pumped-storage unit

    protected static Log log = LogFactory.getLog(SCUCAlg.class);

    protected boolean outputDetail = false;
    protected double gapTolerance = 0.03;

    private boolean respectStartupShutdownOutput = false;
    private boolean respectNetworkConstraints = false;
    private boolean respectContingency = false;

    private boolean useBenderDecomposition = false;

    protected int maxIteration = 1000; // default value
    protected final double ZERO = 0.01;

    protected SCUCData scucData;

    /**
     * optimal gen output [gens][ti]
     */
    protected double[][] genOutput = null;

    /**
     * optimal gen status [gens][ti]
     */
    protected int[][] genStatus = null;
    private long endTime;
    private long beginTime;
    protected int cur_iteration;

    public Calresult optimize() {
        beginTime = System.currentTimeMillis();
        Calresult result = null;
        try {
            result = solve();
            result.setSCUCData(scucData);
//            System.out.println("**************************************************************************************");
//            System.out.println(Tools.getObjValue(result,scucData,1));
//            System.out.println(Utility.getTotalActualCost(scucData.getGenList(), result.getGenStatus(), result.getGenOutput()));

//            // get unit status
//            scucData.setGenStatus(genStatus);
//
//            // TODO: check min_on/dn_time constraint
//
//            // execute economic dispatch to get the power output of each unit at each hour
//            EconomicDispatchable ed = new MIPAlg();
//            ed.setOutputDetail(isOutputDetail());
//            ed.setRespectStartupShutdownOutput(isRespectStartupShutdownOutput());
//            ed.solve(genStatus, scucData);
//            scucData.setGenOutput(ed.getGenOut());
//
//            // do not need to check reserve constraint because
////            checkReserveConstraint();
//
//            // calculate total cost
//            final double cost = Utility.getTotalActualCost(scucData.getGenList(), genStatus, scucData.getGenOutput());
//            scucData.setObjective(cost);

            endTime = System.currentTimeMillis();
//            print_results(false);
        } catch (InfeasibleException e) {
            log.error(e);
            print_results(true);
        }
        return result;
    }

    /**
     * solve the security constrainted unit commitment problem
     */
    public Calresult solve() throws InfeasibleException {
        beforehand_process();
        Calresult result = null;
        result = callSolver("Whole_Model");
        afterward_process();
        return result;
    }

    abstract protected void beforehand_process();

    abstract protected void afterward_process();

    abstract protected void addCuts(List<LeqConstraint> cuts);

    abstract protected Calresult callSolver(final String s) throws InfeasibleException;

    private void checkReserveConstraint() throws InfeasibleException {
        double[] reserve = scucData.getReserve();
        if (reserve == null || reserve.length != scucData.getTiNum()) return;

        final Generator[] gens = scucData.getGens();
        final int[][] status = scucData.getGenStatus();
        final double[][] output = scucData.getGenOutput();
        for (int t = 0; t < scucData.getTiNum(); t++) {
            double totalCapcity = 0;
            double totalOutput = 0;
            for (int i = 0; i < scucData.getGenNum(); i++) {
                if (status[i][t] == 1) {
                    totalCapcity += gens[i].getMaxP();
                    totalOutput += output[i][t];
                }
            }
            if (totalCapcity < totalOutput + reserve[t]) {
                String s = MessageFormat.format("Insufficient capacity to respect reserve constraint at hour {0}", t);
                throw new InfeasibleException(s, scucData);
            }
        }
    }

    public static String printStr(final String s, int width) {
        StringBuilder sb = new StringBuilder(width);
        int padding = Math.max(1, width - s.length()); // At _least_ 1 space
        for (int k = 0; k < padding; k++) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    private void print_results(final boolean onlyPrintUnitStatus) {
        Generator[] gens = scucData.getGens();
        int[][] gen_status = scucData.getGenStatus();
        double[][] gen_out = scucData.getGenOutput();
        int no_of_gen = scucData.getGenNum();
        int no_of_ti = scucData.getTiNum();

        if (!onlyPrintUnitStatus) {
            log.info("==========Final Solution==========");
            log.info(MessageFormat.format("Optimal solution: Total cost = {0,number,#.###}", scucData.getObjective()));
            log.info(MessageFormat.format("Solution time = {0,number,#.######} sec.", (endTime - beginTime) / 1000.0));
        }

        StringBuilder sb;
        // title
        sb = new StringBuilder();
        final int hourLength = 5;
        sb.append(printStr("Hour", hourLength));
        final int statusLength = 6;
        for (int i = 0; i < no_of_gen; i++) sb.append(printStr("S_" + gens[i].getName(), statusLength));
        final int powerLength = 8;
        if (!onlyPrintUnitStatus) {
            for (int i = 0; i < no_of_gen; i++) sb.append(printStr("P_" + gens[i].getName(), powerLength));
        }
        log.info(sb.toString());

        // details
        for (int t = 0; t < no_of_ti; t++) {
            sb = new StringBuilder();
            sb.append(printStr(MessageFormat.format("{0,number,#}", t), hourLength));
            for (int i = 0; i < no_of_gen; i++)
                sb.append(printStr(MessageFormat.format("{0,number}", gen_status[i][t]), statusLength));

            if (!onlyPrintUnitStatus) {
                for (int i = 0; i < no_of_gen; i++)
                    sb.append(printStr(MessageFormat.format("{0,number,####.###}", gen_out[i][t]), powerLength));
            }
            log.info(sb.toString());
        }
    }

    public double getGapTolerance() {
        return gapTolerance;
    }

    public void setGapTolerance(double gap_tolerance) {
        this.gapTolerance = gap_tolerance;
    }

    public int getMaxIteration() {
        return maxIteration;
    }

    public void setMaxIteration(int maxIteration) {
        this.maxIteration = maxIteration;
    }

    public SCUCData getScucData() {
        return scucData;
    }

    public void setScucData(SCUCData scucData) {
        this.scucData = scucData;
    }

    public boolean isOutputDetail() {
        return outputDetail;
    }

    public void setOutputDetail(boolean output_detail) {
        this.outputDetail = output_detail;
    }

    public boolean isRespectStartupShutdownOutput() {
        return respectStartupShutdownOutput;
    }

    public void setRespectStartupShutdownOutput(boolean respectStartupShutdownOutput) {
        this.respectStartupShutdownOutput = respectStartupShutdownOutput;
    }

    public void setRespectNetworkConstraints(boolean respectNetworkConstraints) {
        this.respectNetworkConstraints = respectNetworkConstraints;
    }

    public boolean isRespectNetworkConstraints() {
        return respectNetworkConstraints;
    }

    public void setRespectContingency(boolean respectContingency) {
        this.respectContingency = respectContingency;
    }

    public boolean isRespectContingency() {
        return respectContingency;
    }

    protected abstract void addBranchPowerflowConstraints(final int no_of_branch, final Branch[] branchs, final double[][] PTDF, final String constraintName);

    protected void addContingencyConstraints() {
        List<Branch> branchList = scucData.getBranchList();
        final int no_of_branch = branchList.size();
        Branch branches[] = branchList.toArray(new Branch[no_of_branch]);

        Set<Integer> branchContingencySet = scucData.getBranchContingencySet();
        Integer[] branchIndexes = branchContingencySet.toArray(new Integer[branchContingencySet.size()]);
        for (Integer branchIndex : branchIndexes) {
            // get branches without contingency branch
            Branch remain_branches[] = new Branch[no_of_branch - 1];
            System.arraycopy(branches, 0, remain_branches, 0, branchIndex);
            System.arraycopy(branches, branchIndex + 1, remain_branches, branchIndex, no_of_branch - branchIndex - 1);

            // calculate PTDF[no_of_branch][no_of_bus]
            double[][] PTDF = Utility.calculatePTDF(remain_branches, scucData.getNo_of_bus(), scucData.getRefBusIdx());

            // add constraints for all time intervals
            addBranchPowerflowConstraints(no_of_branch - 1, remain_branches, PTDF
                    , MessageFormat.format("pf_con_{0}", branches[branchIndex].getName()));
        }
    }

    protected void addNetworkConstraints() {
        final List<Branch> branchList = scucData.getBranchList();
        final int no_of_branch = branchList.size();
        if (no_of_branch == 0) return;

        //
        Branch branches[] = branchList.toArray(new Branch[no_of_branch]);

        // calculate PTDF[no_of_branch][no_of_bus]
        double[][] PTDF = Utility.calculatePTDF(branches, scucData.getNo_of_bus(), scucData.getRefBusIdx());

        // add constraints for all time intervals
        addBranchPowerflowConstraints(no_of_branch, branches, PTDF, "pf");
    }

    public boolean isUseBenderDecomposition() {
        return useBenderDecomposition;
    }

    public void setUseBenderDecomposition(boolean useBenderDecomposition) {
        this.useBenderDecomposition = useBenderDecomposition;
    }

    protected void checkNormalNetworkPowerflowConstraint(List<LeqConstraint> cuts, int iteration) {
        // check normal network power flow and get cuts
        double[][] K = scucData.getNodeBranchIncidenceArray();
        int[][] A = scucData.getNodeUnitIncidenceArray();
        int[][] B = scucData.getNodeLoadIncidenceArray();

        double[][] data = scucData.getBranchDataArray();
        double[] x = data[SCUCData.REACTANCE_IDX];
        int[] fromBus = new int[x.length];
        for (int i = 0; i < fromBus.length; i++) fromBus[i] = (int) data[SCUCData.FROMBUS_IDX][i];
        int[] toBus = new int[x.length];
        for (int i = 0; i < toBus.length; i++) toBus[i] = (int) data[SCUCData.TOBUS_IDX][i];
        double[] flowLimit = data[SCUCData.CAPACITY_IDX];

        BenderDCCut benderDCCut = new BenderDCCut();
        benderDCCut.setOutputDetail(isOutputDetail());
        benderDCCut.setK(K);
        benderDCCut.setA(A);
        benderDCCut.setB(B);
        benderDCCut.setX(x);
        benderDCCut.setFromBus(fromBus);
        benderDCCut.setToBus(toBus);
        benderDCCut.setFlowLimit(flowLimit);

        double[][] p = Utility.getGenOutTimeGen(genOutput);
        for (int t = 0; t < scucData.getTiNum(); t++) {
            benderDCCut.setP(p[t]);
            double[] l = scucData.getLoadAtTime(t);
            benderDCCut.setL(l);

            benderDCCut.setName(MessageFormat.format("NormalNetwork_I{0}_T{1}", iteration, t));
            benderDCCut.calculateCuts();

            if (benderDCCut.hasViolations()) {
                double[] coef = benderDCCut.getCoef();
                double rh = benderDCCut.getRh();

                LeqConstraint con = new LeqConstraint();
                con.setName(MessageFormat.format("Network_Cut_I{0}_T{1}", iteration, t));
                con.setTimeInterval(t);
                con.setA(coef);
                con.setB(rh);
                con.setMu(1.0);

                cuts.add(con);
            }
        }
    }


    protected void checkContingencyNetworkPowerflowConstraint(List<LeqConstraint> cuts, int iteration) {
        // check branch contingency network power flow and get cuts
        for (Integer contingencyBranchIndex : scucData.getBranchContingencySet()) {
            double[][] K = scucData.getNodeBranchIncidenceArray(contingencyBranchIndex);
            int[][] A = scucData.getNodeUnitIncidenceArray();
            int[][] B = scucData.getNodeLoadIncidenceArray();

            double[][] data = scucData.getBranchDataArray(contingencyBranchIndex);
            double[] x = data[SCUCData.REACTANCE_IDX];
            int[] fromBus = new int[x.length];
            for (int i = 0; i < fromBus.length; i++) fromBus[i] = (int) data[SCUCData.FROMBUS_IDX][i];
            int[] toBus = new int[x.length];
            for (int i = 0; i < toBus.length; i++) toBus[i] = (int) data[SCUCData.TOBUS_IDX][i];
            double[] flowLimit = data[SCUCData.CAPACITY_IDX];

            BenderDCCut benderDCCut = new BenderDCCut();
            benderDCCut.setOutputDetail(isOutputDetail());
            benderDCCut.setK(K);
            benderDCCut.setA(A);
            benderDCCut.setB(B);
            benderDCCut.setX(x);
            benderDCCut.setFromBus(fromBus);
            benderDCCut.setToBus(toBus);
            benderDCCut.setFlowLimit(flowLimit);

            double[][] p = Utility.getGenOutTimeGen(genOutput);
            for (int t = 0; t < scucData.getTiNum(); t++) {
                benderDCCut.setP(p[t]);
                double[] l = scucData.getLoadAtTime(t);
                benderDCCut.setL(l);

                benderDCCut.setName(MessageFormat.format("Ctg_B{2}_I{0}_T{1}", iteration, t, contingencyBranchIndex));
                benderDCCut.calculateCuts();

                if (benderDCCut.hasViolations()) {
                    double[] coef = benderDCCut.getCoef();
                    double rh = benderDCCut.getRh();

                    LeqConstraint con = new LeqConstraint();
                    con.setName(MessageFormat.format("Ctg_B{2}_Cut_I{0}_T{1}"
                            , iteration, t, contingencyBranchIndex));
                    con.setTimeInterval(t);
                    con.setA(coef);
                    con.setB(rh);
                    con.setMu(1.0);

                    cuts.add(con);
                }
            }
        }
    }
}