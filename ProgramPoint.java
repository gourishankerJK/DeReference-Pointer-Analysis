import java.util.HashMap;

import soot.jimple.Stmt;

public class ProgramPoint {
    int lineNumber;
    LatticeElement latticeElement;
    Stmt stmt;
    boolean markedForPropagation;

    public ProgramPoint(int index, LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        this.lineNumber = index;
        this.latticeElement = latticeElement;
        this.stmt = stmt;
        this.markedForPropagation = markedForPropagation;
    }

    public void print(HashMap<Integer, String> map) {
        PointerLatticeElement e = (PointerLatticeElement) this.latticeElement;
        System.out.println(stmt.getClass().getName());
        for (String key : e.getState().keySet()) {
            String lv = key;
            for (Integer k : map.keySet()) {
                if (key.contains(k.toString()))
                    lv = lv.replace(k.toString(), map.get(k).toString());
            }
            if (!e.getState().get(key).isEmpty()) {
                String rv = e.getState().get(key).toString();
                for (Integer k : map.keySet()) {
                    if (rv.contains(k.toString()))
                        rv = rv.replace(k.toString(), map.get(k).toString());
                }
                System.out.println(String.format("%02d", lineNumber) + ": " + stmt.toString() + "; " + lv + " : "
                        + rv);
            }
        }
        // "markedForPropagation : " + markedForPropagation +
        System.out.println();
    }
}