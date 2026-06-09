import java.io.*;
import java.nio.file.*;

public class FindMvn {
    public static void main(String[] args) {
        String[] paths = System.getenv("PATH").split(File.pathSeparator);
        System.out.println("=== Searching PATH for mvn ===");
        for (String p : paths) {
            File dir = new File(p);
            if (dir.isDirectory()) {
                File mvn = new File(dir, "mvn");
                File mvnBat = new File(dir, "mvn.bat");
                File mvnCmd = new File(dir, "mvn.cmd");
                if (mvn.exists()) System.out.println("FOUND: " + mvn.getAbsolutePath());
                if (mvnBat.exists()) System.out.println("FOUND: " + mvnBat.getAbsolutePath());
                if (mvnCmd.exists()) System.out.println("FOUND: " + mvnCmd.getAbsolutePath());
            }
        }
        System.out.println("=== Searching common Maven locations ===");
        String[] commonPaths = {
            "C:\\tools\\apache-maven*",
            "C:\\Program Files\\apache-maven*",
            "C:\\Program Files\\Maven*",
            "D:\\tools\\apache-maven*",
            "D:\\Program Files\\apache-maven*",
            System.getProperty("user.home") + "\\.m2\\wrapper\\dists\\*"
        };
        for (String pattern : commonPaths) {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                File dir = new File(pattern).getParentFile();
                if (dir != null && dir.isDirectory()) {
                    for (File f : dir.listFiles()) {
                        if (f.isDirectory() && f.getName().contains("maven")) {
                            System.out.println("FOUND: " + f.getAbsolutePath());
                            File binDir = new File(f, "bin");
                            if (binDir.isDirectory()) {
                                for (File bin : binDir.listFiles()) {
                                    System.out.println("  " + bin.getName());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error checking " + pattern + ": " + e.getMessage());
            }
        }
        System.out.println("=== DONE ===");
    }
}
