import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Expr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class PointerLatticePreProcess implements IPreProcess {

    List<ProgramPoint> result = new ArrayList<ProgramPoint>();
    HashMap<String, Unit> visited = new HashMap<>();
    HashMap<Unit, ProgramPoint> UnitToPP = new HashMap<>();
    HashMap<Body, List<Unit>> callerList = new HashMap<>();
    HashMap<Unit, List<ProgramPoint>> UnitToReturnPP = new HashMap<>();

    int lineno = 0;

    public List<ProgramPoint> PreProcess(Body body) {
        List<String> variables = GetRefTypeVariables(body);
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

        // Initial pass to create list of program points

        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineno++));
            if (UnitToPP.get(unit) == null) {
                ProgramPoint programPoint = new ProgramPoint(new PointerLatticeElement(variables), (Stmt) unit, true,
                        body.getMethod().getSubSignature());
                UnitToPP.put(unit, programPoint);
                result.add(programPoint);
            }
        }

        // second pass to link the successors of each program point
        for (Unit unit : body.getUnits()) {
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                    StaticInvokeExpr invokeStmt = (StaticInvokeExpr) stmt.getInvokeExpr();
                    String name = invokeStmt.getMethod().getSignature();
                 
                    Body fn = invokeStmt.getMethod().retrieveActiveBody();
                    if (visited.getOrDefault(name, null) == null) {
                        Unit succ = fn.getUnits().getFirst();
                        visited.put(name, succ);
                        List<ProgramPoint> successor = new ArrayList<>();
                        ProgramPoint p = new ProgramPoint(new PointerLatticeElement(variables), (Stmt) succ, true,
                                invokeStmt.getMethod().getSubSignature());
                        successor.add(p);
                        result.add(p);
                        UnitToPP.put(succ, p);
                        UnitToPP.get(unit).setSuccessors(successor);
                        List<ProgramPoint> returnPoints = new ArrayList<>();
                        for (Unit succs : graph.getSuccsOf(unit)) {
                            returnPoints.add(UnitToPP.get(succs));
                        }
                        callerList.put(fn, Arrays.asList(unit));
                        UnitToReturnPP.put(unit, returnPoints);
                        PreProcess(fn);
                    } else {
                        List<ProgramPoint> successor = new ArrayList<>();
                        ProgramPoint BackEdge = UnitToPP.get(visited.get(name));
                        for (Unit succ : graph.getSuccsOf(unit)) {
                            successor.add(UnitToPP.get(succ));
                        }
                        successor.add(BackEdge);

                        UnitToPP.get(unit).setSuccessors(successor);

                    }

                } else if ((stmt instanceof ReturnStmt) || (stmt instanceof JReturnVoidStmt)) {
                    List<Unit> callers = callerList.getOrDefault(body, new ArrayList<Unit>());
                    ProgramPoint returnStmt = UnitToPP.get(unit);
                    List<ProgramPoint> points = new ArrayList<>();
                    for (Unit caller : callers) {
                        List<ProgramPoint> p = UnitToReturnPP.get(caller);
                        points.addAll(p);
                    }
                    returnStmt.setSuccessors(points);

                } else {
                    List<ProgramPoint> successors = new ArrayList<>();
                    for (Unit succ : graph.getSuccsOf(unit)) {
                        successors.add(UnitToPP.get(succ));
                    }
                    UnitToPP.get(unit).setSuccessors(successors);
                }

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