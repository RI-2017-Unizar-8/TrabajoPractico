import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public static boolean isNumeric(String str)  
	{  
		  try  
		  {  
			  double d = Double.parseDouble(str);  
		  }  
		  catch(NumberFormatException nfe)  
		  {  
			  return false;  
		  }  
		  return true;  
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
						recentlyRead = recentlyRead.replaceAll("\\.", "");
						recentlyRead = recentlyRead.replaceAll(" +", " ");
						
						/* Queremos saber si hay palabras clave: NOMBRES */
						
						String[] arr = recentlyRead.split(" ");    
						List<String> autores = new ArrayList<String>();
						
						
						Pattern pA1 = Pattern.compile(".*escrito por.*");
						Matcher mA1 = pA1.matcher(recentlyRead);
						if (mA1.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if((arr[i].equals("escrito") && arr[i+1].equals("por"))){
									autores.add(arr[i+2]);
									break;
								}
							}
						} 
						
						Pattern pA2 = Pattern.compile(".*hecho por.*");
						Matcher mA2 = pA2.matcher(recentlyRead);
						if (mA2.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if((arr[i].equals("hecho") && arr[i+1].equals("por"))){
									autores.add(arr[i+2]);
									break;
								}
							}
						}
						
						Pattern pA6 = Pattern.compile(".*realizado por.*");
						Matcher mA6 = pA6.matcher(recentlyRead);
						if (mA6.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if((arr[i].equals("realizado") && arr[i+1].equals("por"))){
									autores.add(arr[i+2]);
									break;
								}
							}
						}

						Pattern pA3 = Pattern.compile(".*escribi.* [A-Z].*");
						Matcher mA3 = pA3.matcher(recentlyRead);
						if (mA3.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if(arr[i].contains("escribi")){
									autores.add(arr[i+1]);
									break;
								}
							}
						} 
						
						Pattern pA4 = Pattern.compile(".*hizo [A-Z].*");
						Matcher mA4 = pA4.matcher(recentlyRead);
						if (mA4.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if(arr[i].equals("hizo")){
									autores.add(arr[i+1]);
									break;
								}
							}
						} 
						
						Pattern pA5 = Pattern.compile(".*realiz.* [A-Z].*");
						Matcher mA5 = pA5.matcher(recentlyRead);
						if (mA5.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if(arr[i].contains("realiz")){
									autores.add(arr[i+1]);
									break;
								}
							}
						} 
						
						
						if(autores != null && autores.size()>0){
							in.setAutores(autores);
						}
						
						/* Queremos saber si hay palabras clave: FECHAS */
						
						if(recentlyRead.contains("prefer") ||
							recentlyRead.contains("a poder ser") || 
							recentlyRead.contains("si es posible") || 
							recentlyRead.contains("si puede ser")){
								in.setFechaPreferible(true);
						}
			
						Pattern p1 = Pattern.compile(".*a partir.*[0-9]+.*");
						Matcher m1 = p1.matcher(recentlyRead);
						if (m1.find()){
							arr = recentlyRead.split(" ");    
							for (int i=0; i<arr.length; i++) {
								if(isNumeric(arr[i])){
									int anyo = Integer.valueOf(arr[i]);
									in.setFechaIni(anyo);
									break;
								}
							}
						} else {
						   
							Pattern p2 = Pattern.compile(".*hasta [0-9]+.*");
							Matcher m2 = p2.matcher(recentlyRead);
							if (m2.find()){
								arr = recentlyRead.split(" ");    
								for (int i=0; i<arr.length; i++) {
									if(arr[i].equals("hasta") && isNumeric(arr[i+1])){
										int anyo = Integer.valueOf(arr[i+1]);
										in.setFechaFin(anyo);
										break;
									}
								}
							} else {
							
								Pattern p3 = Pattern.compile(".*desde [0-9]+.*");
								Matcher m3 = p3.matcher(recentlyRead);
								if (m3.find()){
									arr = recentlyRead.split(" ");    
									for (int i=0; i<arr.length; i++) {
										if(arr[i].equals("desde") && isNumeric(arr[i+1])){
											int anyo = Integer.valueOf(arr[i+1]);
											in.setFechaIni(anyo);
											break;
										}
									}
								} else {
								
									Pattern p4 = Pattern.compile(".*entre [0-9]+ y [0-9]+.*");
									Matcher m4 = p4.matcher(recentlyRead);
									if (m4.find()){
										arr = recentlyRead.split(" ");    
										for (int i=0; i<arr.length; i++) {
											if(arr[i].equals("entre") && arr[i+2].equals("y")){
												int anyo = Integer.valueOf(arr[i+1]);
												in.setFechaIni(anyo);
												anyo = Integer.valueOf(arr[i+3]);
												in.setFechaFin(anyo);
												break;
											}
										}
									} else {
									
										Pattern p5 = Pattern.compile(".*entre el [0-9]+ y el [0-9]+.*");
										Matcher m5 = p5.matcher(recentlyRead);
										if (m5.find()){
											arr = recentlyRead.split(" ");    
											for (int i=0; i<arr.length; i++) {
												if(arr[i].equals("entre") && arr[i+3].equals("y")){
													int anyo = Integer.valueOf(arr[i+2]);
													in.setFechaIni(anyo);
													anyo = Integer.valueOf(arr[i+5]);
													in.setFechaFin(anyo);
													break;
												}
											}
										} else {
										
											Pattern p6 = Pattern.compile(".*antes de [0-9]+.*");
											Matcher m6 = p6.matcher(recentlyRead);
											if (m6.find()){
												arr = recentlyRead.split(" ");    
												for (int i=0; i<arr.length; i++) {
													if(arr[i].equals("antes") && arr[i+1].equals("de") && isNumeric(arr[i+2])){
														int anyo = Integer.valueOf(arr[i+2]);
														in.setFechaFin(anyo);
														break;
													}
												}
											} else {
											
												Pattern p7 = Pattern.compile(".*antes del [0-9]+.*");
												Matcher m7 = p7.matcher(recentlyRead);
												if (m7.find()){
													arr = recentlyRead.split(" ");    
													for (int i=0; i<arr.length; i++) {
														if(arr[i].equals("antes") && arr[i+1].equals("del") && isNumeric(arr[i+2])){
															int anyo = Integer.valueOf(arr[i+2]);
															in.setFechaFin(anyo);
															break;
														}
													}
												} else {
												
													Pattern p8 = Pattern.compile(".*del.*[0-9]+.*");
													Matcher m8 = p8.matcher(recentlyRead);
													if (m8.find()){
														arr = recentlyRead.split(" ");    
														for (int i=0; i<arr.length; i++) {
															if(isNumeric(arr[i])){
																int anyo = Integer.valueOf(arr[i]);
																in.setFechaIni(anyo);
																in.setFechaFin(anyo);
															}
														}
													} 
												}
											}
										}
									}
								}
							}
						}
						
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
		

