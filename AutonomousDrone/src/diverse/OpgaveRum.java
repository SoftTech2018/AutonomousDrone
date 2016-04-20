package diverse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class OpgaveRum {

	Koordinat[][] rum;
	int længde, bredde;
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

	public void addGenstandTilKoordinat(Koordinat koordinat, Genstand genstand){
		koordinat.addGenstand(genstand);
	}

	public Koordinat hentKoordinat(int længde, int bredde){
		return rum[længde][bredde];
	}

	public void udskrivKoordinater(){
		Log.writeLog("Der er fundet følgende");
		for (int i = 0; i < længde; i++) {
			for (int j = 0; j < bredde; j++) {
				Log.writeLog(rum[i][j].toString());
			}
		}
	}

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

}
