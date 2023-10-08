import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class HtmlFileFinder {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please provide two directory paths as arguments.");
            return;
        }

        String directoryPath1 = args[0]; // 获取第一个目录路径
        String directoryPath2 = args[1]; // 获取第二个目录路径

        Map<String, String> resultMap1 = new HashMap<>();
        Map<String, String> resultMap2 = new HashMap<>();

        findHtmlFiles(new File(directoryPath1), resultMap1);
        findHtmlFiles(new File(directoryPath2), resultMap2);

        // 遍历resultMap1中的所有key
        for (String key : resultMap1.keySet()) {
            if (resultMap2.containsKey(key)) {
                File sourceFile = new File(resultMap1.get(key));
                File targetFile = new File(resultMap2.get(key));

                try {
                    copyFileUsingChannel(sourceFile, targetFile);
                    System.out.println("Copied " + sourceFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void findHtmlFiles(File directory, Map<String, String> resultMap) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findHtmlFiles(file, resultMap);
            } else if (file.getName().endsWith(".html")) {
                resultMap.put(file.getName(), file.getAbsolutePath());
            }
        }
    }

    private static void copyFileUsingChannel(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest, true).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
}
