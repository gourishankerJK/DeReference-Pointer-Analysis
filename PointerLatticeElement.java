import java.util.HashSet;

import soot.Local;
import soot.NullType;
import soot.RefType;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CmpExpr;
import soot.jimple.Constant;
import soot.jimple.EqExpr;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NeExpr;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
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

    // @Override
    // public LatticeElement tf_assignstmt(Stmt st) {
    // if (st instanceof AssignStmt) {
    // Value leftValue = ((AssignStmt) st).getLeftOp();
    // Value rightValue = ((AssignStmt) st).getRightOp();

    // System.out.println((leftValue instanceof InstanceFieldRef) == true);
    // if (rightValue instanceof Constant) {
    // // System.out.println("Constant Assignment");
    // } else if (leftValue instanceof Local && rightValue instanceof Local) {
    // // System.out.println("Local-to-Local Assignment");
    // } else if (leftValue instanceof InstanceFieldRef) {
    // InstanceFieldRef fieldRef = (InstanceFieldRef) leftValue;
    // String fieldName = fieldRef.getField().getName();
    // String className = fieldRef.getField().getDeclaringClass().getName();
    // String variableName = ((InstanceFieldRef) fieldRef).getBase().toString();
    // System.out.println(variableName + "." + className + "." + fieldName);
    // } else if (leftValue instanceof ArrayRef) {
    // System.out.println("Array Assignment");
    // } else {
    // System.out.println("Unknown Assignment");
    // }
    // } else {
    // System.out.println("Not an Assignment Statement");
    // }
    // return this;
    // }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        if (st instanceof IfStmt) {
            return handleIfCondition(b, (IfStmt) st);
        } else if (st instanceof LookupSwitchStmt) {
            System.out.println("LookupSwitch Statement");
        } else if (st instanceof TableSwitchStmt) {
            System.out.println("TableSwitch Statement");
        } else if (st instanceof IfStmt && st.getUnitBoxes().size() > 1) {
            System.out.println("If Statement with Goto");
        } // Not possible in our case ; If statement with NOP
        else if (st instanceof IfStmt && st.getUnitBoxes().size() == 1
                && st.getUnitBoxes().get(0).getUnit() instanceof NopStmt) {
            System.out.println("If Statement with Nop");
        } else if (st instanceof IfStmt && st.getUnitBoxes().size() == 1
                && st.getUnitBoxes().get(0).getUnit() instanceof ReturnStmt) {
            System.out.println("If Statement with Return");
        } else {
            System.out.println("Unknown Conditional Statement");
        }

        return this;
    }

    private LatticeElement handleConditionTrueNonNull(Value left, Value right) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        result.State.get(right.toString()).retainAll(result.State.get(left.toString()));
        result.State.get(left.toString()).retainAll(result.State.get(right.toString()));
        return (LatticeElement) result;
    }

    private LatticeElement handleConditionTrueOneNull(Value value) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        result.State.get(value.toString()).retainAll(Collections.singleton("null"));
        return (LatticeElement) result;
    }

    private LatticeElement handleConditionFalseOneNull(Value value) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        result.State.get(value.toString()).removeAll((Collections.singleton("null")));
        return (LatticeElement) result;
    }

    private boolean isReferenceType(Type type) {
        return type instanceof RefType;
    }

    private boolean isEqCond(Value value) {
        return value instanceof EqExpr;
    }

    private boolean isNEqCond(Value value) {
        return value instanceof NeExpr;
    }

    private boolean isNullType(Type type) {
        return type.equals(NullType.v());
    }

    private LatticeElement handleIfCondition(boolean condition, IfStmt st) {
        Value t = st.getCondition();
        Value left = t.getUseBoxes().get(0).getValue();
        Value right = t.getUseBoxes().get(1).getValue();

        // Both right and left are Reference Type
        if (isReferenceType(right.getType()) && isReferenceType(left.getType())) {
            return handleReferenceType(condition, t, left, right);
        }
        // Only Right is reference type
        else if (isNullType(right.getType()) && isReferenceType(left.getType())) {

            return handleNullType(condition, t, left);
        }
        // Only left is reference Type
        else if (isNullType(left.getType()) && isReferenceType(right.getType())) {
            return handleNullType(condition, t, right);
        }
        // Both of them are Null 
        else if (isNullType(left.getType()) && isNullType(right.getType())) {
            return handleBothNullType(t, condition);
        }

        return new PointerLatticeElement(this.State);
    }

    private LatticeElement handleReferenceType(boolean condition, Value t, Value left, Value right) {
        if ((isEqCond(t) && condition == true) || (isNEqCond(t) && condition == false)) {
            return handleConditionTrueNonNull(left, right);
        }
        return new PointerLatticeElement(this.State);
    }

    private LatticeElement handleNullType(boolean condition, Value t, Value value) {
        if ((isEqCond(t) && condition == true) || (isNEqCond(t) && condition == false)) {
            return handleConditionTrueOneNull(value);
        } else if ((isEqCond(t) && condition == false) || (isNEqCond(t) && condition == true)) {
            return handleConditionFalseOneNull(value);
        }
        return new PointerLatticeElement(this.State);
    }

    private LatticeElement handleBothNullType(Value t, boolean condition) {
        if ((isEqCond(t) && (condition == true)) || (isNEqCond(t) && (condition == false))) {
            return new PointerLatticeElement(this.State);
        }
        return new PointerLatticeElement();
    }
}
