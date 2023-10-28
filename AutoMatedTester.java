
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AutoMatedTester {
    public static void main(String[] filePairs) {
        // Consume the remaining newline character
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < 4; i +=1) {
            String file1Path = "./target2-mine/Public_Test_fun"+(3+i)+".txt";
            String file2Path = "./Result_fun"+(3+i)+"_public.txt";
            try {
                BufferedReader reader1 = new BufferedReader(new FileReader(file1Path));
                BufferedReader reader2 = new BufferedReader(new FileReader(file2Path));
                String line1 = reader1.readLine();
                String line2 = reader2.readLine();

                int lineNumber = 1;
                while (line1 != null || line2 != null) {
                    if (line1 == null || line2 == null
                            || !getComparisonSubstring(line1).equals(getComparisonSubstring(line2))) {
                        differences.add("Difference found at line " + lineNumber);
                        if (line1 != null) {
                            differences.add("File " + i + ": " + line1);
                        }
                        if (line2 != null) {
                            differences.add("File " + (i + 1) + ": " + line2);
                        }

                        differences.add("---=-------=------=--");
                    }

                    line1 = reader1.readLine();
                    line2 = reader2.readLine();
                    lineNumber++;
                }
                differences.add("Completed File: " + (i + 1));
                  differences.add("XXXXXXXXXXXXXXXXX");

                reader1.close();
                reader2.close();
                // Write differences to a file
                String outputFilePath = "difference" + ".txt";
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
                for (String diff : differences) {
                    writer.write(diff);
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                System.out.println("hello");
                e.printStackTrace();
            }
        }
    }

    private static String getComparisonSubstring(String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex != -1 && colonIndex + 1 < line.length()) {
            return line.substring(colonIndex + 1).trim();
        }
        return line.trim();
    }
}