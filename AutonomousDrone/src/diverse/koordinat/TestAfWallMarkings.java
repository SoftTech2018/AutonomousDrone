package diverse.koordinat;


import gui.WallValuesController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TestAfWallMarkings extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/WallValues.fxml"));
		VBox root = (VBox) loader.load();
		WallValuesController controller = (WallValuesController) loader.getController();
		Scene scene = new Scene(root,600,570);

		primaryStage.setScene(scene);
		primaryStage.setTitle("Skynet 0.1");
		primaryStage.show();
	}
	public static void main(String[] args){
		launch(args);
	}

}
