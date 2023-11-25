import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import soot.tagkit.StringTag;
import soot.tagkit.Tag;

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
        System.out.println("-->" + currentState);
        System.out.println("-->" + incomingState);
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
        System.out.println("--->" + joinedState);
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
        boolean flag = true;
        for (Map.Entry<Stack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            flag = element.State.getOrDefault(entry.getKey(), new PointerLatticeElement()).equals(entry.getValue());
        }
        return flag;
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        if (st instanceof JReturnStmt || st instanceof JReturnVoidStmt) {
            return handleReturnFn(st);
        } else if (st.containsInvokeExpr() && st.getInvokeExpr() instanceof StaticInvokeExpr) {
            return handleCallTransferFn(st);

        } else {
            return handleNormalAssignFn(st);
        }
    }

    private LatticeElement handleReturnFn(Stmt st) {

        Map<Stack<String>, PointerLatticeElement> curState = this.getState();
        Map<Stack<String>, PointerLatticeElement> facts = new HashMap<>();
        for (Map.Entry<Stack<String>, PointerLatticeElement> entry : curState.entrySet()) {
            Stack<String> key = entry.getKey();
            key.pop();
            for (String s : getCallers(st, key.firstElement())) {
                Stack<String> newKey = new Stack<String>();
                newKey.add(s);
                newKey.addAll(key);
                facts.put(key, entry.getValue());
            }
        }
        return new ApproximateCallStringElement(facts);
    }

    private LatticeElement handleNormalAssignFn(Stmt st) {
        Map<Stack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<Stack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            Stack<String> stack = new Stack<>();
            stack.addAll(entry.getKey());
            temp.put(stack, (PointerLatticeElement) entry.getValue().tf_assignstmt(st));
        }
        return new ApproximateCallStringElement(temp);
    }

    private LatticeElement handleCallTransferFn(Stmt st) {
        StaticInvokeExpr stExpr = (StaticInvokeExpr) st.getInvokeExpr();
        Map<Stack<String>, PointerLatticeElement> curState = this.getState();
        int i = 0;
        for (Value arg : stExpr.getArgs()) {
            if (isReferenceType(arg.getType())) {
                for (Map.Entry<Stack<String>, PointerLatticeElement> entry : curState.entrySet()) {
                    System.out.println();
                    HashSet<String> temp = new HashSet<>();
                    PointerLatticeElement p = entry.getValue();
                    temp.addAll(p.getState().get(arg.toString()));
                    PointerLatticeElement exntedPointerLatticeElement = entry.getValue()
                            .addToState("@parameter" + i + ": " + arg.getType(), temp);

                    entry.getKey().push(getCallId(st));

                    entry.setValue(exntedPointerLatticeElement);

                }
            }
            i++;
        }
        return new ApproximateCallStringElement(curState);
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

    private String getCallId(Stmt st) {
        List<Tag> tags = st.getTags();
        for (Tag tag : tags) {
            if (tag instanceof StringTag) {
                return tag.toString();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return this.State.toString();
    }

    private List<String> getCallers(Stmt st, String value) {
        List<Tag> tags = st.getTags();
        List<String> ans = new ArrayList<>();
        for (Tag tag : tags) {
            if (tag instanceof HashMapTag) {
                HashMapTag tagged = (HashMapTag) tag;
                System.out.println("ALL CALLERS for " + value + "  " + tagged.getTag(value));
                return tagged.getTag(value);
            }
        }
        return ans;
    }

}
