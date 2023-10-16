import java.util.HashSet;

import soot.RefType;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JimpleLocal;
import soot.tagkit.Tag;

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
        HashMap<String, HashSet<String>> newState = new HashMap<String, HashSet<String>>();
        for (String key : state.keySet()) {
            HashSet<String> value = new HashSet<String>();
            value.addAll(state.get(key));
            newState.put(key, value);
        }
        this.State = newState;
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

    public static String getLineNumber(Stmt st) {
        List<Tag> tags = st.getTags();
        return String.format("%02d", Integer.parseInt(tags.get(tags.size() - 1).toString()));
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

        LatticeElement result = new PointerLatticeElement(State);

        if (st instanceof JAssignStmt) {
            return tfAssignmentStmt((AssignStmt) st);
        }
        return result;
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        if (st instanceof IfStmt) {
            return tfIfStmt(b, (IfStmt) st);
        }

        return this;
    }

    private LatticeElement tfIfStmt(boolean condition, IfStmt st) {

        Value t = st.getCondition();
        Value left = t.getUseBoxes().get(0).getValue();
        Value right = t.getUseBoxes().get(1).getValue();
        if (condition) {
            if (right.getType() instanceof RefType && left.getType() instanceof RefType) {
                PointerLatticeElement result = new PointerLatticeElement(this.State);
                result.State.get(right.toString()).retainAll(result.State.get(left.toString()));
                result.State.get(left.toString()).retainAll(result.State.get(right.toString()));
                return (LatticeElement) result;
            }

        }
        return this;
    }

    private LatticeElement tfAssignmentStmt(AssignStmt st) {
        System.out.println("Hit assignment: " + st);
        PointerLatticeElement result = new PointerLatticeElement(State);
        Value lhs = st.getLeftOp();
        Value rhs = st.getRightOp();

        System.out.println(lhs.getClass() + " : " + rhs.getClass());

        // x = new ()
        if (lhs instanceof JimpleLocal && rhs instanceof JNewExpr) {
            result.State.get(lhs.toString()).add("new" + (getLineNumber(st)));
        }
        // x = y
        if (lhs instanceof JimpleLocal && rhs instanceof JimpleLocal) {
            result.State.put(lhs.toString(), new HashSet<>(result.State.get(rhs.toString())));
        }

        // x = null
        if (lhs instanceof JimpleLocal && rhs instanceof NullConstant) {
            result.State.get(lhs.toString()).add("null");
        }

        // x.f = null
        if (lhs instanceof JInstanceFieldRef && rhs instanceof NullConstant) {
            JInstanceFieldRef l = (JInstanceFieldRef) lhs;
            for (String pseudoVar : result.State.get(l.getBase().toString())) {
                String key = pseudoVar + "." + l.getField().getName();
                if (pseudoVar == "null") {
                    continue;
                }
                if (!result.State.containsKey(key)) {
                    result.State.put(key, new HashSet<>());
                }
                result.State.get(key).add("null");
            }
        }

        // x = y.f
        if (lhs instanceof JimpleLocal && rhs instanceof JInstanceFieldRef) {
            HashSet<String> res = new HashSet<>();
            JInstanceFieldRef r = (JInstanceFieldRef) rhs;
            for (String pseudoVar : result.State.get(r.getBase().toString())) {
                if (pseudoVar == "null") {
                    continue;
                }
                String key = pseudoVar + "." + r.getField().getName();
                if (!result.State.containsKey(key)) {
                    result.State.put(key, new HashSet<>());
                }
                res.addAll(result.State.get(key));
            }
            result.State.put(lhs.toString(), res);
        }

        // x.f = y.f
        if (lhs instanceof JInstanceFieldRef && rhs instanceof JInstanceFieldRef) {
            HashSet<String> res = new HashSet<>();
            JInstanceFieldRef r = (JInstanceFieldRef) rhs;
            for (String pseudoVar : result.State.get(r.getBase().toString())) {
                if (pseudoVar == "null") {
                    continue;
                }
                String key = pseudoVar + "." + r.getField().getName();
                res.addAll(result.State.get(key));
            }
            JInstanceFieldRef l = (JInstanceFieldRef) lhs;
            for (String pseudoVar : result.State.get(l.getBase().toString())) {
                if (pseudoVar == "null") {
                    continue;
                }
                String key = pseudoVar + "." + l.getField().getName();
                if (!result.State.containsKey(key)) {
                    result.State.put(key, new HashSet<>());
                }
                result.State.put(key, res);
            }
        }

        // x.f = new
        if (lhs instanceof JInstanceFieldRef && rhs instanceof JNewExpr) {
            JInstanceFieldRef l = (JInstanceFieldRef) lhs;
            for (String pseudoVar : result.State.get(l.getBase().toString())) {
                String key = pseudoVar + "." + l.getField().getName();
                if (pseudoVar == "null") {
                    continue;
                }
                if (!result.State.containsKey(key)) {
                    result.State.put(key, new HashSet<>());
                }
                result.State.get(key).add("new" + getLineNumber(st));
            }
        }

        // x.f = y
        if (lhs instanceof JInstanceFieldRef && rhs instanceof JimpleLocal) {
            JInstanceFieldRef l = (JInstanceFieldRef) lhs;
            for (String pseudoVar : result.State.get(l.getBase().toString())) {
                if (pseudoVar == "null") {
                    continue;
                }
                String key = pseudoVar + "." + l.getField().getName();
                if (!result.State.containsKey(key)) {
                    result.State.put(key, new HashSet<>());
                }
                result.State.put(key, result.State.get(rhs.toString()));
            }
        }

        return result;
    }

}
