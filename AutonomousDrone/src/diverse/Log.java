package diverse;
/**
 * Created by Kim Drewes Rasmussen
 * Som en metode til at kunne skrive beskeder til en fil, til fejlfinding
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class Log {

	/**
	 * Statisk metode der skriver til en logfil. Metoden håndterer linje skift
	 * @param string Tager i mod en String der skal skrives til loggen
	 * 
	 */
	
	public static void writeLog(String string){
	//	File file = new File("Log.txt");
		String stringToLog = new Date() + ": " + string + "\n";
		try {
			PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter("Log.txt", true)));
	
			fw.write(stringToLog);
			fw.close();
		} catch (IOException e) {
		   e.printStackTrace();
		   System.out.println("Filen kunne ikke åbnes");
		}
		
		
	}
}
