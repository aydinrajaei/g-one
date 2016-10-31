/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo1 Simulator Project. 
 */
package core;

/**
 * This class calculates the linear equation of each line in edge of the Cast.
 *
 * @author Aydin Rajaei
 */
public class Equation {
	
	/** f(x): Y = mX + y1 - mx1   
	 * range(x): [range1,range2] 
	 */
	private double m;
	private double mx1;
	private double y1;
	private double range1;
	private double range2;

	/**
	 * Creates and calculates the linear equation for a new line.
	 * @param a (m)
	 * @param b (y1)
	 * @param c (mx1)
	 * @param d (range1)
	 * @param e (range2)
	 */
	public Equation(double a, double b, double c, double d, double e) {
		this.m=a;
		this.y1=b;
		this.mx1=c;
		this.range1=d;
		this.range2=e;
	}

	/**
	 * Returns the range1 of the linear equation.
	 * @return range1
	 */
	public double getR1() { 
		return this.range1;
	}
	
	/**
	 * Returns the range2 of the linear equation.
	 * @return range2
	 */
	public double getR2() {
		return this.range2;
	}

	/**
	 * Calculates and returns the y parameter based on the linear equation and the x point
	 * @param x
	 * @return y
	 */
	public double getEquResult(double x){
		double y = (m*x) + y1 - (mx1);
		return y;
	}
	
	public double getM (){
		return this.m;
	}
	
	public double getMx1 (){
		return this.mx1;
	}
	
	public double getY1 (){
		return this.y1;
	}
}
