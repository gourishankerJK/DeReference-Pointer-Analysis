// DO NOT MODIFY this file.
// You can make modifications by overriding the methods in Analysis.java

import java.util.*;

public class PAVBase {

    protected static String getPrgPointName(int st1) {
        String name1 = "in" + String.format("%02d", st1);
        return name1;
    }

    public static class ResultTuple {
        public final String m;
        public final String p;
        public final String v;
        public final List<String> pV;

        public ResultTuple(String method, String prgpoint, String varname,
                List<String> pointerValues) {
            this.m = method;
            this.p = prgpoint;
            this.v = varname;
            this.pV = pointerValues;
        }
    }

    protected static String fmtOutputLine(ResultTuple tup, String prefix) {
        String line = tup.m + ": " + tup.p + ": " + tup.v + ": " + "{";
        List<String> pointerValues = tup.pV;
        int current = 0;
        for(String pointers: pointerValues){
            if (current < (pointerValues.size() - 1)) {
                line += pointers+", ";
            } else {
                line += pointers;
            }
            current++;
        }
        line= line+"}";
        return (prefix + line);
    }
    protected static String fmtOutputLine(ResultTuple tup) {
        return fmtOutputLine(tup, "");
    }

    protected static String[] fmtOutputData(Set<ResultTuple> data, String prefix) {

        String[] outputlines = new String[ data.size() ];

        int i = 0;
        for (ResultTuple tup : data) {
            outputlines[i] = fmtOutputLine(tup, prefix);
            i++;
        }

        Arrays.sort(outputlines);
        return outputlines;
    }
    protected static String[] fmtOutputData(Set<ResultTuple> data) {
        return fmtOutputData(data, "");
    }
}
