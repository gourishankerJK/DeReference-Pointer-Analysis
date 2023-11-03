// This program will plot a CFG for a method using soot
// [ExceptionalUnitGraph feature].
// Arguements : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>

// Ref:
// 1) https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
// 2) https://github.com/soot-oss/soot/wiki/Tutorials

////////////////////////////////////////////////////////////////////////////////
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

////////////////////////////////////////////////////////////////////////////////

import soot.options.Options;
import soot.tagkit.Tag;
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

////////////////////////////////////////////////////////////////////////////////

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
        Iterator mi = targetClass.getMethods().iterator();
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
            IPreProcess preProcess = new PointerLatticePreProcess();
            List<ProgramPoint> preProcessedBody = preProcess.PreProcess(targetMethod.retrieveActiveBody());

            // Compute Least fix point using Kildall's algorithms
            List<List<ProgramPoint>> result = Kildall.ComputeLFP(preProcessedBody);
            // Format the data according to required output
            writeResultToFile(0, targetDirectory, tClass, tMethod, mode, result.get(0));
            System.out.println("Final output written in "
                    + String.format("%s/%s.%s.output.txt", targetDirectory, tClass, tMethod));
            for (int i = 1; i < result.size(); i++) {
                writeResultToFile(i, targetDirectory, tClass, tMethod, mode, result.get(i));
            }
             writeResultToFile(10, targetDirectory, tClass, tMethod, mode, result.get(0));
            System.out.println("Logs of kildall written in "
                    + String.format("%s/%s.%s.fulloutput.txt", targetDirectory, tClass, tMethod));

            drawMethodDependenceGraph(targetMethod);
        } else {
            System.out.println("Method not found: " + tMethod);
        }
    }

    private static void writeResultToFile(int logIndex, String directory, String tClass, String tMethod, String mode,
            List<ProgramPoint> result)
            throws IOException {
        Set<ResultTuple> resultFormatted = getFormattedResult(result, tMethod);
        String[] output = fmtOutputData(resultFormatted, tClass + ".");
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

    private static Set<ResultTuple> getFormattedResult(List<ProgramPoint> result, String method) {
        Set<ResultTuple> resultFormatted = new HashSet<ResultTuple>();
        for (ProgramPoint programPoint : result) {
            Map<String, HashSet<String>> state = ((PointerLatticeElement) programPoint.getLatticeElement()).getState();
            for (String key : state.keySet()) {
                if (state.get(key).size() == 0)
                    continue;

                List<String> varList = new ArrayList<String>(state.get(key));
                Collections.sort(varList);
                List<Tag> tags = programPoint.getStmt().getTags();
                ResultTuple tuple = new ResultTuple(method,
                        String.format("in%02d", Integer.parseInt(tags.get(tags.size() - 1).toString())), key, varList);
                resultFormatted.add(tuple);
            }
        }

        return resultFormatted;
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
