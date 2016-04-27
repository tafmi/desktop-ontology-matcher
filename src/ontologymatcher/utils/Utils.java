
package ontologymatcher.utils;

import java.io.File;


public class Utils {
    
    public static String getFileExtension(File file) {
        return file.isDirectory() ? "" : (file.getName().substring(file.getName().lastIndexOf(".") + 1));
    }
    
    public static boolean isDocument(File file){
         String ext=file.getName().substring(file.getName().lastIndexOf(".") + 1);
        return ext.equals("txt") || ext.equals("pdf") || ext.equals("doc") || ext.equals("docx")
                || ext.equals("xls") || ext.equals("xlsx") || ext.equals("ppt") || ext.equals("pptx");
     }
    
}
