import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Unit;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ApproximateCallStringPreProcess implements IPreProcess {

    private Map<String, ProgramPoint> functionCallMap = new HashMap<>();
    private Map<String, List<ProgramPoint>> functionReturnMap = new HashMap<>();
    static Integer callsiteId = 0;

    public List<ProgramPoint> PreProcess(Body body) {
        List<ProgramPoint> result = new ArrayList<ProgramPoint>();
        HashMap<Unit, ProgramPoint> unitToProgramPoint = new HashMap<>();
        List<String> variables = GetRefTypeVariables(body);
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        int lineno = 0;
        // Initial pass to create list of program points, this loop is needed to track
        // the line numbers
        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineno++));
            ProgramPoint programPoint = new ProgramPoint(new ApproximateCallStringElement(variables), (Stmt) unit, true);
            programPoint.methodName = body.getMethod().getSubSignature();
            unitToProgramPoint.put(unit, programPoint);
            result.add(programPoint);
        }
        // second pass to link the successors of each program point
        List<ProgramPoint> returns = new ArrayList<>();
        for (Unit unit : body.getUnits()) {
            List<ProgramPoint> successors = new ArrayList<>();
            for (Unit succ : graph.getSuccsOf(unit)) {
                successors.add(unitToProgramPoint.get(succ));
            }
            unitToProgramPoint.get(unit).setSuccessors(successors);
            if (unit instanceof JReturnStmt || unit instanceof JReturnVoidStmt) {
                returns.add(unitToProgramPoint.get(unit));
            }
        }
        functionReturnMap.put(body.getMethod().getSubSignature(), returns);
        // Add the processed body into functionCallMap
        functionCallMap.put(body.getMethod().getSubSignature(), result.get(0));

        // Third pass: For each call site in the body, check if the body is available, then process
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr invokeExpr = (StaticInvokeExpr) stmt.getInvokeExpr();
                String functionSignature = invokeExpr.getMethod().getSubSignature();
                // Process this body if not already processed.
                if (!functionCallMap.containsKey(functionSignature)) {
                    List<ProgramPoint> newBody = PreProcess(invokeExpr.getMethod().retrieveActiveBody());
                    result.addAll(newBody);
                }
                unitToProgramPoint.get(unit).callSuccessor = (functionCallMap.get(functionSignature));
                unitToProgramPoint.get(unit).callEdgeId = callsiteId;
                // Here assumption is that from one statement there can only be one call, and its successor can only be one statement.
                for (Unit succ : graph.getSuccsOf(unit)) {
                    for(ProgramPoint retSucc: functionReturnMap.get(functionSignature)) {
                        retSucc.returnSuccessors.add(unitToProgramPoint.get(succ));
                        retSucc.returnEdgeIds.add(callsiteId);
                    }
                }
                callsiteId++;
            }
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
