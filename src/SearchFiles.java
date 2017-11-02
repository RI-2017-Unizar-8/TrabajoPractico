import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;

/**
 * 
 * @author Daniel Martín - 702858
 * @author Pablo Luesia - 698387
 * @date 25/10/17
 * 
 * Clase empleada para llevar a cabo la búsqueda
 *
 */

public class SearchFiles {
	
	private static List<Double> finalScore;
	private static List<Integer> finalIDs;

  private SearchFiles() {}

  /** Busca en los ficheros. */
  public static void main(String[] args) throws Exception {
    String usage = "java SearchFiles -index <indexPath> -infoNeeds <infoNeedsFile> -output <resultsFile>";
    
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String infoNeeds = "";
    String output = "";
    
    for(int i = 0;i < args.length;i++) {
    	if ("-index".equals(args[i])) {
    		index = args[i+1];
    		i++;
    	} else if ("-infoNeeds".equals(args[i])) {
    	  infoNeeds = args[i+1];
    	  i++;
    	} else if ("-output".equals(args[i])) {
    	  output = args[i+1];
    	  i++;
    	} 
    }
    
    /* Creamos el lector de indices. Recordamos que se uso en disco (FS) y no en RAM */
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    
    /* Usamos el analizador español */
    Analyzer analyzer = new SpanishAnalyzer();

    File file = new java.io.File(infoNeeds);
	BufferedReader readerFile = null;
	/* Preparamos el fichero para código */
    try
    {
    	readerFile = new BufferedReader(new FileReader(file));
	} catch (Exception ex) {
	  	System.out.println("Fichero inexistente.");
	}
    
    /* Preparamos el flujo de salida para los resultados */
    FileWriter fw = new FileWriter(output);
    PrintWriter writer = new PrintWriter(fw);
    

    /* Habrá que hacer 3 querys */
    /* Una para titulo, otra para subject y otra para descriptcion */
    /* La explicación se incluye en la documentación de la práctica */
    
    /* Recuperamos los datos de las necesidades, en  un formato manejable */
    ParserXML parserXML = new ParserXML();
    parserXML.parse(readerFile);
    List<InformationNeed> necesidades = parserXML.getInformationNeed();
    
    System.out.println("Mostrando necesidades introducidas: ");
    
    if(necesidades != null){
    	
    	/* Recorremos cada necesidad */
	    for(InformationNeed i : necesidades){
	    	
	    	System.out.print("\nID: " + i.getId() + "\nDES: " + i.getNeed() + "\n\n");
	        
	    	/* preparamos los contenedores de información finales */
	        finalScore = new ArrayList<Double>();
	        finalIDs = new ArrayList<Integer>();

	   
	        /* QUERY 1 - Sobre el titulo */
	        QueryParser parser = new QueryParser("title", analyzer);
	    	String texto = parser.parse(i.getNeed()).toString();
	        Query query = parser.parse(texto);
	    	  	
	    	BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
	    	
	    	/* Consulta normal */
	    	booleanQuery.add(query, BooleanClause.Occur.MUST);   	
	    	
	    	/* Restriccion de la fecha, si la hay */
	    	if(i.getHayFecha()){
	    		
		    	double anyoIni = 1900.0, anyoFin = 2020.0;
		    	
	    		if(i.getAnyoIni() != 0){
	    			anyoIni = (double) i.getAnyoIni();
	    		} 
	    		if(i.getAnyoFin() != 0){
	    			anyoFin = (double) i.getAnyoFin();
	    		} 

	    		Query dateQuery = DoublePoint.newRangeQuery("date",anyoIni,anyoFin);
	    		
	    		if(i.getFechaPreferible() == true)
	    			booleanQuery.add(dateQuery, BooleanClause.Occur.SHOULD);  
	    		else
	    			booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);  
	    		
	    	}
	    	
	    	/* Restriccion de los autores, si los hay */
	    	if(i.getHayAutores()){
	    		
	    		QueryParser autorParser = new QueryParser("creator", analyzer);
		        String autorTexto = "";
		        for(String s : i.getAutores()){
		        	autorTexto += s + " ";
		        }
	    		Query autorQuery = autorParser.parse(autorTexto);	
	    		System.out.println(autorQuery.toString());
		    	booleanQuery.add(autorQuery, BooleanClause.Occur.MUST);   	
	    		
	    	}
	    	
	    	BooleanQuery finalQuery = booleanQuery.build();
	    	
	    	System.out.println("[INF-NEED " + i.getId() + "]: " + "Searching for: " + finalQuery.toString() + ".\n");
	    	
	    	//Date start = new Date();
	        searcher.search(finalQuery, 100);
	        //Date end = new Date();
	        
	        //System.out.println("\tTime: "+(end.getTime()-start.getTime())+" ms\n.");
	        
	        /* Guardamos los resultados sobre titulo */
	        ScoreDoc[] titleHits = doPagingSearch(i, searcher, finalQuery); 
	        
	        
	        
	        /* QUERY 2 - Sobre el subject */
	        parser = new QueryParser("subject", analyzer);
	    	texto = parser.parse(i.getNeed()).toString();
	        query = parser.parse(texto);
	    	  	
	    	booleanQuery = new BooleanQuery.Builder();
	    	
	    	/* Consulta normal */
	    	booleanQuery.add(query, BooleanClause.Occur.MUST);   	
	    	
	    	/* Restriccion de la fecha, si la hay */
	    	if(i.getHayFecha()){
	    		
		    	int anyoIni = 1900, anyoFin = 2020;
		    	
	    		if(i.getAnyoIni() != 0){
	    			anyoIni = i.getAnyoIni();
	    		} 
	    		if(i.getAnyoFin() != 0){
	    			anyoFin = i.getAnyoFin();
	    		} 

	    		Query dateQuery = DoublePoint.newRangeQuery("date",anyoIni,anyoFin);

	    		if(i.getFechaPreferible() == true)
	    			booleanQuery.add(dateQuery, BooleanClause.Occur.SHOULD);  
	    		else
	    			booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);   
	    		
	    	}
	    	
	    	/* Restriccion de los autores, si los hay */
	    	if(i.getHayAutores()){
	    		
	    		QueryParser autorParser = new QueryParser("creator", analyzer);
		        String autorTexto = "";
		        for(String s : i.getAutores()){
		        	autorTexto += s + " ";
		        }
	    		Query autorQuery = autorParser.parse(autorTexto);	
	    		System.out.println(autorQuery.toString());
		    	booleanQuery.add(autorQuery, BooleanClause.Occur.MUST);   	
	    		
	    	}
	    	
	    	finalQuery = booleanQuery.build();
	    	
	    	//start = new Date();
	        searcher.search(finalQuery, 100);
	        //end = new Date();
	        
	        //System.out.println("Time: "+(end.getTime()-start.getTime())+" ms");
	        
	        /* Guardamos los resultados sobre subject */
	        ScoreDoc[] subjectHits = doPagingSearch(i, searcher, finalQuery);
	        
	        
	        
	        /* QUERY 3 - Sobre el description */
	        parser = new QueryParser("description", analyzer);
	    	texto = parser.parse(i.getNeed()).toString();
	        query = parser.parse(texto);
	    	  	
	    	booleanQuery = new BooleanQuery.Builder();
	    	
	    	/* Consulta normal */
	    	booleanQuery.add(query, BooleanClause.Occur.MUST);   	
	    	
	    	/* Restriccion de la fecha, si la hay */
	    	if(i.getHayFecha()){
	    		
		    	int anyoIni = 1900, anyoFin = 2020;
		    	
	    		if(i.getAnyoIni() != 0){
	    			anyoIni = i.getAnyoIni();
	    		} 
	    		if(i.getAnyoFin() != 0){
	    			anyoFin = i.getAnyoFin();
	    		} 

	    		Query dateQuery = DoublePoint.newRangeQuery("date",anyoIni,anyoFin);

	    		if(i.getFechaPreferible() == true)
	    			booleanQuery.add(dateQuery, BooleanClause.Occur.SHOULD);  
	    		else
	    			booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);  
	    		
	    	}
	    	
