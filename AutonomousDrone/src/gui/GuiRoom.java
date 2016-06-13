package gui;


import java.io.IOException;
import java.util.ArrayList;

import diverse.circleCalc.Vector2;
import diverse.koordinat.Koordinat;
import diverse.koordinat.M2;
import diverse.koordinat.OpgaveRum;

import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;


public class GuiRoom extends Canvas{


	private int xLength;
	private int yLength;
	private double minYaw = 0;


	double zoomScale = 5;

	OpgaveRum opgRum;
	GraphicsContext gc;



	public GuiRoom() throws NumberFormatException, IOException{
		super(300,450);
		gc = super.getGraphicsContext2D();
		gc.fillRect(0, 0, 300, 300);


		gc.setStroke(Color.BLACK);
		this.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {


				minYaw = minYaw + 0.1;
				drawVisible();
				System.out.println(minYaw);
			}
		});

	}

	public void drawVisible(){
		clear();

		xLength = opgRum.getWidth();
		yLength = opgRum.getLength();
		double guiWidth = xLength/zoomScale;
		double guiLength = yLength/zoomScale;
		gc.setFill(Color.BLACK);


		//Tegner vægge
		gc.setStroke(Color.BLUE);
		gc.setLineWidth(2);
		gc.strokeRect(54, 40, guiWidth, guiLength);
		ArrayList<Koordinat> objects = opgRum.getFoundObjects();


		//Tegner vægmarkeringer
		gc.setFill(Color.CYAN);
		for (int i = 0; i < opgRum.markingKoordinater.length; i++) {
			if(opgRum.markingKoordinater[i] != null){

				double x = (double) opgRum.markingKoordinater[i].getX()/zoomScale;
				double y = (double) opgRum.markingKoordinater[i].getY()/zoomScale;
				if(x > 180 || x == 0){
					gc.fillRect(x+54, y+40, 2, 4 );	
				}else{
					gc.fillRect(x+54, y+40, 4, 2 );	
				}
			}

		}

		// GuiText
		gc.fillText("Room Map", 10, 10);
		gc.strokeLine(10, 20, 100/zoomScale, 20);
		gc.strokeLine(10, 18, 10, 22);
		gc.strokeLine(100/zoomScale, 18, 100/zoomScale, 22);
		gc.fillText("1m", 100/zoomScale+5 , 20);

		gc.fillText("W0", 150, 267);
		gc.fillText("W3", 35, 150);
		gc.fillText("W2", 150, 38);
		gc.fillText("W1", 250, 150);





		Koordinat k;
		if((k = opgRum.getObstacleCenter()) != null){
			//			System.err.println("******* K: " + k);
			gc.fillOval(k.getX()/zoomScale+50, (k.getY()/zoomScale)+90, 80/zoomScale, 80/zoomScale);
		}



		if(objects.size() > 0){

			for (int i = 0; i < objects.size(); i++) {
				Koordinat koord = objects.get(i);
				gc.setFill(Color.RED);
				gc.fillRect((koord.getX()/5)+54, (koord.getY()/5)+40, 2, 2);
			}

		}
		drawDrone();
		
		drawCircles();
		
	
	
	}



	public void clear() {
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, 250, 300);


	}

	public void setOpgRoom(OpgaveRum opgRum) {
		this.opgRum = opgRum;
		drawVisible();

	}

	private void drawDrone(){

//		opgRum.setDronePosition(new Koordinat(200, 900), minYaw); // ment som test.
		Koordinat dp = opgRum.getDronePosition();



		double yaw;
		if(dp==null){
			return;
		}
		Paint temp = gc.getFill();

		double x = dp.getX()/zoomScale;
		double y = dp.getY()/zoomScale;
		double diameter = 25/zoomScale;


		if ((yaw = opgRum.getDroneYaw()) == -99999){
			yaw = 0;	
		}
		M2 M= new M2(Math.cos(yaw), -Math.sin(yaw),
				Math.sin(yaw),  Math.cos(yaw));


		double x1 = (x-(diameter/2)) +5;
		double x2 = (x-(diameter/2)) +5;
		double x3 = (x+(diameter/2)) +5;
		double x4 = (x+(diameter/2)) +5;

		double y1 = (y-(diameter/2)) +41;
		double y2 = (y+(diameter/2)) +41;
		double y3 = (y-(diameter/2)) +41;
		double y4 = (y+(diameter/2)) +41;



		Vector2 v1 = new Vector2(x1, y1);
		Vector2 v2 = new Vector2(x2, y2);
		Vector2 v3 = new Vector2(x3, y3);
		Vector2 v4 = new Vector2(x4, y4);


		Vector2 P = v1.add(v2).add(v3).add(v4).scale(1.0/4);


		double dx2 = P.x-10;
		double dy2 = P.y;

		Vector2 dv = new Vector2(dx2, dy2);
		dv = M.mul(dv.sub(P)).add(P);



		v1 = M.mul(v1.sub(P)).add(P);
		v2 = M.mul(v2.sub(P)).add(P);
		v3 = M.mul(v3.sub(P)).add(P);
		v4 = M.mul(v4.sub(P)).add(P);


//		System.out.println("omdrejningspunktet P(" + P.x +"," +P.y+")");
//		System.out.println("Dronens position er d(" + x +"," +y +")");


		gc.setFill(Color.RED); // sætter farven til at tegne 
	

		gc.fillOval(v1.x+47, v1.y-2,diameter,diameter );
		gc.fillOval(v2.x+47, v2.y-2,diameter,diameter );
		gc.setFill(Color.WHITE);
		gc.fillOval(v3.x+47, v3.y-2,diameter,diameter );
		gc.fillOval(v4.x+47, v4.y-2,diameter,diameter );

		gc.setFill(temp); // sætter farven tilbage til hvad den var
		
		
	}
	
    public void drawCircles(){
    	
    	// Test skal efterlades udkommenteret
//    	opgRum.setCircleInfo(new Vector2(188, 1055), new Vector2(338, 1060), 100, 300);
    	if(!opgRum.isCircleFlag()){
    		return;
    	}
    	
    	
    	double dist1 = opgRum.getCircleDists()[0]/zoomScale;
   
    	double dist2 = opgRum.getCircleDists()[1]/zoomScale;
     	System.out.println(dist1 + ", " + dist2);
    	Vector2 v1 = opgRum.getCircleCenters()[0];
    	Vector2 v2 = opgRum.getCircleCenters()[1];
    	System.out.println(v1 + ", " + v2);
    	double r1 = dist1/2;
    	double r2 = dist2/2;
    	double x1 = v1.x/zoomScale-r1+54;
    	double y1 = v1.y/zoomScale-r1+40;
    	double x2 = v2.x/zoomScale-r2+54;
    	double y2 = v2.y/zoomScale-r2+40;
    	gc.strokeOval(x1, y1, dist1, dist1);
		gc.strokeOval(x2, y2, dist2, dist2); 
		
    }

}


