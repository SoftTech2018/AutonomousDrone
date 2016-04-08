package diverse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import drone.IDroneControl;

public class TakePicture {

	private BufferedImage image;
	private IDroneControl dc;
	private int counter = 0;

	public TakePicture(IDroneControl dc) {
		this.dc = dc;
	}

	public void takePicture() {
		System.out.println("Forsøger at tage et billede");
		image = dc.getbufImg();
		File outputfile = new File(".\\droneImage" + counter + ".jpg"); 
		try {
			ImageIO.write(image, "jpg", outputfile);
			System.out.println("Billede gemt");
			counter++;
		} catch (IOException e) {
			System.out.println("Kunne ikke gemme billedet");
			e.printStackTrace();
		}
	}
}
