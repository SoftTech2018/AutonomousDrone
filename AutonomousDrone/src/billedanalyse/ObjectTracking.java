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
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;

public class ObjectTracking {

	private OpticalFlow op;
	private BilledManipulation bm;

	private Mat objectImage;
	private Mat objectDescriptors;
	private MatOfKeyPoint fKey;
	private DescriptorMatcher matcher;
	private Point centerPoint;
	private Surf surftest;

	public ObjectTracking(OpticalFlow opFlow, BilledManipulation bm){
		this.op = opFlow;
		this.bm = bm;
		this.matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
		this.surftest = new Surf();
	}

	private Mat procesMat(Mat frame){
		//		frame = bm.gaus(frame);
		//		frame = bm.medianBlur(frame);
		//		frame = bm.canny(frame);
		frame = bm.eq(frame);
		//		frame = bm.houghLines(frame);
		//					frame = bm.filterMat(frame);
		//					frame = bm.canny(frame);
		return frame;
	}
	
	protected Mat trackObject(){
		Mat out = new Mat();
		this.op.getFrame().copyTo(out);
		try {
			if(objectImage==null){ // Første gang metoden kaldes indlæses og behandles objektbilledet
				try {
					BufferedImage img = ImageIO.read(new File(".\\test.png"));
					objectImage = bm.bufferedImageToMat(img);	
					objectImage = bm.edde(objectImage);
					objectImage = bm.thresh(objectImage);
					objectImage = bm.toGray(objectImage);
					objectImage = this.procesMat(objectImage);
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

			out = this.procesMat(out);

			MatOfKeyPoint sKey = bm.getKeyPoints(out);		
			Mat s = bm.getDescriptors(out, sKey);
			if(s.empty()){
				return out;
			}

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
				System.out.println("Antal good_matches: " + good_matches.toArray().length);
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
				System.out.println("Antal best_matches: " + best_matches.size());				
			}

			if(best_matches.isEmpty() || best_matches == null){
				return out;
			}

			//			Mat out2 = new Mat();
			//			Features2d.drawMatches(objectImage, fKey, out, sKey, better_matches_mat, out2);
			//			if(true)
			//				return out2;

			// Beregn center af de bedste matches, og tegn en firkant rundt om objektet
			double centroidX = 0, centroidY = 0;
			// Tjek om der er fundet en passende mængde gode matches
			double score = (double) best_matches.size() / good_matches.toArray().length;

			if(BilledAnalyse.BILLED_DEBUG){
				System.out.printf("* Matchet %.2f procent *\n", score*100);
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
				centerPoint = new Point(centroidX/best_matches.size(), centroidY/best_matches.size());
			} 

			if(BilledAnalyse.BILLED_DEBUG){
				long total = System.nanoTime() - startTime;
				long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
				String debug = "Object tracket på: " + durationInMs + " milisekunder";
				System.out.println(debug);	
			}
		} catch (Exception e){
			e.printStackTrace();
			return out;
		}

		return out;
	}

	protected Mat trackSurfObject(BufferedImage in){
		Mat out = new Mat();
//		this.op.getFrame().copyTo(out);
		try {

			long startTime = System.nanoTime();

			
//			bm.toGray(out);
//			BufferedImage imageB = bm.mat2bufImg(out);
//			out = bm.bufferedImageToMat(imageB);
//			if (true) return out;
			List<Point> matches = surftest.surfDetect(in);
			out = bm.bufferedImageToMat(in);
			System.out.println("goodmatches: "+matches.size());
			
			if(matches.isEmpty() || matches == null || matches.size()<10){
				return out;
			}

			double centroidX = 0, centroidY = 0;
			for(Point p : matches){
				System.out.println("surf x: "+p.x);
				System.out.println("surf y: "+p.y);
				centroidX += p.x;
				centroidY += p.y;
			}

			Point p1 = new Point(centroidX/matches.size()-50, centroidY/matches.size()-50);
			Point p2 = new Point(centroidX/matches.size()+50, centroidY/matches.size()+50);
			Imgproc.rectangle(out, p1, p2, new Scalar(255,0,0), 5); // TODO erstat evt frame med out
			centerPoint = new Point(centroidX/matches.size(), centroidY/matches.size());


			if(BilledAnalyse.BILLED_DEBUG){
				long total = System.nanoTime() - startTime;
				long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
				String debug = "Object tracket pÃ¥: " + durationInMs + " milisekunder";
				System.out.println(debug);	
			}
		} catch (Exception e){
			e.printStackTrace();
			return out;
		}

		return out;
	}

	protected Point getObjectCenter(){
		return centerPoint;
	}


	/**
	 * Finder matches mellem to billeder og forbinder dem med en streg
	 * @param first FÃ¸rste billede
	 * @param second Andet billede
	 * @return Kombineret billede med streger mellem matches
	 */
	protected Mat drawMatches(Mat first, Mat second){
		long startTime = System.nanoTime();// DEBUG
		MatOfKeyPoint fKey = bm.getKeyPoints(first);
		MatOfKeyPoint sKey = bm.getKeyPoints(second);		
		Mat f = bm.getDescriptors(first, fKey);
		Mat s = bm.getDescriptors(second, sKey);

		//		DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
		MatOfDMatch dmatches = new MatOfDMatch();
		matcher.match(f, s, dmatches);
		Mat out = new Mat();
		Features2d.drawMatches(first, fKey, second, sKey, dmatches, out);
		//		Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches1to2, outImg, matchColor, singlePointColor, matchesMask, flags);
		if(BilledAnalyse.BILLED_DEBUG){
			long total = System.nanoTime() - startTime;
			long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
			String debug = "Matches fundet pÃ¥: " + durationInMs + " milisekunder";
			System.out.println(debug);	
		}
		return out;
	}

}
