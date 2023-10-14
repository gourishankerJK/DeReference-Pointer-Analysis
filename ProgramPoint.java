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

    public void print() {
        PointerLatticeElement e = (PointerLatticeElement) this.latticeElement;
        for (String key : e.getState().keySet()) {
            if (!e.getState().get(key).isEmpty())
                System.out.println(String.format("%02d", lineNumber) + ": " + stmt.toString() + "; " + key + " : "
                        + e.getState().get(key).toString());
        }
        // "markedForPropagation : " + markedForPropagation +
        System.out.println("\n");
    }
}