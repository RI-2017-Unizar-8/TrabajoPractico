import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Daniel Martín - 702858
 * @author Pablo Luesia - 698387
 * @date 25/10/17
 * 
 * Clase empleada para parsear el fichero de necesidades recibido como parámetro
 *
 */
public class ParserXML {

	private List<InformationNeed> necesidades;
	
	public ParserXML(){
		
	}
	
	public List<InformationNeed> getInformationNeed(){
		return necesidades;
	}
	
	public void setInformationNeed(ArrayList<InformationNeed> _necesidades){
		necesidades = _necesidades;
	}
	
	public void parse(BufferedReader readerFile){
		
		try {
		
			List<InformationNeed> ins = new ArrayList<InformationNeed>();
			
			String recentlyRead = readerFile.readLine();
			
			while(!recentlyRead.equals("<informationNeeds>")){
				recentlyRead = readerFile.readLine();
			}
			
			System.out.println("Entrando: " + recentlyRead);
			
			/* Mientras haya elementos */
			while(recentlyRead != null && recentlyRead.length() != -1){

				InformationNeed in = new InformationNeed();
				
				/* Para cada elemento */
				while(!recentlyRead.equals("</informationNeed>")){
											
					if(recentlyRead.contains("<identifier>")){
						recentlyRead = recentlyRead.replaceAll("<identifier>", "");
						recentlyRead = recentlyRead.replaceAll("</identifier>", "");			
						in.setId(recentlyRead);		
					}
					
					else if(recentlyRead.contains("<text>")){
						recentlyRead = readerFile.readLine();
						in.setNeed(recentlyRead);
						recentlyRead = readerFile.readLine();
					}
					else {
						recentlyRead = readerFile.readLine();
					}
					
					
					if(recentlyRead == null || recentlyRead.length() == -1 || recentlyRead.equals("</informationNeeds>")){
						break;
					}
					
					if(recentlyRead.equals("</informationNeed>")){
						ins.add(in);
					}

				}
				
				if(recentlyRead == null || recentlyRead.length() == -1 || recentlyRead.equals("</informationNeeds>")){
					break;
				}
				
				recentlyRead = readerFile.readLine();
			
			}
			
			necesidades = ins;
			System.out.println("Introducidas " + necesidades.size() + " necesidades.");
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
		

