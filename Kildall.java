
import java.util.ArrayList;
import java.util.List;

import soot.Body;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Kildall {

    public List<ProgramPoint> ComputeLFP(Body body) {
        List<ProgramPoint> programPoints = new ArrayList<ProgramPoint>();

        // Preprocess - get all the variables
        List<String> variablesInProgram = GetVariables(body);
        int ppIndex = 0;
        for (Unit unit : body.getUnits()) {
            programPoints.add(new ProgramPoint(ppIndex++, new PointerLatticeElement(variablesInProgram), (Stmt) unit,
                    true));
        }
        UnitGraph cfg = new ExceptionalUnitGraph(body);

        ProgramPoint analysisPoint;
        int j = 0;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {

            analysisPoint.markedForPropagation = false;
            int i = 0;

            //System.out.println("Analysing: " + analysisPoint.lineNumber + ": " + analysisPoint.stmt.toString());

            for (ProgramPoint successor : GetSuccessors(analysisPoint, programPoints,
                    cfg)) {
                LatticeElement joinElement;

                if (analysisPoint.stmt.branches()) {
                    joinElement = successor.latticeElement
                            .join_op(analysisPoint.latticeElement.tf_condstmt(i == 0, analysisPoint.stmt));
                } else {
                    joinElement = successor.latticeElement
                            .join_op(analysisPoint.latticeElement.tf_assignstmt(analysisPoint.stmt));

                }

                if (joinElement.equals(successor.latticeElement) && !successor.markedForPropagation) {
                    successor.markedForPropagation = false;
                } else {
                    successor.markedForPropagation = true;
                }

                successor.latticeElement = joinElement;
                i++;
            }

            //System.out.println("Ending iteration : " + j++ + "___________________________________________________\n");

        }
        for (ProgramPoint pp : programPoints) {
            pp.print();
        }
        return programPoints;

    }

    private List<ProgramPoint> GetSuccessors(ProgramPoint programPoint, List<ProgramPoint> programPoints,
            UnitGraph cfg) {
        List<ProgramPoint> result = new ArrayList<>();
        for (Unit unit : cfg.getSuccsOf(programPoint.stmt)) {
            result.add(GetProgramPoint((Stmt) unit, programPoints));
        }
        return result;
    }

    private ProgramPoint GetProgramPoint(Stmt stmt, List<ProgramPoint> programPoints) {
        if (stmt == null)
            return programPoints.get(0);
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.stmt.equals(stmt))
                return programPoint;
        }
        return null;
    }

    private ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.markedForPropagation)
                return programPoint;
        }
        return null;
    }

    public static List<String> GetVariables(Body body) {
        List<String> result = new ArrayList<String>();
        int lineNumber = 0;
        for (Unit unit : body.getUnits()) {
            if (unit.getClass().equals(JAssignStmt.class)) {
                AssignStmt as = (AssignStmt) unit;
                if (as.getRightOp().toString().startsWith("new")) {
                    as.setRightOp(StringConstant.v("new" + lineNumber));
                }
            }
            lineNumber++;
            for (ValueBox vBox : unit.getDefBoxes()) {
                // only consider variables of the reference types -- ASSUMPTION
                if (vBox.getValue().getType().getClass().equals(soot.RefType.class)) {

                    result.add(vBox.getValue().toString());
                }
            }
        }

        System.out.println(result);
        return result;
    }
}
