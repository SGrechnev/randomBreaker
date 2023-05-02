import util.CombinationGenerator;
import util.CUtils;
import util.SubGenerator;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RndBreaker {

    private static final long multiplier = 0x5DEECE66DL;
    private static final long mask = (1L << 48) - 1;

    public static void main(String[] args) {
        printHeader("DemoSubGenerator");
        demoSubGenerator();

        printHeader("DemoBreakByNextInt");
        demoBreakNextInt();

        printHeader("DemoBreakByNextIntWithLimit");
        demoBreakNextIntWithLimit();

        printHeader("DemoMultipleOtp");
        demoMultipleOtp();
    }

    private static void demoSubGenerator() {
        int limit = 1_000_000, p = 6;
        Random rnd = new Random();
        SubGenerator subRnd = new SubGenerator(p+17, getSeed(rnd));
        for (int i = 0; i < 10; i++) {
            rnd.nextInt(limit);
            subRnd.next();
            System.out.printf("rnd seed    = 0x%012x%n", getSeed(rnd)); // bitLength(state) == 48
            System.out.printf("subRnd seed = 0x%012x%n%n", subRnd.getState());
        }
    }

    private static void demoBreakNextInt() {
        Random rnd = new Random();

        System.out.printf("seed_value = 0x%08X%n", getSeed(rnd));
        System.out.println();

        int out1 = rnd.nextInt();
        System.out.printf("seed_value = 0x%08X%n", getSeed(rnd));
        System.out.printf("out1       = 0x%08X%n", out1);
        System.out.println();

        int out2 = rnd.nextInt();
        System.out.printf("seed_value = 0x%08X%n", getSeed(rnd));
        System.out.printf("out2       = 0x%08X%n", out2);
        System.out.println();

        Random resultRnd = breakRnd(out1,out2);
        System.out.printf("resultSeed = 0x%08X%n", getSeed(resultRnd));

        boolean rndEquals = getSeed(rnd).equals(getSeed(resultRnd));
        System.out.println("resultRnd is " + (rndEquals?"":"not ") + "equal to rnd");
        checkEqualSequences(rnd, resultRnd, 10);
    }

    private static void demoBreakNextIntWithLimit() {
        int numberOfOtp = 4;
        OtpGenerator gen = new OtpGenerator();
        int limit = gen.getLimit();

        List<Integer> otps = new ArrayList<>();
        for (int i = 0; i < numberOfOtp; i++) {
            otps.add(gen.generateOtp());
        }
        System.out.println("otps: " + otps);

        List<Long> results = breakOtpGenerator(otps, limit);

        System.out.println("results: " + toHexString(results, 12) + ", size = " + results.size());

        int numberToPredict = 5;
        for(long result: results) {
            Random newRnd = new Random();
            setSeed(newRnd, result);
            List<Integer> predictedOtps = new ArrayList<>();
            for (int i = 0; i < numberOfOtp + numberToPredict - 1; i++) {
                predictedOtps.add(newRnd.nextInt(limit));
            }
            System.out.println("predictedOtps = " + predictedOtps);
        }
        for (int i = 0; i < numberToPredict; i++) {
            System.out.println("newOtp     = " + gen.generateOtp());
        }
    }

    private static void demoMultipleOtp() {
        // Create n Random
        int n = 3;
        Random[] rnds = new Random[n];
        for (int i = 0; i < n; i++) {
            rnds[i] = new Random();
        }
        Random rndIdx = new Random();

        // Fill the list with multiple Random
        List<Integer> otps = new ArrayList<>();
        StringBuilder sb = new StringBuilder("idxs: ");
        for (int i = 0; i < 3*n+1; i++) {
            int idx = rndIdx.nextInt(n);
            sb.append(idx).append(" ");
            otps.add(rnds[idx].nextInt(1_000_000));
        }
        System.out.println("otps: " + otps);
        System.out.println(sb);

        CombinationGenerator combinator = new CombinationGenerator(otps.size(), 4); // combinations of n choose k
        int[] cmb;
        while((cmb = combinator.next()) != null){
            List<Integer> subOtps = CUtils.getByIdxs(otps, cmb);
            List<Long> solution = breakOtpGenerator(subOtps, 1_000_000);
            if (solution.size() > 0) {
                System.out.println("otps[" + Arrays.toString(cmb) + "] = " + subOtps);
                System.out.println("solution: " + toHexString(solution, 8) + ", size = " + solution.size());
                int numberToPredict = 5;
                for (long result : solution) {
                    Random newRnd = new Random();
                    setSeed(newRnd, result);
                    List<Integer> nextOtps = new ArrayList<>();
                    for (int i = 0; i < 4 + numberToPredict - 1; i++) {
                        nextOtps.add(newRnd.nextInt(1_000_000));
                    }
                    System.out.println("nextOtps = " + nextOtps);
                }
            }
        }
    }

    private static void checkEqualSequences(Random rnd1, Random rnd2, int n) {
        for (int i = 0; i < n; i++) {
            assert rnd1.nextLong() == rnd2.nextLong();
        }
    }

    private static Long getSeed(Random rnd) {
        try {
            Field fld = rnd.getClass().getDeclaredField("seed");
            fld.setAccessible(true);
            return ((AtomicLong) fld.get(rnd)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setSeed(Random rnd, long seed) {
        long preSeed = (seed ^ multiplier) & mask;
        rnd.setSeed(preSeed);
    }

    private static Random breakRnd(int out1, int out2) {
        Random rnd = new Random();
        for (int i = 0; i < 0x10000; i++) {
            long testSeed = 0x10000L * out1 + i;
            setSeed(rnd,testSeed);
            int testOut2 = rnd.nextInt();
            if (testOut2 == out2) {
                return rnd;
            }
        }
        throw new IllegalStateException("Random not found!");
    }

    private static List<Long> breakOtpGenerator(List<Integer> otps, int limit){
        int p = Integer.bitCount(limit ^ (limit - 1))-1;
        if (p < 1) {
            return List.of();
        }

        int p_mask = (1<<p) - 1;

        long h_otp0_shifted = (long) (otps.get(0) & p_mask) << 17; // S_0 mod 2^p  <<  17

        List<Integer> shortOtps = otps.stream().map(otp -> otp & p_mask).skip(1).collect(Collectors.toList());
        List<Integer> Ls = new ArrayList<>();

        // brute force 17 low bits
        for (int i = 0; i < 0x20000; i++) {
            SubGenerator subGen = new SubGenerator(p+17, h_otp0_shifted + i);
            boolean correct = true;
            for (int sOtp: shortOtps) {
                long next = subGen.next();
                if (sOtp != next) {
                    correct = false;
                    break;
                }
            }
            if (correct) {
                Ls.add(i);
            }
        }

        Random resultRnd = new Random();
        Set<Long> results = new HashSet<>();

        // brute force 31 high bits
        for (long j = 0; j < (1L << (31-p)); j++) {
            for (int l : Ls) {
                long h = (otps.get(0) + j * limit) & ((1L << 31) - 1);
                long state = (h << 17) + l;
                setSeed(resultRnd, state);
                boolean correct = true;
                for (int otp: otps.subList(1, otps.size())) {
                    long next = resultRnd.nextInt(limit);
                    if (otp != next) {
                        correct = false;
                        break;
                    }
                }
                if (correct) {
                    results.add(state);
                }
            }
        }
        return new ArrayList<>(results);
    }

    private static String toHexString(List<? extends Number> list, int precision) {
        StringBuilder sb = new StringBuilder("[");
        for (Number n: list) {
            sb.append(String.format("0x%0" + precision + "X, ", n));
        }
        sb.delete(sb.length()-2, sb.length());
        sb.append("]");
        return sb.toString();
    }

    private static void printHeader(String header) {
        int N = 60;
        int x = N - 6 - header.length(), x_1 = x / 2, x_2 = x - x_1;
        System.out.println("\033[1;32m");
        System.out.println("#".repeat(N));
        System.out.println("###" + " ".repeat(x_1) + header + " ".repeat(x_2) + "###");
        System.out.println("#".repeat(N) + "\033[0m");
    }
}
