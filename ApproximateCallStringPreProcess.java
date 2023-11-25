import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.toolkits.scalar.LocalNameStandardizer;
import soot.tagkit.LineNumberTag;
import soot.tagkit.StringTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ApproximateCallStringPreProcess {

    private Map<String, ProgramPoint> functionCallMap = new HashMap<>();
    private Map<String, List<ProgramPoint>> functionReturnMap = new HashMap<>();

    public List<String> gatherVariablesList(Body body) {
        renameVariable(body);
        List<String> variables = GetRefTypeVariables(body);
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr invokeExpr = (StaticInvokeExpr) stmt.getInvokeExpr();
                String functionSignature = invokeExpr.getMethod().getSubSignature();
                if (!functionCallMap.containsKey(functionSignature)) {
                    List<String> vars = gatherVariablesList(invokeExpr.getMethod().retrieveActiveBody());
                    variables.addAll(vars);
                }
            }
        }
        return variables;
    }

    public List<ProgramPoint> PreProcess(Body body, List<String> variables) {

        List<ProgramPoint> result = new ArrayList<ProgramPoint>();
        HashMap<Unit, ProgramPoint> unitToProgramPoint = new HashMap<>();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        int lineno = 0;
        // Initial pass to create list of program points, this loop is needed to track
        // the line numbers
        for (Unit unit : body.getUnits()) {
            unit.addTag(new LineNumberTag(lineno++));
            ProgramPoint programPoint = new ProgramPoint(new ApproximateCallStringElement(variables), (Stmt) unit,
                    true);

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

        // Third pass: For each call site in the body, check if the body is available,
        // then process
        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr invokeExpr = (StaticInvokeExpr) stmt.getInvokeExpr();
                String functionSignature = invokeExpr.getMethod().getSubSignature();
                String callEdgeId = String.format("%s.%s.in%02d", body.getMethod().getDeclaringClass(),
                        invokeExpr.getMethod().getName(), getLineNumber(stmt));

                // Process this body if not already processed.
                if (!functionCallMap.containsKey(functionSignature)) {
                    List<ProgramPoint> newBody = PreProcess(invokeExpr.getMethod().retrieveActiveBody(), variables);
                    result.addAll(newBody);
                }
                unitToProgramPoint.get(unit).callSuccessor = (functionCallMap.get(functionSignature));

                // tagging in progress
                unit.addTag(new StringTag(callEdgeId));
                unitToProgramPoint.get(unit).callEdgeId = callEdgeId;
                // Here assumption is that from one statement there can only be one call, and
                // its successor can only be one statement.

                for (Unit succ : graph.getSuccsOf(unit)) {
                    for (ProgramPoint retSucc : functionReturnMap.get(functionSignature)) {
                        boolean tagged_done = false;
                        retSucc.returnSuccessors.add(unitToProgramPoint.get(succ));
                        retSucc.returnEdgeIds.add(callEdgeId);

                        for (Tag tag : retSucc.getStmt().getTags()) {
                            if (tag instanceof HashMapTag) {
                                tagged_done = false;
                                HashMapTag tagged = (HashMapTag) tag;
                                tagged.UpdateMapTag(functionSignature, callEdgeId);
                            }
                        }
                        if (!tagged_done) {
                            retSucc.getStmt().addTag(new HashMapTag(functionSignature, callEdgeId));
                        }
                    }
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

    private static int getLineNumber(Stmt st) {
        List<Tag> tags = st.getTags();
        int lineno = 0;
        for (Tag t : tags) {
            if (t instanceof LineNumberTag) {
                lineno = Integer.parseInt(t.toString());
            }
        }
        return lineno;
    }

    public static void renameVariable(Body body) {
        for (Unit unit : body.getUnits()) {
            String functionName = body.getMethod().getName();
            if (unit instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) unit;
                Value leftValue = identityStmt.getLeftOp();
                if (leftValue instanceof Local) {
                    changeName((Local) leftValue, functionName);
                }
            } else if (unit instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt) unit;
                Value leftValue = stmt.getLeftOp();
                Value rightValue = stmt.getRightOp();

                if (leftValue instanceof Local) {
                    changeName((Local) leftValue, functionName);
                }
                if (rightValue instanceof Local) {
                    changeName((Local) rightValue, functionName);

                }
            }
        }
    }

    public static void changeName(Local local, String functionName) {
        String oldName = local.getName();
        String newVariableName = functionName + "::" + oldName;
        if (!oldName.contains("::"))
            local.setName(newVariableName);
    }

}
