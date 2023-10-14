import java.util.HashSet;

import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;

import java.util.HashMap;
import java.util.List;

public class PointerLatticeElement implements LatticeElement {
    /// Some functions that we may use
    // construtor
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
        System.out.println("getLeftOp: " + ((AssignStmt) st).getLeftOp().toString() + " type: "
                + ((AssignStmt) st).getLeftOp().getClass().toString());
        System.out.println("getRightOp: " + ((AssignStmt) st).getRightOp().getClass().toString() + " type: "
                + ((AssignStmt) st).getRightOp().getType().toString());
        // Idenitty for static class
        if (((AssignStmt) st).getRightOp().getClass().equals(soot.jimple.StaticFieldRef.class))
            return this;
        // Identity for primitives
        for (ValueBox v : st.getDefBoxes()) {
            if (!v.getValue().getType().getClass().equals(soot.RefType.class))
                return this;
        }
        Value lhs = ((AssignStmt) st).getLeftOp();
        Value rhs = ((AssignStmt) st).getRightOp();
        if (lhs.getClass().equals(JInstanceFieldRef.class)) {
            System.out.println("Its instanace");
            String baseClass = ((JInstanceFieldRef) lhs).getBase().toString();
            for (String val : this.State.get(baseClass)) {
                String key = val + "." + ((JInstanceFieldRef) lhs).getField().getName();
                if (rhs.getClass().equals(JNewExpr.class))
                    this.State.put(key, this.State.get(rhs.toString()));
                else
                    this.State.put(key, this.State.get(rhs.toString()));

            }
        } else {

            if (rhs.getClass().equals(JNewExpr.class))
                this.State.get(lhs.toString()).add(st.hashCode() + "");
            else
                this.State.get(lhs.toString()).add(rhs.toString());
        }

        return this;
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        // TODO Auto-generated method stub
        if (st.getClass().equals(TableSwitchStmt.class))
            System.out.println("tableswitch: " + ((TableSwitchStmt) st).getKey().toString());
        else if (st.getClass().equals(IfStmt.class))
            System.out.println("tf_condstmt: " + ((IfStmt) st).getCondition().toString());
        return this;
    }

}
