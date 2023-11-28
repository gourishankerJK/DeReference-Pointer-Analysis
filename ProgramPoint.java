import java.util.ArrayList;
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
    public ProgramPoint callSuccessor;
    private String methodName;
    public String className;
    public String callEdgeId;
    public List<ProgramPoint> returnSuccessors;
    public List<String> returnEdgeIds;

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        ApproximateCallStringElement s = (ApproximateCallStringElement) latticeElement;
        ApproximateCallStringElement l = new ApproximateCallStringElement(s.getState());
        this.latticeElement = l;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
        this.returnEdgeIds = new ArrayList<>();
        this.returnSuccessors = new ArrayList<>();
    }

    public LatticeElement getLatticeElement() {
        return this.latticeElement;
    }

    public void setLatticeElement(LatticeElement latticeElement) {
        ApproximateCallStringElement s = (ApproximateCallStringElement) latticeElement;
        ApproximateCallStringElement l = new ApproximateCallStringElement(s.getState());
        this.latticeElement = l;
    }

    public Stmt getStmt() {
        return statement;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public void getStmt(Stmt st) {
        this.statement = st;
    }

    public List<ProgramPoint> getSuccessors() {
        List<ProgramPoint> ls = new ArrayList<>();
        ls.addAll(successors);
        return ls;
    }

    public List<ProgramPoint> getAllSuccessors() {
        List<ProgramPoint> ls = new ArrayList<>();
        if (this.returnSuccessors.size() != 0) {
            return this.returnSuccessors;
        }
        if (callSuccessor != null)
            ls.add(callSuccessor);
        if (this.callEdgeId != null) {
            return ls;
        }
        ls.addAll(successors);

        return ls;
    }

    public void setSuccessors(List<ProgramPoint> s) {
        this.successors = s;
    }

    public void addSuccessors(List<ProgramPoint> s) {
        this.successors.addAll(s);
    }

    public void addSuccessor(ProgramPoint s) {
        this.successors.add(s);
    }

    public boolean isMarked() {
        return this.markedForPropagation;
    }

    public void setMarkPoint(boolean mark) {
        this.markedForPropagation = mark;
    }

    @Override
    public String toString() {
        String ans = "Program Point \n --------------\n";
        ans += "\n Stmt " + statement.toString() + "\n";
        ans += "\n Lattice " + ((ApproximateCallStringElement) latticeElement);
        ans += "\nBoolean " + isMarked();
        ans += "\n Sucessors " + successors;
        ans += "\nCallSucessors " + callSuccessor;
        ans += "\n RetusnSucessors " + returnSuccessors;
        ans += "\nCallEdgeId " + callEdgeId;
        ans += "\n--------------------\n";
        return ans;
    }

    @Override
    public ProgramPoint clone() {
        try {
            ProgramPoint clonedPoint = (ProgramPoint) super.clone();
            if (latticeElement != null) {
                clonedPoint.latticeElement = ((ApproximateCallStringElement) latticeElement).clone();
            }
            if (statement != null) {
                clonedPoint.statement = (Stmt) statement.clone();
            }
            if (successors != null) {
                clonedPoint.successors = new ArrayList<>();
                for (ProgramPoint successor : successors) {
                    clonedPoint.successors.add(successor.clone());
                }
            }
            if (callSuccessor != null) {
                clonedPoint.callSuccessor = callSuccessor.clone();
            }
            if (returnSuccessors != null) {
                clonedPoint.returnSuccessors = new ArrayList<>();
                for (ProgramPoint returnSuccessor : returnSuccessors) {
                    clonedPoint.returnSuccessors.add(returnSuccessor.clone());
                }
            }
            clonedPoint.methodName = this.methodName;
            clonedPoint.className = this.className;
            clonedPoint.callEdgeId = this.callEdgeId;
            clonedPoint.markedForPropagation = this.markedForPropagation;
            clonedPoint.returnEdgeIds = this.returnEdgeIds;
            // Clone other fields as needed
            return clonedPoint;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