	    	/* Restriccion de los autores, si los hay */
	    	if(i.getHayAutores()){
	    		
	    		QueryParser autorParser = new QueryParser("creator", analyzer);
		        String autorTexto = "";
		        for(String s : i.getAutores()){
		        	autorTexto += s + " ";
		        }
	    		Query autorQuery = autorParser.parse(autorTexto);	
	    		System.out.println(autorQuery.toString());
		    	booleanQuery.add(autorQuery, BooleanClause.Occur.MUST);   	
	    		
	    	}
	    	
	    	finalQuery = booleanQuery.build();
	    	
	    	//start = new Date();
	        searcher.search(finalQuery, 100);
	        //end = new Date();
	        
	        //System.out.println("Time: "+(end.getTime()-start.getTime())+" ms");
	        
	        /* Guardamos los resultados sobre descripcion */
	        ScoreDoc[] descriptionHits = doPagingSearch(i, searcher, finalQuery);
	        
	        /* Tras obtener el score en cada parte, aplicamos un ratio */
	        /* Suponemos 3 tipos de prioridades:
	         * 		- El score del titulo vale x1
	         * 		- El score del subject vale x0.5
	         * 		- El score del description vale x0.25
	         */

	         /* Añadimos el Score de titulo */
	        
	        for(ScoreDoc h : titleHits){  	
	        	int id = h.doc;
	        	finalIDs.add(id);
	        	finalScore.add((double) h.score);
	        }
	        
