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

import diverse.koordinat.Koordinat;

public class Log {

	/**
	 * Statisk metode der skriver til en logfil. Metoden h�ndterer linje skift
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
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Filen kunne ikke åbnes");
		}


	}

	public static void writeWallMarking(String[] string){
		String stringToLog;
		PrintWriter fw = null;
		try {
			fw = new PrintWriter(new BufferedWriter(new FileWriter("wallmarks.txt", false)));

			for(int i = 0; i < string.length; i++){
				stringToLog =  string[i] + "\n";
				System.out.println(stringToLog);
				fw.write(stringToLog);
			}


		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Filen kunne ikke åbnes");
		} finally {
			fw.close();
		}
	}
	public static void writeSize(int[] size){

		String stringToLog;
		PrintWriter fw = null;
		try {
			fw = new PrintWriter(new BufferedWriter(new FileWriter("roomSize.txt", false)));

			for(int i = 0; i < size.length; i++){
				stringToLog =  size[i] + "\n";
				System.out.println(stringToLog);
				fw.write(stringToLog);
			}


		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Filen kunne ikke åbnes");
		} finally {
			fw.close();
		}
	}

	public static void writeLog(Koordinat koordinat){

		String stringToLog = "MÅLOBJEKT FUNDET! \t" + koordinat.toString() + "\t FARVE: " + koordinat.getGenstande().getFarve() + "\n"; 

		try {
			PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter("objectLog.txt", true)));

			fw.write(stringToLog);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Filen kunne ikke åbnes");
		}
	}
		public static void writeYawLog(String string){
			String stringToLog = new Date() + ": " + string + "\n";
			try {
				PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter("yawLog.txt", true)));

				fw.write(stringToLog);
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Filen kunne ikke åbnes");
			}
		}

	}
