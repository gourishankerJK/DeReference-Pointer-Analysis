import soot.Body;
import soot.Unit;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;

public class PreProcess {
    public static void TagLineNumberWithStatement(Body body) {
        int lineNumber = 0;
        for (Unit unit : body.getUnits()) {
            if (!(unit instanceof Stmt)) continue;
            LineNumberTag lineNumberTag = new LineNumberTag(lineNumber);
            unit.addTag(lineNumberTag);
            lineNumber++;
        }

    }

}
