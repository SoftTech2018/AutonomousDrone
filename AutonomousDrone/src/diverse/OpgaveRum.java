package diverse;

public class OpgaveRum {
	
	Koordinat[][] rum;
	int længde, bredde;
	
	public OpgaveRum(int længde, int bredde) {
		this.længde = længde;
		this.bredde = bredde;
		rum = new Koordinat[længde][bredde];
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

}
