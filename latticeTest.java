import java.util.HashMap;
import java.util.HashSet;

public class latticeTest {
    public static void main(String[] args)
    {
        HashMap<String, HashSet<String>> testState = new HashMap<String, HashSet<String>>();
         HashMap<String, HashSet<String>> testState2 = new HashMap<String, HashSet<String>>();
        HashSet<String> points = new HashSet<String>();
        HashSet<String> points2 = new HashSet<String>();
        points.add("y");
        points.add("z");
        points2.add("v");
        points2.add("z");
        testState.put("x" , points );
        testState2.put("x" , points2 );
        String[] str1 = new String[] {"x", "y", "z"};
        PointerLatticeElement p = new PointerLatticeElement(str1);
       // p.printState();

    //    PointerLatticeElement q = new PointerLatticeElement(testState2);
    //    q.join_op((LatticeElement)p);
    //    System.out.println(q.equals((LatticeElement)p));
    //    System.out.println(p.equals((LatticeElement)p));
    //    System.out.println(q.equals((LatticeElement)q));
       
    }
}
