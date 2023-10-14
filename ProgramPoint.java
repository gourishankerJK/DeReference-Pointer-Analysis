import soot.jimple.Stmt;

public class ProgramPoint {
    LatticeElement latticeElement;
    Stmt stmt;
    boolean markedForPropagation;

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        this.latticeElement = latticeElement;
        this.stmt = stmt;
        this.markedForPropagation = markedForPropagation;
    }
}