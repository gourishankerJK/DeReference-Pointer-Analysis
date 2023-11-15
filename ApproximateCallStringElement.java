import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import soot.jimple.Stmt;

public class ApproximateCallStringElement implements LatticeElement {
    private Map<Stack<String>, PointerLatticeElement> state;

    public ApproximateCallStringElement() {
        this.state = new HashMap<>();
    }

    public ApproximateCallStringElement(Map<Stack<String>, PointerLatticeElement> state) {
        this.state = state;
    }
    @Override
    public LatticeElement join_op(LatticeElement r) {
        ApproximateCallStringElement rhs = (ApproximateCallStringElement) r;
        Map<Stack<String>, PointerLatticeElement> joinState = new HashMap<>();
        for(Stack<String> key: state.keySet()) {
            if (rhs.getState().containsKey(key)) {
                PointerLatticeElement join = (PointerLatticeElement)state.get(key).join_op(rhs.getState().get(key));
                joinState.put(key, join);
            } else {
                PointerLatticeElement join = new PointerLatticeElement(state.get(key).getState());
                joinState.put(key, join);
            }
        }
        for(Stack<String> key: rhs.getState().keySet()) {
            if (state.containsKey(key)) {
                PointerLatticeElement join = (PointerLatticeElement)state.get(key).join_op(rhs.getState().get(key));
                joinState.put(key, join);
            }else {
                PointerLatticeElement join = new PointerLatticeElement(rhs.getState().get(key).getState());
                joinState.put(key, join);
            }
        }
        return new ApproximateCallStringElement(joinState);
    }

    @Override
    public boolean equals(LatticeElement r) {
        return state.equals(((ApproximateCallStringElement) r).getState());
    }

    @Override
    public LatticeElement tf_assignstmt(Stmt st) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tf_assignstmt'");
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tf_condstmt'");
    }

    public Map<Stack<String>, PointerLatticeElement> getState() {
        return state;
    }
}
