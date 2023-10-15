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

    public void Propagate() {
        int i = 0;
        if (!markedForPropagation)
            return;
        // Unmark and propagate to the successors
        markedForPropagation = false;
        System.out.println(statement.toString());
        for (ProgramPoint successor : successors) {
            System.out.println("\t" + successor.statement.toString());
            LatticeElement joinElement;

            if (statement.branches()) {
                joinElement = successor.latticeElement
                        .join_op(latticeElement.tf_condstmt(i == 0, statement));
            } else {
                joinElement = successor.latticeElement
                        .join_op(latticeElement.tf_assignstmt(statement));
            }
            // Unmark the successor nodes based on the previous value
            if (joinElement.equals(successor.latticeElement) && !successor.markedForPropagation) {
                successor.markedForPropagation = false;
            } else {
                successor.markedForPropagation = true;
                successor.latticeElement = joinElement;
            }
            i++;
        }
    }
}