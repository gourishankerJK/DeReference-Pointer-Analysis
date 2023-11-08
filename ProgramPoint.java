import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
    private HashSet<ProgramPoint> visited = new HashSet<>();

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        PointerLatticeElement s = (PointerLatticeElement) latticeElement;
        PointerLatticeElement l = new PointerLatticeElement(s.getState());
        this.latticeElement = l;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
        this.successors = new ArrayList<>();
    }

    public LatticeElement getLatticeElement() {
        return this.latticeElement;
    }

    public void setLatticeElement(LatticeElement latticeElement) {
        PointerLatticeElement s = (PointerLatticeElement) latticeElement;
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

    public void printProgramPoints() {
        System.out.println("Statment : " + this.statement);
        System.out.println();

        for (ProgramPoint p : this.successors)
            System.out.print("Childrens : " + p.statement + " ");
        System.out.println("\n____");
    }

    public void printProgramPointsChain(int level) {
        if (visited.contains(this))
            return;
        String str = "";
        for (int i = 0; i < level; i++) {
            str += "\t";
        }
        System.out.println(str + this.statement);
        visited.add(this);
        for (ProgramPoint p : this.successors) {

            p.printProgramPointsChain(level + 1);
        }
    }
}