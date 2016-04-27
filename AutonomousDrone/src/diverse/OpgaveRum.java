package diverse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


/**
 * 
 * @author KimDrewes
 * Klasse der repræsenterer et OpgaveRum
 * OpgaveRum's objektet initialiseres ved at angive koordinater i cm
 * Vægmarkeringerne bliver indlæst fra wallmarks.txt (Bliver skrvet efter der indtastes i WallValues GUIen)
 *
 */
public class OpgaveRum {

	Koordinat[][] rum;
	int længde, bredde;

	// Et array til at holde styr på koordinaterne til vægmarkeringerne ( Indlæses i setMarkingsmetoden)
	Koordinat[] markingKoordinater = new Koordinat[16];	

	// markings er et array af de 16 kende vægmarkeringer, bliver tildelt en koordinat vha setMarkings
	WallMarking[] markings = {
			new WallMarking("W00_00"),new WallMarking("W00_01"),
			new WallMarking("W00_02"),new WallMarking("W01_00"),
			new WallMarking("W01_01"),new WallMarking("W01_02"),
			new WallMarking("W01_03"),new WallMarking("W01_04"),
			new WallMarking("W02_00"),new WallMarking("W02_01"),
			new WallMarking("W02_02"),new WallMarking("W03_00"),
			new WallMarking("W03_01"),new WallMarking("W03_02"),
			new WallMarking("W03_03"),new WallMarking("W03_04")
	};


	// Konstruktør tager imod længde og bredde, opretter et koordinatsystem, og sætter markeringerne
	public OpgaveRum(int længde, int bredde) {
		this.længde = længde;
		this.bredde = bredde;
		rum = new Koordinat[længde][bredde];
		for (int i = 0; i < længde; i++) {
			for (int j = 0; j < bredde; j++) {
				rum[i][j] = new Koordinat(i, j);
			}
		}
		setMarkings();

	}


	/* 
	 *         Længde 
	 *    * * * * * * * * * *
	 *    * * * * * * * * * *
	 *    * * * * * * * * * *   Bredde
	 *    * * * * * * * * * *
	 *    * * * * * * * * * *
	 * 
	 */

	/**
	 * Metode til at tilføje genstande til et koordinat
	 * @param koordinat koordinatet hvori genstanden skal lægge
	 * @param genstand angiver hvilken genstand der er tale om (bruge new Genstand())
	 */
	public void addGenstandTilKoordinat(Koordinat koordinat, Genstand genstand){
		koordinat.addGenstand(genstand);
	}

	/**
	 * 
	 * @param længde
	 * @param bredde
	 * @return Returnere det koordinat der ligger på det angivende længde og bredde
	 */
	public Koordinat hentKoordinat(int længde, int bredde){
		return rum[længde][bredde];
	}

	//Udskriver alle koordinaterne til Loggen
	public void udskrivKoordinater(){
		Log.writeLog("Der er fundet følgende");
		for (int i = 0; i < længde; i++) {
			for (int j = 0; j < bredde; j++) {
				Log.writeLog(rum[i][j].toString());
			}
		}
	}

	// Sætter vægmarkeringerne efter wallmarks.txt
	public void setMarkings(){
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("wallmarks.txt")));
			String wallmark;
			for(int i = 0; i< markings.length; i++){
				if((wallmark = br.readLine())!= null){
					System.out.println(wallmark);
					String[] temp = wallmark.split(",");
					int x = Integer.parseInt(temp[0]);
					int y = Integer.parseInt(temp[1]);
					rum[x][y].setMarking(markings[i]);
					markingKoordinater[i]= rum[x][y];
				}

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

	// 
	public int getMarkeringNummer(String markName){
		for(int i = 0; i< markings.length; i++){
			if(markings[i].getString().equals(markName)){
				return i;
			}
		}
		return -1;
	}

	public Koordinat[] getNeighbours(String markName){
		int i = getMarkeringNummer(markName);
		Koordinat middle = markingKoordinater[i];

		Koordinat left;
		Koordinat right;

		if(i == 0){
			left =  markingKoordinater[15];
			right = markingKoordinater[1];
		}else if(i == 15){
			left = markingKoordinater[14];
			right = markingKoordinater[0];
		}else{
			left = markingKoordinater[i-1];;
			right = markingKoordinater[i+1];;	
		}


		Koordinat[] temp = {left, middle, right};
		return temp;
	}

}


