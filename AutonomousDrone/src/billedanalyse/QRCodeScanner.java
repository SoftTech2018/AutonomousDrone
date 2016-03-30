package billedanalyse;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class QRCodeScanner
{

	public void imageUpdated(Image image)
	{
		
		LuminanceSource ls = new BufferedImageLuminanceSource((BufferedImage)image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(ls));
		QRCodeReader qrReader = new QRCodeReader();	
		Map<DecodeHintType, Void> hints = new TreeMap<>(); 
		hints.put(DecodeHintType.TRY_HARDER, null);
		try {
			Result result = qrReader.decode(bitmap, hints);
			
			System.out.println("QR Code data is: "+result.getText());
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("--------");
		} catch (ChecksumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("--------");
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("--------");
		}
		qrReader.reset();
	}
}
