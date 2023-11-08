import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import soot.jimple.Stmt;
import soot.util.dot.DotGraph;
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

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation) {
        PointerLatticeElement s = (PointerLatticeElement) latticeElement;
        PointerLatticeElement l = new PointerLatticeElement(s.getState());
        this.latticeElement = l;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
        this.successors = new ArrayList<>();
        this.functionName = "X";
    }

    public ProgramPoint(LatticeElement latticeElement, Stmt stmt, boolean markedForPropagation, String functionName) {
        PointerLatticeElement s = (PointerLatticeElement) latticeElement;
        PointerLatticeElement l = new PointerLatticeElement(s.getState());
        this.latticeElement = l;
        this.statement = stmt;
        this.markedForPropagation = markedForPropagation;
        this.successors = new ArrayList<>();
        this.functionName = functionName;
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
        if(s.size() != 0) this.successors = s;
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
        HashSet<ProgramPoint> visited = new HashSet<>();
        _createCFG(this, dotGraph, visited);
        dotGraph.plot("./Callgraph.dot");
    }

    private void _createCFG(ProgramPoint programPoint, DotGraph dotGraph, HashSet<ProgramPoint> visited) {
        if (visited.contains(programPoint)) return;
        String parent = programPoint.functionName+ "\n"+programPoint.statement.toString();
        visited.add(programPoint);
        String bgColor = "orange";
        if(programPoint.statement.getJavaSourceStartLineNumber() %2 == 0){
            bgColor = "lightpink";
        }
        DotGraphNode parentNode = dotGraph.drawNode(parent);
         parentNode.setAttribute("color", "green");    
        parentNode.setAttribute("fillcolor",bgColor);
        parentNode.setStyle("filled");
        parentNode.setAttribute("fontsize" , "20");
        for (ProgramPoint p : programPoint.successors) {
            String children = p.functionName+"\n"+p.statement.toString();
            dotGraph.drawEdge(parent, children).setAttribute("color", "blue");;
            _createCFG(p, dotGraph, visited);
        }

    }
}