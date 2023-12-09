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
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import utils.CustomTag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PointerLatticeElement implements LatticeElement, Cloneable {
    private Map<String, HashSet<String>> State;

    public PointerLatticeElement() {
        this.State = new HashMap<String, HashSet<String>>();
    }

    public PointerLatticeElement(List<String> variables) {
        this.State = new HashMap<>();
        for (String variable : variables) {
            this.State.put(variable, new HashSet<>());
        }
    }

    public PointerLatticeElement(Map<String, HashSet<String>> state) {
        HashMap<String, HashSet<String>> newState = new HashMap<String, HashSet<String>>();
        for (String key : state.keySet()) {
            HashSet<String> value = new HashSet<String>();
            if (state.get(key) != null)
                value.addAll(new HashSet<>(state.get(key)));
            newState.put(key, value);
        }
        this.State = newState;
    }

    public Map<String, HashSet<String>> getState() {
        return new PointerLatticeElement(this.State).State;
    }

    @Override
    public PointerLatticeElement clone() {
        try {
            PointerLatticeElement clonedElement = (PointerLatticeElement) super.clone();
            clonedElement.State = new HashMap<>(this.State.size());
            for (Map.Entry<String, HashSet<String>> entry : this.State.entrySet()) {
                String clonedKey = entry.getKey();
                HashSet<String> clonedValue;
                if (entry.getValue() != null)
                    clonedValue = new HashSet<>(entry.getValue());
                else
                    clonedValue = new HashSet<>();
                clonedElement.State.put(clonedKey, clonedValue);
            }
            return clonedElement;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        if (this.State == null)
            return "";
        String ans = "{";
        int size = this.State.keySet().size();

        for (String key : this.State.keySet()) {
            if (this.State.get(key) != null) {

                if (this.State.get(key).size() != 0) {
                    ans += key + " -> ";

                    ans += this.State.get(key).toString() + ((size == 1) ? "" : ", ");
                    ans += System.lineSeparator();
                }
                size--;
            }
        }

        return ans + "}";
    }

    public boolean isEmpty() {
        for (Map.Entry<String, HashSet<String>> entry : this.State.entrySet()) {
            if (entry.getKey().length() != 0 && entry.getValue().size() != 0) {
                return false;
            }
        }
        return true;
    }

    public static String getAllocationSiteSymbol(Stmt st) {
        CustomTag lineNo = (CustomTag) st.getTag("lineNumberTag");
        CustomTag functionName = (CustomTag) st.getTag("functionName");
        return String.format("%s.new%02d", functionName.getStringTag(),
                lineNo.getLineNumber());
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        Map<String, HashSet<String>> input = ((PointerLatticeElement) r).getState();

        HashMap<String, HashSet<String>> joinElementState = new HashMap<String, HashSet<String>>();

        for (String key : input.keySet()) {
            HashSet<String> value = new HashSet<String>();
            value.addAll(input.get(key));
            value.addAll(this.State.getOrDefault(key, new HashSet<String>()));
            joinElementState.put(key, value);
        }
        for (String key : this.State.keySet()) {
            HashSet<String> value = new HashSet<String>();
            value.addAll(this.State.get(key));
            value.addAll(input.getOrDefault(key, new HashSet<String>()));
            value.addAll(joinElementState.getOrDefault(key, new HashSet<String>()));
            joinElementState.put(key, value);

        }

        PointerLatticeElement joinElement = new PointerLatticeElement(joinElementState);

        return (LatticeElement) joinElement;

    }

    @Override
    public boolean equals(LatticeElement r) {
        return this.State.equals(((PointerLatticeElement) r).State);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PointerLatticeElement other = (PointerLatticeElement) obj;

        return this.State.equals(other.State);
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        if (st instanceof JIdentityStmt) {

            JIdentityStmt Jst = (JIdentityStmt) st;
            Value right = Jst.getRightOp();
            Value left = Jst.getLeftOp();
            Map<String, HashSet<String>> result = this.clone().getState();
            result.put(left.toString(), this.State.getOrDefault(right.toString(), new HashSet<>()));
            return new PointerLatticeElement(result);

        } else if (st instanceof JAssignStmt) {
            return tfAssignmentStmt((AssignStmt) st);
        }

        return this;
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

    private LatticeElement tfAssignmentStmt(AssignStmt st) {
        PointerLatticeElement result = new PointerLatticeElement(State);
        Value lhs = st.getLeftOp();
        Value rhs = st.getRightOp();

        if (!(lhs.getType() instanceof soot.RefType)) {
            return result;
        }

        Class<?>[] nonFieldClasses = { JNewExpr.class, JStaticInvokeExpr.class, JVirtualInvokeExpr.class,
                NullConstant.class, JimpleLocal.class };

        // x = new (), x = something(), x = null, x = y
        if (lhs instanceof JimpleLocal && isInstanceOfMultiple(rhs, nonFieldClasses)) {
            return handleNonFieldAssignmentForLocal(st, result, lhs, rhs);
        }

        // x = y.f
        if (lhs instanceof JimpleLocal && rhs instanceof JInstanceFieldRef) {
            return handleFieldAssignmentForLocal(result, lhs, rhs);
        }

        // x.f = null, x.f = new, x.f = something(), x.f = y
        if (lhs instanceof JInstanceFieldRef && isInstanceOfMultiple(rhs, nonFieldClasses)) {
            return handleNonFieldAssignmentForField(st, result, lhs, rhs);
        }

        return result;
    }

    /**
     * Handles the transfer function assignment statements of the form x = y, x =
     * null, x = new(), x = something()
     * 
     * @param result LatticeElement on which the transfer function is to be applied
     * @param lhs    Left hand side of the assignment
     * @param rhs    Right hand side of the assignment
     * @return The modified result object after applying transfer function
     */
    private LatticeElement handleFieldAssignmentForLocal(PointerLatticeElement result, Value lhs, Value rhs) {
        HashSet<String> res = new HashSet<>();
        JInstanceFieldRef r = (JInstanceFieldRef) rhs;

        for (String pseudoVar : result.State.get(r.getBase().toString())) {
            String key = getSymbolicFieldKey(pseudoVar, r);
            if (pseudoVar == "null" || !result.State.containsKey(key)) {
                continue;
            }
            res.addAll(result.State.get(key));
        }
        // strong update for x = y.f
        result.State.put(lhs.toString(), res);
        return result;
    }

    /**
     * Handles the transfer function assignment statements of the form x.f = y, x.f
     * = null, x.f = new(), x.f = something()
     * 
     * @param st     Jimple assignment statement
     * @param result LatticeElement on which the transfer function is to be applied
     * @param lhs    Left hand side of the assignment
     * @param rhs    Right hand side of the assignment
     * @return The modified result object after applying transfer function
     */
    private LatticeElement handleNonFieldAssignmentForField(AssignStmt st, PointerLatticeElement result, Value lhs,
            Value rhs) {
        JInstanceFieldRef l = (JInstanceFieldRef) lhs;

        for (String pseudoVar : result.State.get(l.getBase().toString())) {
            if (pseudoVar == "null") {
                continue;
            }
            String key = getSymbolicFieldKey(pseudoVar, l);
            String symbolicConstant = rhs instanceof NullConstant ? "null" : getAllocationSiteSymbol(st);
            HashSet<String> updatedMap = rhs instanceof JimpleLocal
                    ? new HashSet<>(result.State.get(rhs.toString()))
                    : new HashSet<>(Arrays.asList(symbolicConstant));
            if (!result.State.containsKey(key)) {
                result.State.put(key, new HashSet<>());
            }
            // weak update for all x.f assignments
            result.State.get(key).addAll(updatedMap);
        }
        return result;
    }

    /**
     * Handles the transfer function assignment statements of the form x = y = null,
     * x=y, x=new(), x=something
     * 
     * @param st     Jimple assignment statement
     * @param result LatticeElement on which the transfer function is to be applied
     * @param lhs    Left hand side of the assignment
     * @param rhs    Right hand side of the assignment
     * @return The modified result object after applying transfer function
     */
    private LatticeElement handleNonFieldAssignmentForLocal(AssignStmt st, PointerLatticeElement result, Value lhs,
            Value rhs) {
        String symbolicConstant = rhs instanceof NullConstant ? "null" : getAllocationSiteSymbol(st);
        HashSet<String> updatedMap = rhs instanceof JimpleLocal ? new HashSet<>(result.State.get(rhs.toString()))
                : new HashSet<>(Arrays.asList(symbolicConstant));
        // strong update for x = null, x = y, x = new(), x = something()
        result.State.put(lhs.toString(), updatedMap);

        return result;
    }

    /**
     * Utility to get the key for a symbolic field for example "new01.f"
     * 
     * @param pseudoVar The pseudoVariable for example "new01", this is an element
     *                  in range of LatticeElement function (Var U (pseudoVar X
     *                  fields) -> pseudoVar)
     * @param operand   Field of the class that is being accessed, example in
     *                  new01.f, "f".
     * @return concatenated string in (pseudoVar X fields)
     */
    private String getSymbolicFieldKey(String pseudoVar, JInstanceFieldRef operand) {
        return pseudoVar + "." + operand.getField().getName();
    }

    /**
     * Utility to clear the state (Used in making the state to bot)
     * 
     * @param result PointerLatticeElement whose state needs to be cleared
     * @return Cleared PointerLatticeElement
     */
    public PointerLatticeElement updateState(String value, HashSet<String> parameter) {
        Map<String, HashSet<String>> st = getState();
        st.put(value, parameter);
        return new PointerLatticeElement(st);
    }

    public PointerLatticeElement removeFromState() {
        Map<String, HashSet<String>> st = this.clone().getState();
        for (Map.Entry<String, HashSet<String>> entry : this.State.entrySet()) {
            if (entry.getKey().matches("@parameter.*")) {
                st.remove(entry.getKey());
            }
        }
        return new PointerLatticeElement(st);
    }

    public PointerLatticeElement removeFromState(String value) {
        Map<String, HashSet<String>> st = this.clone().getState();
        for (Map.Entry<String, HashSet<String>> entry : this.State.entrySet()) {
            if (entry.getKey().matches(value)) {
                st.remove(entry.getKey());
            }
        }

        return new PointerLatticeElement(st);
    }
    public PointerLatticeElement removeUnwantedReturnVariables(String returnStmtFname, String varToBeMapped) {
        Map<String, HashSet<String>> st = this.clone().getState();
        for (Map.Entry<String, HashSet<String>> entry : this.State.entrySet()) {
            if ((entry.getKey().contains(returnStmtFname) && entry.getKey().contains("::") ) && entry.getKey()!=varToBeMapped) {
                st.put(entry.getKey(), new HashSet<>());
            }
        }

        return new PointerLatticeElement(st);
    }


    private PointerLatticeElement clearState(PointerLatticeElement result) {
        for (String key : this.State.keySet()) {
            result.State.get(key).clear();
        }
        return result;
    }

    public PointerLatticeElement clearState(String var) {
        if (this.State.containsKey(var))
            this.State.get(var).clear();
        return this;
    }

    /**
     * Checks if given object is of any of the class mentioned in classes at runtime
     * 
     * @param obj     The object to be checked
     * @param classes The classes to be checked against
     * @return If obj is of any of the type mentioned in classes, return true else
     *         return false
     */
    private static boolean isInstanceOfMultiple(Object obj, Class<?>... classes) {
        for (Class<?> clas : classes) {
            if (clas.isInstance(obj)) {
                return true;
            }
        }
        return false;
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
        // if set is empty also then we dont know what can be the result.
        if (!this.State.get(value.toString()).contains("null") && !this.State.get(value.toString()).isEmpty()) {
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
                return null;
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
                return null;
            }
        } else if (result.State.get(right.toString()).size() == 1 && result.State.get(left.toString()).size() == 1) {
            if (result.State.get(right.toString()).contains("null")
                    && result.State.get(left.toString()).contains("null")) {
                return null;
            }
        }
        return (LatticeElement) result;
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
        } else if (isNullType(left.getType()) && isNullType(right.getType())) {
            return handleBothNUll(condition, operation);
        }

        return new PointerLatticeElement(this.State);
    }

    private LatticeElement handleBothNUll(boolean condition, Value operation) {
        PointerLatticeElement result = new PointerLatticeElement(this.State);
        if ((condition == true && isNEqCond(operation)) || (condition == false && isEqCond(operation))) {
            return null;
        }
        return result;
    }
}
