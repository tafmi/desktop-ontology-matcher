
package ontologymatcher.ontology;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ontologymatcher.file.FileAttributes;
import ontologymatcher.lucene.DirectoryIndexer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;


public class Ontology {
    
    private final File file;
    private final OntModel model;
    private final String ns;
    private final String output;
    
    private OntClass thingClass; 
    private OntClass personClass;
    private OntClass organisationClass;
    private OntClass locationClass;
    private OntClass nameClass;
    private OntClass personNameClass;
    private OntClass fileClass;
    
    private ObjectProperty hasLocation;
    private ObjectProperty hasName;
    private ObjectProperty personName;
    private ObjectProperty hasAuthor;
    private ObjectProperty authors;
    private ObjectProperty relatedThing;
    private ObjectProperty relatedFile;
    
    private DatatypeProperty path;
    private DatatypeProperty name;
    private DatatypeProperty fileType;
    private DatatypeProperty creationTime;
    private DatatypeProperty lastModificationTime;
    private DatatypeProperty lastAccessTime;
    private DatatypeProperty size;
    
    private DirectoryIndexer indexer;
    
    private final Map<String,ArrayList<String>> files;
    private final Map<String,String> writtenFiles;
    private final Map<String,String> locations;
    private final Map<String,String> things;
    private final Map<String,String> persons;
    private final Map<String,String> organisations;
    private final Set<String> concepts;
    
    private final Map<String,String> newFiles;
    private final Set<String> newFilesWithAuthors;
    private final Set<String> newFilesWithOrgs;
    
    private Map<String,ArrayList<String>> matches;
    private Map<String,ArrayList<String>> authorMatches;
    private Map<String,ArrayList<String>> orgMatches;
    
