import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class PointerLatticePreProcess implements IPreProcess {

    List<ProgramPoint> result = new ArrayList<ProgramPoint>();
    HashMap<String, Unit> FuncSigToEntryUnit = new HashMap<>();
    HashMap<Unit, ProgramPoint> UnitToPP = new HashMap<>();
    HashMap<Body, List<Unit>> functionCallerList = new HashMap<>();
    HashMap<Unit, List<ProgramPoint>> UnitToReturnPPs = new HashMap<>();

    int lineno = 0;
    int label = 0;

    public List<ProgramPoint> PreProcess(Body body) {
        List<String> variables = GetRefTypeVariables(body);
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

        // Initial pass to create list of program points

        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineno++));
            if (UnitToPP.get(unit) == null) {
                ProgramPoint programPoint = new ProgramPoint(new PointerLatticeElement(variables), (Stmt) unit, true,
                        body.getMethod().getSubSignature(), Integer.toString(label++));
                UnitToPP.put(unit, programPoint);
                result.add(programPoint);
            } else
                result.add(UnitToPP.get(unit));
        }

        // second pass to link the successors of each program point
        for (Unit unit : body.getUnits()) {
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                    StaticInvokeExpr invokeStmt = (StaticInvokeExpr) stmt.getInvokeExpr();
                    String functionSignature = invokeStmt.getMethod().getSignature();
                    if (FuncSigToEntryUnit.getOrDefault(functionSignature, null) == null) {
                        AddCallEdgeToCalledFunction(variables, graph, unit, invokeStmt, functionSignature);
                        PreProcess(invokeStmt.getMethod().retrieveActiveBody());
                    } else {
                        AddBackCallEdge(graph, unit, functionSignature);
                    }

                } else if ((stmt instanceof ReturnStmt) || (stmt instanceof JReturnVoidStmt)) {
                    AddReturnEdge(body, unit);

                } else {
                    AddEdgeIntraProcedural(graph, unit);
                }

            }
        }
        return result;

    }

    private void AddBackCallEdge(ExceptionalUnitGraph graph, Unit unit, String name) {
        List<ProgramPoint> successor = new ArrayList<>();
        ProgramPoint BackEdge = UnitToPP.get(FuncSigToEntryUnit.get(name));
        for (Unit succ : graph.getSuccsOf(unit)) {
            successor.add(UnitToPP.get(succ));
        }
        successor.add(BackEdge);

        UnitToPP.get(unit).setSuccessors(successor);
    }

    private void AddEdgeIntraProcedural(ExceptionalUnitGraph graph, Unit unit) {
        List<ProgramPoint> successors = new ArrayList<>();
        for (Unit succ : graph.getSuccsOf(unit)) {
            successors.add(UnitToPP.get(succ));
        }
        UnitToPP.get(unit).setSuccessors(successors);
    }

    private void AddReturnEdge(Body body, Unit unit) {
        List<Unit> callersOfCurrentFunction = functionCallerList.getOrDefault(body, new ArrayList<Unit>());
        ProgramPoint returnStmtPP = UnitToPP.get(unit);
        List<ProgramPoint> retunProgramPoints = new ArrayList<>();
        for (Unit caller : callersOfCurrentFunction) {
            retunProgramPoints.addAll(UnitToReturnPPs.get(caller));
        }
        returnStmtPP.setSuccessors(retunProgramPoints);
    }

    private void AddCallEdgeToCalledFunction(List<String> variables, ExceptionalUnitGraph graph, Unit unit,
            StaticInvokeExpr invokeStmt, String functionSignature) {
        Body functionBody = invokeStmt.getMethod().retrieveActiveBody();
        Unit targetOfCurrentFunction = functionBody.getUnits().getFirst();
        FuncSigToEntryUnit.put(functionSignature, targetOfCurrentFunction);
        ProgramPoint programPointofTarget = new ProgramPoint(new PointerLatticeElement(variables),
                (Stmt) targetOfCurrentFunction, true,
                functionBody.getMethod().getSubSignature(), Integer.toString(label++));
        UnitToPP.put(targetOfCurrentFunction, programPointofTarget);
        UnitToPP.get(unit).setSuccessors(Arrays.asList(programPointofTarget));
        List<ProgramPoint> returnProgramPoints = new ArrayList<>();
        for (Unit returnPointsOfCalledFunction : graph.getSuccsOf(unit)) {
            returnProgramPoints.add(UnitToPP.get(returnPointsOfCalledFunction));
        }
        functionCallerList.put(functionBody, Arrays.asList(unit));
        UnitToReturnPPs.put(unit, returnProgramPoints);
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