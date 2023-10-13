import java.util.HashSet;
import soot.jimple.Stmt;
import java.util.HashMap;

public class PointerLatticeElement implements LatticeElement {

    /// Some functions that we may use
    // construtor
    private HashMap<String, HashSet<String>> State;

    public PointerLatticeElement() {
        this.State = new HashMap<String, HashSet<String>>();
    }

    public PointerLatticeElement(String[] variables) {
        for (String variable : variables) {
            HashSet<String> g = new HashSet<String>();
            this.State.put(variable, g);
        }
    }

    public PointerLatticeElement(HashMap<String, HashSet<String>> state) {
        this.State = state;
    }

    public HashMap<String, HashSet<String>> getState() {
        return this.State;
    }

    public void setState(HashMap<String, HashSet<String>> state) {
        this.State = state;
    }

    public void printState() {
        System.out.print("{");
        for (String key : this.State.keySet()) {
            System.out.print("{" + key + ":{");
            for (String value : this.State.get(key)) {
                System.out.print(value + ",");
            }
            System.out.print("}},");
        }
        System.out.print("}");
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        HashMap<String, HashSet<String>> input = ((PointerLatticeElement) r).getState();

        HashMap<String, HashSet<String>> joinElementState = new HashMap<String, HashSet<String>>();

        for (String key : input.keySet()) {
            HashSet<String> value = new HashSet<String>();
            value.addAll(input.get(key));
            value.addAll(this.State.get(key));
            joinElementState.put(key, value);
        }
        PointerLatticeElement joinE = new PointerLatticeElement();
        joinE.setState(joinElementState);
        return (LatticeElement) joinE;

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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tf_assignstmt'");
    }

    @Override
    public LatticeElement tf_condstmt(boolean b, Stmt st) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tf_condstmt'");
    }

}
