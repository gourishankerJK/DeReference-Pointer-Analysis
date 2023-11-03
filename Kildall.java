import java.util.ArrayList;
import java.util.List;

public class Kildall {
    static List<List<ProgramPoint>> logFactLists = new ArrayList<>();

    public static List<List<ProgramPoint>> ComputeLFP(List<ProgramPoint> programPoints) {
        ProgramPoint analysisPoint;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {
            Propagate(analysisPoint);
        }
        logFactLists.add(0, programPoints);

        return logFactLists;
    }

    private static void Propagate(ProgramPoint analysisPoint) {
        int i = 0;
        if (!analysisPoint.isMarked())
            return;
        // Unmark and propagate to the successors
        analysisPoint.setMarkPoint(false);
        List<ProgramPoint> logFact = new ArrayList<>();
        for (ProgramPoint successor : analysisPoint.getSuccessors()) {
            LatticeElement joinElement;
            if (analysisPoint.getStmt().branches()) {
                joinElement = successor.getLatticeElement()
                        .join_op(analysisPoint.getLatticeElement().tf_condstmt(i == 1, analysisPoint.getStmt()));
            } else {
                joinElement = successor.getLatticeElement()
                        .join_op(analysisPoint.getLatticeElement().tf_assignstmt(analysisPoint.getStmt()));
            }
            // Unmark the successor nodes based on the previous value
            if (joinElement.equals(successor.getLatticeElement()) && !successor.isMarked()) {
                successor.setMarkPoint(false);
            } else {
                successor.setMarkPoint(true);
                successor.setLatticeElement(joinElement);
                logFact.add(successor);
            }
            i++;
        }
        logFactLists.add(logFact);
    }

    private static ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.isMarked())
                return programPoint;
        }
        return null;
    }
}
