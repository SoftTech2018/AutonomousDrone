package billedanalyse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import diverse.QrFirkant;
import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;
import javafx.scene.image.Image;

public interface IBilledAnalyse {

	/**
	 * Finder den gennemsnitlige distance fra kamera til vektor i et 6x6 gitter
	 * @param frame Den frame der skal tegnes på (ændres ikke)
	 * @param vectors Vektorer der beregnes på
	 * @param degree Hvor mange grader har dronen bevæget sig
	 * @param Antal kollonner og rækker billedet deles i
	 * @return Frame med påtegnet gitter
	 */
	Mat calcDistances(Mat distFrame, double degree, int size);

	/**
	 * Finder den gennemsnitlige magnitude for vektorerne i hver firkant i et size * size størrelse billede
	 * @param vectors Vektoren der analyseres
	 * @param frame Billedet der hører til vektorerne
	 * @param size Antal rækker og kollonner billedet opsplittes i
	 * @return Array med magnitude værdier for vektorer i billedet
	 */
	double[][] calcOptMagnitude(int size);

//	void qrread(Mat frame);
	
	/**
	 * Giver det senest fundne centerpunkt på det trackede objekt.
	 * @return Centerpunktet i pixelværdier
	 */
	Point getObjectCenter();

	Mat bufferedImageToMat(BufferedImage bi);

	ArrayList<Vektor> getVektorArray();

	/**
	 * Hent et array af behandlede images
	 * Image[0] er det originale farvebillede
	 * Image[1] er optical flow behandlet billede
	 * Image[2] er object tracking billede
	 * @return 
	 */
	Mat[] getImages();

	void setImg(Mat frame);

	void setObjTrack(boolean objTrack);

	void setGreyScale(boolean greyScale);
	
	void setQR(boolean qr);

	void setWebCam(boolean webcam);

	void setOpticalFlow(boolean opticalFlow);

	/**
	 * Hent den seneste ubehandlede Mat frame
	 * @return
	 */
	Mat getMatFrame();
	
	QrFirkant getFirkant();

	void setImage(Mat frame);
	
	void findQR(Mat frame);
	
	String getQrt();

	Image mat2Image(Mat mat);

	void setOpgaveRum(OpgaveRum opgaveRum);

	/**
	 * Find dronens koordinat i rummet. Returnerer null hvis dronens koordinat ikke er opdateret
	 * indenfor et givent tidsinterval.
	 * @return dronens koordinat
	 */
	Koordinat getDroneKoordinat();

	/**
	 * Bestem hvorvidt dronen skal forsøge at læse QR koder og deraf finde sin position
	 * @param drone
	 */
	void setDroneLocator(boolean drone);

	ArrayList<Squares> getColorSquares();
	
	void setMaxVal(int val);
	void setMinVal(int val);

	boolean isPapKasseLocator();

	void setPapKasseLocator(boolean papKasseLocator);

	int[] getPapKasse();

}