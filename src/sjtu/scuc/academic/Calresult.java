package sjtu.scuc.academic;

import java.io.Serializable;

/**
 * Created by zhai on 2016/8/8.
 */
public class Calresult implements Serializable {
    private int[][] genY=null;
    private int targetflag=0;
    private int[][] genStatus = null;
    private double[][] genOutput = null;
    private double lambda[]=null;
    private double mu[]=null;

    private SCUCData scucData;

    private double bestObjValue=0;


    public Calresult() {

    }

    public Calresult(int[][] genStatus, double[][] genOutput, double bestObjValue, int[][] genY) {
        this.genStatus=genStatus;
        this.genOutput=genOutput;
        this.genY=genY;
        this.bestObjValue=bestObjValue;
    }

    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }

    private String mode=null;

    public Calresult(String mode, int[][] genStatus, double[][] genOutput, double bestObjValue,int[][] genY) {
        this.mode=mode;
        this.genStatus=genStatus;
        this.genOutput=genOutput;
        this.genY=genY;
        this.bestObjValue=bestObjValue;
    }

    public SCUCData getSCUCData() {
        return scucData;
    }
    public void setSCUCData(SCUCData scucData) {
        this.scucData = scucData;
    }

    public void setGenY(int[][] genY) {
        this.genY = genY;
    }

    public int[][] getGenY() {
//        genY=[no_of_ti][no_of_gen]
        if(genY!=null){
            return genY;
        }
        else {
            genY=U2Y(genStatus);
        }
        return genY;
    }

    private int[][] U2Y(int[][] genStatus) {
        int[][] temp=new int[genStatus.length][genStatus[0].length];
        for (int i = 0; i < genStatus.length; i++) {
            int initial=scucData.getGenList().get(i).getInitialConditionHour();
            for (int t = 0; t < genStatus[0].length; t++) {
                if (t==0){
                    if((initial<0)&(genStatus[i][t]==1)){
                        temp[i][t]=1;
                    }
                }
                else {
                    if(genStatus[i][t]-genStatus[i][t-1]==1){
                        temp[i][t]=1;
                    }
                }
            }
        }
        return temp;
    }

    public double[] getLambda() {
        return lambda;
    }

    public void setLambda(double[] lambda) {
        this.lambda = lambda;
    }

    public double[] getMu() {
        return mu;
    }

    public void setMu(double[] mu) {
        this.mu = mu;
    }

    public int getTargetflag() {
        return targetflag;
    }

    public void setTargetflag(int targetflag) {
        this.targetflag = targetflag;
    }

    public int[][] getGenStatus() {
        return genStatus;
    }

    public void setGenStatus(int[][] genStatus) {
        this.genStatus = genStatus;
    }

    public double[][] getGenOutput() {
        return genOutput;
    }

    public void setGenOutput(double[][] genOutput) {
        this.genOutput = genOutput;
    }

    public double getBestObjValue() {
        return bestObjValue;
    }

    public void setBestObjValue(double bestObjValue) {
        this.bestObjValue = bestObjValue;
    }

}
