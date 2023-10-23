import java.util.HashSet;
import java.util.List;
import java.util.Map;

import soot.jimple.Stmt;

/**
 * A class to hold the program point abstraction
 */
public class ProgramPoint {
    LatticeElement latticeElement;
    Stmt statement;
    boolean markedForPropagation;
    List<ProgramPoint> successors;

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        this.latticeElement = latticeElement;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
    }

    public static void PrintProgramPoints(String analysisMethod, List<ProgramPoint> programPoints) {
        int i = 0;
        for (ProgramPoint programPoint : programPoints) {
            Map<String, HashSet<String>> latticeState = ((PointerLatticeElement) programPoint.latticeElement)
                    .getState();
            for (String key : latticeState.keySet()) {
                if (latticeState.get(key).size() == 0)
                    continue;
                System.out.println(
                        String.format("%s: in%02d: %s: {%s}", analysisMethod, i, key,
                                latticeState.get(key).toString().replace('[', ' ').replace(']', ' ')));
            }
            // ((PointerLatticeElement) programPoint.latticeElement).printState();
            i++;
        }
    }
}