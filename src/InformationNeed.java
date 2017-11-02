import java.util.List;

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
	private List<String> autores;
	private boolean hayAutores;
	private int anyoIni;
	private int anyoFin;
	private boolean hayFecha;
	private boolean fechaPreferible;
	
	
	public InformationNeed() {
		id = "";
		need = "";
		hayFecha = false;
		hayAutores = false;
		fechaPreferible = false;
		anyoIni = 0;
		anyoFin = 0;
	}
	
	public InformationNeed(String _id, String _need){
		id = _id;
		need = _need;
		fechaPreferible = false;
		hayFecha = false;
		hayAutores = false;
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
	
	public void setFechaIni(int fechaIni){
		anyoIni = fechaIni;
		hayFecha = true;
	}
	
	public void setFechaFin(int fechaFin){
		anyoFin = fechaFin;
		hayFecha = true;
	}
	
	public void setAutores(List<String> _autores){
		autores = _autores;
		hayAutores = true;
	}
	
	public void setFechaPreferible(boolean _p){
		fechaPreferible = _p;
	}
	
	public boolean getHayAutores(){
		return hayAutores;
	}
	
	public boolean getHayFecha(){
		return hayFecha;
	}
	
	public int getAnyoFin(){
		return anyoFin;
	}
	
	public int getAnyoIni(){
		return anyoIni;
	}
	
	public List<String> getAutores(){
		return autores;
	}
	
	public boolean getFechaPreferible(){
		return fechaPreferible;
	}
}
