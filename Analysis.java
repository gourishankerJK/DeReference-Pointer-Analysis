// This program will plot a CFG for a method using soot
// [ExceptionalUnitGraph feature].
// Arguements : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>

// Ref:
// 1) https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
// 2) https://github.com/soot-oss/soot/wiki/Tutorials

////////////////////////////////////////////////////////////////////////////////
import java.io.FileWriter;
import java.io.IOException;
////////////////////////////////////////////////////////////////////////////////
import java.util.*;

////////////////////////////////////////////////////////////////////////////////

import soot.options.Options;
import soot.Unit;
import soot.Scene;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.UnitPrinter;
import soot.NormalUnitPrinter;

import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;
import utils.CustomTag;
import utils.FixedSizeStack;

public class Analysis extends PAVBase {
    static String targetDirectory;
    static String mClass;
    static String tClass;
    static String tMethod;

    public Analysis() {
        /*************************************************************
         * XXX you can implement your analysis here
         ************************************************************/
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println(
                    "Incorrect usage, usage format: java Analysis <targetDirectory> <mainClass> <targetClass> <targetMethod>");
        }
        targetDirectory = args[0];
        mClass = args[1];
        tClass = args[2];
        tMethod = args[3];
        String mode = "";
        try {
            mode = args[4];
        } catch (Exception e) {

        }
        boolean methodFound = false;

        List<String> procDir = new ArrayList<String>();
        procDir.add(targetDirectory);

        // Set Soot options
        soot.G.reset();
        Options.v().set_process_dir(procDir);
        // Options.v().set_prepend_classpath(true);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("cg.spark", "verbose:false");

        Scene.v().loadNecessaryClasses();

        SootClass entryClass = Scene.v().getSootClassUnsafe(mClass);
        SootMethod entryMethod = entryClass.getMethodByNameUnsafe("main");
        SootClass targetClass = Scene.v().getSootClassUnsafe(tClass);
        SootMethod targetMethod = entryClass.getMethodByNameUnsafe(tMethod);

        Options.v().set_main_class(mClass);
        Scene.v().setEntryPoints(Collections.singletonList(entryMethod));

        // System.out.println (entryClass.getName());
        System.out.println("tclass: " + targetClass);
        System.out.println("tmethod: " + targetMethod);
        System.out.println("tmethodname: " + tMethod);
        Iterator<SootMethod> mi = targetClass.getMethods().iterator();
        while (mi.hasNext()) {
            SootMethod sm = (SootMethod) mi.next();
            // System.out.println("method: " + sm);
            if (sm.getName().equals(tMethod)) {
                methodFound = true;
                break;
            }
        }

