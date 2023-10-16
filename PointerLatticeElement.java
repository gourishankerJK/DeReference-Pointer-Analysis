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
<<<<<<< HEAD
import soot.jimple.InstanceFieldRef;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NeExpr;
import soot.jimple.NopStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
=======
import soot.jimple.NullConstant;
>>>>>>> origin/master
import soot.jimple.Stmt;
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
<<<<<<< HEAD
        return tags.get(tags.size() - 1).toString();
=======
        return String.format("%02d", Integer.parseInt(tags.get(tags.size() - 1).toString()));
>>>>>>> origin/master
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
            return ifCond(b, (IfStmt) st);
        } else if (st instanceof LookupSwitchStmt) {
            System.out.println("LookupSwitch Statement");
        } else if (st instanceof TableSwitchStmt) {
            System.out.println("TableSwitch Statement");
        } else if (st instanceof IfStmt && st.getUnitBoxes().size() > 1) {
            System.out.println("If Statement with Goto");
        } else if (st instanceof IfStmt && st.getUnitBoxes().size() == 1
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

    private LatticeElement conditionSatisfied(Value left, Value right) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        result.State.get(right.toString()).retainAll(result.State.get(left.toString()));
        result.State.get(left.toString()).retainAll(result.State.get(right.toString()));
        return (LatticeElement) result;
    }

    private LatticeElement conditionSatisfiedNonNull(Value value) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        if (this.State.get(value.toString()).contains("null")) {
            result.State.get("null").retainAll(Collections.singleton("null"));
        }

        return result;
    }

    private LatticeElement conditionNotSatisfiedNonNull(Value value) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        if (this.State.get(value.toString()).contains("null")) {
            result.State.get("null").removeAll((Collections.singleton("null")));
        }
        return result;
    }

    private boolean isReferenceType(Type type) {
        return type instanceof RefType;
    }

    private boolean isNullType(Type type) {
        return type.equals(NullType.v());
    }

    private LatticeElement ifCond(boolean condition, IfStmt st) {
        Value t = st.getCondition();
        Value left = t.getUseBoxes().get(0).getValue();
        Value right = t.getUseBoxes().get(1).getValue();
        System.out.println("Type :" +left + (right.getType() instanceof InstanceFieldRef));
        if (isReferenceType(right.getType()) && isReferenceType(left.getType())) {
            if (t instanceof EqExpr) {
                if (condition == true) {
                    return conditionSatisfied(left, right);
                }
            } else if (t instanceof NeExpr) {
                if (condition == false) {
                    return conditionSatisfied(left, right);
                }
            }
        } else if (isNullType(right.getType()) && isReferenceType(left.getType())) {
            if (t instanceof EqExpr) {
                if (condition == true) {
                    return conditionSatisfiedNonNull(left);
                } else {
                    return conditionNotSatisfiedNonNull(left);
                }
            } else if (t instanceof NeExpr) {
                if (condition == false) {
                    return conditionSatisfiedNonNull(left);
                } else {
                    return conditionNotSatisfiedNonNull(left);
                }
            }
        } else if (isNullType(left.getType()) && isReferenceType(right.getType())) {
            if (t instanceof EqExpr) {
                if (condition == true) {
                    return conditionSatisfiedNonNull(right);
                } else {
                    return conditionNotSatisfiedNonNull(left);
                }
            } else if (t instanceof NeExpr) {
                if (condition == false) {
                    return conditionSatisfiedNonNull(right);
                } else {
                    return conditionNotSatisfiedNonNull(right);
                }
            }
        }
        return (LatticeElement) new PointerLatticeElement(this.State);
    }
}
