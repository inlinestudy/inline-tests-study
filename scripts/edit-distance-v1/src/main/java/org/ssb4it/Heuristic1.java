package org.ssb4it;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public class Heuristic1 {
    public static String getVerdict(List<Integer> lengths, List<Integer> eds, String label) {
        int zeroEdCount = 0;
        int theOneLength = 0;
        int minEd = Integer.MAX_VALUE;
        List<Integer> edLeqHalfLenIdx = new ArrayList<>();
        List<Integer> edGtHalfLenIdx = new ArrayList<>();
        String otherLabel = null;
        if (label.equals("MANY_TO_ONE")) {
            theOneLength = lengths.get(lengths.size() - 1);
            otherLabel = "DELETE";
        } else if (label.equals("ONE_TO_MANY")) {
            theOneLength = lengths.get(0);
            otherLabel = "ADD";
        }
        for (int i = 0; i < eds.size(); i++) {
            if (eds.get(i) == 0) {
                zeroEdCount++;
                continue;
            }
            if (eds.get(i) <= theOneLength / 2) {
                edLeqHalfLenIdx.add(eds.get(i));
            } else {
                edGtHalfLenIdx.add(eds.get(i));
            }
        }
        StringBuilder sb = new StringBuilder();
        if (zeroEdCount == 1) {
            for (Iterator<Integer> it = eds.iterator(); it.hasNext();) {
                int ed = it.next();
                sb.append(ed == 0 ? "NO_CHANGE" : otherLabel);
                sb.append(it.hasNext() ? "," : "");
            }
            sb.append(";HIGH");
            return sb.toString();
        } else if (zeroEdCount > 1) {
            boolean first = true;
            for (Iterator<Integer> it = eds.iterator(); it.hasNext();) {
                int ed = it.next();
                if (ed == 0 && first) {
                    first = false;
                    sb.append("NO_CHANGE");
                } else {
                    sb.append(otherLabel);
                }
                sb.append(it.hasNext() ? "," : "");
            }
            sb.append(";LOW");
            return sb.toString();
        }
        if (edLeqHalfLenIdx.size() == 1) {
            int targetEd = edLeqHalfLenIdx.get(0);
            for (Iterator<Integer> it = eds.iterator(); it.hasNext();) {
                int ed = it.next();
                if (ed == targetEd) {
                    sb.append("1:1");
                } else {
                    sb.append(otherLabel);
                }
                sb.append(it.hasNext() ? "," : "");
            }
            sb.append(";HIGH");
            return sb.toString();
        } else if (edLeqHalfLenIdx.size() > 1) {
            return "NEED_INSPECTION";
        }
        return "NEED_INSPECTION";
    }
}
