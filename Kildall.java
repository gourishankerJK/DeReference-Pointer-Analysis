import java.util.List;

public class Kildall {

    public static List<ProgramPoint> ComputeLFP(List<ProgramPoint> programPoints) {
        ProgramPoint analysisPoint;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {
            analysisPoint.Propagate();
        }
        return programPoints;
    }

    private static ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.markedForPropagation)
                return programPoint;
        }
        return null;
    }
}
