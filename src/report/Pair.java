package report;

import core.GeoDTNHost;

public class Pair {
	private GeoDTNHost host;
	private double inTime;
	private double outTime;

	public Pair(GeoDTNHost x, double y, double z) {
		setPair(x,y,z);
	}
	
	public Pair(GeoDTNHost x, double y) {
		setPair(x,y);
	}

	public void setPair(GeoDTNHost x, double y, double z) {
		this.host = x;
		this.inTime = y;	
		this.outTime = z;
	}
	
	public void setPair(GeoDTNHost x, double y) {
		this.host = x;
		this.inTime = y;	
		this.outTime = 0;
	}
	
	public void setPair(double z) {
		this.outTime = z;
	}

	public GeoDTNHost getGeoHost() {
		return this.host;
	}

	public double getInTime() {
		return this.inTime;
	}
	
	public double getOutTime() {
		return this.outTime;
	}

//	public String toString() {
//		String temp = host.toString() + "  " + format(inTime) + "  " + format(outTime);
//		return temp;
//	}
	
//	protected String format(double value) {
//		return String.format("%." + 2 + "f", value);
//	}
}
