import java.util.HashMap;
import java.util.HashSet;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.options.Options;

public class latticeTest {
    // HashMap<String, HashSet<String>> testState = new HashMap<String,
    // HashSet<String>>();
    // HashMap<String, HashSet<String>> testState2 = new HashMap<String,
    // HashSet<String>>();
    // HashSet<String> points = new HashSet<String>();
    // HashSet<String> points2 = new HashSet<String>();
    // points.add("y");
    // points.add("z");
    // points2.add("v");
    // points2.add("z");
    // testState.put("x" , points );
    // testState2.put("x" , points2 );
    // String[] str1 = new String[] {"x", "y", "z"};
    // PointerLatticeElement p = new PointerLatticeElement(str1);
    // p.printState();

    // PointerLatticeElement q = new PointerLatticeElement(testState2);
    // q.join_op((LatticeElement)p);
    // System.out.println(q.equals((LatticeElement)p));
    // System.out.println(p.equals((LatticeElement)p));
    // System.out.println(q.equals((LatticeElement)q));
    public static void main(String[] args) {

        Options.v().set_soot_classpath("/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/jre/lib/rt.jar:./target2-mine/");
        String className = "BasicTest";
        String methodName = "fun1";

        // Create a Soot class and load the necessary classes
        SootClass sootClass = Scene.v().loadClassAndSupport(className);
        sootClass.setApplicationClass();

        // Retrieve the method you want to analyze
        SootMethod sootMethod = sootClass.getMethodByName(methodName);

        HashMap<String, HashSet<String>> testState = new HashMap<String, HashSet<String>>();
        HashMap<String, HashSet<String>> testState2 = new HashMap<String, HashSet<String>>();
        HashSet<String> points = new HashSet<String>();
        HashSet<String> points2 = new HashSet<String>();
        points.add("temp$0");
        points.add("z");
        points2.add("v");
        points2.add("z");
        testState.put("x", points);
        testState2.put("", points2);
        PointerLatticeElement p = new PointerLatticeElement(testState2);
        // p.printState();
        // Create a JimpleBody for the method
        JimpleBody jimpleBody = (JimpleBody) sootMethod.retrieveActiveBody();
        for (Unit unit : jimpleBody.getUnits()) {
            // Process the unit
            System.out.println("Statement: " + unit);
        }
        int i = 0;
        for (Unit unit : jimpleBody.getUnits()) {
            // Check if the unit is an assignment statement

            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();

                // Check if the right-hand side is a new expression
                if (rightOp instanceof NewExpr) {
                    // Rename the right operand
                    String newVarName = "new_" + i;
                    assignStmt.setRightOp(Jimple.v().newLocal(newVarName, rightOp.getType()));
                }
            }
            i++;
        }
        System.out.println("--------");
        for (Unit unit : jimpleBody.getUnits()) {
            // Process the unit

            System.out.println("After Statement: " + unit);
        }

    }
}
