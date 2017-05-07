package sjtu.scuc.academic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class Doit {
    private static Log log = LogFactory.getLog(Main.class);

    public void mainprocess() {
        PropertyConfigurator.configure("log4j.properties");

        SCUCData scucData10 = getSCUCData("UC-context.xml");
        final SCUCData scucData10_ori = getSCUCData("UC-context.xml");
        SCUCData scucData36 = getSCUCData("UC-context1.xml");
        final SCUCData scucData36_ori = getSCUCData("UC-context1.xml");


        SCUCData scucDataFull=getSCUCDataFUll(scucData10,scucData36);
        //        used for 4+6

        scucData10.setTotalLoad(gettemp10load(scucData10, scucDataFull));
        scucData36.setTotalLoad(gettemp10load(scucData36, scucDataFull));

        System.out.println(double2str(scucData10.getTotalLoad()));
        System.out.println(double2str(scucData36.getTotalLoad()));

        int num_of_points = 9;
        double[] tieline = initializeTieline(scucData10_ori, scucData36_ori);
        System.out.println("Tieline：  " + double2str(tieline));

        BigResult br_1;
        BigResult br_2;
        ArrayList<BigResult> bigResultlist = new ArrayList<BigResult>();
        br_1 = doMath(scucData10, scucData36, tieline, 0, null, null, scucData10_ori, scucData36_ori);
        tieline = initializeTieline(scucData10_ori, scucData36_ori);
        br_2 = doMath(scucData10, scucData36, tieline, 1, null, null, scucData10_ori, scucData36_ori);

        double step;
        double point;
        for (int i = 0; i < num_of_points; i++) {
            System.out.println("------------------------------------------------------------------");
            tieline = initializeTieline(scucData10_ori, scucData36_ori);
            step = 1 / (double) (num_of_points + 1);
            point = (i + 1) * step;
            bigResultlist.add(doMath(scucData10, scucData36, tieline, point, br_1, br_2, scucData10_ori, scucData36_ori));
        }
        for (int i = 0; i < bigResultlist.size(); i++) {
            System.out.print(Tools.getObjValue(bigResultlist.get(i).getResult10(), scucData10_ori, 1) + Tools.getObjValue(bigResultlist.get(i).getResult36(), scucData36_ori, 1) + ",");
//            System.out.print(Tools.getObjValue(bigResultlist.get(i).getResult10(), scucData10_ori, 1)+ ",");
        }
        System.out.println();
        for (int i = 0; i < bigResultlist.size(); i++) {
            System.out.print(Tools.getObjValue(bigResultlist.get(i).getResult10(), scucData10_ori, 2) + Tools.getObjValue(bigResultlist.get(i).getResult36(), scucData36_ori, 2) + ",");
//            System.out.print(Tools.getObjValue(bigResultlist.get(i).getResult10(), scucData10_ori, 2) + ",");
        }
        return;
        //        used for 4+6
//        scucData10.setTotalLoad(gettemp10load(scucData10, scucDataFull));
//        scucData36.setTotalLoad(gettemp10load(scucData36, scucDataFull));


    }

    private void fuckthestartup(SCUCData scucData) {
        for (int i = 0; i < scucData.getGenList().size(); i++) {
            scucData.getGenList().get(i).setStartupCost(0);
        }
    }

    private static BigResult doMath(SCUCData scucData10, SCUCData scucData36, double[] tieline, double v, BigResult br_1, BigResult br_2, SCUCData scucData10_ori, SCUCData scucData36_ori) {
        scucData10 = refineSCUCTarget(scucData10, v, br_1, br_2, scucData10_ori);
        scucData36 = refineSCUCTarget(scucData36, v, br_1, br_2, scucData36_ori);
//        SCUCData scucDataFull = getSCUCDataFUll(scucData10, scucData36);
        double[] oriLoad10 = deepcopyDoubleArray(scucData10.getTotalLoad());
        double[] oriLoad36 = deepcopyDoubleArray(scucData36.getTotalLoad());
        SCUCSolver scucSolver10 = getSCUCSolver("UC-context.xml");
        SCUCSolver scucSolver36 = getSCUCSolver("UC-context.xml");
        Calresult calresult1 = null;
        Calresult calresult2 = null;
        double result;
        double bestresult = Double.MAX_VALUE;
        ArrayList<Double> resultlist = new ArrayList<Double>();
        BigResult best = new BigResult();

        long beginTime = System.currentTimeMillis();
        do {
            scucSolver10 = getSCUCSolver("UC-context.xml");
            scucSolver36 = getSCUCSolver("UC-context.xml");
            tieline = refineTieline(calresult1, calresult2, tieline);
            scucData10.setTotalLoad(getLoadwithTieline(oriLoad10, tieline, 0));
            scucData36.setTotalLoad(getLoadwithTieline(oriLoad36, tieline, 1));
            calresult1 = scucSolver10.optimize(scucData10);
            calresult2 = scucSolver36.optimize(scucData36);
            System.out.println(double2str(calresult1.getLambda()));
            System.out.println(double2str(calresult2.getLambda()));
            result = Tools.getObjValue(calresult1, scucData10, 1) + Tools.getObjValue(calresult2, scucData36, 1);
            if (result < bestresult) {
                bestresult = result;
                best.setResult10(calresult1);
                best.setResult36(calresult2);
                best.setResult(result);
                best.setTieline(deepcopyDoubleArray(tieline));
            }
            System.out.println("Calculation result:   " + result);
            System.out.println("Tieline：  " + double2str(tieline));
            System.out.println("**************************************************************************************");
            resultlist.add(new Double(result));
        } while (keeponbitch(resultlist));
        long endTime = System.currentTimeMillis();
        System.out.println("Elapsed time is " + (endTime - beginTime));
        afterwardProcess(scucData10, scucData36, oriLoad10, oriLoad36);
        return best;


        //        calculate fulll system
//        System.out.println("--------------------------FULL SYSTEM--------------------------");
//        beginTime = System.currentTimeMillis();
//        SCUCSolver scucSolverFull = getSCUCSolver("UC-context.xml");
//        Calresult fullresult = scucSolverFull.optimize(scucDataFull);
//        System.out.println("Optimal Tieline:    " + double2str(getOptTieline(oriLoad10, scucData10, fullresult)));
//
//        tieline=getOptTieline(oriLoad10, scucData10, fullresult);
//        scucData10.setTotalLoad(getLoadwithTieline(oriLoad10, tieline, 0));
//        scucData36.setTotalLoad(getLoadwithTieline(oriLoad36, tieline, 1));
//        calresult1 = scucSolver10.optimize(scucData10);
//        calresult2 = scucSolver36.optimize(scucData36);
//        System.out.println(double2str(calresult1.getLambda()));
//        System.out.println(double2str(calresult2.getLambda()));
//        result = Tools.getObjValue(calresult1, scucData10, 1) + Tools.getObjValue(calresult2, scucData36, 1);
//        System.out.println("Calculation result:   " + result);
//        endTime = System.currentTimeMillis();
//        System.out.println("Elapsed time is " + (endTime - beginTime));
//        System.out.println();
    }

    private static void afterwardProcess(SCUCData scucData10, SCUCData scucData36, double[] oriLoad10, double[] oriLoad36) {
        scucData10.setTotalLoad(oriLoad10);
        scucData36.setTotalLoad(oriLoad36);
    }

    private static SCUCData refineSCUCTarget(SCUCData scucData, double v, BigResult br_1, BigResult br_2, SCUCData scucData_ori) {
        double result1 = 1;
        double result2 = 1;
        double[] newA = {0, 0, 0};
        GeneratorWithQuadraticCostCurve gen;
        if (v == 0) {
            return scucData;
        } else if (v == 1) {
            for (int i = 0; i < scucData.getGenList().size(); i++) {
                gen = (GeneratorWithQuadraticCostCurve) scucData_ori.getGenList().get(i);
                scucData.getGenList().get(i).setA(deepcopyDoubleArray(new double[]{gen.getGasc(), gen.getGasb(), gen.getGasa()}));
                scucData.getGenList().get(i).setStartupCost(0);
            }
            return scucData;
        } else {
            result1 = br_1.getResult();
            result2 = br_2.getResult();
        }

        System.out.println("Carbon Tax" + result1 * v / (result2 * (1 - v)));
        System.out.println(v);
        for (int i = 0; i < scucData.getGenList().size(); i++) {
            gen = (GeneratorWithQuadraticCostCurve) scucData_ori.getGenList().get(i);
//            newA[0] = (1 - v) * gen.getOria()[0] / result1 + v * gen.getGasc() / result2;
//            newA[1] = (1 - v) * gen.getOria()[1] / result1 + v * gen.getGasb() / result2;
//            newA[2] = (1 - v) * gen.getOria()[2] / result1 + v * gen.getGasa() / result2;
//            scucData.getGenList().get(i).setStartupCost(gen.getStartupCost() * (1 - v) / result1);
            newA[0] = gen.getOria()[0] + result1 * v * gen.getGasc() / (result2 * (1 - v));
            newA[1] = gen.getOria()[1] + result1 * v * gen.getGasb() / (result2 * (1 - v));
            newA[2] = gen.getOria()[2] + result1 * v * gen.getGasa() / (result2 * (1 - v));
            scucData.getGenList().get(i).setA(new double[]{newA[0], newA[1], newA[2]});
        }
        return scucData;
    }


    private static SCUCData deepcopySCUCData(SCUCData scucData) {
//        DO NOT WORK!
        SCUCData result = new SCUCData();
        result.setTotalLoad(deepcopyDoubleArray(scucData.getTotalLoad()));
        result.setGenList(scucData.getGenList());
        return result;
    }

    private static double[] gettemp10load(SCUCData scucDatatemp, SCUCData scucDataFull) {
        int no_of_time = scucDataFull.getTiNum();
        List<Generator> genListtemp = scucDatatemp.getGenList();
        List<Generator> genListFull = scucDataFull.getGenList();
        Generator tempGen = null;
        double totalFull = 0;
        double totaltemp = 0;
        double[] oriTotalLoads = scucDataFull.getTotalLoad();
        double[] newLoads = new double[no_of_time];
        for (Iterator iter = genListFull.iterator(); iter.hasNext(); ) {
            tempGen = (Generator) iter.next();
            totalFull = totalFull + tempGen.getMaxP();
        }
        for (Iterator iter = genListtemp.iterator(); iter.hasNext(); ) {
            tempGen = (Generator) iter.next();
            totaltemp = totaltemp + tempGen.getMaxP();
        }
        double propotion = totaltemp / totalFull;
        for (int i = 0; i < no_of_time; i++) {
//            WARNING! the /2 part is ...........
            newLoads[i] = oriTotalLoads[i] * propotion/2;
        }
        return newLoads;
    }

    private static SCUCData getSCUCDataFUll(SCUCData scucData10, SCUCData scucData36) {
        SCUCData scucDataFull = getSCUCData("UC-context.xml");
        List<Generator> genList = new ArrayList<Generator>();
        double[] loads = new double[scucData10.getTiNum()];
        for (Iterator iter = scucData10.getGenList().iterator(); iter.hasNext(); ) {
            genList.add((Generator) iter.next());
        }
        for (Iterator iter = scucData36.getGenList().iterator(); iter.hasNext(); ) {
            genList.add((Generator) iter.next());
        }
        for (int i = 0; i < scucData10.getTiNum(); i++) {
            loads[i] = scucData10.getTotalLoad()[i] + scucData36.getTotalLoad()[i];
        }
        scucDataFull.setTotalLoad(loads);
        scucDataFull.setGenList(genList);
        return scucDataFull;
    }

    private static double[] initializeTieline(SCUCData scucData10, SCUCData scucData36) {
        double totalCapability10 = 0;
        double totalCapability36 = 0;
        double[] load10 = scucData10.getTotalLoad();
        double[] load36 = scucData36.getTotalLoad();
        double[] tieline = new double[scucData10.getTiNum()];
//        double maxTieline = 100;
//        for (int i = 0; i < scucData10.getGenList().size(); i++) {
//            totalCapability10 = totalCapability10 + scucData10.getGenList().get(i).getMaxP();
//        }
//        for (int i = 0; i < scucData36.getGenList().size(); i++) {
//            totalCapability36 = totalCapability36 + scucData36.getGenList().get(i).getMaxP();
//        }
//        for (int i = 0; i < scucData10.getTiNum(); i++) {
//            tieline[i] = -0.8 * Math.abs(load10[i] - (load10[i] + load36[i]) * totalCapability10 / (totalCapability10 + totalCapability36));
//            if (tieline[i] > maxTieline) {
//                tieline[i] = maxTieline;
//            } else if (tieline[i] < -maxTieline) {
//                tieline[i] = -maxTieline;
//            }
//        }
        return tieline;
    }

    private static double[] getOptTieline(double[] oriLoad10, SCUCData scucData10, Calresult fullresult) {
        double tempOutput = 0;
        double maxload = 100;
        double[] tieline = new double[scucData10.getTiNum()];
        List<Generator> genList = scucData10.getGenList();
        double[] result = new double[scucData10.getTiNum()];
        for (int i = 0; i < scucData10.getTiNum(); i++) {
            tempOutput = 0;
            for (int j = 0; j < genList.size(); j++) {
                tempOutput = tempOutput + fullresult.getGenOutput()[j][i];
            }
            tieline[i] = tempOutput - oriLoad10[i];
        }
        for (int i = 0; i < tieline.length; i++) {
            if (tieline[i] > maxload) {
                tieline[i] = maxload;
            } else if (tieline[i] < -maxload) {
                tieline[i] = -maxload;
            }

        }
        return tieline;
    }

    private static double[] deepcopyDoubleArray(double[] oritieline) {
        if (oritieline == null) {
            return null;
        }

        double[] result = new double[oritieline.length];
        result = Arrays.copyOf(oritieline, oritieline.length);
        // For Java versions prior to Java 6 use the next:
        // System.arraycopy(original[i], 0, result[i], 0, original[i].length);
        return result;
    }

    private static boolean keeponbitch(ArrayList resultlist) {
        if (resultlist.size() >= 2) {
            return false;
//            return false;
        }
//        此处的问题是，arraylist得到的double是个类，不能直接用减法
//        if (Math.abs(Double.parseDouble(resultlist.get(resultlist.size() - 2).toString()) - Double.parseDouble(resultlist.get(resultlist.size() - 1).toString())) < 0.05 * Double.parseDouble(resultlist.get(resultlist.size() - 1).toString())) {
//            return false;
//        }
        return false;
//        return true;
    }

    private static String double2str(double[] d) {
        String[] s = new String[d.length];

        for (int i = 0; i < s.length; i++)
            s[i] = String.valueOf((int) Math.floor(d[i]));
        StringBuilder builder = new StringBuilder();
        for (String a : s) {
            builder.append(a);
            builder.append(',');
        }
        return builder.toString();
    }


    private static double[] refineTieline(Calresult calresult1, Calresult calresult2, double[] tieline) {
        double maxload = 100;
        if ((calresult1 == null) || (calresult2 == null)) {
            return tieline;
        } else {
            double[] lambda1 = calresult1.getLambda();
            double[] lambda2 = calresult2.getLambda();
            double thebase = 0;
            final int no_of_ti = calresult1.getSCUCData().getTiNum();
            for (int i = 0; i < no_of_ti; i++) {
                if (Math.abs(calresult1.getLambda()[i] - calresult2.getLambda()[i]) < 10) {
                    continue;
                }
                thebase = findthebase(calresult1, calresult2, i);
//                tieline[i] = tieline[i] - 0.1*(lambda1[i] - lambda2[i]) / Math.abs(lambda1[i]- lambda2[i]);
//                tieline[i] = tieline[i] - thebase * (lambda1[i] - lambda2[i]) / Math.abs(lambda1[i]- lambda2[i]);
//                tieline[i] = tieline[i] - thebase * (lambda1[i] - lambda2[i]);
                tieline[i] = tieline[i] - thebase * (lambda1[i] - lambda2[i]) / Math.max(lambda1[i], lambda2[i]);
                if (tieline[i] > maxload) {
                    tieline[i] = maxload;
                } else if (tieline[i] < -maxload) {
                    tieline[i] = -maxload;
                }
            }
        }

        return tieline;
    }

    private static double findthebase(Calresult calresult1, Calresult calresult2, int time) {
        int no_of_ti = calresult1.getLambda().length;
        int no_of_gen1 = calresult1.getGenOutput().length;
        int no_of_gen2 = calresult2.getGenOutput().length;
        double load1 = 0;
        double load2 = 0;
        for (int i = 0; i < no_of_gen1; i++) {
            load1 = load1 + calresult1.getGenOutput()[i][time];
        }
        for (int i = 0; i < no_of_gen2; i++) {
            load2 = load2 + calresult2.getGenOutput()[i][time];
        }
        double load = Math.min(load1, load2);

        return 0.05 * load;
    }

    private static double[] getLoadwithTieline(double[] oriLoads, double[] tieline, int i) {
        double[] loads = new double[oriLoads.length];
        for (int j = 0; j < oriLoads.length; j++) {
            if (i == 0) {
                loads[j] = oriLoads[j] + tieline[j];
            } else {
                loads[j] = oriLoads[j] - tieline[j];
            }
        }
        return loads;
    }

    public static SCUCSolver getSCUCSolver(String s) {
        ApplicationContext ctx = new FileSystemXmlApplicationContext(s);
        SCUCSolver scucSolver = (SCUCSolver) ctx.getBean("scucSolver");
//        scucSolver.printInfo();
        return scucSolver;
    }

    public static SCUCData getSCUCData(String s) {
        ApplicationContext ctx = new FileSystemXmlApplicationContext(s);

        InputFileConfigure inputFileConfigure = (InputFileConfigure) ctx.getBean("inputFileConfigure");
        if (!inputFileConfigure.readLoadAndReserve()) {
            log.error("Error in reading totalLoad data!");
            return null;
        }
        if (!inputFileConfigure.readGenerators()) {
            log.error("Error in reading generator data!");
            return null;
        }
        if (!inputFileConfigure.readLeqConstraints()) {
            log.error("Error in reading leq constraints data!");
            return null;
        }

        if (!inputFileConfigure.validateInputData()) {
            return null;
        }

        SCUCData scucData = (SCUCData) ctx.getBean("scucData");
        SCUCSolver scucSolver = (SCUCSolver) ctx.getBean("scucSolver");
        scucData = refineData(scucData, 1, 1, true);

        scucSolver.printInfo();
        return scucData;
    }

    private static SCUCData refineData(SCUCData scucData, double k, double bigger, boolean isRamp) {
//        double k=1;//kΪ�仯����
//        double bigger=5;//�����౶��,��Ϊ��ǰ�Ķ��ٱ�
        scucData.setBigger(bigger);
        scucData.setK(k);
        scucData.setRamp(isRamp);
        scucData = refineLoad(scucData, k, bigger);
        scucData = refineGen(scucData, k, bigger);
        scucData = refineiniP(scucData, k);
        return scucData;
    }

    private static SCUCData refineLoad(SCUCData scucData, double k, double bigger) {
        double[] loads = scucData.getTotalLoad();
        double[] finalloads = new double[(loads.length - 1) * ((int) (k - 1)) + loads.length];
        finalloads[0] = loads[0] * bigger;

        for (int i = 0; i < loads.length - 1; i++) {
            for (int j = 0; j < k; j++) {
                finalloads[(int) (i * k + j)] = bigger * (loads[i] + j * (loads[i + 1] - loads[i]) / k);
            }
        }
        finalloads[finalloads.length - 1] = loads[loads.length - 1] * bigger;

//        finalloads[0]=800;

        scucData.setTotalLoad(finalloads);
        return scucData;
    }

    private static SCUCData refineGen(SCUCData scucData, double k, double bigger) {
        List<Generator> Genlist = scucData.getGenList();
        int size = Genlist.size();
        int intk = (int) k;
        for (int i = 0; i < Genlist.size(); i++) {
            Genlist.get(i).setMin_on_time(scucData.getGenList().get(i).getMin_on_time() * intk);
            Genlist.get(i).setMin_dn_time(scucData.getGenList().get(i).getMin_dn_time() * intk);
            Genlist.get(i).setRamp_rate((scucData.getGenList().get(i).getRamp_rate()) / (k));
            Genlist.get(i).setInitialConditionHour(scucData.getGenList().get(i).getInitialConditionHour() * intk);

        }
        List<Generator> GenListTemp = Genlist;
        for (int i = 0; i < (int) (bigger - 1); i++) {
            for (int j = 0; j < size; j++) {
                Genlist.add(Genlist.get(j));
            }
        }
        scucData.setGenList(Genlist);

        return scucData;
    }

    private static SCUCData refineiniP(SCUCData scucData, double k) {
        List<Generator> Genlist = scucData.getGenList();
//        �����֮��

        double totalcap = 0;
        Generator tempGen = null;
        double[] load = scucData.getTotalLoad();
        for (int i = 0; i < Genlist.size(); i++) {
            tempGen = Genlist.get(i);
            if (tempGen.getInitialConditionHour() >= 0) {
                totalcap = totalcap + tempGen.getMaxP();
            }

        }
        for (int i = 0; i < Genlist.size(); i++) {
            Genlist.get(i).setInitialP(getInitialP(totalcap, Genlist.get(i), load[load.length - 1], load[0], k));
        }
        scucData.setGenList(Genlist);
        return scucData;
    }

    private static double getInitialP(double totalcap, Generator generator, double loadend, double loadstart, double k) {
        double minP = generator.getMinP();
        double maxP = generator.getMaxP();
//        double target=(loadstart)*(generator.getMaxP()/totalcap);
        double temp = (loadend + (loadstart - loadend) * (1 - (1 / k)));
        double target = temp * (generator.getMaxP() / totalcap);
        if (target >= maxP) {
            target = maxP;
        } else if (target <= minP) {
            target = minP;
        } else {

        }
        if (generator.getInitialConditionHour() <= 0) {
            target = 0;
        }
        return target;
    }

}
