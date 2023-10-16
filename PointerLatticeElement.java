import java.util.HashSet;

import soot.Local;
import soot.NullType;
import soot.RefType;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CmpExpr;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.NeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JimpleLocal;
import soot.tagkit.Tag;

import java.util.Collections;
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
        return tags.get(tags.size() - 1).toString();
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
        // if (st.getClass().equals(TableSwitchStmt.class))
        // System.out.println("tableswitch: " + ((TableSwitchStmt)
        // st).getKey().toString());
        // System.out.println((b == true)? "trueBranch" : "False Branch "+ this.State);
        if (st instanceof IfStmt) {
            return ifCond(b, (IfStmt) st);
        }

        return this;
    }

    private LatticeElement conditionSatisfied(Value left, Value right) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        result.State.get(right.toString()).retainAll(result.State.get(left.toString()));
        result.State.get(left.toString()).retainAll(result.State.get(right.toString()));
        return (LatticeElement) result;
    }

    private LatticeElement conditionSatisfiedNonNull(Value value){
            PointerLatticeElement result = new PointerLatticeElement(this.State);
            if(this.State.get(value.toString()).contains("null")){
                  result.State.get("null").retainAll(Collections.singleton("null"));
            }
            return result;
    }
     private LatticeElement conditionNotSatisfiedNonNull(Value value){
            PointerLatticeElement result = new PointerLatticeElement(this.State);
            if(this.State.get(value.toString()).contains("null")){
                  result.State.get("null").removeAll((Collections.singleton("null"));
            }
            return result;
    }

    private LatticeElement ifCond(boolean condition, IfStmt st) {
        Value t = st.getCondition();
        Value left = t.getUseBoxes().get(0).getValue();
        Value right = t.getUseBoxes().get(1).getValue();
        System.out.println(right.getType());
        if (right.getType() instanceof RefType && left.getType() instanceof RefType) {
            if (t instanceof EqExpr) {
                if (condition == true) {
                    return conditionSatisfied(left, right);
                }
            } else if (t instanceof NeExpr) {
                if (condition == false) {
                    return conditionSatisfied(left, right);
                }
            }
        } else if (right.getType().equals(NullType.v()) && left.getType() instanceof RefType) {
            if (t instanceof EqExpr) {
                if (condition == true) {
                    return conditionSatisfiedNonNull(left);
                }
            } else if (t instanceof NeExpr) {
                if (condition == false) {
                    return conditionSatisfiedNonNull(left);
                }
            }
        } else if (left.getType().equals(NullType.v()) && right.getType() instanceof RefType) {
            if (t instanceof EqExpr) {
                if (condition == true) {
                    return conditionSatisfiedNonNull(right);
                }
            } else if (t instanceof NeExpr) {
                if (condition == false) {
                    return conditionSatisfiedNonNull(right);
                }
            }
        }
        return (LatticeElement) new PointerLatticeElement(this.State);
    }
}
