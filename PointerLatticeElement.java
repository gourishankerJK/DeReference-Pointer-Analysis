

import java.util.HashMap;

import soot.jimple.Stmt;

public class PointerLatticeElement implements LatticeElement{
    HashMap<>
    @Override
    public LatticeElement join_op(LatticeElement r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'join_op'");
    }

    @Override
    public boolean equals(LatticeElement r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'equals'");
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
