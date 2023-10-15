import java.util.HashSet;

import soot.Body;
import soot.RefType;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JimpleLocal;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PointerLatticeElement implements LatticeElement {
    private HashMap<String, HashSet<String>> State;

    public PointerLatticeElement() {
        this.State = new HashMap<String, HashSet<String>>();
    }

    public PointerLatticeElement(List<String> variables) {
        this.State = new HashMap<>();
        for (String variable : variables) {
            this.State.put(variable, new HashSet<>());
        }
    }

    public PointerLatticeElement(HashMap<String, HashSet<String>> state) {
        this.State = state;
    }

    public HashMap<String, HashSet<String>> getState() {
        return this.State;
    }

    public void printState() {
        for (String key : this.State.keySet()) {
            System.out.print(key + " :");
            System.out.println(this.State.get(key).toString());
        }
        System.out.println();

    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        HashMap<String, HashSet<String>> input = ((PointerLatticeElement) r).getState();

        HashMap<String, HashSet<String>> joinElementState = new HashMap<String, HashSet<String>>();

        for (String key : input.keySet()) {
            HashSet<String> value = new HashSet<String>();
            value.addAll(input.get(key));
            if (this.State.containsKey(key))
                value.addAll(this.State.get(key));
            joinElementState.put(key, value);
        }
        PointerLatticeElement joinElement = new PointerLatticeElement(joinElementState);

        return (LatticeElement) joinElement;

    }

    @Override
    public boolean equals(LatticeElement r) {
        HashMap<String, HashSet<String>> input = ((PointerLatticeElement) r).getState();
        for (String key : input.keySet()) {
            if (!this.State.get(key).equals(input.get(key)))
                return false;
        }
        return true;
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        // no actual assignments happening example virtual invoke
        if (st.getDefBoxes().isEmpty()) {
            return this;
        }

        // do nothing for identity or noop statements
        if (st.getClass().equals(JIdentityStmt.class) || st.getClass().equals(JNopStmt.class))
            return this;
        // Handle Assignment statements here
        Value lhs = ((AssignStmt) st).getLeftOp();
        Value rhs = ((AssignStmt) st).getRightOp();
        // Idenitty if rhs is static, or if lhs is a primitive type
        if (rhs.getClass().equals(StaticFieldRef.class) || !lhs.getType().getClass().equals(RefType.class))
            return this;
        // TODO: Modify the below logic to handle all cases
        // If lhs is class.field: TODO: handle for rhs as well
        if (lhs.getClass().equals(JInstanceFieldRef.class)) {
            String baseClass = ((JInstanceFieldRef) lhs).getBase().toString();
            for (String val : this.State.get(baseClass)) {
                String key = val + "." + ((JInstanceFieldRef) lhs).getField().getName();
                if (rhs.getClass().equals(JNewExpr.class))
                    this.State.put(key, this.State.get(rhs.toString()));
                else
                    this.State.put(key, this.State.get(rhs.toString()));
            }
        } else {
            // Use hash code for new assignments
            if (rhs.getClass().equals(JNewExpr.class))
                this.State.get(lhs.toString())
                        .add("new" + String.format("%02d", Integer.parseInt(st.getTags().get(1).toString())));
            // If rhs is also a reference then take the map of that reference and assign to
            // current lhs
            else if (rhs.getClass().equals(JimpleLocal.class)) {
                this.State.put(lhs.toString(), State.get(rhs.toString()));
                // Other cases just add the string to the current map
            } else {
                this.State.get(lhs.toString()).add(rhs.toString());
            }
        }

        return this;
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        if (st.getClass().equals(TableSwitchStmt.class))
            System.out.println("tableswitch: " + ((TableSwitchStmt) st).getKey().toString());
        else if (st.getClass().equals(IfStmt.class))
            System.out.println("tf_condstmt: " + ((IfStmt) st).getCondition().toString());
        return this;
    }

    public static List<ProgramPoint> PreProcessForKildall(Body body) {
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

    public static void PrintProgramPoints(List<ProgramPoint> programPoints) {
        int i = 0;
        for (ProgramPoint programPoint : programPoints) {
            System.out.println(String.format("----------%02d", i) + programPoint.statement.toString());
            ((PointerLatticeElement) programPoint.latticeElement).printState();
            i++;
        }
    }
}
