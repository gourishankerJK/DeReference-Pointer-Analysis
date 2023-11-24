import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Kildall {
    static List<List<ProgramPoint>> logFactLists = new ArrayList<>();

    public static List<List<ProgramPoint>> ComputeLFP(List<ProgramPoint> programPoints) throws IOException {
        ProgramPoint analysisPoint;
        Integer i=0;
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
        if (analysisPoint.callEdgeId != null) {
            ProgramPoint successor = analysisPoint.callSuccessor;
            LatticeElement joinElement = successor.getLatticeElement()
                    .join_op(analysisPoint.getLatticeElement().tf_assignstmt(analysisPoint.getStmt()));
            if (joinElement.equals(successor.getLatticeElement()) && !successor.isMarked()) {

                successor.setMarkPoint(false);
            } else {
                successor.setMarkPoint(true);
                successor.setLatticeElement(joinElement);
            }
            // if the analysis point is a return point
            // Case 1: The successor had a call site that was a normal invoke
            //      In this case there is nothing to update for the caller function
            // Case 2: The successor had a call site that has an assignment
            //      In this case we need to perform strong update.
        } else if (analysisPoint.returnSuccessors.size() != 0) {
            int index = 0;
            for (ProgramPoint successor : analysisPoint.returnSuccessors) {
                LatticeElement joinElement = successor.getLatticeElement()
                        .join_op(analysisPoint.getLatticeElement().tf_returnstmt(analysisPoint.returnEdgeIds.get(index),
                                analysisPoint.getStmt()));
                index++;
                if (joinElement.equals(successor.getLatticeElement()) && !successor.isMarked()) {

                    successor.setMarkPoint(false);
                } else {
                    successor.setMarkPoint(true);
                    successor.setLatticeElement(joinElement);
                }
            }
        } else {

            for (ProgramPoint successor : analysisPoint.getAllSuccessors()) {
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
                }
                i++;
                logFact.add(new ProgramPoint(successor.getLatticeElement(), successor.getStmt(), successor.isMarked()));
            }
        }
        logFactLists.add(logFact);
    }



    private static void MakeInterProceduralGraph(List<ProgramPoint> preProcessedBody, Integer i) throws IOException {

        FileWriter fileWriter = new FileWriter(String.format("./iterations/callgraph_" + i.toString()+ ".dot"));
        int j = 0;
        fileWriter.write("digraph G { node [shape=\"Mrectangle\"]" + System.lineSeparator());
        fileWriter.write("subgraph cluster_" + j++ + " {" + System.lineSeparator());


        String currentName = preProcessedBody.get(0).methodName;
        for (ProgramPoint pp : preProcessedBody) {
            for (ProgramPoint s : pp.getSuccessors()) {
                if (pp.methodName != currentName) {
                    fileWriter.write(
                            "}" + "subgraph cluster_" + j++ + " {"
                                    + System.lineSeparator());
                    currentName = pp.methodName;
                }

                String label = "[ label=\"" + s.getLatticeElement().toString() + "\"]";
                if (s.isMarked()) {
                    label = "[ label=\"" + s.getLatticeElement().toString() + "\", color=red]";
                }
                fileWriter.write("\"" + pp.methodName + " " + pp.getStmt() + "\" -> \"" + s.methodName + " "
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
                fileWriter.write("\"" + pp.methodName + " " + pp.getStmt() + "\" -> \"" + pp.callSuccessor.methodName
                        + " " + pp.callSuccessor.getStmt() + "\"" + "[ label=\"" + pp.callEdgeId + "\n" + pp.callSuccessor.getLatticeElement() + "\", style=dotted," + markred + "]"
                        + System.lineSeparator());
            }
            int k = 0;
            for (ProgramPoint returnPoints : pp.returnSuccessors) {
                String markred = " color=red ";
                if (!returnPoints.isMarked()) {
                    markred = "";
                } 
                fileWriter.write("\"" + pp.methodName + " " + pp.getStmt() + "\" -> \"" + returnPoints.methodName + " "
                            + returnPoints.getStmt() + "\"" + "[ label=\"" + pp.returnEdgeIds.get(k) + "\n" + returnPoints.getLatticeElement() 
                            + "\", style=dotted" + markred+ "]"
                            + System.lineSeparator());
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
