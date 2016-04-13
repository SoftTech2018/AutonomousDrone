package billedanalyse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.ddogleg.struct.FastQueue;
import org.opencv.core.Point;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

public class Surf {
	
	
	private ExampleAssociatePoints app;
	
	public Surf(){
		
		Class imageType = GrayF32.class;

		DetectDescribePoint detDesc = createFromPremade(imageType);
//		DetectDescribePoint detDesc = createFromComponents(imageType);

		// Might as well have this example do something useful, like associate two images
		ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
		AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

		// load and match images
		app = new ExampleAssociatePoints(detDesc,associate,imageType);

		
				
	}
	
	public List<Point> surfDetect(BufferedImage imageB){				
		return app.associate(imageB);
	}
	
	/**
	 * For some features, there are pre-made implementations of DetectDescribePoint.  This has only been done
	 * in situations where there was a performance advantage or that it was a very common combination.
	 */
	private static <T extends ImageGray, TD extends TupleDesc>
	DetectDescribePoint<T, TD> createFromPremade( Class<T> imageType ) {
		return (DetectDescribePoint)FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(2, 4, 600, 1, 4, 4, 4), null,null, imageType);
		//		return (DetectDescribePoint)FactoryDetectDescribe.sift(new ConfigCompleteSift(-1,5,300));
	}

	/**
	 * Any arbitrary implementation of InterestPointDetector, OrientationImage, DescribeRegionPoint
	 * can be combined into DetectDescribePoint.  The syntax is more complex, but the end result is more flexible.
	 * This should only be done if there isn't a pre-made DetectDescribePoint.
	 */
	private static <T extends ImageGray, TD extends TupleDesc>
	DetectDescribePoint<T, TD> createFromComponents( Class<T> imageType ) {
		// create a corner detector
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
		GeneralFeatureDetector corner = FactoryDetectPoint.createShiTomasi(new ConfigGeneralDetector(1000,5,1), false, derivType);
		InterestPointDetector detector = FactoryInterestPoint.wrapPoint(corner, 1, imageType, derivType);

		// describe points using BRIEF
		DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(new ConfigBrief(true), imageType);

		// Combine together.
		// NOTE: orientation will not be estimated
		return FactoryDetectDescribe.fuseTogether(detector, null, describe);
	}
}
