/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ontologymatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;
import ontologymatcher.exceptions.InvalidArgumentException;
import ontologymatcher.utils.Utils;

/**
 *
 * @author Teo
 */
public class Main {

    /**
     * @param args the command line arguments
     * @throws ontologymatcher.exceptions.InvalidArgumentException
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     * @throws org.xml.sax.SAXException
     * @throws org.apache.tika.exception.TikaException
     * @throws org.apache.lucene.queryparser.classic.ParseException
     */
    public static void main(String[] args) throws InvalidArgumentException, IOException, FileNotFoundException, SAXException, TikaException, ParseException {
        // TODO code application logic here
        
        try{
             if (args.length != 2) {
                throw new InvalidArgumentException();
            }
             else{
                String inputDirectory;
                String outputPath;
                
                inputDirectory=GetInputPath(args[0]);
                outputPath=GetOutputPath(args[1]);
                
                OntologyMatcher matcher = new OntologyMatcher(inputDirectory,outputPath);
                matcher.start();
                
             }
        }
        catch (IOException ex) {
            System.out.println(String.format("Error: %s", ex.getMessage()));
        }
        catch (InvalidArgumentException invalidArgEx) {
            System.out.println("Application usage:");
            System.out.println("<executable_name>  <input_directory>  <output_path>");
            
        }
    }
    private static String GetInputPath(String argument) throws FileNotFoundException, IOException {
        String inputDirectory = argument;

        File file = new File(inputDirectory);

        if (!file.exists()) {
            throw new FileNotFoundException("Input directory does not exist. Please specify a valid input directory.");
        }

        return inputDirectory;
    }

    private static String GetOutputPath(String argument) throws IOException {
        String outputPath = argument;

        File file = new File(outputPath);

        if(!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
            throw new IOException("Something is wrong with the output path you specified. Please check the path and try again.");
        }
        
        if (file.isDirectory()) {
            throw new IOException("The file specified as output is a directory. Please specify a name that is not a directory.");
        }
        
        if(Utils.getFileExtension(file).equals(".owl")){
            throw new IOException("The file specified as output is not an owl file. Please specify a name that is an owl file.");
        }

        return outputPath;
    }
    
}
