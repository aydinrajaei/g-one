/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo1 Simulator Project. 
 */
package core;

import java.util.ArrayList;
import java.util.List;

/**
 * The Cast class keeps the cast information and it finds out: if a node is inside the cast or outside of it.
 *
 * @author Aydin Rajaei
 */
public class Cast {

	/** Identifier of the cast */
	private String id;
	
	List<Coord> theCast = new ArrayList <Coord>();
	List<Equation> theEqu = new ArrayList <Equation>();
	
	/**
	 * It takes an arrayList of coordinations and creates a new cast based on these points. 
	 * @param cast
	 */
	public Cast(List<Coord> cast) {
		for (int i = 0; i<cast.size(); i++)
	      {
	        Coord temp = new Coord(cast.get(i).getX(),cast.get(i).getY());
			theCast.add(temp);
	      }
		equFind();
	}

	/**
	 * It prints the cast information in the output.
	 *
	 */
	public void printcast(){
		for (int i = 0; i<theCast.size(); i++)
	      {
	         System.out.print(theCast.get(i).getX() + "  ");
	         System.out.println(theCast.get(i).getY());
	      }
	}
	
	/**
	 * This method looks if a point is inside the cast or outside of it.
	 * It gets the Coordinate (x,y) of a point and returns a boolean result.
	 * The calculation is based on the "Ray Casting Algorithm" that is being used in GIS (geographical information systems).
	 * 
	 * @param a (x)
	 * @param b (y)
	 * @return flag : true if inside
	 */
	public boolean checkThePoint(double a, double b){

		double af = a + 0.02;
		boolean flag = false;
		
		List<Double> test = new ArrayList<Double>(); 
		
		for (int k = 0; k < theEqu.size(); k++){
			double r1=theEqu.get(k).getR1();
			double r2=theEqu.get(k).getR2();
			
			if(r1 < r2) {
					if(a>=r1 && a<=r2){
						double result = theEqu.get(k).getEquResult(a);
						test.add(result);	
					}
				}
			
			else{
					if(a<=r1 && a>=r2){
						double result = theEqu.get(k).getEquResult(a);
						test.add(result);	
					}
				}
		}
		
		int s = 0;
		int cs = 0;
		for(int i = 0; i<test.size(); i++){
			double g = test.get(i);
			if(g>b){s++;}
			if(g<b){cs++;}
		}
		
		if( s%2==1 && cs%2==1){flag = true;}
		if((s%2==0 && cs%2==1)|(s%2==1 && cs%2==0)){
			
			List<Double> test2 = new ArrayList<Double>(); 
			for (int k = 0; k < theEqu.size(); k++){
			
				double r3=theEqu.get(k).getR1();
				double r4=theEqu.get(k).getR2();
				
				if(r3 < r4) {
						if(af>=r3 && af<=r4){			
							double result = theEqu.get(k).getEquResult(af);
							test2.add(result);	
						}					
					}
				
				else{
						if(af<=r3 && af>=r4){						
							double result = theEqu.get(k).getEquResult(af);
							test2.add(result);	
						}				
					}
			}
			
			int ss = 0;
			int scs = 0;
			
			for(int i = 0; i<test2.size(); i++){		
				double h = test2.get(i);		
				if(h>b){ss++;}		
				if(h<b){scs++;}
			}
			
			if(ss%2==1&&scs%2==1){flag = true;}
		}
		
		return flag;
	}
	
	/**
	 * This method looks if a point is inside the cast or outside of it.
	 * It takes the Coordinate (x,y) of a point and returns a boolean result.
	 * The calculation is based on the "Ray Casting Algorithm" that is being used in GIS (geographical information systems).
	 *
	 * @param Coord x
	 * @return isInside : true if inside
	 */
	public boolean checkThePoint(Coord x){
		
		boolean isInside = checkThePoint(x.getX(),x.getY());
		return isInside;
	}
	
	
	public List<Coord> getTheCast() {
		return this.theCast;
	} 
	
	/**
	 * Returns a string representation of the cast
	 * @return a string representation of the cast
	 */
	public String toString () {
		return id;
	}
	
	public void setId(int i) {
		String name = "Cast"+ i;
		this.id = name;
	}
	
	/**
	 * This Method calculates all the linear equations of edge lines in the cast.
	 *
	 */
	private void equFind(){
		double x1,x2,y1,y2;
		double m,mx1;		
		for (int j = 0; j<theCast.size(); j++){	
			
			if (j==theCast.size()-1){	
				x1 = theCast.get(j).getX();
				y1 = theCast.get(j).getY();
				x2 = theCast.get(0).getX();
				y2 = theCast.get(0).getY();
				
				if(x1!=x2){
					m = (y2-y1)/(x2-x1);
					mx1 = m * x1;
					Equation current = new Equation(m,y1,mx1,x1,x2);
					theEqu.add(current);
				}			
			}
			
			else{		
				x1 = theCast.get(j).getX();
				y1 = theCast.get(j).getY();
				x2 = theCast.get(j+1).getX();
				y2 = theCast.get(j+1).getY();
			
				if(x1!=x2){			
					m = (y2-y1)/(x2-x1);
					mx1 = m * x1;
					Equation current = new Equation(m,y1,mx1,x1,x2);
					theEqu.add(current);				
				}
			}
		}
	}
	
	/**
	 * This method returns an estimation of center point of the cast
	 * It is implemented for the EvrRouter class
	 * @return Coord
	 */
	public Coord getCenter() {
		
		double sumXs = 0;
		double sumYs = 0;
		
		for (int i=0; i<this.theCast.size(); i++) {
			 sumXs += this.theCast.get(i).getX();
			 sumYs += this.theCast.get(i).getY();
		}

		double meanXs = (sumXs / this.theCast.size());
		double meanYs = (sumYs / this.theCast.size());
		
		Coord centerPoint = new Coord(meanXs, meanYs);
		
		return centerPoint;
		
	}
	
	/**
	 * This method returns an estimation of radius of the cast
	 * It is implemented for the EvrRouter class
	 * @param c center point of the cast
	 * @return Coord
	 */
	public Double getRadius(Coord c) {
		
		double radius = 0;
		double x1 = c.getX();
		double y1 = c.getY();
		double x2,y2;
		
		for (int i=0; i<this.theCast.size(); i++) {
			x2 = this.theCast.get(i).getX();
			y2 = this.theCast.get(i).getY();
			
			double temp = Math.sqrt(((x2-x1)*(x2-x1))+((y2-y1)*(y2-y1)));
			
			if (temp >= radius){
				radius = temp;
			}
		}
		
		return radius;
	}
	
	public Coord getNearestPoint(Coord c) {
		
		Coord nearestPoint = null;
		Double destination = null;
		double x1 = c.getX();
		double y1 = c.getY();
		double x2,y2;
		
		for (int i=0; i<this.theCast.size(); i++) {
			x2 = this.theCast.get(i).getX();
			y2 = this.theCast.get(i).getY();
			
			double temp = Math.sqrt(((x2-x1)*(x2-x1))+((y2-y1)*(y2-y1)));
			
			if ((nearestPoint == null) || (temp <= destination)) {
				nearestPoint = theCast.get(i);
				destination = temp;
			}
		}	
		
		return nearestPoint;
			
	}
	
	public List<Equation> getEquList () {
		return this.theEqu;
	}
		
}
