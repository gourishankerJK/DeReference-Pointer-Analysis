import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import utils.CustomTag;

public class ApproximateCallStringPreProcess {
    private HashSet<String> visited = new HashSet<>();
    private Map<String, ProgramPoint> functionCallMap = new HashMap<>();
    private Map<String, List<ProgramPoint>> functionReturnMap = new HashMap<>();
    private HashMap<Unit, ProgramPoint> unitToProgramPoint = new HashMap<>();

    public List<ProgramPoint> PreProcess(Body body) {
        List<String> variables = gatherVariablesList(body);
        List<ProgramPoint> result = _preProcess(body, variables, body.getMethod().getName());
        Map<String, List<String>> callersList = getCallersList(result);
        checkForInfiniteLoops(body);
        tagCallerListToReturnUnit(result, callersList);
        return result;
    }

    private void checkForInfiniteLoops(Body body) {
        HashSet<Unit> isUnitVisited = new HashSet<>();
        HashSet<Unit> isUnitStVisited = new HashSet<>();
        ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(body);
       _checkForInfiniteLoops(makeList(body), cfg, isUnitVisited, isUnitStVisited, 0);
    }

    private List<Unit> makeList(Body body) {
        return new ArrayList<>(body.getUnits());

    }

    private Boolean isParentCondtional(Unit test, ExceptionalUnitGraph cfg) {
        Boolean flag = false;
        for (Unit currentUnit : cfg) {
            // Check if the current unit is the unit of interest
            if (currentUnit.equals(test)) {
                // Get the predecessors of the parent unit
                
                List<Unit> preds = cfg.getPredsOf(currentUnit);
                for (Unit parentUnit  : preds) {
                    // Check if the parent unit is a conditional statement
                    if (parentUnit instanceof IfStmt || parentUnit instanceof SwitchStmt) {
                        flag =  true;
                        break;
                    }
                }

                break;
            }
        }
        return flag;
    }

    private Boolean _checkForInfiniteLoops(List<Unit> units, ExceptionalUnitGraph cfg, HashSet<Unit> isUnitVisited,
            HashSet<Unit> isUnitStVisited,
            int i) {

        for (int j = i; j < units.size(); j++) {
            Unit unit = units.get(j);
            Stmt st = (Stmt) unit;
            if (st instanceof JReturnStmt || st instanceof JReturnVoidStmt)
                return false;
            else {

                if (st.containsInvokeExpr() && st.getInvokeExpr() instanceof StaticInvokeExpr
                        && !isParentCondtional(st, cfg)) {
                    // jth statement is not recursive
                    if (!isUnitStVisited.contains(unit)) {
                        isUnitStVisited.add(unit);
                        _checkForInfiniteLoops(units, cfg, isUnitVisited, isUnitStVisited, j + 1);
                        // isUnitStVisited.remove(unit);

                    }

                    // jth statement is recursive
                    if (!isUnitVisited.contains(unit)) {

                        isUnitVisited.add(unit);
                        Body body = st.getInvokeExpr().getMethod().retrieveActiveBody();
                        if (_checkForInfiniteLoops(makeList(body), new ExceptionalUnitGraph(body),
                                isUnitVisited, isUnitStVisited, 0)) {
                            unitToProgramPoint.get(unit).InfiniteLoop = true;
                            isUnitVisited.remove(unit);
                            return true;
                        }
                        isUnitVisited.remove(unit);

                    } else {
                        unitToProgramPoint.get(unit).InfiniteLoop = true;
                        isUnitVisited.remove(unit);
                        return true;
                    }

                }
            }
        }
        return true;

    }

    private Map<String, List<String>> getCallersList(List<ProgramPoint> body) {
        Map<String, List<String>> whoIsCallingMap = new HashMap<>();
        for (ProgramPoint progPoint : body) {
            if (progPoint.callEdgeId != null) {
                String method = progPoint.callSuccessor.getMethodName();
                for (ProgramPoint progPoint1 : body) {
                    if (progPoint1.getMethodName() == method) {
                        if (progPoint1.callEdgeId != null) {
                            if (whoIsCallingMap.get(progPoint1.callEdgeId) != null)
                                whoIsCallingMap.get(progPoint1.callEdgeId).add(progPoint.callEdgeId);
                            else {
                                List<String> t = new ArrayList<>();
                                t.add(progPoint.callEdgeId);
                                whoIsCallingMap.put(progPoint1.callEdgeId, t);
                            }

                        }
                    }
                }
            }
        }
        return whoIsCallingMap;
    }

    private void tagCallerListToReturnUnit(List<ProgramPoint> programPoints, Map<String, List<String>> value) {
        for (ProgramPoint programPoint : programPoints) {
            Stmt st = programPoint.getStmt();
            if (st instanceof JReturnStmt || st instanceof JReturnVoidStmt) {
                st.addTag(new CustomTag("CallersList", value));
            }
        }
    }

