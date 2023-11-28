import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.CustomTag;

public class Kildall {
    static List<List<ProgramPoint>> logFactLists = new ArrayList<>();

    public static List<List<ProgramPoint>> ComputeLFP(List<ProgramPoint> programPoints) throws IOException {
        ProgramPoint analysisPoint;
        Integer i = 0;
        while ((analysisPoint = GetMarkedProgramPoint(programPoints)) != null) {
            Propagate(analysisPoint);
            MakeInterProceduralGraph(programPoints, i++);
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

            markOrUnmarkProgramPoint(successor, joinElement);
            i++;
            logFact.add(successor.clone());
        }
        logFactLists.add(logFact);
    }

    private static void markOrUnmarkProgramPoint(ProgramPoint successor, LatticeElement joinElement) {
        if (joinElement.equals(successor.getLatticeElement()) && !successor.isMarked()) {

            successor.setMarkPoint(false);
        } else {
            successor.setMarkPoint(true);
            successor.setLatticeElement(joinElement);
        }
    }

    private static void MakeInterProceduralGraph(List<ProgramPoint> preProcessedBody, Integer i) throws IOException {

        FileWriter fileWriter = new FileWriter(String.format("./iterations/callgraph_" + i.toString() + ".dot"));
        int j = 0;
        fileWriter.write("digraph G { node [shape=\"Mrectangle\"]" + System.lineSeparator());
        fileWriter.write("subgraph cluster_" + j++ + " {" + System.lineSeparator());

        String currentName = preProcessedBody.get(0).getMethodName();
        for (ProgramPoint pp : preProcessedBody) {
            for (ProgramPoint s : pp.getSuccessors()) {
                if (pp.getMethodName() != currentName) {
                    fileWriter.write(
                            "}" + "subgraph cluster_" + j++ + " {"
                                    + System.lineSeparator());
                    currentName = pp.getMethodName();
                }

                String label = "[ label=\"" + s.getLatticeElement().toString().replace("\n", "\\l") + "\"]";
                if (s.isMarked()) {
                    label = "[ label=\"" + s.getLatticeElement().toString().replace("\n", "\\l") + "\", color=red]";
                }
                fileWriter.write("\"" + pp.getMethodName() + " " + pp.getStmt() + "\" -> \"" + s.getMethodName() + " "
                        + s.getStmt() + "\"" + label
                        + System.lineSeparator());
            }
        }
        fileWriter.write("}" + System.lineSeparator());
        for (ProgramPoint pp : preProcessedBody) {
            if (pp.callSuccessor != null) {
                String markred = " color=red ";
                if (!pp.callSuccessor.isMarked()) {
                    markred = "";
                }
                fileWriter.write(
                        "\"" + pp.getMethodName() + " " + pp.getStmt() + "\" -> \"" + pp.callSuccessor.getMethodName()
                                + " " + pp.callSuccessor.getStmt() + "\"" + "[ label=\"" + pp.callEdgeId + "\n" +
                                pp.callSuccessor.getLatticeElement().toString().replace("\n", "\\l")
                                + "\", style=dotted," + markred + "]"
                                + System.lineSeparator());
            }
            int k = 0;
            for (ProgramPoint returnPoints : pp.returnSuccessors) {
                String markred = " color=red ";
                if (!returnPoints.isMarked()) {
                    markred = "";
                }
                fileWriter.write("\"" + pp.getMethodName() + " " + pp.getStmt() + "\" -> \""
                        + returnPoints.getMethodName() + " "
                        + returnPoints.getStmt() + "\"" + "[ label=\"" + pp.returnEdgeIds.get(k) + "\", style=dotted"
                        + markred + "]"
                        + System.lineSeparator());
                // returnPoints.getLatticeElement().toString().replace("\n", "\\l")
                k++;
            }
        }
        fileWriter.write("}");
        fileWriter.close();
    }

    private static ProgramPoint GetMarkedProgramPoint(List<ProgramPoint> programPoints) {
        for (ProgramPoint programPoint : programPoints) {
            if (programPoint.isMarked())
                return programPoint;

        }
        return null;
    }
}
