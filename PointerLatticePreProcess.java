import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Body;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class PointerLatticePreProcess implements IPreProcess {

    public List<ProgramPoint> PreProcessForKildall(Body body) {
        List<ProgramPoint> result = new ArrayList<ProgramPoint>();
        HashMap<Unit, ProgramPoint> mp = new HashMap<>();
        List<String> variables = GetRefTypeVariables(body);
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        int lineno = 0;
        // Initial pass to create list of program points
        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineno));
            ProgramPoint programPoint = new ProgramPoint(new PointerLatticeElement(variables), (Stmt) unit, true);
            mp.put(unit, programPoint);
            result.add(programPoint);
        }
        // second pass to link the successors of each program point
        for (Unit unit : body.getUnits()) {
            List<ProgramPoint> successors = new ArrayList<>();
            for (Unit succ : graph.getSuccsOf(unit)) {
                successors.add(mp.get(succ));
            }
            mp.get(unit).successors = successors;
        }
        return result;
    }

    public static List<String> GetRefTypeVariables(Body body) {
        List<String> result = new ArrayList<String>();
        for (Unit unit : body.getUnits()) {
            for (ValueBox vBox : unit.getDefBoxes()) {
                // only consider variables of the reference types -- ASSUMPTION
                if (vBox.getValue().getType().getClass().equals(soot.RefType.class) &&
                        !vBox.getValue().getClass().equals(JInstanceFieldRef.class)) {
                    result.add(vBox.getValue().toString());
                }
            }

        }
        return result;
    }
}