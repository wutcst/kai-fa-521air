import java.io.*;

public class FindMvn2 {
    public static void main(String[] args) {
        String userHome = System.getProperty("user.home");
        File wrapperDists = new File(userHome, ".m2/wrapper/dists");
        if (!wrapperDists.isDirectory()) {
            System.out.println("ERROR: " + wrapperDists + " is not a directory or doesn't exist");
            return;
        }
        for (File distDir : wrapperDists.listFiles()) {
            if (distDir.isDirectory()) {
                System.out.println("Distribution: " + distDir.getName() + " (dir)");
                File[] subs = distDir.listFiles();
                if (subs != null) {
                    for (File sub : subs) {
                        System.out.println("  Hash: " + sub.getName() + " (dir)");
                        File binDir = new File(sub, "bin");
                        if (binDir.isDirectory()) {
                            File[] bins = binDir.listFiles();
                            if (bins != null) {
                                for (File b : bins) {
                                    System.out.println("    " + b.getName());
                                }
                            }
                        } else {
                            System.out.println("    (no bin dir)");
                        }
                    }
                }
            } else {
                System.out.println("Distribution: " + distDir.getName() + " (file)");
            }
        }
    }
}
