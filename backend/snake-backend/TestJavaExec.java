import java.io.*;

public class TestJavaExec {
    public static void main(String[] args) throws Exception {
        System.out.println("Java execution test starting...");
        System.out.println("user.dir: " + System.getProperty("user.dir"));
        System.out.println("JAVA_HOME: " + System.getenv("JAVA_HOME"));
        System.out.println("PATH: " + System.getenv("PATH"));

        // Test 1: Simple command
        System.out.println("\n--- Test 1: dir command ---");
        ProcessBuilder pb1 = new ProcessBuilder("cmd.exe", "/c", "dir", "D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend\\src\\test\\java\\com\\snake");
        pb1.redirectErrorStream(true);
        Process p1 = pb1.start();
        BufferedReader r1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
        String line;
        while ((line = r1.readLine()) != null) {
            System.out.println(line);
        }
        p1.waitFor();

        // Test 2: Find mvn binary
        System.out.println("\n--- Test 2: Find mvn binary ---");
        File mvnScript = new File("C:\\Users\\Lenovo\\.m2\\wrapper\\dists\\apache-maven-3.9.15\\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\\bin\\mvn.cmd");
        System.out.println("mvn.cmd exists: " + mvnScript.exists());
        System.out.println("mvn.cmd isFile: " + mvnScript.isFile());

        // Test 3: Run mvn.cmd
        if (mvnScript.exists()) {
            System.out.println("\n--- Test 3: Running mvn.cmd test ---");
            ProcessBuilder pb2 = new ProcessBuilder(
                mvnScript.getAbsolutePath(),
                "test",
                "-f", "D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend\\pom.xml",
                "-Dmaven.test.failure.ignore=true"
            );
            pb2.directory(new File("D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend"));
            pb2.environment().put("JAVA_HOME", "D:\\java21");
            pb2.redirectErrorStream(true);

            // Create log file
            File logFile = new File("D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend\\maven_java_output.log");
            pb2.redirectOutput(ProcessBuilder.Redirect.to(logFile));

            Process p2 = pb2.start();
            int exitCode = p2.waitFor();
            System.out.println("mvn.cmd exit code: " + exitCode);
            System.out.println("Log file size: " + logFile.length());
        }
    }
}