        if (methodFound) {
            // Preprocess for the pointer lattice element
            Kildall.ComputeLFP((new ApproximateCallStringPreProcess()).PreProcess(targetMethod.retrieveActiveBody()));
        } else {
            System.out.println("Method not found: " + tMethod);
        }
    }

    public static void formatAndWriteToFile(List<ProgramPoint> p, boolean finalResult, int logIndex) {
        List<String> result = formatResult(p, targetDirectory, tClass, tMethod);
        try {
            writeResultToFile(logIndex, targetDirectory, tClass, tMethod, mClass, result);

            if (finalResult) {
                System.out.println(
                        String.format("Logs of kildall written in %s/%s.%s.fulloutput.txt",
                                targetDirectory, tClass,
                                tMethod));
                writeResultToFile(0, targetDirectory, tClass, tMethod, mClass, result);

                System.out.println(
                        String.format("Final output written in%s/%s.%s.output.txt", targetDirectory, tClass, tMethod));
            }
            MakeInterProceduralGraph(p, logIndex);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void MakeInterProceduralGraph(List<ProgramPoint> preProcessedBody, Integer i) throws IOException {

        FileWriter fileWriter = new FileWriter(String.format("./iterations/callgraph_" + i.toString() + ".dot"));
        int j = 0;
        fileWriter.write(
                "digraph G { label =\"" + i + "\"node [shape=\"box\"]"
                        + System.lineSeparator());
        fileWriter.write("subgraph cluster_" + j++ + " {" + System.lineSeparator());
        String str = "\"%s %s\" -> \"%s %s\" %s%n";
        String Label = "[label=\"%s\"color=%s style=%s]";
        String currentName = preProcessedBody.get(0).getMethodName();
        for (ProgramPoint pp : preProcessedBody) {
            for (ProgramPoint s : pp.getSuccessors()) {
                if (pp.getMethodName() != currentName) {
                    fileWriter.write(
                            "}" + "subgraph cluster_" + j++ + " {"
                                    + System.lineSeparator());
                    currentName = pp.getMethodName();
                }

                String color = s.isMarked() ? "red" : "black";
                 if(pp.InfiniteLoop) color = "purple";
                writeDotFile(fileWriter , Label, s.getLatticeElement().toString().replace("\n", "\\l"), color,
                        "solid",str, pp.getMethodName(), pp.getStmt().toString(), s.getMethodName(),
                        s.getStmt().toString());

            }
        }
        fileWriter.write("}" + System.lineSeparator());
        for (ProgramPoint pp : preProcessedBody) {
            if (pp.callSuccessor != null) {
                String color = pp.callSuccessor.isMarked() ? "red" : "black";
                writeDotFile(fileWriter , Label,
                        pp.callEdgeId + "\\l\\l" + pp.callSuccessor.getLatticeElement().toString().replace("\n", "\\l"),
                        color, "dashed",str,
                        pp.getMethodName(), pp.getStmt().toString(), pp.callSuccessor.getMethodName(), pp.callSuccessor.getStmt().toString()
                         );
            }

            int k = 0;
            for (ProgramPoint returnPoints : pp.returnSuccessors) {
                String color = !returnPoints.isMarked() ? "black" : "red";
                writeDotFile(fileWriter , Label, pp.returnEdgeIds.get(k), color, "dashed",str,
                        pp.getMethodName(), pp.getStmt().toString(), returnPoints.getMethodName(), returnPoints.getStmt().toString());
                k++;
            }
        }
        fileWriter.write("}");
        fileWriter.close();
    }

    public static void writeDotFile(FileWriter fileWriter, String Label, String labelText, String color, String style,
            String str, String lMethodName, String lStatement, String rMethodName, String rStatement) {
        String label = String.format(Label, labelText, color, style);
        String dottedString = String.format(str, lMethodName, lStatement, rMethodName,
                rStatement, label, System.lineSeparator());
        try {
            fileWriter.write(dottedString);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void writeResultToFile(int logIndex, String directory, String tClass, String tMethod, String mode,
            List<String> output)
            throws IOException {
        FileWriter fileWriter;
        String type = logIndex != 0 ? "fulloutput" : "output";
        try {

            if (mode.equals("test")) {
                fileWriter = new FileWriter(String.format("./actual-output/%s.%s.%s.txt", tClass, tMethod, type),
                        logIndex > 1);
            } else {
                fileWriter = new FileWriter(String.format("%s/%s.%s.%s.txt", directory, tClass, tMethod, type),
                        logIndex > 1);
            }
            for (String str : output) {
                fileWriter.write(str + System.lineSeparator());
            }
            if (logIndex >= 1)
                fileWriter.write("\n");
            fileWriter.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static int getLineNumber(Stmt st) {
        return ((CustomTag) st.getTag("lineNumberTag")).getLineNumber();
    }

    private static String formatEntry(Map.Entry<String, HashSet<String>> p) {
        String res = "";
        if (p.getKey().contains("::")) {
            res += p.getKey().split("::")[1];
        } else {
            res += p.getKey();
        }
        res += ": {";
        int i = 0;
        List<String> sorted = new ArrayList<>(p.getValue());
        Collections.sort(sorted);
        for (String s : sorted) {
            i++;
            if (i == p.getValue().size())
                res += s;
            else
                res += s + ", ";
        }
        res += "}";
        return res;
    }

    private static List<String> formatResult(List<ProgramPoint> result, String directory, String tClass,
            String tMethod) {
        String ans = "";
        List<String> outputs = new ArrayList<String>();
        for (ProgramPoint programPoint : result) {
            Map<FixedSizeStack<String>, PointerLatticeElement> superState = ((ApproximateCallStringElement) programPoint
                    .getLatticeElement()).getState();
            for (Map.Entry<FixedSizeStack<String>, PointerLatticeElement> entry : superState.entrySet()) {
                if ( entry.getValue()!=null)
                for (Map.Entry<String, HashSet<String>> p : entry.getValue().getState().entrySet()) {
                    String functionName = ((CustomTag) programPoint.getStmt().getTag("functionName")).getStringTag();
                    if (p.getValue().size() != 0
                            && (p.getKey().matches(functionName + ".*") || !p.getKey().matches(".*::.*"))
                            && !p.getKey().matches("@.*")) {
                        String ek = entry.getKey().size() == 0 ? "@"
                                : entry.getKey().toString();
                        ans = (functionName + ": "
                                + String.format("in%02d:", getLineNumber(programPoint.getStmt()))
                                + " "
                                + ek + " => " + formatEntry(p));
                        outputs.add(ans);
                    }
                }
            }
        }
        Collections.sort(outputs);
        return outputs;
    }

    private static void drawMethodDependenceGraph(SootMethod entryMethod) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();
            ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

            CFGToDotGraph cfgForMethod = new CFGToDotGraph();
            cfgForMethod.drawCFG(graph);
            DotGraph cfgDot = cfgForMethod.drawCFG(graph);
            cfgDot.plot("cfg.dot");
        }
    }

    public static void printUnit(int lineno, Body b, Unit u) {
        UnitPrinter up = new NormalUnitPrinter(b);
        u.toString(up);
        String linenostr = String.format("%02d", lineno) + ": ";
        System.out.println(linenostr + up.toString());
    }

    private static void printInfo(SootMethod entryMethod) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();

            Integer lineno = 0;
            for (Unit u : body.getUnits()) {
                if (!(u instanceof Stmt)) {
                    continue;
                }
                printUnit(lineno, body, u);
                lineno++;
            }

        }
    }

}
