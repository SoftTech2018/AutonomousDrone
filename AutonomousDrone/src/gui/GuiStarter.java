package gui;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiStarter extends Application{

	// Udskriver debug-beskeder til konsollen
	protected static final boolean GUI_DEBUG = true;

	@Override
	public void start(Stage primaryStage) {
		try {

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GuiFXML.fxml"));
			AnchorPane root = (AnchorPane) loader.load();
			GuiController controller = loader.getController();
			Scene scene = new Scene(root,600,570);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Skynet 0.1");
			primaryStage.show();

			// Lytter til key-events til at styre dronen.
			scene.setOnKeyReleased(new EventHandler<KeyEvent>(){
				@Override
				public void handle(KeyEvent arg0) {
					if(GuiStarter.GUI_DEBUG){
						System.out.println("Key pressed: " + arg0.getCode());						
					}

					//					switch(arg0.getCode()){
					//					case NUMPAD1: controller.flyUp(null); break;
					//					case NUMPAD2: controller.hover(null); break;
					//					case NUMPAD3: controller.flyDown(null); break;
					//					case NUMPAD4: controller.turnLeft(null); break;
					//					case NUMPAD5: controller.goBack(null); break;
					//					case NUMPAD6: controller.turnRight(null); break;
					//					case NUMPAD7: controller.goLeft(null); break;
					//					case NUMPAD8: controller.goForward(null); break;
					//					case NUMPAD9: controller.goRight(null); break;
					//					case ENTER: controller.takeoff(null); break;
					//					default:
					//						break;
					//					}

					switch(arg0.getCode()){
					case UP: controller.flyUp(null); break;
					case SPACE: controller.hover(null); break;
					case DOWN: controller.flyDown(null); break;
					case A: controller.turnLeft(null); break;
					case S: controller.goBack(null); break;
					case D: controller.turnRight(null); break;
					case LEFT: controller.goLeft(null); break;
					case W: controller.goForward(null); break;
					case RIGHT: controller.goRight(null); break;
					case ENTER: controller.takeoff(null); break;
					case Q: controller.goLeft(null); break;
					case E: controller.goRight(null); break;
					default:
						break;
					}
				}		
			});

//			scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
//				@Override
//				public void handle(KeyEvent event) {
//					if(GuiStarter.GUI_DEBUG){
//						System.out.println("Key released!");						
//					}
//					controller.hover(null);				
//				}				
//			});

			// K�res n�r vinduet lukkes - hvad skal der ske hvis vinduet lukkes mens dronen flyver?
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we) {
					if(GUI_DEBUG){
						System.out.println("Vinduet lukkes.");
					}
					//		        	  controller.land();
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
