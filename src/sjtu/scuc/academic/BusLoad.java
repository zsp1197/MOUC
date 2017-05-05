package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class BusLoad {
    private String name;

    private int busIdx;
    private double[] load;

    public void setName(String name) {
        this.name = name;
    }

    public int getBusIdx() {
        return busIdx;
    }

    public void setBusIdx(int busIdx) {
        this.busIdx = busIdx;
    }

    public double[] getLoad() {
        return load;
    }

    public void setLoad(double[] load) {
        this.load = load;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BusLoad busLoad = (BusLoad) o;

        if (!name.equals(busLoad.name)) return false;

        return true;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }
}