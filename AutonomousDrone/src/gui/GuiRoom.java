package gui;



import diverse.koordinat.OpgaveRum;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;

public class GuiRoom extends Application{

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("OpgaveRum");
		Group root = new Group();
		Canvas canvas = new Canvas(320, 300);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		drawRoom(gc);
		root.getChildren().add(canvas);
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}

	public void drawRoom(GraphicsContext gc){
		OpgaveRum opgRum = new OpgaveRum(301, 271);
		for (int i = 0; i < opgRum.markingKoordinater.length; i++) {
			gc.setLineWidth(5);
			gc.strokeLine(opgRum.markingKoordinater[i].getX(), opgRum.markingKoordinater[i].getY(), opgRum.markingKoordinater[i].getX()+1, opgRum.markingKoordinater[i].getY()+1);
		}
	}
}
