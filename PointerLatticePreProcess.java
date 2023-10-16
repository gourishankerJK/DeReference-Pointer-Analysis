import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Unit;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class PointerLatticePreProcess implements IPreProcess {

    public  List<ProgramPoint> PreProcess(Body body) {
        List<ProgramPoint> result = new ArrayList<ProgramPoint>();
        HashMap<Unit, ProgramPoint> mp = new HashMap<>();
        List<String> variables = GetRefTypeVariables(body);
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        int lineno = 0;
        // Initial pass to create list of program points
        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineno++));
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
        for (Local local : body.getLocals()) {
            if (local.getType().getClass().equals(RefType.class)) {
                result.add(local.toString());
            }
        }
        return result;
    }
}