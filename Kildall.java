import java.util.ArrayList;
import java.util.List;


public class Kildall {
    static List<List<ProgramPoint>> logFactLists = new ArrayList<>();

    public static List<List<ProgramPoint>> ComputeLFP(List<ProgramPoint> programPoints) {
        ProgramPoint analysisPoint;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {
            Propagate(analysisPoint);
        }
        logFactLists.add(0 , programPoints);
        return logFactLists;
    }

    private static void Propagate(ProgramPoint analysisPoint) {
        int i = 0;
        if (!analysisPoint.markedForPropagation)
            return;
        // Unmark and propagate to the successors
        analysisPoint.markedForPropagation = false;
        List<ProgramPoint> logFact = new ArrayList<>();
        for (ProgramPoint successor : analysisPoint.successors) {
            LatticeElement joinElement;

            if (analysisPoint.statement.branches()) {
                joinElement = successor.latticeElement
                        .join_op(analysisPoint.latticeElement.tf_condstmt(i == 1, analysisPoint.statement));
            } else {
                joinElement = successor.latticeElement
                        .join_op(analysisPoint.latticeElement.tf_assignstmt(analysisPoint.statement));
            }
            // Unmark the successor nodes based on the previous value
            if (joinElement.equals(successor.latticeElement) && !successor.markedForPropagation) {
                successor.markedForPropagation = false;
            } else {
                successor.markedForPropagation = true;
                successor.latticeElement = joinElement;
                logFact.add(successor);
            }
            i++;
        }
        logFactLists.add(logFact);
    }

    private static ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.markedForPropagation)
                return programPoint;
        }
        return null;
    }
}
