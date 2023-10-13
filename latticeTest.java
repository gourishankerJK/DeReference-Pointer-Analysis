import java.util.HashMap;
import java.util.HashSet;

public class latticeTest {
    public static void main(String[] args)
    {
        HashMap<String, HashSet<String>> testState = new HashMap<String, HashSet<String>>();
        HashSet<String> points = new HashSet<String>();
        points.add("y");
        points.add("z");
        testState.put("x" , points );
        String[] str1 =new String[] {"x", "y", "z"};
        PointerLatticeElement p = new PointerLatticeElement(testState);
        p.printState();

       // PointerLatticeElement q = new PointerLatticeElement(str1);
       
    }
}
