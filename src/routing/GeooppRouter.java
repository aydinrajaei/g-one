/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import util.Tuple;
import core.Cast;
import core.CastSim;
import core.Connection;
import core.Coord;
import core.Equation;
import core.GeoDTNHost;
import core.GeoMessage;
import core.GeoSimScenario;
import core.Settings;
import core.SimClock;

/**
 * Implementation of "Geoopp: Geocasting for opportunistic networks"
 * S. Lu and Y. Liu
 *
 */
public class GeooppRouter extends GeoActiveRouter {
	
	/** Message Geoopp pi Rate key */
	public static final String MSG_GEOOPP_PROPERTY = "GeooppRouter" + "." +"maxPi";
	
	/** Message Geoopp flag */
	public static final String MSG_GEOOPPFLAG_PROPERTY = "GeooppRouter" + "." +"flag";
	
	/** List of cells in the map (pre-defined)*/
	List<Cast> cellList;

	/**current cell*/
	Cast currentCell = null;
	
	/**if there is a visit in the current cell*/
	int currentCellVisitAny = 0;
	
	/**This is the list of intervisiting time for various cells */
	HashMap<Cast, ArrayList<Double>> cellVisitingTimes = new HashMap<Cast, ArrayList<Double>> ();
	
	/**This is the list of intervisiting time for various cells */
	HashMap<Cast, ArrayList<Integer>> cellContactHistory = new HashMap<Cast, ArrayList<Integer>> ();
	
	/** Initial EVR rate */
	protected double initialMaxPi = 0;
	

