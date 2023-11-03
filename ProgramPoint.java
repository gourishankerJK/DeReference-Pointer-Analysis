import java.util.List;

import soot.jimple.Stmt;

/**
 * A class to hold the program point abstraction
 */
public class ProgramPoint {
    private LatticeElement latticeElement;
    private Stmt statement;
    private boolean markedForPropagation;
    private List<ProgramPoint> successors;

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        this.latticeElement = latticeElement;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
    }

    public LatticeElement getLatticeElement() {
       return this.latticeElement;
    }

    public void setLatticeElement(LatticeElement latticeElement) {
        PointerLatticeElement s = (PointerLatticeElement)latticeElement;
        PointerLatticeElement l = new PointerLatticeElement(s.getState());
        this.latticeElement = l;
    }

    public Stmt getStmt() {
        return statement;
    }

    public void getStmt(Stmt st) {
        this.statement = st;
    }

    public List<ProgramPoint> getSuccessors() {
        return successors;
    }

    public void setSuccessors(List<ProgramPoint> s) {
        this.successors = s;
    }

    public boolean isMarked() {
        return this.markedForPropagation;
    }

    public void setMarkPoint(boolean mark) {
        this.markedForPropagation = mark;
    }
}