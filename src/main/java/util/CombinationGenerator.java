package util;

import java.util.Arrays;

public class CombinationGenerator {
    private final int n;
    private final int k;
    private final int[] state;
    private boolean isEnd = false;

    public CombinationGenerator(int n, int k) {
        this.n = n;
        this.k = k;
        this.state = new int[k+2];
        this.reset();
    }

    private void reset() {
        for (int i = 0; i < this.k; i++) {
            this.state[i] = i;
        }
        this.state[this.k] = this.n;
        this.state[this.k+1] = 0;
    }

    public int[] next() {
        if (this.isEnd) {
            return null;
        }
        int[] result = Arrays.copyOf(this.state, k);
        int j = 0;
        for(;j<k+1 && this.state[j] + 1 == this.state[j+1];j++) {
            this.state[j] = j;
        }
        if(j < k) {
            this.state[j]++;
        } else {
            this.isEnd = true;
        }
        return result;
    }
}
