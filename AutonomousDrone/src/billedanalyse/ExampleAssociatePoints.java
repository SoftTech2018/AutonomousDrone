package billedanalyse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ddogleg.struct.FastQueue;
import org.opencv.core.Point;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

public class ExampleAssociatePoints<T extends ImageGray, TD extends TupleDesc> {

	// algorithm used to detect and describe interest points
	DetectDescribePoint<T, TD> detDesc;
	// Associated descriptions together by minimizing an error metric
	AssociateDescription<TD> associate;

	// location of interest points
	public List<Point2D_F64> pointsA;
	public List<Point2D_F64> pointsB;
	
	private BufferedImage imageA;
	private T inputA;
	private FastQueue<TD> descB;

	Class<T> imageType;

	public ExampleAssociatePoints(DetectDescribePoint<T, TD> detDesc,
			AssociateDescription<TD> associate,
			Class<T> imageType) {
		this.detDesc = detDesc;
		this.associate = associate;
		this.imageType = imageType;
		
//		imageA = UtilImageIO.loadImage(UtilIO.pathExample("C:/Users/ministeren/git/AutonomousDrone/AutonomousDrone/coderaw.png"));
//		inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
		
		// stores the location of detected interest points
		pointsA = new ArrayList<Point2D_F64>();
		
		
		// stores the description of detected interest points
		FastQueue<TD> descA = UtilFeature.createQueue(detDesc,100);
		
		
//		describeImage(inputA,pointsA,descA);
		
		associate.setSource(descA);
	}

	/**
	 * Detect and associate point features in the two images.  Display the results.
	 */
	public List<Point> associate(BufferedImage imageB )
	{	
		long surfTime = System.nanoTime();
		
		
		T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);
		
		pointsB = new ArrayList<Point2D_F64>();
		descB = UtilFeature.createQueue(detDesc,100);
		
		// describe each image using interest points
		describeImage(inputB,pointsB,descB);

		// Associate features between the two images
		associate.setDestination(descB);

		associate.associate();		

		long surftotal = System.nanoTime() - surfTime;
		FastQueue<AssociatedIndex> goodmatches = new FastQueue<AssociatedIndex>(AssociatedIndex.class, true);
		List<Point> points = new ArrayList<Point>();
		
		
		for(AssociatedIndex match : associate.getMatches().toList()){
//			System.out.println("fitScore: "+match.fitScore);
			if(match.fitScore<0.025){
//				System.out.println("match x: "+pointsB.get(match.dst).x);
//				System.out.println("match y: "+pointsB.get(match.dst).y);
				Point pt = new Point(pointsB.get(match.dst).x,pointsB.get(match.dst).y);
				points.add(pt);
				goodmatches.add(match);
			}
		}
		
		
		long surfdurationInMs = TimeUnit.MILLISECONDS.convert(surftotal, TimeUnit.NANOSECONDS);
		String surfdebug = "SURF: " + surfdurationInMs + " milisekunder";
		System.out.println(surfdebug);
		
		return points;
	}

	/**
	 * Detects features inside the two images and computes descriptions at those points.
	 */
	private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs )
	{
		detDesc.detect(input);

		for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
			points.add( detDesc.getLocation(i).copy() );
			descs.grow().setTo(detDesc.getDescription(i));
		}
//		System.out.println("Points: "+points.size());
	}
}