    private List<String> gatherVariablesList(Body body) {
        renameVariable(body);
        List<String> variables = GetRefTypeVariables(body);
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr invokeExpr = (StaticInvokeExpr) stmt.getInvokeExpr();
                String functionSignature = invokeExpr.getMethod().getSubSignature();
                if (!visited.contains(functionSignature)) {
                    visited.add(body.getMethod().getSubSignature());
                    List<String> vars = gatherVariablesList(invokeExpr.getMethod().retrieveActiveBody());
                    variables.addAll(vars);
                }
            }
        }
        return variables;
    }

    private List<ProgramPoint> _preProcess(Body body, List<String> variables, String MethodName) {

        List<ProgramPoint> result = new ArrayList<ProgramPoint>();

        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        int lineno = 0;
        // Initial pass to create list of program points, this loop is needed to track
        // the line numbers
        for (Unit unit : body.getUnits()) {
            unit.addTag(new CustomTag("lineNumberTag", lineno++));
            unit.addTag(new CustomTag("baseClass", body.getMethod().getDeclaringClass().toString()));
            unit.addTag(new CustomTag("functionName", body.getMethod().getName()));
            ProgramPoint programPoint = new ProgramPoint(
                    MethodName != null ? new ApproximateCallStringElement(variables)
                            : new ApproximateCallStringElement(),
                    (Stmt) unit,
                    true);

            programPoint.setMethodName(body.getMethod().getName());
            programPoint.className = body.getMethod().getDeclaringClass().toString();

            // if (stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt)
            // InfiniteLoop = false;
            // if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof
            // StaticInvokeExpr) {
            // if (stmt.getInvokeExpr().getMethod().equals(body.getMethod()) &&
            // InfiniteLoop)
            // programPoint.InfiniteLoop = true;
            // }

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

        // Third pass: For each call site in the body, check if the body is available,
        // then process
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr invokeExpr = (StaticInvokeExpr) stmt.getInvokeExpr();
                String functionSignature = invokeExpr.getMethod().getSubSignature();
                String callEdgeId = String.format("%s.in%02d",
                        body.getMethod().getName(), getLineNumber(stmt));

                // Process this body if not already processed.
                if (!functionCallMap.containsKey(functionSignature)) {
                    List<ProgramPoint> newBody = _preProcess(invokeExpr.getMethod().retrieveActiveBody(),
                            variables, null);
                    result.addAll(newBody);
                }
                unitToProgramPoint.get(unit).callSuccessor = (functionCallMap.get(functionSignature));
                // tagging in progress
                unit.addTag(new CustomTag("CallerIdTag", callEdgeId));
                unitToProgramPoint.get(unit).callEdgeId = callEdgeId;
                // Here assumption is that from one statement there can only be one call, and
                // its successor can only be one statement, meaning this for loop will EXECUTE
                // ONLY 1 TIME.
                for (Unit succ : graph.getSuccsOf(unit)) {
                    variableMappingForReturnAssign(unitToProgramPoint, unit, functionSignature, callEdgeId, succ);
                }
            }
        }
        return result;
    }

    private void variableMappingForReturnAssign(HashMap<Unit, ProgramPoint> unitToProgramPoint, Unit unit,
            String functionSignature,
            String callEdgeId, Unit succ) {
        for (ProgramPoint returnProgramPoint : functionReturnMap.get(functionSignature)) {
            returnProgramPoint.returnSuccessors.add(unitToProgramPoint.get(succ));
            returnProgramPoint.returnEdgeIds.add(callEdgeId);
            if (returnProgramPoint.getStmt() instanceof JReturnStmt && unit instanceof JAssignStmt) {
                String lhs = ((JAssignStmt) unit).getLeftOp().toString();
                CustomTag returnVarTag = (CustomTag) returnProgramPoint.getStmt().getTag("ReturnVars");
                // the return site may be to different points, therefore using callEdgeId as the
                // key
                if (returnVarTag == null) {
                    returnVarTag = new CustomTag("ReturnVars", callEdgeId,
                            lhs, true);
                    returnProgramPoint.getStmt().addTag(returnVarTag);
                } else {
                    returnVarTag.updateReturnVariableMap(callEdgeId, lhs);
                }
            }
        }
    }

    private List<String> GetRefTypeVariables(Body body) {
        List<String> result = new ArrayList<String>();
        for (Local local : body.getLocals()) {

            if (local.getType().getClass().equals(RefType.class)) {
                result.add(local.toString());
            }
        }
        return result;
    }

    private int getLineNumber(Stmt st) {
        return ((CustomTag) st.getTag("lineNumberTag")).getLineNumber();
    }

    private void renameVariable(Body body) {
        for (Unit unit : body.getUnits()) {
            String functionName = body.getMethod().getName();
            if (unit instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) unit;
                Value leftValue = identityStmt.getLeftOp();
                if (leftValue instanceof Local) {
                    addScopeOfFunction((Local) leftValue, functionName);
                }
            } else if (unit instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt) unit;
                Value leftValue = stmt.getLeftOp();
                Value rightValue = stmt.getRightOp();

                if (leftValue instanceof Local) {
                    addScopeOfFunction((Local) leftValue, functionName);
                }
                if (rightValue instanceof Local) {
                    addScopeOfFunction((Local) rightValue, functionName);

                }
            }
        }
    }

    private void addScopeOfFunction(Local local, String functionName) {
        String oldName = local.getName();
        String newVariableName = functionName + "::" + oldName;
        if (!oldName.contains("::"))
            local.setName(newVariableName);
    }

}
