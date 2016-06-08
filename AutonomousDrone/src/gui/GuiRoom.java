package gui;

import java.io.IOException;
import java.util.ArrayList;


import diverse.koordinat.Koordinat;
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
	double zoomScale = 5;

	OpgaveRum opgRum;
	GraphicsContext gc;



	public GuiRoom() throws NumberFormatException, IOException{
		super(200,822);
		gc = super.getGraphicsContext2D();
		gc.fillRect(0, 0, 200, 300);

		this.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				try {
					clear();
				} catch (NumberFormatException e) {
					e.printStackTrace();

				} catch (IOException e) {
					e.printStackTrace();

				}
			}
		});

	}

	public void drawVisible(){

		xLength = opgRum.getWidth();
		yLength = opgRum.getLength();
		double guiWidth = xLength/zoomScale;
		double guiLength = yLength/zoomScale;
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 40, guiWidth, guiLength);

		gc.setStroke(Color.BLUE);
		gc.setLineWidth(2);
		gc.strokeRect(4, 40, guiWidth, guiLength);
		ArrayList<Koordinat> objects = opgRum.getFoundObjects();

		gc.setFill(Color.CYAN);

		for (int i = 0; i < opgRum.markingKoordinater.length; i++) {
			if(opgRum.markingKoordinater[i] != null){

				double x = (double) opgRum.markingKoordinater[i].getX()/zoomScale;
				double y = (double) opgRum.markingKoordinater[i].getY()/zoomScale;
				if(x > 180 || x == 0){
					gc.fillRect(x+4, y+40, 2, 4 );	
				}else{
					gc.fillRect(x+4, y+40, 4, 2 );	
				}
			}

		}
		if(objects.size() > 0){

			for (int i = 0; i < objects.size(); i++) {
				Koordinat koord = objects.get(i);
				gc.setFill(Color.RED);
				gc.fillRect((koord.getX()/5)+4, (koord.getY()/5)+40, 2, 2);
			}

		}

		gc.fillText("Room Map", 10, 10);

		gc.strokeLine(10, 20, 100/zoomScale, 20);
		gc.strokeLine(10, 18, 10, 22);
		gc.strokeLine(100/zoomScale, 18, 100/zoomScale, 22);
		gc.fillText("1,m", 100/zoomScale+5 , 20);



		Koordinat k;
		if((k = opgRum.getObstacleCenter()) != null){
			//			System.err.println("******* K: " + k);
			gc.fillOval(k.getX()/zoomScale, (k.getY()/zoomScale)+40, 80/zoomScale, 80/zoomScale);
		}

		drawDrone();

	}



	public void clear() throws NumberFormatException, IOException{
		gc.clearRect(0, 0, 300, 322);
		drawVisible();

	}

	public void setOpgRoom(OpgaveRum opgRum) {
		this.opgRum = opgRum;
		drawVisible();

	}

	public void drawDrone(){
		System.err.println("********************** ***********************");
		opgRum.setDronePosition(new Koordinat(300, 400)); // ment som test.
		Koordinat dp = opgRum.getDronePosition();
		Paint temp = gc.getFill();

		double x = dp.getX()/zoomScale;
		double y = dp.getY()/zoomScale;
		double diameter = 25/zoomScale;
		
		gc.setFill(Color.WHITE); // sætter farven til at tegne 
		
		gc.fillOval(x-(diameter/2), y-(diameter/2),diameter,diameter );
		gc.fillOval(x-(diameter/2), y+(diameter/2),diameter,diameter );
		gc.fillOval(x+(diameter/2), y-(diameter/2),diameter,diameter );
		gc.fillOval(x+(diameter/2), y+(diameter/2),diameter,diameter );
	
		gc.setFill(temp); // sætter farven tilbage til hvad den var
		

	}









}


