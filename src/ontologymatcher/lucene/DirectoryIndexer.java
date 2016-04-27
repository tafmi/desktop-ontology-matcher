
package ontologymatcher.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import ontologymatcher.file.FileMetadata;
import ontologymatcher.utils.Utils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;


public final class DirectoryIndexer {
    
    private final Directory directory;
    private final IndexWriter iwriter;
    private final Map<String,FileMetadata> fileMeta;
    
    public DirectoryIndexer(File file) throws IOException, FileNotFoundException, SAXException, TikaException{
        
        directory= new RAMDirectory();
        EnglishAnalyzer analyzer=new EnglishAnalyzer();
        IndexWriterConfig config=new IndexWriterConfig(analyzer);
        iwriter = new IndexWriter(directory,config);
        fileMeta =new HashMap<>();
        indexDirectory(file);
        iwriter.close();
        tfidf();
    }
    
    
    public void indexDirectory(File path) throws IOException, FileNotFoundException, SAXException, TikaException{
        if(path.isDirectory()){
           if( path.list()!=null){
              File[] files=path.listFiles();
              for (File file : files){
                indexDirectory(file); 
              }
           }
           else{
               return;
           }
       }
       else{    
           indexFile(path);
       }
    }
    
    private void indexFile(File file) throws IOException, FileNotFoundException, SAXException, TikaException{
        
        if(file.isHidden() || !file.canRead() || !file.exists()){
           return;
        }
        FileMetadata meta=new FileMetadata(file);
        String path = file.getCanonicalPath();
        fileMeta.put(path, meta);
         Document document=new Document();
         document.add(new Field("path",path,TextField.TYPE_STORED));
         document.add(new Field("filename",meta.getFileName(),TextField.TYPE_STORED));
         String author=meta.getAuthor();
         if(author!=null && !author.matches("^\\s*$") && !author.equals("")){
             document.add(new Field("author",author,TextField.TYPE_STORED));
         }
         String organisation=meta.getCompany();
         if(organisation!=null && !organisation.matches("^\\s*$") && !organisation.equals("")){
             document.add(new Field("organisation",organisation,TextField.TYPE_STORED));
         }
         if(Utils.isDocument(file)){
            FieldType fieldType = new FieldType();
            fieldType.setStoreTermVectors(true);
            fieldType.setStoreTermVectorPositions(true);
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            fieldType.setStored(true);
            String contents=meta.getContents();
            if(contents!=null){                
                     document.add(new Field("contents",contents,fieldType));      
            }
         }
         iwriter.addDocument(document);
    }
    
