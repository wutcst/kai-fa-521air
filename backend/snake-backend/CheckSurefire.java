import java.io.*;

public class CheckSurefire {
    public static void main(String[] args) {
        File surefire = new File("D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend\\target\\surefire-reports");
        if (!surefire.isDirectory()) {
            System.out.println("surefire-reports directory does not exist");
            return;
        }
        System.out.println("=== Surefire Reports ===");
        File[] files = surefire.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("(empty directory)");
        } else {
            for (File f : files) {
                System.out.println(f.getName() + " - " + f.length() + " bytes");
            }
        }

        // Also check if any txt files exist
        System.out.println("\n=== All files in target/ ===");
        listFiles(new File("D:\\RjSnake2\\kai-fa-521air\\backend\\snake-backend\\target"), 0);
    }

    static void listFiles(File dir, int depth) {
        if (depth > 3) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                StringBuilder indent = new StringBuilder();
                for (int i = 0; i < depth; i++) indent.append("  ");
                System.out.println(indent + f.getName() + (f.isDirectory() ? "/" : ""));
                if (f.isDirectory()) listFiles(f, depth + 1);
            }
        }
    }
}
