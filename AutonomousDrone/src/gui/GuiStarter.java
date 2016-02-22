package gui;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiStarter extends Application{
	
	public static final boolean DEBUG = true;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GuiFXML.fxml"));
			BorderPane root = (BorderPane) loader.load();
			GuiController controller = loader.getController();
			Scene scene = new Scene(root,600,500);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Skynet 0.1");
			primaryStage.show();
			
			// Lytter til key-events til at styre dronen.
			scene.setOnKeyPressed(new EventHandler<KeyEvent>(){
				@Override
				public void handle(KeyEvent arg0) {
					if(GuiStarter.DEBUG){
						System.out.println("Key pressed: " + arg0);						
					}
					
					switch(arg0.getCode()){
					case NUMPAD1: controller.flyUp(null); break;
					case NUMPAD2: controller.hoover(null); break;
					case NUMPAD3: controller.flyDown(null); break;
					case NUMPAD4: controller.turnLeft(null); break;
					case NUMPAD5: controller.goBack(null); break;
					case NUMPAD6: controller.turnRight(null); break;
					case NUMPAD7: controller.goLeft(null); break;
					case NUMPAD8: controller.goForward(null); break;
					case NUMPAD9: controller.goRight(null); break;
					case ENTER: controller.takeoff(null); break;
					default:
						break;
					}
				}		
			});
			
			// Køres når vinduet lukkes - hvad skal der ske hvis vinduet lukkes mens dronen flyver?
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
		          public void handle(WindowEvent we) {
		        	  if(DEBUG){
		        		  System.out.println("Vinduet lukkes.");
		        	  }
		        	  controller.landDrone();
		          }
		      });
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}

}
