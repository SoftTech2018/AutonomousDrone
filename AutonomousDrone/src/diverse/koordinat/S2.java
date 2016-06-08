package diverse.koordinat;
import java.awt.Graphics;

import diverse.circleCalc.Vector2;
import javafx.scene.canvas.GraphicsContext;


public class S2 {
	public Vector2 o;	 //Origo
	public M2 F,S,T; //Flip, Scale, Transform, 
	
	public S2(double sx, double sy, double ox, double oy) {
		o = new Vector2(ox,oy);
		F = new M2(1, 0,
				   0,-1);
		S = new M2(sx,0,
				   0, sy);
		T = F.mul(S);
	}
	
	public Vector2 transform(Vector2 v){
		return T.mul(v).add(o);
	}
	
	public void drawLine(GraphicsContext gc,Vector2 p1, Vector2 p2){
		Vector2 p1w = transform(p1);
		Vector2 p2w = transform(p2);
		gc.strokeLine((int)p1w.x, (int)p1w.y, (int)p2w.x, (int)p2w.y);
		
	}
	
	public void drawPoint(GraphicsContext gc, Vector2 p){
		Vector2 pw = transform(p);
		gc.fillOval((int)pw.x, (int)pw.y, 2, 2);
	}
	
	public void drawAxes(GraphicsContext gc){
		drawLine(gc, new Vector2(0,0), new Vector2(1,0));
		//Beautify axes		
		drawLine(gc, new Vector2(0,0), new Vector2(0,1));
	}
	
}
