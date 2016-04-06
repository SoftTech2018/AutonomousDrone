package billedanalyse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

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

	/**
	 * Finder matches mellem to billeder og forbinder dem med en streg
	 * @param first Første billede
	 * @param second Andet billede
	 * @return Kombineret billede med streger mellem matches
	 */
	Mat drawMatches(Mat first, Mat second);

	MatOfKeyPoint getKP();

	void qrread(Mat frame);

	Mat bufferedImageToMat(BufferedImage bi);


	/**
	 * Udfører Optical Flow analyse mellem to frames og tegner resultatet på det returnede frame. 
	 * Optical Flow udføres først anden gang metoden kaldes.
	 * @param frame Frame der bliver påtegnet vektorer
	 * @param optFlow Hvorvidt der skal udføres Optical Flow
	 * @param objTrack Hvorvidt der skal trackes objekter
	 * @return Mat frame - påtegnet resultatet (vektorer)
	 */


	ArrayList<Vektor> getVektorArray();

	Image[] getImages();

	void setImg(Mat frame);

	void setObjTrack(boolean objTrack);

	void setGreyScale(boolean greyScale);
	
	void setQR(boolean qr);

	void setWebCam(boolean webcam);

	void setOpticalFlow(boolean opticalFlow);

	Mat getMatFrame();

	void setImage(Mat frame);
	
	void findQR(Mat frame);

}