package util;

/**
 * Linear congruential generator with `bitSize`-bit state and coefficients of java.util.Random.
 */
public class SubGenerator {

    private static final long addend = 0xBL;
    private final long multiplier;
    private final long mask;
    private long state;

    public SubGenerator(int bitSize, Long seed) {
        this.mask = (1L << bitSize) - 1;
        this.multiplier = 0x5DEECE66DL & this.mask;
        this.state = seed & this.mask;
    }

    public long getState() {
        return this.state;
    }

    public void setState(Long state) {
        this.state = state & this.mask;
    }

    public int next() {
        this.state = (this.state * multiplier + addend) & mask;
        return (int) (this.state >> 17);
    }

    @Override
    public String toString() {
        return "util.SubGenerator{" +
                "state=" + state +
                ", multiplier=" + multiplier +
                ", mask=" + mask +
                '}';
    }
}