    private int currentLocationInstance;
    private int currentFileInstance;
    private int currentPersonInstance;
    private int currentOrganisationInstance;
    private int currentNameInstance;
    private int currentPersonNameInstance;
    private int currentThingInstance;
   
    
    public Ontology(String input,String out){
        output = out;
        file = new File(input);
        model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM); 
        try (InputStream in = FileManager.get().open(out)) {
            if (in == null)
            {
                throw new IllegalArgumentException( "File"+out+" not found");
            }
            model.read(in, "");
        }
        catch (IOException ex) {
            System.out.println(String.format("Error: %s", ex.getMessage()));
        }
        ns=model.getNsPrefixURI("");
        concepts = new HashSet<>();
        files = new HashMap<>();
        writtenFiles = new HashMap<>();
        newFiles = new HashMap<>();
        locations = new HashMap<>();
        things = new HashMap<>();
        persons = new HashMap<>();
        organisations = new HashMap<>();
        newFilesWithAuthors = new HashSet<>();
        newFilesWithOrgs = new HashSet<>();
    }
    
    private void getClassesAndProperties(){
        
        thingClass = model.getOntClass(ns+"Thing");
        personClass = model.getOntClass(ns+"Person");
        organisationClass = model.getOntClass(ns+"Organisation");
        locationClass = model.getOntClass(ns+"Location");
        nameClass = model.getOntClass(ns+"Name");
        personNameClass = model.getOntClass(ns+"Person_Name");
        fileClass = model.getOntClass(ns+"File");
        
        hasLocation = model.getObjectProperty(ns+"hasLocation");
        hasName = model.getObjectProperty(ns+"hasName");
        personName = model.getObjectProperty(ns+"personName");
        hasAuthor = model.getObjectProperty(ns+"hasAuthor");
        authors = model.getObjectProperty(ns+"Authors");
        relatedThing = model.getObjectProperty(ns+"relatedThing");
        relatedFile = model.getObjectProperty(ns+"relatedFile");
        
        path = model.getDatatypeProperty(ns+"path");
        name = model.getDatatypeProperty(ns+"name");
        creationTime = model.getDatatypeProperty(ns+"creatioTime");
        lastModificationTime = model.getDatatypeProperty(ns+"lastModificationTime");
        lastAccessTime = model.getDatatypeProperty(ns+"lastAccessTime");
        fileType = model.getDatatypeProperty(ns+"fileType");
        size = model.getDatatypeProperty(ns+"size");
    }
    
    public void readTriples(){
        
         StmtIterator iter=model.listStatements();
          while (iter.hasNext()){
              Statement stmt=iter.nextStatement();
              Resource subject=stmt.getSubject();
              Property predicate=stmt.getPredicate();
              RDFNode object=stmt.getObject();
              if(object.isLiteral() 
               && !(predicate.getNameSpace().equals(model.getNsPrefixURI("rdfs")))
               && predicate.getLocalName().equals("name") ){
                  Literal literalobject=object.asLiteral();
                  String concept=literalobject.getValue().toString();
                  concepts.add(concept);
                  StmtIterator iter1=model.listStatements();
                  while (iter1.hasNext()){
                      Statement stmt1=iter1.nextStatement();
                      Resource subject1=stmt1.getSubject();
                      Property predicate1=stmt1.getPredicate();
                      RDFNode object1=stmt1.getObject();
                      if(object1.isResource() && subject.getLocalName().equals(object1.asResource().getLocalName())
                      && predicate1.getLocalName().equals("personName") ){
                          persons.put(concept, subject1.getLocalName());
                      }
                      if(object1.isResource() && subject.getLocalName().equals(object1.asResource().getLocalName())
                      && predicate1.getLocalName().equals("hasName") ){
                          
                          if(subject1.getLocalName().startsWith("File_")){
                              StmtIterator iter2 = model.listStatements();
                              String path = "";
                              String extension = "";
                               while (iter2.hasNext()){
                                   
                                   Statement stmt2=iter2.nextStatement();
                                   Resource subject2=stmt2.getSubject();
                                   Property predicate2=stmt2.getPredicate();
                                   RDFNode object2=stmt2.getObject();
                                   if(subject2.getLocalName().equals(subject1.getLocalName()) && predicate2.getLocalName().equals("fileType")){
                                       extension = object2.asLiteral().getValue().toString();
                                   }
                                   if(subject2.getLocalName().equals(subject1.getLocalName()) && predicate2.getLocalName().equals("hasLocation")){
                                      StmtIterator iter3 = model.listStatements();
                                      while (iter3.hasNext()){
                                           Statement stmt3=iter3.nextStatement();
                                           Resource subject3=stmt3.getSubject();
                                           Property predicate3=stmt3.getPredicate();
                                           RDFNode object3=stmt3.getObject();
                                           if(subject3.getLocalName().equals(object2.asResource().getLocalName()) && predicate3.getLocalName().equals("path")){
                                              path = object3.asLiteral().getValue().toString();
                                           }
                                   
                                      } 
                                   }
                               }
                               ArrayList<String> list;
                               if(files.containsKey(concept)){
                                   list = files.get(concept);
                                   list.add(subject1.getLocalName());
                                   files.put(concept, list);
                               }
                               else{
                                   list = new ArrayList<>();
                                   list.add(subject1.getLocalName());
                                   files.put(concept, list);
                               }
                               writtenFiles.put(path+File.separator+concept+"."+extension, subject1.getLocalName());
                              
                          }
                          else if(subject1.getLocalName().startsWith("Thing_")){
                              things.put(concept, subject1.getLocalName());
                          }
                          else if(subject1.getLocalName().startsWith("Organisation_")){
                              organisations.put(concept, subject1.getLocalName());
                          }
                      }
                  }
                  
              }
              else if(object.isLiteral() 
               && !(predicate.getNameSpace().equals(model.getNsPrefixURI("rdfs")))
               && predicate.getLocalName().equals("path") ){
                  locations.put(object.asLiteral().getValue().toString(),subject.getLocalName());
              }
          }
        
    }
    
    public void compareConceptsAndFiles() throws IOException, FileNotFoundException, SAXException, TikaException, ParseException{
        matches = new HashMap<>();
        authorMatches = new HashMap<>();
        orgMatches = new HashMap<>();
        if(concepts.isEmpty()){
            System.out.println("The ontology you specified has no concpets for comparison.");
        }
        else{
            
            indexer = new DirectoryIndexer(file);
            for(String concept:concepts){
                ArrayList<String> authorfiles = indexer.searchAuthors(concept);
                if(!authorfiles.isEmpty()){
                   authorMatches.put(concept,authorfiles);
                } 
                ArrayList<String> organisationfiles = indexer.searchOrganisations(concept);
                if(!organisationfiles .isEmpty()){
                   orgMatches.put(concept,organisationfiles );
                }
                ArrayList<String> filenamefiles = indexer.searchFilenames(concept);
                if(!filenamefiles.isEmpty()){
                   matches.put(concept,filenamefiles);
                }
                ArrayList<String> contentfiles = indexer.searchContents(concept);
                if(!contentfiles.isEmpty()){
                   matches.put(concept,contentfiles);
                }
            }
            
        }
        
        
    }
    
    public void createFileIntances() throws IOException, ParseException{
        if(matches.isEmpty() && authorMatches.isEmpty() && orgMatches.isEmpty()){
            System.out.println("No matches were found between ontology concepts and files metadata.");
        }
        else{
            getClassesAndProperties();
            getCurrentInstances();
            if(!authorMatches.isEmpty()){
                addAuthorFiles();
            }
            if(!orgMatches.isEmpty()){
                addOrganisationFiles();
            }
            if(!matches.isEmpty()){
                addFiles();
            }
            addRemainingAuthorsAndOrgs();
            write();
        }
    }
    
    private void getCurrentInstances(){
        currentLocationInstance = 0;
        while(model.getIndividual(ns+"Location_"+currentLocationInstance) != null){
            currentLocationInstance++;
        }
        currentFileInstance = 0;
        while(model.getIndividual(ns+"File_"+currentFileInstance) != null){
            currentFileInstance++;
        }
        currentPersonInstance = 0;
        while(model.getIndividual(ns+"Person_"+currentPersonInstance) != null){
            currentPersonInstance++;
        }
        currentOrganisationInstance = 0;
        while(model.getIndividual(ns+"Organisation_"+currentOrganisationInstance) != null){
            currentOrganisationInstance++;
        }
        currentNameInstance = 0;
        while(model.getIndividual(ns+"Name_"+currentNameInstance) != null){
            currentNameInstance++;
        }
        currentPersonNameInstance = 0;
        while(model.getIndividual(ns+"Person_Name_"+currentPersonNameInstance) != null){
            currentPersonNameInstance++;
        }
        currentThingInstance = 0;
        while(model.getIndividual(ns+"Thing_"+currentThingInstance) != null){
            currentThingInstance++;
        }
    }
    
    private void addRemainingAuthorsAndOrgs(){
        for(Map.Entry pair:newFiles.entrySet()){
            String fileInstance = (String)pair.getValue();
            if(!newFilesWithAuthors.contains(fileInstance)){
                String author = indexer.getFileMeta().get((String)pair.getKey()).getAuthor();
                if(author!=null && !author.matches("^\\s*$") && !author.equals("")){
                    Individual authorIndividual = createPersonIndividual(author);
                    Individual fileIndividual = model.getIndividual(ns+fileInstance);
                    model.add(fileIndividual,hasAuthor,authorIndividual);
                    model.add(authorIndividual,authors,fileIndividual);
                }   
            }
            if(!newFilesWithOrgs.contains(fileInstance)){
                String organisation = indexer.getFileMeta().get((String)pair.getKey()).getCompany();
                if(organisation!=null && !organisation.matches("^\\s*$") && !organisation.equals("")){
                   Individual orgIndividual = createOrganisationIndividual(organisation);
                   Individual fileIndividual = model.getIndividual(ns+fileInstance);
                   model.add(fileIndividual,relatedThing,orgIndividual);
                   model.add(orgIndividual,relatedFile,fileIndividual); 
                }     
            }
        }
    }
    
    private void addAuthorFiles() throws IOException, ParseException{
        for(Map.Entry pair:authorMatches.entrySet()){
                String author = (String)pair.getKey();
                Individual authorIndividual;
                if(persons.containsKey(author)){
                    authorIndividual = model.getIndividual(ns+persons.get(author));
                }
                else{
                    authorIndividual = createPersonIndividual(author);
                    addOldThingRelations(author,authorIndividual);
                }
                ArrayList<String> authorfiles = (ArrayList<String>)pair.getValue();
                for(String file:authorfiles){
                    Individual fileIndividual;
                    if(writtenFiles.containsKey(file)){
                       fileIndividual = model.getIndividual(ns+writtenFiles.get(file));
                    }
                    else if(newFiles.containsKey(file)){
                        fileIndividual = model.getIndividual(ns+newFiles.get(file));
                    }
                    else{
                        String filename = indexer.getFileMeta().get(file).getFileName();
                        fileIndividual = createFileIndividual(filename,new FileAttributes(new File(file)));
                        newFiles.put(file, fileIndividual.getLocalName());
                    }
                    String filepath = indexer.getFileMeta().get(file).getFileParentPath();
                    Individual locationIndividual;
                    if(locations.containsKey(filepath)){
                        locationIndividual = model.getIndividual(ns+locations.get(filepath));
                    }
                    else{
                        locationIndividual = createLocationIndividual(filepath);
                        //locations.put(filepath, locationIndividual.getLocalName());
                    }
                    model.add(fileIndividual,hasLocation,locationIndividual);
                    model.add(locationIndividual,relatedFile,fileIndividual);
                    
                    model.add(fileIndividual,hasAuthor,authorIndividual);
                    model.add(authorIndividual,authors,fileIndividual);
                    newFilesWithAuthors.add(fileIndividual.getLocalName());
                    addOldAuthorFileRelations(author,fileIndividual);
                }
            }
    }
    
    private void addOldAuthorFileRelations(String concept,Individual fileIndividual){
        if(!organisations.isEmpty() && organisations.containsKey(concept)){
            Individual orgIndividual = model.getIndividual(ns+organisations.get(concept));
            model.add(fileIndividual,relatedThing,orgIndividual);
            model.add(orgIndividual,relatedFile,fileIndividual);
            
        }
        if(!things.isEmpty() && things.containsKey(concept)){
            Individual thingIndividual = model.getIndividual(ns+things.get(concept));
            model.add(fileIndividual,relatedThing,thingIndividual);
            model.add(thingIndividual,relatedFile,fileIndividual);   
        }
    }
    

    
    private void addOrganisationFiles() throws IOException, ParseException{
        for(Map.Entry pair:orgMatches.entrySet()){
                String organisation = (String)pair.getKey();
                Individual orgIndividual;
                if(organisations.containsKey(organisation)){
                    orgIndividual = model.getIndividual(ns+persons.get(organisation));
                }
                else{
                    orgIndividual = createOrganisationIndividual(organisation);
                    //connect new author individual with old file individuals
                    addOldThingRelations(organisation,orgIndividual);
                }
                ArrayList<String> orgfiles = (ArrayList<String>)pair.getValue();
                for(String file:orgfiles){
                    Individual fileIndividual;
                    if(writtenFiles.containsKey(file)){
                       fileIndividual = model.getIndividual(ns+writtenFiles.get(file));
                    }
                    else if(newFiles.containsKey(file)){
                        fileIndividual = model.getIndividual(ns+newFiles.get(file));
                    }
                    else{
                        String filename = indexer.getFileMeta().get(file).getFileName();
                        fileIndividual = createFileIndividual(filename,new FileAttributes(new File(file)));
                        newFiles.put(file, fileIndividual.getLocalName());
                    }
                    String filepath = indexer.getFileMeta().get(file).getFileParentPath();
                    Individual locationIndividual;
                    if(locations.containsKey(filepath)){
                        locationIndividual = model.getIndividual(ns+locations.get(filepath));
                    }
                    else{
                        locationIndividual = createLocationIndividual(filepath);
                    }
                    model.add(fileIndividual,hasLocation,locationIndividual);
                    model.add(locationIndividual,relatedFile,fileIndividual);
                    
                    model.add(fileIndividual,relatedThing,orgIndividual);
                    model.add(orgIndividual,relatedFile,fileIndividual);
                    newFilesWithOrgs.add(fileIndividual.getLocalName());
                    addOldOrgFileRelations(organisation,fileIndividual);
                    
                }
            }
    }
    
    private void addOldOrgFileRelations(String concept,Individual fileIndividual){
        if(!persons.isEmpty() && persons.containsKey(concept)){
            Individual personIndividual = model.getIndividual(ns+persons.get(concept));
            model.add(fileIndividual,relatedThing,personIndividual);
            model.add(personIndividual,relatedFile,fileIndividual);      
        }
        if(!things.isEmpty() && things.containsKey(concept)){
             Individual thingIndividual = model.getIndividual(ns+things.get(concept));
             model.add(fileIndividual,relatedThing,thingIndividual);
             model.add(thingIndividual,relatedFile,fileIndividual);   
        }
    }
    
    private void addFiles() throws IOException{
        for(Map.Entry pair:matches.entrySet()){
            String concept = (String)pair.getKey();
            ArrayList<Individual> relatedThings = new ArrayList<>();
            if(persons.containsKey(concept)){
                relatedThings.add(model.getIndividual(ns+persons.get(concept)));        
            }
            if(organisations.containsKey(concept)){
                relatedThings.add(model.getIndividual(ns+organisations.get(concept)));
            }
            if(things.containsKey(concept)){
                relatedThings.add(model.getIndividual(ns+things.get(concept)));
            }
            if(relatedThings.isEmpty()){
                Individual thingIndividual = createThingIndividual(concept);
                relatedThings.add(thingIndividual);
                //things.put(concept,thingIndividual.getLocalName());
                addOldThingRelations(concept,thingIndividual);
            }
            ArrayList<String> files = (ArrayList<String>)pair.getValue();
            for(String file:files){
                Individual fileIndividual;
                    if(writtenFiles.containsKey(file)){
                       fileIndividual = model.getIndividual(ns+writtenFiles.get(file));
                    }
                    else if(newFiles.containsKey(file)){
                        fileIndividual = model.getIndividual(ns+newFiles.get(file));
                    }
                    else{
                       //fileIndividual = createFileIndividual();
                        String filename = indexer.getFileMeta().get(file).getFileName();
                        fileIndividual = createFileIndividual(filename,new FileAttributes(new File(file)));
                        newFiles.put(file, fileIndividual.getLocalName());
                    }
                    String filepath = indexer.getFileMeta().get(file).getFileParentPath();
                    Individual locationIndividual;
                    if(locations.containsKey(filepath)){
                        locationIndividual = model.getIndividual(ns+locations.get(filepath));
                    }
                    else{
                        locationIndividual = createLocationIndividual(filepath);
                        //locations.put(filepath, locationIndividual.getLocalName());
                    }
                    model.add(fileIndividual,hasLocation,locationIndividual);
                    model.add(locationIndividual,relatedFile,fileIndividual);
                    
                    for(Individual thing:relatedThings){
                        model.add(fileIndividual,relatedThing,thing);
                        model.add(thing,relatedFile,fileIndividual);
                    }
            }
                    
        }
    }
    
    private void addOldThingRelations(String concept,Individual thingIndividual){
        if(!files.isEmpty()){
            ArrayList<String> list = files.get(concept);
            for(String file:list){
                Individual fileIndividual = model.getIndividual(ns+file);
                model.add(fileIndividual,relatedThing,thingIndividual);
                model.add(thingIndividual,relatedFile,fileIndividual);
            }
        }      
    }
    
    private Individual createNameIndividual(String name){
        String instance="Name_"+currentNameInstance;
        currentNameInstance++;
        Individual nameIndividual = model.createIndividual(ns+instance,nameClass);
        nameIndividual.addLiteral(this.name,name);
        return nameIndividual;
    }
    
    private Individual createPersonNameIndividual(String personName){
        String instance="Person_Name_"+currentPersonNameInstance;
        currentPersonNameInstance++;    
        Individual personNameIndividual = model.createIndividual(ns+instance,personNameClass);
        personNameIndividual.addLiteral(name,personName);
        return personNameIndividual;
    }
    
    private Individual createFileIndividual(String filename,FileAttributes attributes){
        String instance="File_"+currentFileInstance;
        currentFileInstance++;
        
        Individual nameIndividual = createNameIndividual(filename);
        
        Individual fileIndividual = model.createIndividual(ns+instance,fileClass);
        fileIndividual.addLiteral(fileType,attributes.getType());
        fileIndividual.addLiteral(creationTime,attributes.getCreationTime());
        fileIndividual.addLiteral(lastModificationTime,attributes.getModificationTime());
        fileIndividual.addLiteral(lastAccessTime,attributes.getAccessTime());
        fileIndividual.addLiteral(size,attributes.getSize());
        
        model.add(fileIndividual,hasName,nameIndividual);
        
        return fileIndividual;
    }
    
    private Individual createLocationIndividual(String path){
        
        String instance = "Location_"+currentLocationInstance;
        locations.put(path,instance);
        Individual locationIndividual = model.createIndividual(ns+instance,locationClass);     
        locationIndividual.addLiteral(this.path, path);    
        currentLocationInstance++;
        return locationIndividual;
    }
    
    private Individual createPersonIndividual(String personName){
        String instance = "Person_"+currentPersonInstance;
        persons.put(personName,instance);
        Individual personIndividual = model.createIndividual(ns+instance,personClass);
        Individual personNameIndividual = createPersonNameIndividual(personName);
        model.add(personIndividual,this.personName,personNameIndividual);
        currentPersonInstance++;
        return personIndividual;
    }
    
    private Individual createOrganisationIndividual(String companyName){
        String instance = "Organisation_"+currentOrganisationInstance;
        organisations.put(companyName,instance);
        Individual orgIndividual = model.createIndividual(ns+instance,organisationClass);
        Individual nameIndividual = createNameIndividual(companyName);
        model.add(orgIndividual,hasName,nameIndividual);
        currentOrganisationInstance++;
        return orgIndividual;
    }
    
    private Individual createThingIndividual(String thingName){
        String instance = "Thing_"+currentThingInstance;
        things.put(thingName,instance);
        Individual thingIndividual = model.createIndividual(ns+instance,thingClass);
        Individual nameIndividual = createNameIndividual(thingName);
        model.add(thingIndividual,hasName,nameIndividual);
        currentThingInstance++;
        return thingIndividual;
    }
    
    public void write()throws FileNotFoundException, IOException{
        FileWriter out = null;
        try {
            out = new FileWriter(output);
            model.writeAll(out, "RDF/XML-ABBREV", null);
        } 
        finally {
            if (out != null) {
                  try {out.close();} catch (IOException ignore) {}
            }
        }
    }
}
