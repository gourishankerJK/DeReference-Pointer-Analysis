import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import soot.jimple.Stmt;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphEdge;
import soot.util.dot.DotGraphNode;

/**
 * A class to hold the program point abstraction
 */
public class ProgramPoint {
    private LatticeElement latticeElement;
    private Stmt statement;
    private boolean markedForPropagation;
    private List<ProgramPoint> successors;
    private String functionName;
    private String label;

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation, String label) {
        PointerLatticeElement s = (PointerLatticeElement) latticeElement;
        PointerLatticeElement l = new PointerLatticeElement(s.getState());
        this.latticeElement = l;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
        this.successors = new ArrayList<>();
        this.functionName ="x";
        this.label = label;
    }

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation, String functionName,
            String label) {
        PointerLatticeElement s = (PointerLatticeElement) latticeElement;
        PointerLatticeElement l = new PointerLatticeElement(s.getState());
        this.latticeElement = l;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
        this.successors = new ArrayList<>();
        this.functionName = functionName;
        this.label = label;
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
        if (s.size() != 0)
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

    public void createCFG() {
        DotGraph dotGraph = new DotGraph("CallGraph");
        DotGraph cluster = dotGraph.createSubGraph("cluster_0");
        cluster.setAttribute("fillcolor", "aqua");
        cluster.setAttribute("style", "filled");
        cluster.setAttribute("label", "InterProcedural_Graph");
        HashSet<ProgramPoint> visited = new HashSet<>();
        _createCFG(this, cluster, visited);
        dotGraph.plot("./Callgraph.dot");
    }

    private void _createCFG(ProgramPoint programPoint, DotGraph dotGraph, HashSet<ProgramPoint> visited) {
        if (visited.contains(programPoint))
            return;
        String[] colors = { "red", "blue", "purple", "black", "green", "navyblue", "orange" };
        String parent = programPoint.functionName + "\n\n" + programPoint.statement.toString();
        visited.add(programPoint);
        String bgColor = "orange";
        int index = programPoint.statement.getJavaSourceStartLineNumber() % 7;
        if (programPoint.statement.getJavaSourceStartLineNumber() % 2 == 0) {
            bgColor = "lightpink";
        }
        DotGraphNode parentNode = dotGraph.drawNode(parent);
        parentNode.setAttribute("color", "green");
        parentNode.setAttribute("fillcolor", bgColor);
        parentNode.setStyle("filled");
        parentNode.setAttribute("fontsize", "20");
        for (ProgramPoint p : programPoint.successors) {
            String children = p.functionName + "\n\n" + p.statement.toString();
            DotGraphEdge edge = dotGraph.drawEdge(parent, children);
            edge.setAttribute("color", colors[index]);
            edge.setLabel("  " + programPoint.label);
            edge.setAttribute("fontsize", "30");
            _createCFG(p, dotGraph, visited);
        }

    }
}