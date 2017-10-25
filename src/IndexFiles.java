
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
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


public class IndexFiles {
  
  private IndexFiles() {}

  /** Indexa los ficheros de un directorio **/
  
  public static void main(String[] args) {
    String usage = "java IndexFiles -index <indexPath> -docs <docsPath>";
    String indexPath = "index";
    String docsPath = null;
    
    /* Comprobamos los argumentos */
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      }
    }

    /* Comprobamos que se ha introducido bien el directorio de la colección */
    if (docsPath == null) {
      System.err.println("ERROR en el path de los documentos.\nModo de uso: " + usage);
      System.exit(1);
    }

    /* Comprobamos que el directorio introducido es correcto */
    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("El directorio (path) de documentos '" +docDir.getAbsolutePath()+ "' no existe o no puede ser leido.");
      System.exit(1);
    }
    
    /* Los datos de entrada son correctos */
    Date start = new Date();
    try {
      System.out.println("Indexando documentos en '" + indexPath + "'...");

      /* Creamos el Directorio en FS (File System), es decir, en Disco) */
      /* Se prefiere en Disco frente a RAM */
      Directory dir = FSDirectory.open(Paths.get(indexPath));
      
      /* Se elige un analizador español */
      Analyzer analyzer = new SpanishAnalyzer();
      
      /* Creamos la instancia de configuración del escritor de índice */
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      /* Permitimos CREATE_OR_APPEND porque los índices están en disco */
      /* Es poco probable que deba liberarse memoria o modificar un índice */
      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

      /* Creamos el escritor en sí */
      IndexWriter writer = new IndexWriter(dir, iwc);
      
      /* Llamamos a la función encargada de ello */
      indexDocs(writer, docDir);

      writer.close();
      
      System.out.println("Completado.");

      /* Mostramos el tiempo empleado */
      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println("Lanzada una excepción tipo: " + e.getClass() +
       "\nMensaje obtenido: " + e.getMessage());
    }
  }

  /** Dado el Indexador y el Directorio, indexa los documentos **/
  
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {

	/* Comprobamos que el fichero es leíble */
    if (file.canRead()) {
    	/* Comprobamos si el fichero es, además, un directorio */
    	if (file.isDirectory()) {
    		/* Recuperamos una lista de los ficheros */
    		String[] files = file.list();
    		if (files != null) {
    			for (int i = 0; i < files.length; i++) {
    				/* Recursividad fichero a fichero */
    				indexDocs(writer, new File(file, files[i]));
    			}
    		}
    	/* Si es archivo, hay que indexarlo */
    	} else {
	        FileInputStream fis;
	        /* Intentamos abrir el fichero */
	        try {
	        	fis = new FileInputStream(file);
	        } catch (FileNotFoundException fnfe) {
	        	return;
	        }
	
	        try {
	
		          /* Creamos un nuevo documento */
		          Document doc = new Document();
		
		          /* Guardamos la ruta del fichero */
		          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
		          doc.add(pathField);
		
		          /* Entramos en el contenido de los ficheros */
		          try {
			         
		        	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			          DocumentBuilder builder = factory.newDocumentBuilder();
			          
			          org.w3c.dom.Document document = builder.parse(file.getPath());
			          
			          /* La raiz de los archivos de RI */
			          NodeList list = document.getElementsByTagName("oai_dc:dc");
			          
			          /* Cada archivo tiene un elemento de este tipo */
			          Node parent = list.item(0);
			          Element nodo = (Element) parent;
			          
			          /* Campos que queremos realmente */
			          TextField title = null;
			          TextField subject = null;
			          TextField description = null;
			          
			          /* Campo de titulo */
			          if(nodo.getElementsByTagName("dc:title") != null && nodo.getElementsByTagName("dc:title").getLength()>0){
			        	  for(int i = 0; i<nodo.getElementsByTagName("dc:title").getLength(); ++i){
				        	  title = new TextField("title", nodo.getElementsByTagName("dc:title").item(i).getTextContent(), Field.Store.YES);
				        	  doc.add(title);
			        	  }
			          }
			          
			          /* Campo de subject */
			          if(nodo.getElementsByTagName("dc:subject") != null && nodo.getElementsByTagName("dc:subject").getLength()>0){
			        	  for(int i = 0; i<nodo.getElementsByTagName("dc:subject").getLength(); ++i){
			        		  subject = new TextField("subject", nodo.getElementsByTagName("dc:subject").item(i).getTextContent(), Field.Store.YES);
				        	  doc.add(subject);
			        	  }
			          }
			          
			          /* Campo de descripción */
			          if(nodo.getElementsByTagName("dc:description") != null && nodo.getElementsByTagName("dc:description").getLength()>0){
			        	  for(int i = 0; i<nodo.getElementsByTagName("dc:description").getLength(); ++i){
			        		  description = new TextField("description", nodo.getElementsByTagName("dc:description").item(i).getTextContent(), Field.Store.YES);
				        	  doc.add(description);
			        	  }
			          }
			          
			          
		          } catch (Exception e){
		        	  System.err.println("Error en la indexación: " + e.getMessage());
		          }

	            writer.updateDocument(new Term("path", file.getPath()), doc);

		        } finally {
		        	fis.close();
		        }
    		}
    	}
  	}
}