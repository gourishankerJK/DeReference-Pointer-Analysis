
import java.util.ArrayList;
import java.util.List;

import soot.Body;
import soot.Unit;
import soot.ValueBox;

public class Kidall {
    public static List<PointerLatticeElement> ComputeLFP(Body body) {
        List<PointerLatticeElement> result = new ArrayList<PointerLatticeElement>();

        System.out.println(GetVariables(body).toString());
        result.add(new PointerLatticeElement(GetVariables(body)));

        return result;
    }

    public static List<String> GetVariables(Body body) {
        ArrayList<String> result = new ArrayList<String>();
        for (Unit unit : body.getUnits()) {
            for (ValueBox vBox : unit.getDefBoxes()) {
                result.add(vBox.toString());
            }
        }

        return result;
    }
}
