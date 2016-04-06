package diverse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import drone.IDroneControl;

public class TakePicture {

	private BufferedImage image;
	private IDroneControl dc;

	public TakePicture(IDroneControl dc) {
		this.dc = dc;
	}

	public void takePicture() {
		System.out.println("Forsøger at tage et billede");
		image = dc.getbufImg();
		File outputfile = new File(".\\droneImage.jpg"); {

			try {
				ImageIO.write(image, "jpg", outputfile);
				System.out.println("Billede gemt");
			} catch (IOException e) {
				System.out.println("Kunne ikke gemme billedet");
				e.printStackTrace();
			}

			//Kode til at tage billede fra drone 
			TakePicture picture = new TakePicture(dc);
			picture.takePicture();
		}
	}
}
