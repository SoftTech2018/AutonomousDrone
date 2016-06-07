package diverse.koordinat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import diverse.Log;
import diverse.circleCalc.Vector2;
import diverse.koordinat.Genstand.FARVE;
import sun.security.ssl.KerberosClientKeyExchange;


/**Rettet 7/6 kl 9:30	
 * 
 * @author KimDrewess
 * Klasse der repræsenterer et OpgaveRum
 * OpgaveRum's objektet initialiseres ved at angive koordinater i cm
 * Vægmarkeringerne bliver indlæst fra wallmarks.txt (Bliver skrvet efter der indtastes i WallValues GUIen)
 *
 */
public class OpgaveRum {

	private Koordinat[][] rum;
	private int længde = 0;
	private int bredde = 0;
	private boolean isMarkingOk = true;
	private ArrayList<Koordinat> fundneGenstande = new ArrayList<>();
	private Koordinat obstacleCenter = null;

	public int getLength() {
		return længde;
	}


	public int getWidth() {
		return bredde;
	}
	// Et array til at holde styr på koordinaterne til vægmarkeringerne ( Indlæses i setMarkingsmetoden)
	public Koordinat[] markingKoordinater = new Koordinat[20];	

	// markings er et array af de 200 kende vægmarkeringer, bliver tildelt en koordinat vha setMarkings
	WallMarking[] markings = {
			new WallMarking("W00_00"),new WallMarking("W00_01"),
			new WallMarking("W00_02"),new WallMarking("W00_03"),
			new WallMarking("W00_04"),new WallMarking("W01_00"),
			new WallMarking("W01_01"),new WallMarking("W01_02"),
			new WallMarking("W01_03"),new WallMarking("W01_04"),
			new WallMarking("W02_00"),new WallMarking("W02_01"),
			new WallMarking("W02_02"),new WallMarking("W02_03"),
			new WallMarking("W02_04"),new WallMarking("W03_00"),
			new WallMarking("W03_01"),new WallMarking("W03_02"),
			new WallMarking("W03_03"),new WallMarking("W03_04")
	};


	// Konstruktør tager imod længde og bredde, opretter et koordinatsystem, og sætter markeringerne
	public OpgaveRum() throws NumberFormatException, IOException {
		setSize();

		rum = new Koordinat[bredde][længde];
		for (int i = 0; i < bredde; i++) {
			for (int j = 0; j < længde; j++) {
				rum[i][j] = new Koordinat(i, j);
			}
		}
		setMarkings();
		
		for (int i = 0; i < bredde; i = i+100) {
			for (int j = 0; j < længde; j=j+100) {
				System.out.println(i + " " + j);
				addGenstandTilKoordinat(rum[i][j], new Genstand(FARVE.RØD));
			
			}
		}

	}


	/**
	 * setSize() læser fra roomSize.txt og sætter længde og bredde af rummet.
	 * @throws NumberFormatException
	 * @throws IOException
	 */

	private void setSize() throws NumberFormatException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File("roomSize.txt")));
		String size;
		if((size = br.readLine())!= null){
			bredde = Integer.parseInt(size)+1; // lægger én til
		}
		if((size = br.readLine())!= null){
			længde = Integer.parseInt(size)+1; // Lægger én til.
		}
		br.close();
	}


	/**
	 * Metode til at tilføje genstande til et koordinat
	 * @param koordinat koordinatet hvori genstanden skal lægge
	 * @param genstand angiver hvilken genstand der er tale om (bruge new Genstand())
	 */
	public void addGenstandTilKoordinat(Koordinat koordinat, Genstand genstand){
		koordinat.addGenstand(genstand);
		fundneGenstande.add(koordinat);
	}

	/**
	 * 
	 * @param længde
	 * @param bredde
	 * @return Returnere det koordinat der ligger på det angivende længde og bredde
	 */
	public Koordinat hentKoordinat(int længde, int bredde){
		return rum[bredde][længde];
	}

	//Udskriver alle koordinaterne til Loggen
	public void udskrivKoordinater(){
		Log.writeLog("Der er fundet følgende");
		for (int i = 0; i < bredde; i++) {
			for (int j = 0; j < længde; j++) {
				Log.writeLog(rum[i][j].toString());
			}
		}
	}

	// Sætter vægmarkeringerne efter wallmarks.txt
	public void setMarkings(){
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File("wallmarks.txt")));
			String wallmark;
			for(int i = 0; i< markings.length; i++){
				try {
					if((wallmark = br.readLine())!= null){
						
						System.out.println(wallmark);
						String[] temp = wallmark.split(",");
						int x = Integer.parseInt(temp[0]);
						int y = Integer.parseInt(temp[1]);
					
						markingKoordinater[i]= new Koordinat(x, y);
						
					}

				} catch (Exception e) {

				}
			}


			br.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	//Bruges til at udskrive når der er fundet en genstand til loggen
	public void writeMarkingsToLog(){
		for(int i = 0; i < længde ;i++){
			for(int j = 0; j < bredde ;j++){
				try{
					if(rum[i][j].getMarking() instanceof WallMarking){
						Log.writeLog("Væg Markering fundet på :" + i+ "," +j);
					}
				}catch(NullPointerException e){

				}

			}
		}
	}

	// hjælpe metode til getMultiMarkings()
	private int getMarkeringNummer(String markName){
		for(int i = 0; i< markings.length; i++){
			if(markings[i].getString().equals(markName)){
				return i;
			}
		}
		return -1;
	}


	/**
	 * 
	 * @param markName Den String som QR code scanneren returnerer
	 * @return Returnere et Array med den aflæste QR Plakat, samt positionerne af dens naboer i form af Vektor2 objekter
	 */
	public Vector2[] getMultiMarkings(String markName){
		int i = getMarkeringNummer(markName);
		Vector2 middle = markingKoordinater[i].getVector();

		Vector2 left;
		Vector2 right;

		if(i == 0){
			left =  markingKoordinater[15].getVector();
			right = markingKoordinater[1].getVector();
		}else if(i == 15){
			left = markingKoordinater[14].getVector();
			right = markingKoordinater[0].getVector();
		}else{
			left = markingKoordinater[i-1].getVector();
			right = markingKoordinater[i+1].getVector();	
		}

		Vector2[] temp = {left, middle, right};
		return temp;
	}

	public void setLength(int length){
		this.længde = længde;
	}
	public void setWidth(int width){
		this.bredde = width;
	}

	public ArrayList<Koordinat> getFoundObjects(){
		return fundneGenstande;
	}

	public void setObstacleCenter(Koordinat k){
		obstacleCenter = rum[k.getX()][k.getY()];
	}
	
	public boolean erForhindring(Koordinat k){
		
		Vector2 obstacle = obstacleCenter.getVector();
		Vector2 searchPoint = k.getVector();
		Vector2 temp = obstacle.sub(searchPoint);
		double afstand = Math.sqrt(Math.pow(temp.x, 2) + Math.pow(temp.y, 2));
		if(afstand > 80){
			return true;
		}
		
		return false;
	}
	public Koordinat getObstacleCenter(){
		return obstacleCenter;
	}
	
	
}




