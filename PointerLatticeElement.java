import java.util.HashSet;
import soot.NullType;
import soot.RefType;
import soot.Type;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.NeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.tagkit.Tag;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PointerLatticeElement implements LatticeElement {
    private Map<String, HashSet<String>> State;

    public PointerLatticeElement() {
        this.State = new TreeMap<String, HashSet<String>>();
    }

    public PointerLatticeElement(List<String> variables) {
        this.State = new TreeMap<>();
        for (String variable : variables) {
            this.State.put(variable, new HashSet<>());
        }
    }

    public PointerLatticeElement(Map<String, HashSet<String>> state) {
        TreeMap<String, HashSet<String>> newState = new TreeMap<String, HashSet<String>>();
        for (String key : state.keySet()) {
            HashSet<String> value = new HashSet<String>();
            value.addAll(state.get(key));
            newState.put(key, value);
        }
        this.State = newState;
    }

    public Map<String, HashSet<String>> getState() {
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
        Map<String, HashSet<String>> input = ((PointerLatticeElement) r).getState();

        TreeMap<String, HashSet<String>> joinElementState = new TreeMap<String, HashSet<String>>();

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
        Map<String, HashSet<String>> input = ((PointerLatticeElement) r).getState();
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
        PointerLatticeElement result = new PointerLatticeElement(State);
        Value lhs = st.getLeftOp();
        Value rhs = st.getRightOp();
        System.out.println("Operands " + lhs + " " + rhs.getClass());

        if (!(lhs.getType() instanceof soot.RefType)) {
            return result;
        }

        // x = new ()
        if (lhs instanceof JimpleLocal && (rhs instanceof JNewExpr || (rhs instanceof JStaticInvokeExpr)
                || (rhs instanceof JVirtualInvokeExpr))) {
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
                HashSet<String> updatedMap = new HashSet<>(result.State.get(rhs.toString()));
                if (!result.State.containsKey(key)) {
                    result.State.put(key, updatedMap);
                }
                result.State.get(key).addAll(updatedMap);
            }
        }

        return result;
    }

    /**
     * Evaluates a conditional statement in the program.
     *
     * @param b  The boolean condition of the statement
     * @param st The statement to be evaluated
     * @return The lattice element representing the result of the evaluation
     */
    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        if (st instanceof IfStmt) {
            return handleIfCondition(b, (IfStmt) st);
        }

        return new PointerLatticeElement(this.State);
    }

    private PointerLatticeElement clearState(PointerLatticeElement result) {
        for (String key : this.State.keySet()) {
            result.State.get(key).clear();
        }
        return result;
    }

    /**
     * Checks if the given type is a reference type.
     *
     * @param type The type to check
     * @return True if the type is a reference type, false otherwise
     */
    private boolean isReferenceType(Type type) {
        return type instanceof RefType;
    }

    /**
     * Checks if the given value is an instance of the EqExpr class.
     *
     * @param value The value to check.
     * @return True if the value is an instance of EqExpr, false otherwise.
     */
    private boolean isEqCond(Value value) {
        return value instanceof EqExpr;
    }

    /**
     * Checks if the given value is an instance of the NeExpr class.
     *
     * @param value The value to check.
     * @return True if the value is an instance of NeExpr, false otherwise.
     */
    private boolean isNEqCond(Value value) {
        return value instanceof NeExpr;
    }

    /**
     * Checks if the given Type object is of NullType.
     *
     * @param type The Type object to check.
     * @return true if the Type object is of NullType, false otherwise.
     */
    private boolean isNullType(Type type) {
        return type.equals(NullType.v());
    }

    /**
     * Handles the true branch when the value is null.
     *
     * @param value The value to handle
     * @return The resulting lattice element after handling the true branch
     */
    private LatticeElement handleTrueBranchWithNull(Value value) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        // send empty if doesn'operation contain null;
        if (!this.State.get(value.toString()).contains("null")) {
            for (String key : this.State.keySet()) {
                result.State.get(key).clear();
            }
        }
        // send only null;
        else {
            result.State.get(value.toString()).retainAll(Collections.singleton("null"));
        }
        return (LatticeElement) result;
    }

    /**
     * Handles the false branch when the value is null.
     *
     * @param value The value to check for null.
     * @return The updated lattice element after handling the false branch.
     */
    private LatticeElement handleFalseBranchWithNull(Value value) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        // send empty if contains null;
        if (this.State.get(value.toString()).contains("null")) {
            // if it contains only NUll , send \bot
            if (this.State.get(value.toString()).size() == 1) {
                result = clearState(result);
                // else filter out null
            } else {
                result.State.get(value.toString()).removeAll(Collections.singleton("null"));
            }
        }
        return (LatticeElement) result;
    }

    /**
     * Handles the case when the type of a value is reference type, based on the
     * given condition, operation, and value.
     *
     * @param condition The condition of the if statement
     * @param operation The operation being performed
     * @param left      The left value in the operation
     * @param right     The right value in the operation
     * @return The updated lattice element
     */
    private LatticeElement handleIfReferenceType(boolean condition, Value operation, Value left, Value right) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        if ((isEqCond(operation) && condition == true) || (isNEqCond(operation) && condition == false)) {
            result.State.get(right.toString()).retainAll(result.State.get(left.toString()));
            result.State.get(left.toString()).retainAll(result.State.get(right.toString()));
            if (result.State.get(right.toString()).size() == 0) {
                result = clearState(result);
            }
            return (LatticeElement) result;
        }
        return result;
    }

    /**
     * Handles the case when the type of a value is null, based on the given
     * condition, operation, and value.
     *
     * @param condition The boolean condition to evaluate
     * @param operation The operation being performed
     * @param value     The value to handle
     * @return The lattice element representing the result of the operation
     */
    private LatticeElement handleIfNullType(boolean condition, Value operation, Value value) {
        if (isEqCond(operation)) {
            if (condition == true) {
                return handleTrueBranchWithNull(value);
            } else {
                return handleFalseBranchWithNull(value);
            }
        } else if (isNEqCond(operation)) {
            if (condition == true) {
                return handleFalseBranchWithNull(value);
            } else {
                return handleTrueBranchWithNull(value);
            }
        }
        return new PointerLatticeElement(this.State);
    }

    /**
     * Handles the if condition statement and returns the corresponding lattice
     * element based on the condition and types of the operands.
     *
     * @param condition The boolean condition of the if statement
     * @param st        The IfStmt object representing the if statement
     * @return The lattice element representing the result of the if condition
     *         evaluation
     */
    private LatticeElement handleIfCondition(boolean condition, IfStmt st) {
        Value operation = st.getCondition();
        Value left = operation.getUseBoxes().get(0).getValue();
        Value right = operation.getUseBoxes().get(1).getValue();

        // Both right and left are Reference Type
        if (isReferenceType(right.getType()) && isReferenceType(left.getType())) {
            return handleIfReferenceType(condition, operation, left, right);
        }
        // Only Right is reference type
        else if (isNullType(right.getType()) && isReferenceType(left.getType())) {

            return handleIfNullType(condition, operation, left);
        }
        // Only left is reference Type
        else if (isNullType(left.getType()) && isReferenceType(right.getType())) {
            return handleIfNullType(condition, operation, right);
        }

        return new PointerLatticeElement(this.State);
    }

}
