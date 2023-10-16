import java.util.List;

public class Kildall {

    public static List<ProgramPoint> ComputeLFP(List<ProgramPoint> programPoints) {
        ProgramPoint analysisPoint;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {
            Propagate(analysisPoint);
        }
        return programPoints;
    }

    private static void Propagate(ProgramPoint analysisPoint) {
        int i = 0;
        if (!analysisPoint.markedForPropagation)
            return;
        // Unmark and propagate to the successors
        analysisPoint.markedForPropagation = false;
        for (ProgramPoint successor : analysisPoint.successors) {
            System.out.println("\t" + successor.statement.toString());
            LatticeElement joinElement;

            if (analysisPoint.statement.branches()) {
                joinElement = successor.latticeElement
                        .join_op(analysisPoint.latticeElement.tf_condstmt(i == 0, analysisPoint.statement));
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
            }
            i++;
        }
    }

    private static ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.markedForPropagation)
                return programPoint;
        }
        return null;
    }
}
