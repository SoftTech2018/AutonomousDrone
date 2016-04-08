package billedanalyse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;

public class ObjectTracking {
	
	private OpticalFlow op;
	private BilledManipulation bm;
	
	private Mat objectImage;
	private Mat objectDescriptors;
	private MatOfKeyPoint fKey;
	private DescriptorMatcher matcher;
	
	public ObjectTracking(OpticalFlow opFlow, BilledManipulation bm){
		this.op = opFlow;
		this.bm = bm;
		this.matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
	}
	
	protected Mat trackObject(){
		Mat out = new Mat();
		this.op.getFrame().copyTo(out);;
		try {
			if(objectImage==null){
				try {
					BufferedImage img = ImageIO.read(new File(".\\test.png"));
					objectImage = bm.bufferedImageToMat(img);
					//					first = bm.gaus(first);
					objectImage = bm.edde(objectImage);
					objectImage = bm.thresh(objectImage);
					objectImage = bm.toGray(objectImage);
					objectImage = bm.medianBlur(objectImage);
					objectImage = bm.canny(objectImage);
					//										objectImage = bm.eq(objectImage);
					objectImage = bm.houghLines(objectImage);
					//					first = bm.filterMat(first);
					//													first = bm.houghLines(first);
					//									first = bm.canny(first);

					fKey = bm.getKeyPoints(objectImage);
					objectDescriptors = bm.getDescriptors(objectImage, fKey);
				} catch (IOException e) {
					e.printStackTrace();
					return out;
				}
			} 
			//				first.copyTo(out); // Billede af object der trackes vises hvis der ikke er nok matches.
			//						for(int i =0; i < fKey.toList().size(); i++){	
			//							Point p = fKey.toList().get(i).pt;
			//							Imgproc.circle(out, p, 4, new Scalar(255,255,255));
			//						}
			//			System.err.println(fKey.toList().size());
			//
			//			if(true)
			//				return out;


			long startTime = System.nanoTime();
			//			Mat image32S = new Mat();
			//out.convertTo(image32S, CvType.CV_32SC1);
			//			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			//			Imgproc.findContours(image32S, contours, new Mat(), Imgproc.RETR_FLOODFILL, Imgproc.CHAIN_APPROX_SIMPLE);
			//			for (int i = 0; i < contours.size(); i++) {
			//			    Imgproc.drawContours(out, contours, 0, new Scalar(255, 255, 255), 10);
			//			}
			//			if(true)
			//				return out;
			//			out = bm.toGray(out);
			out = bm.medianBlur(out);
			//			out = bm.thresh(out);
			//			out = bm.gaus(out);
			//			out = bm.edde(out);
			out = bm.canny(out);
			//						out = bm.eq(out);
			out = bm.houghLines(out);

			MatOfKeyPoint sKey = bm.getKeyPoints(out);		
			Mat s = bm.getDescriptors(out, sKey);
			if(s.empty()){
				return out;
			}
			//			System.out.println(s.size().toString());

			//			MatOfDMatch dmatches = new MatOfDMatch();
			//			matcher.match(f, s, dmatches);
			List<MatOfDMatch> matchesList = new ArrayList<MatOfDMatch>();
			matcher.knnMatch(objectDescriptors, s, matchesList, 2);

			// ratio test
			LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
			for (Iterator<MatOfDMatch> iterator = matchesList.iterator(); iterator.hasNext();) {
				MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
				if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.9) {
					//				if (matOfDMatch.toArray()[0].distance < 0.7*matOfDMatch.toArray()[1].distance) {
					good_matches.add(matOfDMatch.toArray()[0]);	            	
				}
			}
			if(BilledAnalyse.BILLED_DEBUG){				
				System.err.println("Antal good_matches: " + good_matches.toArray().length);
			}
			if(good_matches.toArray().length < 50){
				return out;
			}

			// get keypoint coordinates of good matches to find homography and remove outliers using ransac
			List<Point> pts1 = new ArrayList<Point>();
			List<Point> pts2 = new ArrayList<Point>();
			for(int i = 0; i<good_matches.size(); i++){
				pts1.add(fKey.toList().get(good_matches.get(i).queryIdx).pt);
				pts2.add(sKey.toList().get(good_matches.get(i).trainIdx).pt);
			}

			// convertion of data types - there is maybe a more beautiful way
			Mat outputMask = new Mat();
			MatOfPoint2f pts1Mat = new MatOfPoint2f();
			pts1Mat.fromList(pts1);
			MatOfPoint2f pts2Mat = new MatOfPoint2f();
			pts2Mat.fromList(pts2);

			// Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
			// the smaller the allowed reprojection error (here 15), the more matches are filtered 
			Mat Homog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 100, outputMask, 2000, 0.995);

			// outputMask contains zeros and ones indicating which matches are filtered
			LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
			for (int i = 0; i < good_matches.size(); i++) {
				if (outputMask.get(i, 0)[0] != 0.0) {
					better_matches.add(good_matches.get(i));
				}
			}

			MatOfDMatch better_matches_mat = new MatOfDMatch();
			better_matches_mat.fromList(better_matches);
			List<DMatch> best_matches = better_matches_mat.toList();

			if(BilledAnalyse.BILLED_DEBUG){
				System.err.println("Antal best_matches: " + best_matches.size());				
			}

			if(best_matches.isEmpty() || best_matches == null){
				return out;
			}

			//	    Features2d.drawMatches(first, fKey, second, sKey, better_matches_mat, out);

			// Beregn center af de bedste matches, og tegn en firkant rundt om objektet
			double centroidX = 0, centroidY = 0;
			// Tjek om der er fundet en passende mængde gode matches
			double score = (double) best_matches.size() / good_matches.toArray().length;

			if(BilledAnalyse.BILLED_DEBUG){
				System.out.printf("Matchet %.2f procent\n", score*100);
			}

			if(score > 0.3){
				for(int i =0; i < best_matches.size(); i++){			
					Point pt2 = sKey.toList().get(best_matches.get(i).trainIdx).pt;
					centroidX += pt2.x;
					centroidY += pt2.y;
					Imgproc.circle(out, pt2, 2, new Scalar(0,255,0)); // TODO erstat evt frame med out
				}
				Point p1 = new Point(centroidX/best_matches.size()-50, centroidY/best_matches.size()-50);
				Point p2 = new Point(centroidX/best_matches.size()+50, centroidY/best_matches.size()+50);
				Imgproc.rectangle(out, p1, p2, new Scalar(255,0,0), 5); // TODO erstat evt frame med out
			} 

			if(BilledAnalyse.BILLED_DEBUG){
				long total = System.nanoTime() - startTime;
				long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
				String debug = "Object tracket på: " + durationInMs + " milisekunder";
				System.out.println(debug);	
			}
		} catch (Exception e){
			return out;
		}

		return out;
	}

}
