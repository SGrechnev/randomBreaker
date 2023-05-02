package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CUtils {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(101, 102, 103, 104, 105, 106));

        CombinationGenerator combinator = new CombinationGenerator(6,4);
        int[] cmb;
        while((cmb = combinator.next()) != null){
            System.out.println("list{" + Arrays.toString(cmb) + "} = " + getByIdxs(list, cmb));
        }
    }

    public static <T> List<T> getByIdxs(List<T> list, int[] idxs) {
        List<T> result = new ArrayList<>();
        for (int idx: idxs) {
            result.add(list.get(idx));
        }
        return result;
    }
}
