import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileComparator {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java FileComparator <file1> <file2>");
            return;
        }

        String file1 = args[0];
        String file2 = args[1];

        try {
            boolean identical = compareFiles(file1, file2);
            if (identical) {
                System.out.println("Files are identical.");
            }
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
        }
    }

    public static boolean compareFiles(String file1, String file2) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(file1));
             BufferedReader reader2 = new BufferedReader(new FileReader(file2))) {

            String line1;
            String line2;
            int lineNum = 1;

            while ((line1 = reader1.readLine()) != null | (line2 = reader2.readLine()) != null) {
                // If lines are not equal, print the difference and return false
                if ((line1 == null && line2 != null) || (line1 != null && line2 == null) || !line1.equals(line2)) {
                    System.out.println("Difference found at line " + lineNum + ":");
                    System.out.println("File 1: " + (line1 != null ? line1 : "EOF"));
                    System.out.println("File 2: " + (line2 != null ? line2 : "EOF"));
                    return false;
                }
                lineNum++;
            }
        }
        return true; // Files are identical
    }
}
