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
        String targetDirectory = args[0];
        String mClass = args[1];
        String tClass = args[2];
        String tMethod = args[3];
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
            printInfo(targetMethod);
            /*************************************************************
             * XXX This would be a good place to call the function
             * which performs the Kildalls iterations over the LatticeElement.
             *************************************************************/
            // Preprocess for the pointer lattice element
            List<ProgramPoint> preProcessedBody = (new ApproximateCallStringPreProcess())
                    .PreProcess(targetMethod.retrieveActiveBody());

            // Compute Least fix point using Kildall's algorithms

            List<List<ProgramPoint>> result = Kildall.ComputeLFP(preProcessedBody);

            List<String> output = formatResult(result.get(0), targetDirectory, tClass, tMethod);
            writeResultToFile(0, targetDirectory, tClass, tMethod, mode, output);
            System.out.println("Final output written in "
                    + String.format("%s/%s.%s.output.txt", targetDirectory, tClass, tMethod));
            for (int i = 1; i < result.size(); i++) {
                writeResultToFile(i, targetDirectory, tClass, tMethod, mode,
                        formatResult(result.get(i), targetDirectory, tClass, tMethod));
            }
            writeResultToFile(10, targetDirectory, tClass, tMethod, mode, output);
            System.out.println("Logs of kildall written in "
                    + String.format("%s/%s.%s.fulloutput.txt", targetDirectory, tClass,
                            tMethod));
            MakeInterProceduralGraph(result.get(0));
            drawMethodDependenceGraph(targetMethod);
        } else {
            System.out.println("Method not found: " + tMethod);
        }
    }

    private static void MakeInterProceduralGraph(List<ProgramPoint> preProcessedBody) throws IOException {

        FileWriter fileWriter = new FileWriter(String.format("./callgraph.dot"));
        int j = 0;
        Random random = new Random();
        fileWriter.write("digraph G { node [shape=\"Mrectangle\"]" + System.lineSeparator());
        fileWriter.write("subgraph cluster_" + j++ + " {" + System.lineSeparator());
        String[] bgColors = {
                "red",
                "green",
                "yellow",
                "orange",
                "purple",
                "pink",
                "brown",
                "aqua"
        };

        String currentName = preProcessedBody.get(0).getMethodName();
        for (ProgramPoint pp : preProcessedBody) {
            for (ProgramPoint s : pp.getSuccessors()) {
                if (pp.getMethodName() != currentName) {
                    int index = random.nextInt(bgColors.length);
                    fileWriter.write(
                            "}" + "subgraph cluster_" + j++ + " {"
                                    + System.lineSeparator());
                    currentName = pp.getMethodName();
                }
                fileWriter.write("\"" + pp.getMethodName() + " " + pp.getStmt() + "\" -> \"" + s.getMethodName() + " "
                        + s.getStmt() + "\"" + "[ label=\"" + s.getLatticeElement().toString() + "\"]"
                        + System.lineSeparator());
            }
        }
        fileWriter.write("}" + System.lineSeparator());
        for (ProgramPoint pp : preProcessedBody) {
            if (pp.callSuccessor != null) {
                fileWriter.write("\"" + pp.getMethodName() + " " + pp.getStmt() + "\" -> \"" + pp.callSuccessor.getMethodName()
                        + " " + pp.callSuccessor.getStmt() + "\"" + "[ label=\"" + pp.callEdgeId + "\", style=dotted ]"
                        + System.lineSeparator());
            }
            int k = 0;
            for (ProgramPoint returnPoints : pp.returnSuccessors) {
                fileWriter.write("\"" + pp.getMethodName() + " " + pp.getStmt() + "\" -> \"" + returnPoints.getMethodName() + " "
                        + returnPoints.getStmt() + "\"" + "[ label=\"" + pp.returnEdgeIds.get(k) + "\", style=dotted ]"
                        + System.lineSeparator());
                k++;
            }
        }
        fileWriter.write("}");
        fileWriter.close();
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
                System.out.println(str);
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
        for (String s : p.getValue()) {
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
                for (Map.Entry<String, HashSet<String>> p : entry.getValue().getState().entrySet()) {
                    String baseClass = ((CustomTag) programPoint.getStmt().getTag("baseClass")).getStringTag();
                    String functionName = ((CustomTag) programPoint.getStmt().getTag("functionName")).getStringTag();
                    System.out.println(functionName + " "+ programPoint.getStmt() +" |" + programPoint.getMethodName());
                    if (p.getValue().size() != 0
                            && (p.getKey().matches(functionName + ".*") || !p.getKey().matches(".*::.*"))
                            && !p.getKey().matches("@.*")) {
                        String ek = entry.getKey().size() == 0 ? "@"
                                : entry.getKey().toString();
                        ans = (baseClass + "." + functionName + ": "
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