	        /* Añadimos el Score de subject pero x0.5 */
	        
	        for(ScoreDoc h : subjectHits){
	        	int id = h.doc;
	        	boolean already = false;
	        	for(int it=0;it<finalIDs.size();it++){
	        		/* Si ya estaba, incrementamos el score */
	        		if(finalIDs.get(it) == id){
	        			finalScore.set(it, finalScore.get(it) + (double)h.score * 0.5);
	        			already = true;
	        			break;
	        		}
	        	}
	        	/* Si ese documento aun no estaba, lo anyadimos */
	        	if(!already){
	        		finalIDs.add(id);
		        	finalScore.add((double) h.score);
	        	}
	        }
	        
	        /* Añadimos el Score de description pero x0.25 */
	        
	        for(ScoreDoc h : descriptionHits){
	        	int id = h.doc;
	        	boolean already = false;
	        	for(int it=0;it<finalIDs.size();it++){
	        		/* Si ya estaba, incrementamos el score */
	        		if(finalIDs.get(it) == id){
	        			finalScore.set(it, finalScore.get(it) + (double)h.score * 0.25);
	        			already = true;
	        			break;
	        		}
	        	}
	        	/* Si ese documento aun no estaba, lo anyadimos */
	        	if(!already){
	        		finalIDs.add(id);
		        	finalScore.add((double) h.score);
	        	}
	        }
	        
	        /* Clase para almacenar y ordenar los resultados */
	        class Resultado implements Comparable<Resultado>{
	        	
	        	private int idDoc;
	        	private double score;
	        
	        	public Resultado(int _idDoc, double _score){
	        		idDoc = _idDoc; score = _score;
	        	}
	        	
	        	public int getIdDoc(){
	        		return idDoc;
	        	}
	        	
	        	public double getScore(){
	        		return score;
	        	}
	        	
	        	@Override
	        	public int compareTo(Resultado o) {
	        	    if(this.score > o.score)
	        	    	return -1;
	        	    else if(this.score < o.score)
	        	    	return 1;
	        	    else
	        	    	return 0;
	        	}
	        	
	        }
	        
	        
	        /* Guardamos los resultados y los ordenamos según scre */
	        List<Resultado> resFinal = new ArrayList<Resultado>();
	        for(int it=0;it<finalIDs.size();it++){
	        	resFinal.add(new Resultado(finalIDs.get(it),finalScore.get(it)));
	        }         
	        Collections.sort(resFinal);
	        
	        
	        /* Mostramos el resultado final */
	        for(int it=0;it<resFinal.size();it++){
	        	int idDoc = resFinal.get(it).getIdDoc();
	        	Document doc = searcher.doc(idDoc);
	        	String path = doc.get("path");
	        	
	            /* Además, lo enviamos al flujo de salida */
	            if (path != null) {
	            	System.out.println((it+1) + ". " + path + "\t" + " SCORE: " + resFinal.get(it).getScore());   
	            	while(path.contains("\\")){
	            		path = path.substring(path.indexOf("\\")+1);
	            	}
	            	writer.println(i.getId() + "\t" + path);
	            } 
	        }	        
	    }
    }
    
    /* Cerramos los flujos */
    writer.close();
    reader.close();
  }

  /** Muestra los resultados paginados **/
  public static  ScoreDoc[] doPagingSearch(InformationNeed necesidad, IndexSearcher searcher, Query query) throws IOException {
 
	/* Indica cuántos resultados queremos */
    TopDocs results = searcher.search(query, 10000);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = (int)results.totalHits;
    System.out.println(numTotalHits + " total matching documents.");
   
    /*
     
     
     ******************************************************
     
     Esta función puede parecer redundante tal y como está.
     
     No obstante, es muy cómoda para trabajar sobre los resultados 
     de cada query en individual, y modularizarla.
 
     
     ******************************************************
     
     
    int start = 0;
    int end = Math.min(numTotalHits, hitsPerPage);
          
    end = Math.min(hits.length, start + hitsPerPage);
      
    
    for (int i = start; i < end; i++) {
    	
        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
       
        
        if (path != null) {
          System.out.println((i+1) + ". " + path);
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }
        
        
    }
      
    end = Math.min(numTotalHits, start + hitsPerPage);
    
    */
    
    return hits;
    
  } 
}
