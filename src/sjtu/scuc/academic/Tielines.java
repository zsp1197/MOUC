package sjtu.scuc.academic;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Zhai Shaopeng on 2017/5/10 14:37.
 * E-mail: zsp1197@163.com
 */
public class Tielines implements Serializable {
    double[][] tieMax;
    double tieTolerance =0.000001;
    double[][][] tielines;

    public Tielines(double[][] tiemax) {
        if(!checkTieMax(tiemax)){
            throw new java.lang.Error("not symmetric! or not 0 diagonal element");
        }
        this.setTieMax(tiemax);
    }

    private boolean checkTieMax(double[][] tiemax) {
//        查看矩阵是否对称以及对角元素是否为0
        int d1=tiemax.length;
        int d2=tiemax[0].length;
        if(d1!=d2){
            return false;
        }
        for (int i = 0; i < d1; i++) {
            if(tiemax[i][i]!=0){
                return false;
            }
            for (int j = 0; j < d2; j++) {
                if(tiemax[i][j]!=tiemax[j][i]){
                    return false;
                }
            }
        }
        return true;
    }

    public void print_tielines(int from,int to){
        final int no_of_sys=tielines[0].length;
        final int no_of_ti=tielines[0][0].length;
        double[] tieline=tielines[from][to];
        System.out.println();
        System.out.println("联络线：");
        for (int t = 0; t < no_of_ti; t++) {
            System.out.print(String.format("%.1f",tieline[t])+" ");
        }
        System.out.println();
    }

    public double[][] getTieMax() {
        return tieMax;
    }

    public void setTieMax(double[][] tiemax) {
        if(!checkTieMax(tiemax)){
            throw new java.lang.Error("not symmetric! or not 0 diagonal element");
        }
        this.tieMax = tiemax;
    }

    public double[][][] getTielines() {
        return tielines;
    }

    public void setTielines(double[][][] ties) {
        if(!checkTieLines(ties)){
            throw new java.lang.Error("not negative symmetric, or not 0 diagonal element, or tieMax is violated");
        }
        this.tielines=ties;
    }

    public void setTielines_with_maxRefine(double[][][] ties) {
        ties=refine2max(ties);
        if(!checkTieLines(ties)){
            throw new java.lang.Error("not negative symmetric, or not 0 diagonal element");
        }
        this.tielines=ties;
    }

    private double[][][] refine2max(double[][][] ties) {
        int d1=ties.length;
        int d2=ties[0].length;
        int d3=ties[0][0].length;
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d2; j++) {
                for (int k = 0; k < d3; k++) {
                    if(ties[i][j][k]>getTieMax()[i][j]){
                        ties[i][j][k]=getTieMax()[i][j];
                    }
                    if(ties[i][j][k]<-getTieMax()[i][j]){
                        ties[i][j][k]=-getTieMax()[i][j];
                    }
                }
            }
        }
        return ties;
    }

    private boolean checkTieLines(double[][][] ties) {
        //        查看矩阵是否对称以及对角元素是否为0
        int d1=ties.length;
        int d2=ties[0].length;
        int d3=ties[0][0].length;
        if(d1!=d2){
            return false;
        }
        for (int i = 0; i < d1; i++) {
            if(!Arrays.equals(ties[i][i],new double[d3])){
                return false;
            }
            for (int j = 0; j < d2; j++) {
                for (int k = 0; k < d3; k++) {
                    if(Math.abs(ties[i][j][k]+ties[j][i][k])> tieTolerance){
                        return false;
                    }
                    if(Math.abs(ties[i][j][k])>getTieMax()[i][j]){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public double getSysOut(int si, int t) {
        double result=0;
//        int no_of_ti=tielines[0][0].length;
        int no_of_sys=tielines[0].length;
        for (int i = 0; i < no_of_sys; i++) {
            result=result+tielines[si][i][t];
        }
        return result;
    }

    public void deltaSetTielines(double[][][] tielinesdelta) {
        int no_of_sys=tielines[0].length;
        int no_of_ti=tielines[0][0].length;
        double[][][] result=tielines;
        for (int i = 0; i < no_of_sys; i++) {
            for (int j = 0; j < no_of_sys; j++) {
                for (int t = 0; t < no_of_ti; t++) {
                    result[i][j][t]=result[i][j][t]+tielinesdelta[i][j][t];
                }
            }
        }
//        setTielines(result);
        setTielines_with_maxRefine(result);
    }
}
