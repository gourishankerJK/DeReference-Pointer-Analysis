
import java.util.ArrayList;
import java.util.List;

import soot.Body;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
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

            System.out.println("Analysing: " + analysisPoint.stmt.toString());

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

                if (joinElement.equals(successor.latticeElement)) {
                    successor.markedForPropagation = false;
                } else {
                    successor.markedForPropagation = true;
                }

                successor.latticeElement = joinElement;
                i++;
            }

            System.out.println("___________________________________________________Ending iteration : " + j++);
            for (ProgramPoint pp : programPoints) {
                pp.print();
            }

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
        for (Unit unit : body.getUnits()) {
            for (ValueBox vBox : unit.getDefBoxes()) {
                result.add(vBox.getValue().toString());
            }
        }

        return result;
    }
}