    private void tfidf() throws IOException{
        try (DirectoryReader ireader = DirectoryReader.open(directory)) {
            IndexSearcher isearcher=new IndexSearcher(ireader);
            for(int i=0;i<ireader.maxDoc();i++){
                int j=0;
                float totalTfIdf=0;
                float maxTfIdf=0;
                HashMap<String,Float> contents=new HashMap<>();
                Terms terms = ireader.getTermVector(i,"contents");
                if (terms != null && terms.size() > 0){
                    TFIDFSimilarity tfidfSIM = new DefaultSimilarity();
                    TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
                    BytesRef term = null;
                    while ((term = termsEnum.next()) != null) {
                        DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
                        int docIdEnum;
                        while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                            org.apache.lucene.index.Term termInstance = new org.apache.lucene.index.Term("contents", term);
                            long indexDf = ireader.docFreq(termInstance);
                            float tf = tfidfSIM.tf(docsEnum.freq());
                            float idf = tfidfSIM.idf(indexDf, ireader.getDocCount("contents"));
                            float tfidf=tf*idf;
                            if(tfidf>maxTfIdf){
                                maxTfIdf=tfidf;
                            }
                            totalTfIdf=totalTfIdf+tfidf;
                            j++;
                            contents.put(term.utf8ToString(), tfidf);
                        }
                    }
                }
                String hightfidfwords="";
                if(j>0){
                    float mTfIdf=totalTfIdf/j;
                    float limit=(mTfIdf+maxTfIdf)/2;
                    for (Map.Entry pair : contents.entrySet()) {
                        if((Float)pair.getValue()>limit){
                            hightfidfwords+=" "+(String)pair.getKey();
                        }
                    }
                }
                Document doc=new Document();
                doc.add(new Field("path",isearcher.doc(i).get("path"),TextField.TYPE_STORED));
                doc.add(new Field("hightfidfcontents",hightfidfwords,TextField.TYPE_STORED));
                IndexWriterConfig config=new IndexWriterConfig(new EnglishAnalyzer());
                try (IndexWriter writer = new IndexWriter(directory,config)) {
                    writer.addDocument(doc);
                }
            }
        }
     }

    
    public ArrayList<String> searchFilenames(String term) throws IOException, ParseException{
        ArrayList<String> results = new ArrayList<>();
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(ireader);
        PhraseQuery query = new PhraseQuery();
        query.setSlop(0);
        String[] splitterm=term.split("\\s+");
        for(String word:splitterm){
            query.add(new Term("filename",EnglishStemmer(word)));
        }
        
        TopDocs topdocs=searcher.search(query,null,10000);
        ScoreDoc[] hits=topdocs.scoreDocs;
        for (ScoreDoc hit : hits) {
                int docID = hit.doc;
                Document doc=searcher.doc(docID);
                results.add(doc.get("path"));
        }
        return results;
    }
    
    public ArrayList<String> searchOrganisations(String term) throws IOException{
        ArrayList<String> results = new ArrayList<>();
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(ireader);
        PhraseQuery query = new PhraseQuery();
        query.setSlop(0);
        String[] splitterm=term.split("\\s+");
        for(String word:splitterm){
            query.add(new Term("organisation",EnglishStemmer(word)));
        }
        
        TopDocs topdocs=searcher.search(query,null,10000);
        ScoreDoc[] hits=topdocs.scoreDocs;
        for (ScoreDoc hit : hits) {
                int docID = hit.doc;
                Document doc=searcher.doc(docID);
                results.add(doc.get("path"));
        }
        return results;
    }
    
   
    public ArrayList<String> searchAuthors(String term) throws IOException, ParseException{
        ArrayList<String> results = new ArrayList<>();
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(ireader);
        PhraseQuery query = new PhraseQuery();
        query.setSlop(0);
        String[] splitterm=term.split("\\s+");
        for(String word:splitterm){
            query.add(new Term("author",EnglishStemmer(word)));
        }
        TopDocs topdocs=searcher.search(query,null,10000);
        ScoreDoc[] hits=topdocs.scoreDocs;
        for (ScoreDoc hit : hits) {
                int docID = hit.doc;
                Document doc=searcher.doc(docID);
                results.add(doc.get("path"));
        } 
        
        return results;
    }
    
    public ArrayList<String> searchContents(String term) throws IOException, ParseException{
        ArrayList<String> results = new ArrayList<>();
        ArrayList<String> tfidfresults = new ArrayList<>();
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(ireader);
        String[] splitterm=term.split("\\s+");
         for(String split:splitterm){
            QueryParser tfidfparser=new QueryParser("hightfidfcontents",new EnglishAnalyzer());
            Query query=tfidfparser.parse(QueryParser.escape(split));
            TopDocs topdocs=searcher.search(query,null,10000);
            ScoreDoc[] hits=topdocs.scoreDocs;
            for (ScoreDoc hit : hits) {
                   int docID = hit.doc;
                   Document doc=searcher.doc(docID);
                   tfidfresults.add(doc.get("path"));
            } 
         }
         ArrayList<String> cresults=new ArrayList<>();
         PhraseQuery query = new PhraseQuery();
         query.setSlop(0);
         for(String word:splitterm){
             query.add(new Term("contents",EnglishStemmer(word)));
         }
         TopDocs topdocs=searcher.search(query,null,10000);
         ScoreDoc[] hits=topdocs.scoreDocs;
         for (ScoreDoc hit : hits) {
                int docID = hit.doc;
                Document doc=searcher.doc(docID);
                cresults.add(doc.get("path"));
         }
         for(String res:cresults){
             if(tfidfresults.contains(res)){
                 results.add(res);
             }
         }
        
        return results;
    }
    
    private String EnglishStemmer(String term) throws IOException {
       String sTerm="";  
       try (EnglishAnalyzer analyzer = new EnglishAnalyzer()) {
            TokenStream ts = analyzer.tokenStream("fieldName", new StringReader(term));
            ts.reset();
            while (ts.incrementToken()){
                CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);
                sTerm+=ca.toString()+" ";
            }
            analyzer.close();
        }
        if(sTerm.equals("")){
            sTerm=term;
        }
        return sTerm.replaceAll("\\s+$", "");
   }

    public Map<String, FileMetadata> getFileMeta() {
        return fileMeta;
    }
    
    
    
}
