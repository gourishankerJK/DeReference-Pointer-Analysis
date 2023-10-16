import java.util.List;

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

    public static void PrintProgramPoints(List<ProgramPoint> programPoints) {
        int i = 0;
        for (ProgramPoint programPoint : programPoints) {
            System.out.println(String.format("----------%02d", i) + programPoint.statement.toString());
            ((PointerLatticeElement) programPoint.latticeElement).printState();
            i++;
        }
    }
}