import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import utils.CustomTag;

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

    public List<ProgramPoint> PreProcess(Body body, String mainFunction, List<String> variables) {

        List<ProgramPoint> result = new ArrayList<ProgramPoint>();
        HashMap<Unit, ProgramPoint> unitToProgramPoint = new HashMap<>();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        int lineno = 0;
        // Initial pass to create list of program points, this loop is needed to track
        // the line numbers
        for (Unit unit : body.getUnits()) {
            unit.addTag(new CustomTag("lineNumberTag", lineno++));
            unit.addTag(new CustomTag("baseClass", body.getMethod().getDeclaringClass().toString()));
            unit.addTag(new CustomTag("functionName", body.getMethod().getName()));
            ProgramPoint programPoint = new ProgramPoint(
                    new ApproximateCallStringElement(variables, body.getMethod().getName().equals(mainFunction)),
                    (Stmt) unit,
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
                    List<ProgramPoint> newBody = PreProcess(invokeExpr.getMethod().retrieveActiveBody(), mainFunction,
                            variables);
                    result.addAll(newBody);
                }
                unitToProgramPoint.get(unit).callSuccessor = (functionCallMap.get(functionSignature));

                // tagging in progress
                unit.addTag(new CustomTag("CallerIdTag", callEdgeId));
                unitToProgramPoint.get(unit).callEdgeId = callEdgeId;
                // Here assumption is that from one statement there can only be one call, and
                // its successor can only be one statement.

                for (Unit succ : graph.getSuccsOf(unit)) {
                    for (ProgramPoint retSucc : functionReturnMap.get(functionSignature)) {
                        retSucc.returnSuccessors.add(unitToProgramPoint.get(succ));
                        retSucc.returnEdgeIds.add(callEdgeId);
                        CustomTag callersTag = (CustomTag) (retSucc.getStmt().getTag("CallersTag"));
                        if (callersTag != null)
                            callersTag.UpdateMapTag(functionSignature, callEdgeId);
                        else {
                            String whereIhavetoReturnId = String.format("%s.%s.in%02d",
                                    body.getMethod().getDeclaringClass(),
                                    invokeExpr.getMethod().getName(), getLineNumber(retSucc.getStmt()));
                            System.out.println(whereIhavetoReturnId);
                            CustomTag tag = new CustomTag("CallersTag", whereIhavetoReturnId, callEdgeId);
                            retSucc.getStmt().addTag(tag);
                            System.out.println("Heoldfsdafod" + retSucc.getStmt().getTags());
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
        return ((CustomTag) st.getTag("lineNumberTag")).getLineNumber();
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
