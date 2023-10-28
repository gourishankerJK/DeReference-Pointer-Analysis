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
}