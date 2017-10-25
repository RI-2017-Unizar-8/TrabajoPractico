package lucene;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.document.DoublePoint;


/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
  
  private IndexFiles() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        try {

          // make a new, empty document
          Document doc = new Document();

          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize 
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);

          // Add the last modified date of the file a field named "modified".
          // Use a LongField that is indexed (i.e. efficiently filterable with
          // NumericRangeFilter).  This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          //doc.add(new LongPoint("modified", file.lastModified()));  
          Field dateField = new StringField("modified", "" + file.lastModified(), Field.Store.YES);
          doc.add(dateField);
          
          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          //doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8"))));
          
          try {
	         
        	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	          DocumentBuilder builder = factory.newDocumentBuilder();
	          
	          org.w3c.dom.Document document = builder.parse(file.getPath());
	          
	          NodeList list = document.getElementsByTagName("csw:Record");
	          Node parent = list.item(0);
	          
	          Element nodo = (Element) parent;
	          
	          /* PRACTICA 2 */
	          
	          NodeList listaGeo = nodo.getElementsByTagName("ows:BoundingBox");
	          if(listaGeo != null && listaGeo.getLength() > 0){
	        	  Node parentGeo = listaGeo.item(0);
		          Element nodoGeo = (Element) parentGeo;  
		          
		          NodeList upperCorner = nodoGeo.getElementsByTagName("ows:UpperCorner");
		          NodeList lowerCorner = nodoGeo.getElementsByTagName("ows:LowerCorner");
		          
		          Element uC = (Element) upperCorner.item(0);
		          Element lC = (Element) lowerCorner.item(0);
		          
		          String uCS_i = uC.getTextContent().substring(0,uC.getTextContent().indexOf(" ")).trim();
		          String lCS_i = lC.getTextContent().substring(0,lC.getTextContent().indexOf(" ")).trim();
		          String uCS_d = uC.getTextContent().substring(uC.getTextContent().indexOf(" ")).trim();
		          String lCS_d = lC.getTextContent().substring(lC.getTextContent().indexOf(" ")).trim();
		          
		          Field westField = new StringField("west", uCS_i, Field.Store.YES);
		          Field eastField = new StringField("east", lCS_i, Field.Store.YES);
		          Field northField = new StringField("north", uCS_d, Field.Store.YES);
		          Field southField = new StringField("south", lCS_d, Field.Store.YES);
		          
		         
		          doc.add(westField);
		          doc.add(eastField);
		          doc.add(northField);
		          doc.add(southField);	

	          }

	          
	         
	          /* FIN DE PRACTICA 2 */           
	          
	          TextField title = null;
	          StringField identifier = null;
	          TextField subject = null;
	          StringField type =  null;
	          TextField description = null;
	          TextField creator = null;
	          TextField publisher = null;
	          StringField format = null;
	          StringField language = null;
	          
	          /* Campos a añadir */
	          if(nodo.getElementsByTagName("dc:title") != null && nodo.getElementsByTagName("dc:title").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:title").getLength(); ++i){
		        	  title = new TextField("title", nodo.getElementsByTagName("dc:title").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(title);
	        	  }
	          
	          if(nodo.getElementsByTagName("dc:identifier") != null && nodo.getElementsByTagName("dc:identifier").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:identifier").getLength(); ++i){
		        	  identifier = new StringField("identifier", nodo.getElementsByTagName("dc:identifier").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(identifier);
	        	  }	
	          
	          if(nodo.getElementsByTagName("dc:subject") != null && nodo.getElementsByTagName("dc:subject").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:subject").getLength(); ++i){
		        	  subject = new TextField("subject", nodo.getElementsByTagName("dc:subject").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(subject);
	        	  }
	          
	          if(nodo.getElementsByTagName("dc:type") != null && nodo.getElementsByTagName("dc:type").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:type").getLength(); ++i){
		        	  type = new StringField("type", nodo.getElementsByTagName("dc:type").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(type);
	        	  }	
	          
	          if(nodo.getElementsByTagName("dc:description") != null && nodo.getElementsByTagName("dc:description").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:description").getLength(); ++i){
		        	  description = new TextField("description", nodo.getElementsByTagName("dc:description").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(description);
	        	  }	  
	          
	          if(nodo.getElementsByTagName("dc:creator") != null && nodo.getElementsByTagName("dc:creator").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:creator").getLength(); ++i){
		        	  creator = new TextField("creator", nodo.getElementsByTagName("dc:creator").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(creator);
	        	  }	 
	          
	          if(nodo.getElementsByTagName("dc:publisher") != null && nodo.getElementsByTagName("dc:publisher").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:publisher").getLength(); ++i){
		        	  publisher = new TextField("publisher", nodo.getElementsByTagName("dc:publisher").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(publisher);
	        	  }	
	          
	          if(nodo.getElementsByTagName("dc:format") != null && nodo.getElementsByTagName("dc:format").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:format").getLength(); ++i){
		        	  format = new StringField("format", nodo.getElementsByTagName("dc:format").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(format);
	        	  }	
	          
	          if(nodo.getElementsByTagName("dc:language") != null && nodo.getElementsByTagName("dc:language").getLength()>0)
	        	  for(int i = 0; i<nodo.getElementsByTagName("dc:language").getLength(); ++i){
		        	  language = new StringField("language", nodo.getElementsByTagName("dc:language").item(i).getTextContent(), Field.Store.YES);
		        	  doc.add(language);
	        	  }	                  

	          
          } catch (Exception e){
        	  System.out.println("ERR" + e.getMessage());
          }
          
          
          
          
          
          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.getPath()), doc);
          }
          
        } finally {
          fis.close();
        }
      }
    }
  }
}