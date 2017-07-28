package worldBuilder;

public class Gradient {
	public double dZdx;
	public double dZdy;
	
	public Gradient( double dzdx, double dzdy ) {
		this.dZdx = dzdx;
		this.dZdy = dzdy;
	}
	
	public double slope() {
		return Math.sqrt(dZdx*dZdx + dZdy*dZdy);
	}
}
