package billedanalyse;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import diverse.Log;

public class QRCodeScanner
{
	
	public String qrt = "";
	
	public String applyFilters(Mat frame){
		String qrText="";
		for (int i = 1; i < 15; i++) {
			qrText = imageUpdated(frame,i);
			if(qrText.length()<3){
//				Log.writeLog("ID på switch ved return: "+i);
				continue;
			} else {
//				Log.writeLog("ID på switch ved return: "+i +", "+qrText);
				qrt = qrText;
				return qrText;
			}
		}
		return qrText;
	}
	
	public String imageUpdated(Mat frame, int i){
		String qrt = "";
		Mat temp = new Mat();
		frame.copyTo(temp);
		switch (i) {
		case 1:
			break;
		case 2:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 50, 255, Imgproc.THRESH_BINARY);
			break;
		case 3:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 55, 255, Imgproc.THRESH_BINARY);
			break;
		case 4:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 60, 255, Imgproc.THRESH_BINARY);
			break;
		case 5:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 65, 255, Imgproc.THRESH_BINARY);
			break;
		case 6:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 70, 255, Imgproc.THRESH_BINARY);
			break;
		case 7:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 75, 255, Imgproc.THRESH_BINARY);
			break;
		case 8:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 80, 255, Imgproc.THRESH_BINARY);
			break;
		case 9:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 85, 255, Imgproc.THRESH_BINARY);
			break;
		case 10:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 100, 255, Imgproc.THRESH_BINARY);
			break;
		case 11:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 105, 255, Imgproc.THRESH_BINARY);
			break;
		case 12:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 110, 255, Imgproc.THRESH_BINARY);
			break;
		case 13:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 115, 255, Imgproc.THRESH_BINARY);
			break;
		case 14:
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(temp, temp, 120, 255, Imgproc.THRESH_BINARY);
			break;
		}
		
		Image image = toBufferedImage(temp);
		LuminanceSource ls = new BufferedImageLuminanceSource((BufferedImage)image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(ls));
		QRCodeReader qrReader = new QRCodeReader();
		try {
			Result result = qrReader.decode(bitmap);
			System.out.println("QR Code data is: "+result.getText());
			qrt = result.getText();
			int x = 0;
			int y = 0;
			for (ResultPoint rp : result.getResultPoints()){
				x += rp.getX();
				y += rp.getY();
			}
			x = (int) (x/result.getResultPoints().length);
			y = (int) (y/result.getResultPoints().length);
			qrt += "," + x + "," + y;
		} catch (NotFoundException e) {
		} catch (ChecksumException e) {
		} catch (FormatException e) {
		}
		qrReader.reset();
		return qrt;
	}
	
	public String imageUpdated(Mat frame){
		Mat temp = new Mat();
		frame.copyTo(temp);
		Image image = toBufferedImage(temp);
		LuminanceSource ls = new BufferedImageLuminanceSource((BufferedImage)image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(ls));
		QRCodeReader qrReader = new QRCodeReader();	
		try {
			Result result = qrReader.decode(bitmap);
			System.out.println("QR Code data is: "+result.getText());
			qrt = result.getText();
			int x = 0;
			int y = 0;
			for (ResultPoint rp : result.getResultPoints()){
				x += rp.getX();
				y += rp.getY();
			}
			x = (int) (x/result.getResultPoints().length);
			y = (int) (y/result.getResultPoints().length);
			qrt += "," + x + "," + y;
		} catch (NotFoundException e) {
		} catch (ChecksumException e) {
		} catch (FormatException e) {
		}
		qrReader.reset();
		return qrt;
	}
	
	public String imageUpdated(BufferedImage image){
//		String qrt = "";
		LuminanceSource ls = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(ls));
		QRCodeReader qrReader = new QRCodeReader();	
//		Map<DecodeHintType, Void> hints = new TreeMap<>(); 
//		hints.put(DecodeHintType.TRY_HARDER, null);
		try {
			Result result = qrReader.decode(bitmap);
//			System.out.println("QR Code data is: "+result.getText());
			qrt = result.getText();
			for (ResultPoint rp : result.getResultPoints()){
//				System.out.println(rp.getX() + "," + rp.getY());
			}
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
		return qrt;
	}
	
	public String getQrt(){
		return qrt;
	}

	//Metoden er hentet fra stackoverflow: http://stackoverflow.com/questions/15670933/opencv-java-load-image-to-gui
	public Image toBufferedImage(Mat m){
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
	
	
	
}
