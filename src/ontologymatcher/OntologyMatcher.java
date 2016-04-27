
package ontologymatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import ontologymatcher.ontology.Ontology;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;


public class OntologyMatcher {
    
    private final String input;
    private final String output;
    
    public OntologyMatcher(String input,String output){
        
        this.input = input;
        this.output = output;
    }
    
    public void start() throws IOException, SAXException, FileNotFoundException, TikaException, ParseException{
        
        Logger.getRootLogger().setLevel(Level.OFF);
        Ontology ontology = new Ontology(input,output);
        ontology.readTriples();
        ontology.compareConceptsAndFiles();
        ontology.createFileIntances();
        
    }
    
}
