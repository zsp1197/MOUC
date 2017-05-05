package sjtu.scuc.academic;

/**
 * Created by Zhai Shaopeng on 2017/5/4.
 * E-mail: zsp1197@sjtu.edu.cn
 */
public class Branch {
    /**
     * from bus's index: begin from 0
     */
    private int fromBusIdx;
    /**
     * to bus's index: begin from 0
     */
    private int toBusIdx;

    /**
     * reactance (p.u.)
     */
    private double x;

    /**
     * capacity (MW)
     */
    private double capacity;
    private String name;

    public Branch(int fromBusIdx, int toBusIdx) {
        assert (fromBusIdx != toBusIdx);
        this.fromBusIdx = fromBusIdx;
        this.toBusIdx = toBusIdx;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Branch branch = (Branch) o;

        if (!name.equals(branch.name)) return false;

        return true;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public int getFromBusIdx() {
        return fromBusIdx;
    }

    public int getToBusIdx() {
        return toBusIdx;
    }

    public double getX() {
        return x;
    }

    public double getCapacity() {
        return capacity;
    }
}
