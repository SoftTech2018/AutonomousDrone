package drone;

import com.google.zxing.Result;

import billedanalyse.TagListener;
import de.yadrone.base.IARDrone;

public class OpgaveAlgoritme {

	private Result object;
	private IDroneControl dc = new DroneControl();
	protected boolean doStop = false;
	

	public OpgaveAlgoritme(){
		
		while (!doStop) {
			try {
				if (object == null) { //Indsæt objekt listener her
					
				}
			}
			catch(Exception exc)
			{
				exc.printStackTrace();
			}
		}
	

	}
}