	/**
	 * Constructor. Creates a new Evr router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public GeooppRouter(Settings s) {
		super(s);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GeooppRouter(GeooppRouter r) {
		super(r);
		
	}
	
	/**
	 * Creating a new Message with the properties of EVR Router
	 */
	@Override 
	public boolean createNewGeoMessage(GeoMessage msg) {
		makeRoomForNewGeoMessage(msg.getSize());
		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_GEOOPP_PROPERTY, new Double(initialMaxPi));
		msg.addProperty(MSG_GEOOPPFLAG_PROPERTY, new Boolean(false));
		addToGeoMessages(msg, true);
		return true;
	}
	
	/**
	 * Returns a list of message-connections tuples of the messages whose
	 * recipient is some host that we're connected to at the moment.
	 * @return a list of message-connections tuples
	 */
	@Override
	protected List<Tuple<GeoMessage, Connection>> getMessagesForConnected() {
		if (getNrofGeoMessages() == 0 || getConnections().size() == 0) {
			/* no messages -> empty list */
			return new ArrayList<Tuple<GeoMessage, Connection>>(0); 
		}

		List<Tuple<GeoMessage, Connection>> forTuples = new ArrayList<Tuple<GeoMessage, Connection>>();
		
		//First deliver the deliverable messages
		for (GeoMessage m : getGeoMessageCollection()) {
			for (Connection con : getConnections()) {
				GeoDTNHost to = (GeoDTNHost) con.getOtherNode(getGeoHost());
				
				//Flooding Phase
				if (m.getTo().checkThePoint(to.getLocation())) {
					forTuples.add(new Tuple<GeoMessage, Connection>(m,con));
				}
			}
		}
		
		//Second - hand in messages with better chance of delivery
		for (GeoMessage m : getGeoMessageCollection()) {
			for (Connection con : getConnections()) {
				GeoDTNHost to = (GeoDTNHost) con.getOtherNode(getGeoHost());
				double messageGeooppPiRate = (Double) m.getProperty(MSG_GEOOPP_PROPERTY);
				boolean messageGeooppFlag = (Boolean) m.getProperty(MSG_GEOOPPFLAG_PROPERTY);
				
				//First Phase of Routing procedure
				if ( !messageGeooppFlag && messageGeooppPiRate < ((GeooppRouter) to.getGeoRouter()).getPiCast(m)) {
					forTuples.add(new Tuple<GeoMessage, Connection>(m,con));
				}
			}
		}
		
		return forTuples;
	}
	
	@Override
	public void update() {
		super.update();
		
		//initial cellList
		if(cellList == null){
			CastSim CSE = GeoSimScenario.getInstance().getCells();
			cellList = CSE.getCastList();
			
			for (int i=0; i<cellList.size(); i++) {
				ArrayList<Double> temp = null;
				this.cellVisitingTimes.put(cellList.get(i), temp);
				
				ArrayList<Integer> tempVisit = null;
				this.cellContactHistory.put(cellList.get(i), tempVisit);
			}
		}
		
		// updating visiting times and contact history for cells beginning
		Boolean inCellFlag = false;
		
		for(Cast key : cellVisitingTimes.keySet()) {
					
			if (key.checkThePoint(this.getGeoHost().getLocation())) {
				inCellFlag = true;
				
				//for Pi(c)
				List<Connection> lc = this.getGeoHost().getConnections();
				if (!lc.isEmpty()) {this.currentCellVisitAny = 1;}
			}
			
			if (key.checkThePoint(this.getGeoHost().getLocation()) && key != currentCell) {
						
				ArrayList<Double> times = new ArrayList<Double>();
				if (this.cellVisitingTimes.get(key) != null){
					times = this.cellVisitingTimes.get(key);
				}
				double simTime = (Double)SimClock.getTime();
				times.add(simTime);
				this.cellVisitingTimes.put(key, times);
				
				//for Pi(c) - updates the Xi(c) in the list
				if (currentCell != null) {
					ArrayList<Integer> visits = new ArrayList<Integer>();
					if (this.cellContactHistory.get(currentCell) != null){
						visits = this.cellContactHistory.get(currentCell);
					}
					visits.add(currentCellVisitAny);
					this.cellContactHistory.put(currentCell, visits);
				}
				//
				
				this.currentCell = key;
			}
		}
				
		if(!inCellFlag) {
			this.currentCell = null;
		}
		// update visiting time for cells end
		
		// update EVR rate for the messages
		updateGeooppRateForAllGeoMessage();
				
		// update in recipient cast flag
		updateArrivedInDestenitionFlag();
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableGeoMessages() != null) {
			return;
		}
		
	}
	
	/**
	 * Updates if the message has arrived to the destination cast.
	 */
	private void updateArrivedInDestenitionFlag() {
		
		for (GeoMessage m : getGeoMessageCollection()) {
			boolean insideRecipient = m.getTo().checkThePoint(this.getGeoHost().getLocation());
			m.updateProperty(MSG_GEOOPPFLAG_PROPERTY, insideRecipient);
		}		
	}

	/**
	 * Updates the Geoopp Pi Rate for each GeoMessage inside the buffer
	 */
	protected void updateGeooppRateForAllGeoMessage() {

		for (GeoMessage m : getGeoMessageCollection()) {
			double pi = getPiCast(m);
			m.updateProperty(MSG_GEOOPP_PROPERTY, pi);
		}
	}
	
	//This method returns the max of Pi(cells) that are located in the cast destination
	public double getPiCast(GeoMessage gm) {
		//pCast = max ( P cells that are located inside the cast)
		double piMaxCellsInCast = 0;
		
		Cast x = gm.getTo();
		
		List<Cast> cellsInTheCast = new ArrayList<Cast>();
		
		// This for loop finds which cell is located in the cast
		for (Cast currentCell : cellList) {
			
			currentCellInLoop: for(Equation ec : currentCell.getEquList()) {
				
				for(Equation e : x.getEquList()) {
					
					//If two lines are parallel
					if (e.getM() == ec.getM()) { continue; }
						
					double m = (e.getM() - ec.getM());
					double c = ((ec.getY1() - ec.getMx1()) - (e.getY1() - e.getMx1())); //was not correct
					double xSolved = c/m;
					
					if (e.getR1() <= e.getR2()) {
						if (xSolved >= e.getR1() && xSolved <= e.getR2()) {
							cellsInTheCast.add(currentCell);
							break currentCellInLoop;
						}
					} else {
						if (xSolved >= e.getR2() && xSolved <= e.getR1()) {
							cellsInTheCast.add(currentCell);
							break currentCellInLoop;
						}
					}
					
				}
			}
		}
		
		//Calculating the P(gm) based on the Pimax of the involved cells
		for (Cast cell : cellsInTheCast) {
			double temp = getPimax(cell, gm);
			if (temp > piMaxCellsInCast) {
				piMaxCellsInCast = temp;
			}
		}
		
		return piMaxCellsInCast;
		
	}
	
	//This method calculates the PiMax out of all cells going to dCell (destination cell)
	private double getPimax(Cast dCell, GeoMessage gm){
		//PiMax = max (Pcell1 - PcellN)
		double piMaxForMessage = 0;
		
		List<Cast> cells = this.cellList;
		
		for (Cast key : cells) {
			double PiCell = getPi(key, dCell, gm);
			if (PiCell > piMaxForMessage) {
				piMaxForMessage = PiCell;
			}
		}

		return piMaxForMessage;
	}
	
	/**
	 * This method calculates the Pi of a geomessage via viaCell
	 * @param viaCell
	 * @param m
	 * @return Pi of a message via a cell
	 */
	private double getPi(Cast viaCell, Cast dCell, GeoMessage gm){
		//Pi(dCell) = PviaCell(dCell) = Pi(m) * Pi(v) * Pi(c)
		
		//Computing efficient version
		double PiC = getPiC(viaCell);
		
		if (PiC == 0) {
			return 0;
		} else {
			
			double PiV = getPiV(viaCell, gm);
			
			if (PiV == 0) {
				return 0;
			} else {
				
				double PiM = getPiM(viaCell, dCell);
				double PviaCell = PiM * PiV * PiC;
				return PviaCell;
			}
		}
		
	}
	
	/**
	 * This method calculates the Pi(m)
	 * 
	 * @param viaCell
	 * @param dCell
	 * @return Pi(m)
	 */
	private double getPiM(Cast viaCell, Cast dCell) {
		//Pi(m) = (CD - ID)/CD
		Coord currentLocation = this.getGeoHost().getLocation();
		Coord destination = dCell.getCenter();
		Coord viaCellI = viaCell.getCenter();
		
		double CD = Math.sqrt(Math.pow((currentLocation.getX()-destination.getX()),2) + Math.pow((currentLocation.getY()-destination.getY()),2));
		double ID = Math.sqrt(Math.pow((viaCellI.getX()-destination.getX()),2) + Math.pow((viaCellI.getY()-destination.getY()),2));
		
		double PiM = (CD - ID) / CD;
		return PiM;
	}
	
	/**
	 * 
	 * @param viaCell cell i
	 * @param dCell destination
	 * @param m the message
	 * @return Pi(v)
	 */
	private double getPiV(Cast viaCell, GeoMessage gm) {
		//Pi(v) >= 1 - (variance/(texp-tv-mean)^2)
		
		double PiV = 0;
		
		ArrayList<Double> timeList = new ArrayList<Double>();
		
		if(this.cellVisitingTimes.get(viaCell) !=null) {
			timeList = this.cellVisitingTimes.get(viaCell);
		}
		
		ArrayList<Double> xIvs = new ArrayList<Double>();
		
		if (timeList.size() >= 2 ) {
		
			for (int i = (timeList.size()); i>1; i--) {
				double xI = timeList.get(i-1) - timeList.get(i-2);
				xIvs.add(xI);
			}
		
			double temp = 0;
			for (int i=0; i<xIvs.size(); i++) {
				temp += xIvs.get(i);
			}
		
			double mean = (temp / xIvs.size());
			
			double squareDifSum = 0;
			for (int j=0; j<xIvs.size(); j++) {
				squareDifSum += Math.pow((xIvs.get(j) - mean),2);
			}
			
			double variance = (squareDifSum / xIvs.size());
			
			double tEXP = (gm.getCreationTime() + (60.0 * gm.getTtl())); //t_expiry - TTL is based on minutes 
			double tV = timeList.get(timeList.size() - 1); //most recent visiting time
			
			if ((tEXP - tV) <= mean) {return 0;} // Pi(v) calculation is not valid under this condition
			
			PiV = 1 - (variance / Math.pow((tEXP - tV - mean) ,2));
			return PiV;
			
		} else {return 0;}//No historical data
	}
	
	/**
	 * This method calculates the Pi(c)
	 * @param viaCell
	 * @return Pi(c)
	 */
	private double getPiC(Cast viaCell) {
		//Pi(c) <= 1 - (variance/(1-mean)^2)
		
		double PiC = 0;
		
		ArrayList<Integer> visits = new ArrayList<Integer>();
		
		if(this.cellContactHistory.get(viaCell) !=null) {
			visits = this.cellContactHistory.get(viaCell);
		}
		
		if (visits.size() >= 1 ) {
			
			int temp = 0;
			for (int i=0; i<visits.size(); i++) {
				temp += visits.get(i);
			}
		
			double mean = (temp / visits.size());
			
			if (mean == 1) {return 1;} //if mean = 1 then Pi(c) = 1
			
			double squareDifSum = 0;
			for (int j=0; j<visits.size(); j++) {
				squareDifSum += Math.pow((visits.get(j) - mean),2);
			}
			
			double variance = (squareDifSum / visits.size());
			
			PiC = 1 - (variance / Math.pow((1 - mean) ,2));
			
			return PiC;
			
		} else {return 0;}//No historical data
		
	}
	
	
	@Override
	public GeooppRouter replicate() {
		return new GeooppRouter(this);
	}

}
