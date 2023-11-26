import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import soot.RefType;
import soot.Type;
import soot.Value;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import utils.CustomTag;
import utils.FixedSizeStack;

public class ApproximateCallStringElement implements LatticeElement, Cloneable {
    Map<FixedSizeStack<String>, PointerLatticeElement> State;

    public ApproximateCallStringElement(Map<FixedSizeStack<String>, PointerLatticeElement> originalState) {
        Map<FixedSizeStack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : originalState.entrySet()) {
            temp.put(entry.getKey().clone(), entry.getValue().clone());
        }
        this.State = temp;
    }

    public ApproximateCallStringElement(List<String> variables, boolean mainFunction) {
        Map<FixedSizeStack<String>, PointerLatticeElement> temp = new HashMap<>();
        FixedSizeStack<String> stack = new FixedSizeStack<>();
        if (mainFunction)
            stack.pushBack("@");
        temp.put(stack, new PointerLatticeElement(variables));
        this.State = temp;
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        ApproximateCallStringElement currentState = this.clone();
        ApproximateCallStringElement incomingState = ((ApproximateCallStringElement) r).clone();
        Map<FixedSizeStack<String>, PointerLatticeElement> joinedState = new HashMap<>();

        for (FixedSizeStack<String> key : currentState.State.keySet()) {
            if (key.size() == 0)
                continue;
            if (!incomingState.State.containsKey(key)) {
                joinedState.put(key, currentState.State.get(key));
            } else {
                PointerLatticeElement currentPointer = currentState.State.get(key);
                PointerLatticeElement incomingPointer = incomingState.State.get(key);
                PointerLatticeElement joinedPointer = (PointerLatticeElement) currentPointer.join_op(incomingPointer);
                joinedState.put(key, joinedPointer);
            }
        }

        for (FixedSizeStack<String> key : incomingState.State.keySet()) {
            if (key.size() == 0)
                continue;
            if (!currentState.State.containsKey(key)) {
                joinedState.put(key, incomingState.State.get(key));
            }
        }
        return new ApproximateCallStringElement(joinedState);
    }

    public Map<FixedSizeStack<String>, PointerLatticeElement> getState() {
        return this.clone().State;
    }

    @Override
    public boolean equals(LatticeElement r) {
        ApproximateCallStringElement element = (ApproximateCallStringElement) r;
        return this.State.equals(element.getState());
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        if (st.containsInvokeExpr() && st.getInvokeExpr() instanceof StaticInvokeExpr) {
            return handleCallTransferFn(st);
        } else {
            return handleNormalAssignFn(st);
        }
    }

    private LatticeElement handleReturnFn(Stmt st, String returnEdge) {
        Map<FixedSizeStack<String>, PointerLatticeElement> curState = this.clone().getState();
        Map<FixedSizeStack<String>, PointerLatticeElement> facts = new HashMap<>();

        for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            FixedSizeStack<String> callString = entry.getKey().clone();
            PointerLatticeElement value = entry.getValue().clone();
            /// if the call edge doesn't corresponds to return edge
            if (!returnEdge.equals(callString.popBack()) || (callString.size() == 0)) {
                curState.remove(callString);
            } else {
                List<String> callers = getCallers(st, callString.getfrontElement());
                // remove @parameter.*
                value = value.removeFromState();
                // caller is main itself ...
                if (callers.size() == 0) {
                    facts.put(callString, value);
                } else {
                    for (String s : callers) {
                        FixedSizeStack<String> newKey = callString.clone();
                        newKey.pushFront(s);
                        facts.put(callString, value);
                    }
                }
            }
        }
        return new ApproximateCallStringElement(facts);
    }

    private LatticeElement handleNormalAssignFn(Stmt st) {
        Map<FixedSizeStack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            FixedSizeStack<String> stack = entry.getKey().clone();
            temp.put(stack, (PointerLatticeElement) entry.getValue().tf_assignstmt(st));
        }
        return new ApproximateCallStringElement(temp);
    }

    private LatticeElement handleCallTransferFn(Stmt st) {
        StaticInvokeExpr stExpr = (StaticInvokeExpr) st.getInvokeExpr();
        Map<FixedSizeStack<String>, PointerLatticeElement> curState = this.clone().getState();
        int i = 0;
        for (Value arg : stExpr.getArgs()) {
            if (isReferenceType(arg.getType())) {
                for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : curState.entrySet()) {

                    HashSet<String> temp = new HashSet<>();
                    PointerLatticeElement p = entry.getValue();
                    temp.addAll(p.getState().get(arg.toString()));
                    PointerLatticeElement exntedPointerLatticeElement = entry.getValue()
                            .addToState("@parameter" + i + ": " + arg.getType(), temp);
                    entry.getKey().pushBack(getCallId(st));
                    entry.setValue(exntedPointerLatticeElement);

                }
            }
            i++;
        }
        // System.out.println(curState);

        return new ApproximateCallStringElement(curState);
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        Map<FixedSizeStack<String>, PointerLatticeElement> temp = new HashMap<>();
        for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
            FixedSizeStack<String> stack = entry.getKey().clone();
            temp.put(stack, (PointerLatticeElement) entry.getValue().tf_condstmt(b, st));
        }
        return new ApproximateCallStringElement(temp);
    }

    private boolean isReferenceType(Type type) {
        return type instanceof RefType;
    }

    @Override
    public String toString() {
        return this.State.toString();
    }

    @Override
    public ApproximateCallStringElement clone() {
        try {
            ApproximateCallStringElement clonedElement = (ApproximateCallStringElement) super.clone();
            clonedElement.State = new HashMap<>(this.State.size());
            for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : this.State.entrySet()) {
                FixedSizeStack<String> clonedKey = entry.getKey().clone();
                PointerLatticeElement clonedValue = entry.getValue().clone();
                clonedElement.State.put(clonedKey, clonedValue);
            }
            return clonedElement;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCallId(Stmt st) {
        CustomTag tag = (CustomTag) st.getTag("CallerIdTag");
        if (tag != null)
            return tag.getStringTag();
        return null;
    }

    private List<String> getCallers(Stmt st, String value) {
        CustomTag t = (CustomTag) st.getTag("CallersTag");
        return t.getHashMapTag(value);
    }

    @Override
    public LatticeElement tf_returnstmt(String Edge, Stmt st) {
        return handleReturnFn(st, Edge);
    }

}
