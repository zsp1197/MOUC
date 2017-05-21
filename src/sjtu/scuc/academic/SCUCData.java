package sjtu.scuc.academic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.*;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class SCUCData extends Throwable {

    protected static Log log = LogFactory.getLog(SCUCData.class);

    protected List<Generator> genList = new ArrayList<Generator>();

    /**
     * totalLoad need to be balanced
     */
    protected double[] totalLoad = null;

    public double[] getOriTotalLoad() {
        return oriTotalLoad;
    }

    public void setOriTotalLoad(double[] oriTotalLoad) {
        this.oriTotalLoad = oriTotalLoad;
    }

    protected double[] oriTotalLoad = null;
    /**
     * generation reserve need to be reserved
     */
    protected double[] reserve = null;

    protected List<LeqConstraint> constraints = new ArrayList<LeqConstraint>();
    protected Map<Integer, List<LeqConstraint>> ti_constraints_map = new HashMap<Integer, List<LeqConstraint>>();

    // output

    private double[] nomalize_coefficentes;

    /**
     * [gen_idx][ti]
     * 1列代表发电机编号，2列代表对应编号发电机的状态
     */
    private int[][] genStatus;
    /**
     * [gen_idx][ti]
     */
    private double[][] genOutput;

    /**
     * [gen_idx][ti]
     */
    private int[][] genConditionHours;

    public double getNormalization() {
        return normalization;
    }


    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    private double normalization=1e-6;

//    Notice! the result12 here is modified as the bestObjValue is the average among all systems

    public void setTargetflag(int targetflag) {
        this.targetflag = targetflag;
    }

    private int targetflag;

    public String getMode() {
        return mode;
    }

    public int getKsplit() {
        return ksplit;
    }
    private int ksplit=20;

    public void setMode(String mode) {
        this.mode = mode;
    }
    private String mode="mo";

    public boolean isRamp() {
        return isRamp;
    }

    public void setRamp(boolean ramp) {
        isRamp = ramp;
    }

    boolean isRamp=true;

    double k;
    public double getBigger() {
        return bigger;
    }

    public void setBigger(double bigger) {
        this.bigger = bigger;
    }

    public double getK() {
        return k;
    }

    public void setK(double k) {
        this.k = k;
    }

    double bigger;

    private double objective;
    private List<Branch> branchList = new ArrayList<Branch>();
    private int refBusIdx;
    private int no_of_bus = 0;
    private List<BusLoad> busLoadList = new ArrayList<BusLoad>();
    /**
     * contingency branch index
     */
    private Set<Integer> branchContingencySet = new HashSet<Integer>();
    private double[][] nodeBranchIncidenceArray = null;
    private int[][] nodeUnitIncidenceArray = null;
    private int[][] nodeLoadIncidenceArray = null;
    private double[][] branchDataArray = null;

    final public static int REACTANCE_IDX = 0;
    final public static int FROMBUS_IDX = 1;
    final public static int TOBUS_IDX = 2;
    final public static int CAPACITY_IDX = 3;
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    int index=0;

    public double getCapability(){
        double capability=0;
        Generator[] gens=getGens();
        for (int i = 0; i < gens.length; i++) {
            capability=capability+gens[i].getMaxP();
        }
        return capability;
    }

    public double[] getTotalLoad() {
        return totalLoad;
    }

    //将本文中totalload设置为输入totalload
    public void setTotalLoad(double[] totalLoad) {
        if (totalLoad == null) return;

        final int len = totalLoad.length;
        if (len == 0) return;

        this.totalLoad = new double[len];
        System.arraycopy(totalLoad, 0, this.totalLoad, 0, len);
    }
    //将本文中reserve设置为输入reserve
    public void setReserve(double[] reserve) {
        if (reserve == null) return;

        final int len = reserve.length;
        if (len == 0) return;

        this.reserve = new double[len];
        System.arraycopy(reserve, 0, this.reserve, 0, len);
    }
    public List<LeqConstraint> getConstraints() {
        return constraints;
    }

    public List<Generator> getGenList() {
        return genList;
    }

    public void setGenList(List<Generator> genList) {
        int i = 1;
        for (Generator gen : genList) {
            final String name = gen.getName();
            if (name == null || name.length() == 0) {
                gen.setName(MessageFormat.format("U{0}", i++));
            }
        }
        this.genList = genList;
    }

    public void addGenerator(Generator gen) {
        final String name = gen.getName();
        if (name == null || name.length() == 0) {
            gen.setName(MessageFormat.format("U{0}", genList.size()));
        }
        if (gen.getInitialConditionHour() > 0 && gen.getInitialP() < gen.getMinP()) gen.setInitialP(gen.getMinP());
        genList.add(gen);
    }

    public void addConstraint(LeqConstraint con) {
        Utility.addConstraint(con, constraints, ti_constraints_map);
    }

    public int getGenNum() {
        return genList.size();
    }

    public int getTiNum() {
        return totalLoad.length;
    }

    public double[] getReserve() {
        return reserve;
    }

    public Map<Integer, List<LeqConstraint>> getTi_constraints_map() {
        return ti_constraints_map;
    }


    public int[][] getGenStatus() {
        return genStatus;
    }

    public int[][] getGenConditionHours() {
        return genConditionHours;
    }

    public double[][] getGenOutput() {
        return genOutput;
    }

    public void setGenStatus(int[][] genStatus) {
        // set genStatus. 1:ON, 0:OFF.
        this.genStatus = genStatus;

        // update generator condition hours.
        genConditionHours = new int[getGenNum()][getTiNum()];

        Generator[] gens = getGens();
        for (int i = 0; i < getGenNum(); i++) {

            int t = 0;
            int initialCondionHour = gens[i].getInitialConditionHour();
            // if initial contidion hours are 0, we take it as off for 1 hours.
//            如果发电机已经打开，就运行时间加一小时
            if (initialCondionHour == 0) initialCondionHour = -1;
            if (initialCondionHour > 0) { // on
                if (genStatus[i][t] == 0) {
                    genConditionHours[i][t] = -1;
                } else if (genStatus[i][t] == 1) {
                    genConditionHours[i][t] = initialCondionHour + 1;
                }
            } else { // off   下面对应的是初始状态
                if (genStatus[i][t] == 0) {
                    genConditionHours[i][t] = initialCondionHour - 1;
                } else if (genStatus[i][t] == 1) {
                    genConditionHours[i][t] = 1;
                }
            }

            //
            for (t = 1; t < getTiNum(); t++) {
                if (genConditionHours[i][t - 1] > 0) { // on
                    if (genStatus[i][t] == 0) {
                        genConditionHours[i][t] = -1;
                    } else if (genStatus[i][t] == 1) {
                        genConditionHours[i][t] = genConditionHours[i][t - 1] + 1;
                    }
                } else { // off 如果之前关闭，则继续关闭一小时，否则不变
                    if (genStatus[i][t] == 0) {
                        genConditionHours[i][t] = genConditionHours[i][t - 1] - 1;
                    } else if (genStatus[i][t] == 1) {
                        genConditionHours[i][t] = 1;
                    }
                }
            }

        }
    }

    public void setGenOutput(double[][] genOutput) {
        this.genOutput = genOutput;
    }

    public double getObjective() {
        return objective;
    }

    public void setObjective(double objective) {
        this.objective = objective;
    }

    public Generator[] getGens() {

//        for (int i = 0; i < genList.size(); i++) {
//            genList.get(i).setInitialP(0);
//            genList.get(i).setInitialConditionHour(0);
//        }

        return genList.toArray(new Generator[genList.size()]);
    }

    public void addBranch(Branch branch) {
        if (no_of_bus < (branch.getFromBusIdx() + 1)) no_of_bus = branch.getFromBusIdx() + 1;
        if (no_of_bus < (branch.getToBusIdx() + 1)) no_of_bus = branch.getToBusIdx() + 1;
        branchList.add(branch);
    }

    public List<Branch> getBranchList() {
        return branchList;
    }

    public void setRefBusIdx(int refBusIdx) {
        this.refBusIdx = refBusIdx;
    }

    public int getRefBusIdx() {
        return refBusIdx;
    }

    public int getNo_of_bus() {
        return no_of_bus;
    }

    public void addBusLoad(BusLoad busLoad) {
        final String name = busLoad.getName();
        if (name == null || name.length() == 0) {
            busLoad.setName(MessageFormat.format("L{0}", busLoadList.size()));
        }
        busLoadList.add(busLoad);
    }

    public List<BusLoad> getBusLoadList() {
        return busLoadList;
    }

    public void addBranchContingencyForAllTime(int branchIndex) {
        branchContingencySet.add(branchIndex);
    }

    public Set<Integer> getBranchContingencySet() {
        return branchContingencySet;
    }

    public double[] getLoadAtTime(int t) {
        double[] loads = new double[busLoadList.size()];
        for (int i = 0; i < loads.length; i++) loads[i] = busLoadList.get(i).getLoad()[t];
        return loads;
    }

    public double[][] getNodeBranchIncidenceArray() {
        if (nodeBranchIncidenceArray == null) {
            final List<Branch> list = getBranchList();
            final int no_of_branch = list.size();
            final Branch branches[] = list.toArray(new Branch[no_of_branch]);
            nodeBranchIncidenceArray = new double[no_of_bus][no_of_branch];
            for (int i = 0; i < no_of_branch; i++) {
                nodeBranchIncidenceArray[branches[i].getFromBusIdx()][i] = 1;
                nodeBranchIncidenceArray[branches[i].getToBusIdx()][i] = -1;
            }
        }
        return nodeBranchIncidenceArray;
    }

    public int[][] getNodeUnitIncidenceArray() {
        if (nodeUnitIncidenceArray == null) {
            final Generator[] gens = getGens();
            final int no_of_gen = getGenNum();
            nodeUnitIncidenceArray = new int[no_of_bus][no_of_gen];
            for (int i = 0; i < no_of_gen; i++) nodeUnitIncidenceArray[gens[i].getBusIdx()][i] = 1;
        }
        return nodeUnitIncidenceArray;
    }

    public int[][] getNodeLoadIncidenceArray() {
        if (nodeLoadIncidenceArray == null) {
            final List<BusLoad> list = getBusLoadList();
            final int no_of_load = list.size();
            final BusLoad busLoads[] = list.toArray(new BusLoad[no_of_load]);
            nodeLoadIncidenceArray = new int[no_of_bus][no_of_load];
            for (int i = 0; i < no_of_load; i++) nodeLoadIncidenceArray[busLoads[i].getBusIdx()][i] = 1;
        }

        return nodeLoadIncidenceArray;
    }

    public double[][] getBranchDataArray() {
        if (branchDataArray == null) {
            final List<Branch> list = getBranchList();
            final int no_of_branch = list.size();
            final Branch branches[] = list.toArray(new Branch[no_of_branch]);
            branchDataArray = new double[4][no_of_branch];
            for (int i = 0; i < no_of_branch; i++) {
                branchDataArray[REACTANCE_IDX][i] = branches[i].getX();
                branchDataArray[FROMBUS_IDX][i] = branches[i].getFromBusIdx();
                branchDataArray[TOBUS_IDX][i] = branches[i].getToBusIdx();
                branchDataArray[CAPACITY_IDX][i] = branches[i].getCapacity();
            }
        }
        return branchDataArray;
    }

    public double[][] getNodeBranchIncidenceArray(Integer contingencyBranchIndex) {
        double[][] originalArray = getNodeBranchIncidenceArray();
        double[][] array = new double[no_of_bus][originalArray[0].length - 1];
        for (int i = 0; i < no_of_bus; i++) {
            System.arraycopy(originalArray[i], 0, array[i], 0, contingencyBranchIndex);
            System.arraycopy(originalArray[i], contingencyBranchIndex + 1
                    , array[i], contingencyBranchIndex
                    , array[i].length - contingencyBranchIndex);
        }
        return array;
    }

    public double[][] getBranchDataArray(Integer contingencyBranchIndex) {
        double[][] originalArray = getBranchDataArray();
        double[][] array = new double[originalArray.length][originalArray[0].length - 1];
        for (int i = 0; i < originalArray.length; i++) {
            System.arraycopy(originalArray[i], 0, array[i], 0, contingencyBranchIndex);
            System.arraycopy(originalArray[i], contingencyBranchIndex + 1
                    , array[i], contingencyBranchIndex
                    , array[i].length - contingencyBranchIndex);
        }
        return array;
    }

    public int getTargetflag() {
        return targetflag;
    }

    //    it's for normalization
    public double[] getNormalize_coefficentes() {
        return nomalize_coefficentes;
    }

    public void setNomalize_coefficentes(double[] nomalize_coefficentes) {
        this.nomalize_coefficentes = nomalize_coefficentes;
    }
}