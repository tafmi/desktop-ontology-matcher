
package ontologymatcher.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import ontologymatcher.utils.Utils;


public class FileAttributes {
       
    private final String creationTime;
    private final String modificationTime;
    private final String accessTime;
    private final String type;
    private final String size;
    
    public FileAttributes(File file) throws IOException{
        
        Path filePath = file.toPath();
        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);

        creationTime=attr.creationTime().toString();
        accessTime=attr.lastAccessTime().toString();
        modificationTime=attr.lastModifiedTime().toString();
        type=Utils.getFileExtension(file);
        size=String.valueOf(attr.size());
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getModificationTime() {
        return modificationTime;
    }

    public String getType() {
        return type;
    }

    public String getSize() {
        return size;
    }

    public String getAccessTime() {
        return accessTime;
    }
    
    
    
}
