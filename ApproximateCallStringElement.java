import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import soot.RefType;
import soot.Type;
import soot.Value;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;

public class ApproximateCallStringElement implements LatticeElement {
    Map<Stack<String>, PointerLatticeElement> State;

    public ApproximateCallStringElement(Map<Stack<String>, PointerLatticeElement> originalState) {
        Map<Stack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<Stack<String>, PointerLatticeElement> entry : originalState.entrySet()) {
            Stack<String> stack = new Stack<>();
            stack.addAll(entry.getKey());
            temp.put(stack, new PointerLatticeElement(entry.getValue().getState()));
        }
        this.State = temp;
    }

    public ApproximateCallStringElement(List<String> variables) {
        Map<Stack<String>, PointerLatticeElement> temp = new HashMap<>();
        Stack<String> stack = new Stack<>();
        stack.add("@");
        temp.put(stack, new PointerLatticeElement(variables));
        this.State = temp;
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        ApproximateCallStringElement element = (ApproximateCallStringElement) r;
        ApproximateCallStringElement currentState = new ApproximateCallStringElement(this.State);
        ApproximateCallStringElement incomingState = new ApproximateCallStringElement(element.State);

        Map<Stack<String>, PointerLatticeElement> joinedState = new HashMap<>();

        for (Stack<String> key : currentState.State.keySet()) {
            if (!incomingState.State.containsKey(key)) {
                joinedState.put(key, currentState.State.get(key));
            } else {
                PointerLatticeElement currentPointer = currentState.State.get(key);
                PointerLatticeElement incomingPointer = incomingState.State.get(key);
                PointerLatticeElement joinedPointer = (PointerLatticeElement) currentPointer.join_op(incomingPointer);
                joinedState.put(key, joinedPointer);
            }
        }

        for (Stack<String> key : incomingState.State.keySet()) {
            if (!currentState.State.containsKey(key)) {
                joinedState.put(key, incomingState.State.get(key));
            }
        }

        return new ApproximateCallStringElement(joinedState);
    }

    public Map<Stack<String>, PointerLatticeElement> getState() {
        Map<Stack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<Stack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            Stack<String> stack = new Stack<>();
            stack.addAll(entry.getKey());
            temp.put(stack, new PointerLatticeElement(entry.getValue().getState()));
        }
        return temp;
    }

    @Override
    public boolean equals(LatticeElement r) {
        ApproximateCallStringElement element = (ApproximateCallStringElement) r;
        return this.State.equals(element.State);
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {

        Map<Stack<String>, PointerLatticeElement> curState = this.getState();
        if (st instanceof JReturnStmt || st instanceof JReturnVoidStmt) {
            System.out.println("return Stmt");

            return this;
        } else if (st.containsInvokeExpr() && st.getInvokeExpr() instanceof StaticInvokeExpr) {
            StaticInvokeExpr stExpr = (StaticInvokeExpr) st.getInvokeExpr();
            for (Value arg : stExpr.getArgs()) {
                if (!isReferenceType(arg.getType())) {
                    for (Map.Entry<Stack<String>, PointerLatticeElement> entry : curState.entrySet()) {
                        entry.getKey().add("123");
                        entry.getValue().clearState(arg.toString());
                    }
                }
            }
            
            return new ApproximateCallStringElement(curState);

        } else {
            Map<Stack<String>, PointerLatticeElement> temp = new HashMap<>();
            for (Map.Entry<Stack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
                Stack<String> stack = new Stack<>();
                stack.addAll(entry.getKey());
                temp.put(stack, (PointerLatticeElement) entry.getValue().tf_assignstmt(st));
            }
            return new ApproximateCallStringElement(temp);
        }
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        Map<Stack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<Stack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            Stack<String> stack = new Stack<>();
            stack.addAll(entry.getKey());
            temp.put(stack, (PointerLatticeElement) entry.getValue().tf_condstmt(b, st));
        }
        return new ApproximateCallStringElement(temp);
    }

    private boolean isReferenceType(Type type) {
        return type instanceof RefType;
    }

}
