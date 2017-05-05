package sjtu.scuc.academic;

/**
 * Created by zhai on 2017/1/14.
 */
public class BigResult {
    private Calresult result10;

    public BigResult(Calresult result10, Calresult result36, double[] tieline, double result) {
        this.result10 = result10;
        this.result36 = result36;
        this.tieline = tieline;
        this.result = result;
    }

    private Calresult result36;

    private double[] tieline;

    private double result;

    public BigResult() {

    }

    public double[] getTieline() {
        return tieline;
    }

    public void setTieline(double[] tieline) {
        this.tieline = tieline;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public Calresult getResult10() {
        return result10;
    }

    public void setResult10(Calresult result10) {
        this.result10 = result10;
    }

    public Calresult getResult36() {
        return result36;
    }

    public void setResult36(Calresult result36) {
        this.result36 = result36;
    }
}
