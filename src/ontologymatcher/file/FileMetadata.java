
package ontologymatcher.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


public class FileMetadata {
    
    private final String fileName;
    private final String fileParentPath;
    private final String author;
    private final String company;
    private final String contents;
    
    public FileMetadata(File file) throws FileNotFoundException, IOException, SAXException, TikaException{
        
        Metadata metadata = new Metadata();
        InputStream input = new FileInputStream(file);
        ContentHandler handler = new BodyContentHandler(10*1024*1024);
          
        Parser parser = new AutoDetectParser();        
        ParseContext context = new ParseContext();
        parser.parse(input, handler, metadata,context);
        fileName = file.getName().replaceFirst("[.][^.]+$", "");
        fileParentPath = file.getParent();
        author = metadata.get(Metadata.AUTHOR);
        company = metadata.get(Metadata.COMPANY);
        contents = handler.toString();
    }

    public String getFileName() {
        return fileName;
    }
    

    public String getAuthor() {
        return author;
    }

    public String getCompany() {
        return company;
    }

    public String getFileParentPath() {
        return fileParentPath;
    }

    public String getContents() {
        return contents;
    }
    
    
    
}
