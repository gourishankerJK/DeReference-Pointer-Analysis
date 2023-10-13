
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Unit;
import soot.ValueBox;

public class Kildall {
    public static List<PointerLatticeElement> ComputeLFP(Body body) {
        List<PointerLatticeElement> result = new ArrayList<PointerLatticeElement>();

        body.getUnits().forEach((unit) -> {
            result.add(new PointerLatticeElement(GetVariables(body)));
        });

        result.forEach((r) -> {
            r.printState();
        });

        return result;
    }

    public static List<String> GetVariables(Body body) {
        List<String> result = new ArrayList<String>();
        for (Unit unit : body.getUnits()) {
            for (ValueBox vBox : unit.getDefBoxes()) {
                result.add(vBox.toString());
            }
        }

        return result;
    }
}
