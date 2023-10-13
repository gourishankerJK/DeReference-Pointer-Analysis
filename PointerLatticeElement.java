
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import soot.jimple.Stmt;

public class PointerLatticeElement implements LatticeElement {

    private HashMap<String, Set<String>> State;

    // Utility function to merge 2 sets
    private static <T> Set<T> set_merge(Set<T> set_1, Set<T> set_2) {
        Set<T> my_set = set_1.stream().collect(Collectors.toSet());
        my_set.addAll(set_2);
        return my_set;
    }

    public PointerLatticeElement(List<String> variables) {
        this.State = new HashMap<>();
        for (String val : variables) {
            this.State.put(val, new HashSet<>());
        }
    }

    public PointerLatticeElement(HashMap<String, Set<String>> state) {
        this.State = state;
    }

    public PointerLatticeElement() {
    }

    public HashMap<String, Set<String>> getState() {
        return this.State;
    }

    public void printState() {
        for (String key : this.State.keySet()) {
            System.out.print(key + " :");
            System.out.println(this.State.get(key).toString());
        }
        System.out.println();

    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        HashMap<String, Set<String>> input = ((PointerLatticeElement) r).getState(),
                joinElementState = new HashMap<String, Set<String>>();

        for (String key : input.keySet()) {
            joinElementState.put(key, set_merge(this.State.get(key), input.get(key)));
        }
        PointerLatticeElement joinElement = new PointerLatticeElement(joinElementState);

        return (LatticeElement) joinElement;

    }

    @Override
    public boolean equals(LatticeElement r) {
        HashMap<String, Set<String>> input = ((PointerLatticeElement) r).getState();
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
