import java.io.IOException;
import java.util.List;

import utils.CustomTag;

public class Kildall {

    public static void ComputeLFP(List<ProgramPoint> programPoints) throws IOException {
        ProgramPoint analysisPoint;
        Integer i = 0;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {
            Propagate(analysisPoint);
            Analysis.formatAndWriteToFile(programPoints, GetMarkedProgramPoint(programPoints) == null, i++);
        }
    }

    private static void Propagate(ProgramPoint analysisPoint) {
        int i = 0;
        if (!analysisPoint.isMarked())
            return;
        // Unmark and propagate to the successors
        analysisPoint.setMarkPoint(false);
        // Need to make normal successor as identity 
        // This is because now we only pass parameters to the function and not the whole state.
        if (analysisPoint.callSuccessor != null) {
            LatticeElement join = analysisPoint.getSuccessors().get(0)
                                    .getLatticeElement().join_op(analysisPoint.getLatticeElement());
            analysisPoint.getSuccessors().get(0).setLatticeElement(join);
        }

        for (ProgramPoint successor : analysisPoint.getAllSuccessors()) {
            LatticeElement joinElement;

            if (analysisPoint.getStmt().branches()) {
                joinElement = successor.getLatticeElement()
                        .join_op(analysisPoint.getLatticeElement().tf_condstmt(i == 1, analysisPoint.getStmt()));
            } else {
                if (analysisPoint.returnEdgeIds.size() != 0) {
                    analysisPoint.getStmt().addTag(new CustomTag("returnEdgeId", analysisPoint.returnEdgeIds.get(i)));
                }
                joinElement = successor.getLatticeElement()
                        .join_op(analysisPoint.getLatticeElement().tf_assignstmt(analysisPoint.getStmt()));
                analysisPoint.getStmt().removeTag("returnEdgeId");
            }
            // Unmark the successor nodes based on the previous value

            if (joinElement.equals(successor.getLatticeElement()) && !successor.isMarked()) {

                successor.setMarkPoint(false);
            } else {
                successor.setMarkPoint(true);
                successor.setLatticeElement(joinElement);
            }
            i++;

        }

    }
    private static ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.isMarked())
                return programPoint;

        }
        return null;
    }
}
