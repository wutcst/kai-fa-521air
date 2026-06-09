import java.io.*;

public class CheckLogs {
    public static void main(String[] args) {
        File dir = new File("D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend");
        File[] logs = dir.listFiles((d, n) -> n.endsWith(".log"));
        if (logs != null) {
            for (File f : logs) {
                System.out.println(f.getName() + " - " + f.length() + " bytes");
            }
        } else {
            System.out.println("No log files found");
        }

        // Check target directory
        File target = new File(dir, "target");
        if (target.isDirectory()) {
            System.out.println("\ntarget/ exists:");
            File[] contents = target.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    System.out.println("  " + f.getName());
                }
            }
        } else {
            System.out.println("\ntarget/ does NOT exist");
        }
    }
}
