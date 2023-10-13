
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.effect.Light.Point;
import soot.jimple.Stmt;

public class PointerLatticeElement implements LatticeElement {

    /// Some functions that we may use
    public static <T> Set<T> set_merge(Set<T> set_1, Set<T> set_2) {
        Set<T> my_set = set_1.stream().collect(Collectors.toSet());
        my_set.addAll(set_2);
        return my_set;
    }

    private HashMap<String, Set<String>> State;

    public HashMap<String, Set<String>> getState() {
        return this.State;
    }

    public void setState(HashMap<String, Set<String>> state) {
        this.State = state;
    }

    public void printState() {
        System.out.print("{");
        for (String key : this.State.keySet()) {
            System.out.print("{" + key + ":{");
            for (String value : this.State.get(key)) {
                System.out.print(value + ",");
            }
            System.out.print("}} , \n");
        }
        System.out.print("}");
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        HashMap<String, Set<String>> input = ((PointerLatticeElement) r).getState();

        HashMap<String, Set<String>> joinElementState = new HashMap<String, Set<String>>();

        for (String key : input.keySet()) {
            joinElementState.put(key, set_merge(this.State.get(key), input.get(key)));
        }
        PointerLatticeElement joinE = new PointerLatticeElement();
        joinE.setState(joinElementState);

        return (LatticeElement) joinE;

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
