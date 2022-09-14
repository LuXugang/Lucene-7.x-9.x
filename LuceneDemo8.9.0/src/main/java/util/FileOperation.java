package util;

import java.io.File;

/**
 * @author Lu Xugang
 * @date 2021/6/18 9:46 上午
 */
public class FileOperation {
    public static void deleteFile(String filePath) {
        File dir = new File(filePath);
        if (dir.exists()) {
            File[] tmp = dir.listFiles();
            assert tmp != null;
            for (File aTmp : tmp) {
                if (aTmp.isDirectory()) {
                    deleteFile(filePath + "/" + aTmp.getName());
                } else {
                    aTmp.delete();
                }
            }
            dir.delete();
        }
    }
}
