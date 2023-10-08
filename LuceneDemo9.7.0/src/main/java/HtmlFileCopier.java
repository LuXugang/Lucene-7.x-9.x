import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class HtmlFileCopier {

    public static void main(String[] args) {

        String sourceDirectory = "/Users/luxugang/Documents/blog/Lucene";
        String targetDirectory = "/Users/luxugang/project/test";

        copyHtmlFiles(new File(sourceDirectory), new File(targetDirectory));
    }

    public static void copyHtmlFiles(File sourceDir, File targetDir) {
        if (sourceDir == null || !sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }

        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if(file.getName().contains("image")) continue;
                copyHtmlFiles(file, targetDir);
            } else if (file.getName().matches("^\\d+\\.html$")) { // 正则表达式检查
                File targetFile = new File(targetDir, file.getName());
                try {
                    copyFile(file, targetFile);
                    System.out.println("Copied " + file.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        // 如果目标文件已存在，先删除它
        if (dest.exists()) {
            dest.delete();
        }

        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
}
