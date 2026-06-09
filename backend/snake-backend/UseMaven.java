import java.io.*;
import java.nio.file.*;

public class UseMaven {
    public static void main(String[] args) throws Exception {
        // Find Maven home
        String userHome = System.getProperty("user.home");
        File wrapperDists = new File(userHome, ".m2/wrapper/dists");

        File mavenHome = null;
        String[] versions = {"apache-maven-3.9.15", "apache-maven-3.9.11-bin", "apache-maven-3.9.15-bin", "apache-maven-3.9.6-bin"};

        if (wrapperDists.isDirectory()) {
            for (File dir : wrapperDists.listFiles()) {
                if (dir.isDirectory()) {
                    // Check if this directory contains a hash subdirectory with mvn
                    for (File sub : dir.listFiles()) {
                        if (sub.isDirectory()) {
                            File mvnScript = new File(sub, "bin/mvn");
                            File mvnCmd = new File(sub, "bin/mvn.cmd");
                            if (mvnScript.exists()) {
                                System.out.println("FOUND mvn at: " + mvnScript.getAbsolutePath());
                                mavenHome = sub;
                            }
                            if (mvnCmd.exists()) {
                                System.out.println("FOUND mvn.cmd at: " + mvnCmd.getAbsolutePath());
                                if (mavenHome == null) mavenHome = sub;
                            }
                        }
                    }
                }
            }
        }

        if (mavenHome == null) {
            System.out.println("ERROR: Could not find Maven installation");
            System.exit(1);
        }

        System.out.println("Using Maven at: " + mavenHome.getAbsolutePath());

        // Build Maven command
        File mvnScript = new File(mavenHome, "bin/mvn");
        if (!mvnScript.exists()) {
            System.out.println("ERROR: mvn script not found at " + mvnScript);
            System.exit(1);
        }

        // Make sure it's executable
        mvnScript.setExecutable(true);

        String projectDir = "D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend";

        ProcessBuilder pb = new ProcessBuilder(
            mvnScript.getAbsolutePath(),
            "test",
            "-f", projectDir + "\\pom.xml",
            "-Dmaven.test.failure.ignore=true"
        );
        pb.directory(new File(projectDir));
        pb.environment().put("JAVA_HOME", "D:\\java21");
        pb.environment().put("PATH", "D:\\java21\\bin;" + System.getenv("PATH"));

        // Redirect output to file
        File outputFile = new File(projectDir, "maven_full_output.log");
        pb.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
        pb.redirectErrorStream(true);

        System.out.println("Starting Maven test...");
        Process process = pb.start();
        int exitCode = process.waitFor();
        System.out.println("Maven exit code: " + exitCode);
        System.out.println("Output written to: " + outputFile.getAbsolutePath());
    }
}
