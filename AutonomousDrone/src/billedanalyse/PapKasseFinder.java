package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import diverse.PunktNavigering;

public class PapKasseFinder {

	private static final int REAL_HEIGHT = 390;

	/**
	 * Finder papkasser baseret på farve i et billede
	 * @param org Billedet der skal analyseres
	 * @return Koordinaterne for centrum af de identificerede papkasser. Returnerer null hvis intet blev fundet
	 */
	public Mat findPapkasse(Mat org){
		
		PunktNavigering pn = new PunktNavigering();

//				org = bufferedImageToMat(UtilImageIO.loadImage(UtilIO.pathExample("C:/Users/ministeren/git/AutonomousDrone/AutonomousDrone/kasse12.jpg")));


		Mat out = new Mat();
		org.copyTo(out);
		Mat temp = new Mat();
		org.copyTo(temp);

		Imgproc.cvtColor(org, temp, Imgproc.COLOR_BGR2HSV);

		double huemin = 90;
		double huemax = 140;
		double satmin = 100;
		double satmax = 200;
		double valmin = 40;
		double valmax = 190;

		//		double huemin = 90;
		//		double huemax = 140;
		//		double satmin = 120;
		//		double satmax = 240;
		//		double valmin = 90;
		//		double valmax = 190;

		Scalar minblue = new Scalar(huemin, satmin, valmin);
		Scalar maxblue = new Scalar(huemax, satmax, valmax);

		Core.inRange(temp, minblue, maxblue, out);		

		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));

		Imgproc.erode(out, out, erodeElement);
		Imgproc.dilate(out, out, dilateElement);
		Imgproc.dilate(out, out, dilateElement);
		Imgproc.erode(out, out, erodeElement);

		Mat findCont = new Mat();
		out.copyTo(findCont);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(findCont, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		List<Point> centers = new ArrayList<Point>();
		if(contours.size() > 0){

			for (int i=0; i< contours.size(); i++){
				Moments moment = Imgproc.moments(contours.get(i));
				double area = moment.get_m00();
				int x = (int)(moment.get_m10() / area);
				int y = (int)(moment.get_m01() / area);
				if(area>1000){
					double mudiff = Math.abs(moment.get_mu20()-moment.get_mu02());
					if (mudiff<150000){
						centers.add(new Point(x,y));
						Imgproc.drawContours(org, contours, i, new Scalar(255,0,0), 3);
						Imgproc.rectangle(out, new Point(x-(Math.sqrt(area)/2), y-(Math.sqrt(area)/2)), new Point(x+(Math.sqrt(area)/2), y+(Math.sqrt(area)/2)), new Scalar(255,0,0), 3);						
					}					
				}
			}
		}

		int sumx = 0;
		int sumy = 0;
		int centerx = 0;
		int centery = 0;
		Point kassecenter = new Point();
		Point kasseh = new Point();
		Point kassel = new Point();
		if (!centers.isEmpty()&& centers.size()>=2){
//			for(Point p : centers){
//				sumx+=p.x;
//				sumy+=p.y;
//			}
//			centerx = sumx / centers.size();
//			centery = sumy / centers.size();
			for(int i = 0; i<centers.size(); i++){
				for(int j = i+1; j<centers.size(); j++){
					if(j<centers.size()){
						double centerix = centers.get(i).x;
						double centerjx = centers.get(j).x;
						if(centerix>centerjx*0.9 && centerix<centerjx*1.1){
							int ydiff = (int) Math.abs(centers.get(i).y-centers.get(j).y);
							int ydiffhalv = ydiff/2;
							double nyy = 0;
							if(centers.get(i).y<centers.get(j).y){
								nyy = centers.get(i).y+ydiffhalv;
							} else {
								nyy = centers.get(i).y-ydiffhalv;
							}
							kassecenter.x = centerix;
							kassecenter.y = nyy;
							int dist = (int) pn.calcDist(ydiff, REAL_HEIGHT);
							Imgproc.circle(org, new Point(kassecenter.x,kassecenter.y), 2, new Scalar(255, 255, 255),2);			
							Imgproc.putText(org, " "+dist+" mm", new Point(kassecenter.x,kassecenter.y), 1, 2, new Scalar(255, 255, 255), 2);							
						}
					}
				}
			}
		}

		//		UtilImageIO.saveImage(toBufferedImage(org), UtilIO.pathExample("C:/Users/ministeren/git/AutonomousDrone/AutonomousDrone/kasse.png"));
//				ShowImages.showWindow(toBufferedImage(org),"Title",true);
		return out;
	}

	public BufferedImage toBufferedImage(Mat m){
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if ( m.channels() > 1 ) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte [] b = new byte[bufferSize];
		m.get(0,0,b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);  
		return image;
	}

	public Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}

}

