package sjtu.scuc.academic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import util.HFromString;

import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class InputFileConfigure {

    private static Log log = LogFactory.getLog(InputFileConfigure.class);

    final String delim = ",";

    private String loadFile;
    private String generatorFile;
    private String leqConstraintFile;

    private SCUCData scucData;
    private SCUCAlg scucAlg;

    public SCUCData getScucData() {
        return scucData;
    }

    public void setScucData(SCUCData scucData) {
        this.scucData = scucData;
    }

    public SCUCAlg getScucAlg() {
        return scucAlg;
    }

    public void setScucAlg(SCUCAlg scucAlg) {
        this.scucAlg = scucAlg;
    }

    public String getLoadFile() {
        return loadFile;
    }

    public void setLoadFile(String loadFile) {
        this.loadFile = loadFile;
    }

    public String getGeneratorFile() {
        return generatorFile;
    }

    public void setGeneratorFile(String generatorFile) {
        this.generatorFile = generatorFile;
    }

    public String getLeqConstraintFile() {
        return leqConstraintFile;
    }

    public void setLeqConstraintFile(String leqConstraintFile) {
        this.leqConstraintFile = leqConstraintFile;
    }
    //    读取输入数据
    public boolean readLoadAndReserve() {
        LagrangianAlg scucAlg = null;
        if (this.scucAlg instanceof LagrangianAlg) {
            scucAlg = (LagrangianAlg) this.scucAlg;
        }

        final String pathname = getLoadFile();
        SAXReader reader = new SAXReader();
        Document document;
        try {//解析pathname对应的xml
            document = reader.read(new File(pathname));
        } catch (DocumentException e) {
            log.error(e);
            return false;
        }
        Node root_node = document.getRootElement();

        // read totalLoad balance constraint, corresponding lambda value and its update parameters
        String str_loads = root_node.valueOf("loads").trim();
        if (str_loads != null && str_loads.length() != 0) {
            scucData.setTotalLoad(HFromString.toDoubleArray(str_loads, delim));
        } else {
            String msg = "NO totalLoad is set!";
            log.error(msg);
            return false;
        }

        if (scucAlg != null) {
            String str_lambdas = root_node.valueOf("lambdas").trim();
            if (str_lambdas != null && str_lambdas.length() != 0) {
                scucAlg.setLambda(HFromString.toDoubleArray(str_lambdas, delim));
            } else {
                String msg = "NO lambda is set, default 0 values will be used to begin cur_iteration.";
                log.info(msg);
            }
            String str_lambda_plus = root_node.valueOf("lambda_plus").trim();
            try {
                double lambda_plus = Double.parseDouble(str_lambda_plus);
                scucAlg.setA_lambda_plus(lambda_plus);
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("NO lambda_plus is set, default {0,number,#.###} will be used to update mu.", scucAlg.getA_lambda_plus());
                log.info(msg);
            }
            String str_lambda_minus = root_node.valueOf("lambda_minus").trim();
            try {
                double lambda_minus = Double.parseDouble(str_lambda_minus);
                scucAlg.setA_lambda_minus(lambda_minus);
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("NO lambda_minus is set, default {0,number,#.###} will be used to update mu.", scucAlg.getA_lambda_minus());
                log.info(msg);
            }
        }

        // read reserve constraint, corresponding mu value and its update parameters
        String str_reserves = root_node.valueOf("reserves").trim();
        if (str_reserves != null && str_reserves.length() != 0) {
            scucData.setReserve(HFromString.toDoubleArray(str_reserves, delim));

            if (scucAlg != null) {
                String str_mus = root_node.valueOf("mus").trim();
                if (str_mus != null && str_mus.length() != 0) {
                    scucAlg.setMu(HFromString.toDoubleArray(str_mus, delim));
                } else {
                    String msg = "NO mu is set, default 0 values will be used to begin cur_iteration.";
                    log.info(msg);
                }
                String str_mu_plus = root_node.valueOf("mu_plus").trim();
                try {
                    double mu_plus = Double.parseDouble(str_mu_plus);
                    scucAlg.setA_mu_plus(mu_plus);
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format("NO mu_plus is set, default {0,number,#.###} will be used to update mu.", scucAlg.getA_mu_plus());
                    log.info(msg);
                }
                String str_mu_minus = root_node.valueOf("mu_minus").trim();
                try {
                    double mu_minus = Double.parseDouble(str_mu_minus);
                    scucAlg.setA_mu_minus(mu_minus);
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format("NO mu_minus is set, default {0,number,#.###} will be used to update mu.", scucAlg.getA_mu_minus());
                    log.info(msg);
                }
            }
        } else {
            String msg = "NO reserve constraints.";
            log.info(msg);
        }
        return true;
    }
    //读取发电机数据
    public boolean readGenerators() {
        final String filename = getGeneratorFile();

        SAXReader reader = new SAXReader();
        Document document;
        try {
            document = reader.read(new File(filename));
        } catch (DocumentException e) {
            log.error(e);
            return false;
        }
        Element root = document.getRootElement();
        // iterate through child elements of root with element name "generator"
        int idx = 0;
        for (Iterator i = root.elementIterator("generator"); i.hasNext(); idx++) {
            Element element = (Element) i.next();

            int min_on_time = 1;
            try {
                min_on_time = Integer.parseInt(getAttribute(element, "min_on_time"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO min_on_time data and default value {1,number,#.###} will be used.", idx, min_on_time);
                log.debug(msg);
            }
            int min_dn_time = 1;
            try {
                min_dn_time = Integer.parseInt(getAttribute(element, "min_dn_time"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO min_dn_time data and default value {1,number,#.###} will be used.", idx, min_dn_time);
                log.debug(msg);
            }
            int initialConditionHours = 1;
            try {
                initialConditionHours = Integer.parseInt(getAttribute(element, "initialConditionHours"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO initialConditionHours data and default value {1,number,#.###} will be used.", idx, initialConditionHours);
                log.debug(msg);
            }
            double initialP = 0;
            try {
                initialP = Double.parseDouble(getAttribute(element, "initialP"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO initialP data and default value {1,number,#.###} will be used.", idx, initialP);
                log.debug(msg);
            }
            double minP = 0;
            try {
                minP = Double.parseDouble(getAttribute(element, "minP"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO minP data and default value {1,number,#.###} will be used.", idx, minP);
                log.debug(msg);
            }
            double maxP;
            try {
                maxP = Double.parseDouble(getAttribute(element, "maxP"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO maxP data!", idx);
                log.error(msg);
                return false;
            }
            double startupCost = 0;
            try {
                startupCost = Double.parseDouble(getAttribute(element, "startupCost"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO startupCost data and default value {1,number,#.###} will be used.", idx, startupCost);
                log.debug(msg);
            }
            double rampRate = 9999;
            try {
                rampRate = Double.parseDouble(getAttribute(element, "rampRate"));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("G{0}: NO rampRate data and default value {1,number,#.###} will be used.", idx, rampRate);
                log.debug(msg);
            }

//            double Gasa = 0;
//            try {
//                Gasa = Double.parseDouble(getAttribute(element, "Gasa"));
//            } catch (NumberFormatException e) {
//                String msg = MessageFormat.format("G{0}: NO Gasa data and default value {1,number,#.###} will be used.", idx, Gasa);
//                log.debug(msg);
//            }
//
//            double Gasb = 0;
//            try {
//                Gasb = Double.parseDouble(getAttribute(element, "Gasb"));
//            } catch (NumberFormatException e) {
//                String msg = MessageFormat.format("G{0}: NO Gasb data and default value {1,number,#.###} will be used.", idx, Gasb);
//                log.debug(msg);
//            }
//
//            double Gasc = 0;
//            try {
//                Gasc = Double.parseDouble(getAttribute(element, "Gasc"));
//            } catch (NumberFormatException e) {
//                String msg = MessageFormat.format("G{0}: NO Gasc data and default value {1,number,#.###} will be used.", idx, Gasc);
//                log.debug(msg);
//            }


            Generator gen = null;
            String costCurveType = getAttribute(element, "costCurveType");
            if (costCurveType.equalsIgnoreCase(GeneratorFactory.QuadraticCostCurve)) {
                double aConst;
                double aLinear;
                double aQuadratic;
                double Gasc;
                double Gasb;
                double Gasa;
                try {
                    aConst = Double.parseDouble(getAttribute(element, "aConst"));
                    aLinear = Double.parseDouble(getAttribute(element, "aLinear"));
                    aQuadratic = Double.parseDouble(getAttribute(element, "aQuadratic"));
                    Gasc = Double.parseDouble(getAttribute(element, "Gasc"));
                    Gasb = Double.parseDouble(getAttribute(element, "Gasb"));
                    Gasa = Double.parseDouble(getAttribute(element, "Gasa"));
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format("G{0}: Generator cost curve data error!", idx);
                    log.error(msg);
                    return false;
                }
                if (aQuadratic == 0) {
                    String msg = MessageFormat.format("G{0}: Quadratic item of cost curve cannot be 0!", idx);
                    log.error(msg);
                    return false;
                }

                gen = GeneratorFactory.creator(min_on_time, min_dn_time, initialConditionHours, costCurveType, aConst, aLinear, aQuadratic,Gasc,Gasb,Gasa);
                gen.setMinP(minP);
                gen.setMaxP(maxP);
                gen.setStartupCost(startupCost);
                gen.setInitialP(initialP);
                gen.setRamp_rate(rampRate);
            } else if (costCurveType.equalsIgnoreCase(GeneratorFactory.PiecewiseCostCurve)) {
                double[] breakpoints;
                double[] slopes;
                double x1;
                double y1;
                try {
                    breakpoints = HFromString.toDoubleArray(getAttribute(element, "breakpoints"), delim);
                    slopes = HFromString.toDoubleArray(getAttribute(element, "slopes"), delim);
                    x1 = Double.parseDouble(getAttribute(element, "x1"));
                    y1 = Double.parseDouble(getAttribute(element, "y1"));
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format("G{0}: Generator cost curve data error!", idx);
                    log.error(msg);
                    return false;
                }

                gen = GeneratorFactory.creator(min_on_time, min_dn_time, initialConditionHours, costCurveType, maxP, minP, breakpoints, slopes, x1, y1);
                gen.setStartupCost(startupCost);
                gen.setInitialP(initialP);
                gen.setRamp_rate(rampRate);
            } else {
                String msg = MessageFormat.format("G{0}: costCurveType must be Quadratic or Piecewise.", idx);
                log.error(msg);
                return false;
            }
            scucData.addGenerator(gen);
        }
        return true;
    }

    private String getAttribute(Element element, String attributeName) {
        String s = null;
        try {
            s = element.attribute(attributeName).getValue();
        } catch (NullPointerException e) {
            s = null;
        }
        if (s == null || s.length() == 0) {
            s = element.valueOf(attributeName);
        }
        return s;
    }

    public boolean readLeqConstraints() {
        final String filename = getLeqConstraintFile();
        if (filename == null || filename.length() == 0) return true;

        SAXReader reader = new SAXReader();
        Document document;
        try {
            document = reader.read(new File(filename));
        } catch (DocumentException e) {
            String msg = MessageFormat.format("{0} cannot be found.", filename);
            log.error(msg);
            return false;
        }
        Element root = document.getRootElement();

        int idx = 0;
        for (Iterator i = root.elementIterator("constraint"); i.hasNext(); idx++) {
            Element element = (Element) i.next();

            LeqConstraint con = new LeqConstraint();
            try {
                con.setTimeInterval(Integer.parseInt(element.valueOf("timeinterval")));
                con.setA(HFromString.toDoubleArray(element.valueOf("a"), delim));
                con.setB(Double.parseDouble(element.valueOf("b")));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("c{0}: timeinterval, a and b must be numbers.", idx);
                log.error(msg);
                return false;
            }
            try {
                con.setMu(Double.parseDouble(element.valueOf("mu")));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("c{0}: NO mu is set, default {1,number,#.###} will be used to initial first iteration.", idx, con.getMu());
                log.info(msg);
            }
            try {
                con.setMu_plus(Double.parseDouble(element.valueOf("mu_plus")));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("c{0}: NO mu_plus is set, default {1,number,#.###} will be used to update mu.", idx, con.getMu_plus());
                log.info(msg);
            }
            try {
                con.setMu_minus(Double.parseDouble(element.valueOf("mu_minus")));
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("c{0}: NO mu_minus is set, default {1,number,#.###} will be used to update mu.", idx, con.getMu_minus());
                log.info(msg);
            }
            scucData.addConstraint((LeqConstraint) con);
        }
        return true;
    }

    public boolean validateInputData() {

        double[] loads = scucData.getTotalLoad();
        if (loads == null || loads.length == 0) {
            log.error("No loads!");
            return false;
        }

        List<Generator> gens = scucData.getGenList();
        if (gens == null || gens.size() == 0) {
            log.error("No generators!");
            return false;
        }

        // generator's cost curve type must be the same
        String curveType;
        if (gens.get(0) instanceof GeneratorWithQuadraticCostCurve)
            curveType = GeneratorFactory.QuadraticCostCurve;
        else if (gens.get(0) instanceof GeneratorWithPiecewiseCostCurve)
            curveType = GeneratorFactory.PiecewiseCostCurve;
        else {
            log.error("Generator's cost curve type is wrong!");
            return false;
        }
        for (int i = 1; i < gens.size() - 1; i++) {
            String curCurveType;
            if (gens.get(i) instanceof GeneratorWithQuadraticCostCurve)
                curCurveType = GeneratorFactory.QuadraticCostCurve;
            else if (gens.get(i) instanceof GeneratorWithPiecewiseCostCurve)
                curCurveType = GeneratorFactory.PiecewiseCostCurve;
            else {
                log.error("Generator's cost curve type is wrong!");
                return false;
            }
            if (!curveType.equalsIgnoreCase(curCurveType)) {
                log.error("All generators' cost curve type must be the same in the same case!");
                return false;
            }
        }

        if (this.scucAlg instanceof LagrangianAlg) {
            LagrangianAlg scucAlg = (LagrangianAlg) this.scucAlg;

            // generator's cost curve type must compatible with economic dispatch algorithm
            EconomicDispatchable edAlg = scucAlg.getEdAlg();
            if (edAlg instanceof EconomicDispatch1HourNewton) {
                if (!curveType.equalsIgnoreCase(GeneratorFactory.QuadraticCostCurve)) {
                    log.error("Newton economic dispatch algorithm can only sovle the problem with quadratic cost curve of generator!");
                    return false;
                }
            } else if (edAlg instanceof EconomicDispatch1HourPiecewise) {
                if (!curveType.equalsIgnoreCase(GeneratorFactory.PiecewiseCostCurve)) {
                    log.error("Piecewise economic dispatch algorithm can only sovle the problem with piecewise cost curve of generator!");
                    return false;
                }
            }

            // only lpcplex algorithm can solve leqconstraints
            List<LeqConstraint> cons = scucData.getConstraints();
            if (cons != null && cons.size() > 0) {
                if (!(edAlg instanceof EconomicDispatch1HourCplex)) {
                    log.error("Only Cplex economic dispatch algorithm can deal with extra leq constraints!");
                    return false;
                }
            }
        }

        return true;
    }
}
