

/**
 * 
 * @author Daniel Martín - 702858
 * @author Pablo Luesia - 698387
 * @date 25/10/17
 * 
 * Clase para instanciar una necesidad de informacion
 *
 */

public class InformationNeed {

	private String id;
	private String need;
	
	public InformationNeed() {
		id = "";
		need = "";
	}
	
	public InformationNeed(String _id, String _need){
		id = _id;
		need = _need;
	}
	
	public String getNeed(){
		return need;
	}
	
	public void setNeed(String _need){
		need = _need;
	}
	
	public String getId(){
		return id;
	}
	
	public void setId(String _id){
		id = _id;
	}
	
}
