package diverse.koordinat;

import diverse.circleCalc.Vector2;

public class M2 {
	public double a,b,
				  c,d;
	
	public M2(double a, double b,
	   double c, double d){
		this.a = a;this.b=b;
		this.c = c;this.d=d;
	}
	
	public M2 mul(M2 m){
		return new M2(a*m.a+b*m.c, a*m.b+b*m.d,
					  c*m.a+d*m.c, c*m.b+d*m.d);
	}
	
	public Vector2 mul(Vector2 v){
		return new Vector2(a*v.x+b*v.y,
					  c*v.x+d*v.y);
	}

}
