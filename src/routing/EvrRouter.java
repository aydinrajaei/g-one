/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import util.Tuple;
import core.Cast;
import core.CastSim;
import core.Connection;
import core.Equation;
import core.GeoDTNHost;
import core.GeoMessage;
import core.GeoSimScenario;
import core.Settings;
import core.SimClock;

/**
 * Implementation of "Opportunstic Geocast in Disruption-Tolerant Networks"
 * Yaozhou Ma and Abbas Jamalipour
 *
 */
public class EvrRouter extends GeoActiveRouter {
	
	/** Message EVR Rate key */
	public static final String MSG_EVR_PROPERTY = "EVRRouter" + "." +"rate";
	
	/** Message arrived in cast flag */
	public static final String MSG_EVRF_PROPERTY = "EVRRouter" + "." +"flag";
	
	/** Initial EVR rate */
	protected double initialEVR = 0;
	
	/** List of cells in the map (pre-defined)*/
	List<Cast> cellList;
	
	/**This is the list of intervisiting time for various cells */
	HashMap<Cast, ArrayList<Double>> cellVisitingTimes = new HashMap<Cast, ArrayList<Double>> ();
	
	/**current cell*/
	Cast currentCell = null;
	
	/**
	 * Constructor. Creates a new Evr router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EvrRouter(Settings s) {
		super(s);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EvrRouter(EvrRouter r) {
		super(r);
		
	}
	
	@Override
	public int receiveGeoMessage(GeoMessage m, GeoDTNHost from) {
		return super.receiveGeoMessage(m, from);
	}
	
	@Override
	//ReceiverSide
	public GeoMessage geoMessageTransferred(String id, GeoDTNHost from) {
		GeoMessage msg = super.geoMessageTransferred(id, from);
		double evrRate = 0;
		msg.updateProperty(MSG_EVR_PROPERTY, evrRate);
		return msg;
	}
	
	/**
	 * Creating a new Message with the properties of EVR Router
	 */
	@Override 
	public boolean createNewGeoMessage(GeoMessage msg) {
		makeRoomForNewGeoMessage(msg.getSize());
		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_EVR_PROPERTY, new Double(initialEVR));
		msg.addProperty(MSG_EVRF_PROPERTY, new Boolean(false));
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
				
				//Flooding phase
				if (m.getTo().checkThePoint(to.getLocation())) {
					forTuples.add(new Tuple<GeoMessage, Connection>(m,con));
				}
			}
		}
		
		//Second - hand in messages with better chance of delivery
		for (GeoMessage m : getGeoMessageCollection()) {
			for (Connection con : getConnections()) {
				GeoDTNHost to = (GeoDTNHost) con.getOtherNode(getGeoHost());
				double messageEvrRate = (Double) m.getProperty(MSG_EVR_PROPERTY);
				boolean messageEvrFlag = (Boolean) m.getProperty(MSG_EVRF_PROPERTY);
				
				//First Phase of Routing procedure
				if ( !messageEvrFlag && messageEvrRate < ((EvrRouter) to.getGeoRouter()).getEvrRate(m.getTo())) {
					forTuples.add(new Tuple<GeoMessage, Connection>(m,con));
				}
			}
		}
		
		return forTuples;
	}
	
	@Override
	public void update() {
		super.update();
		
		if(cellList == null){
			CastSim CSE = GeoSimScenario.getInstance().getCells();
			cellList = CSE.getCastList();
			
			for (int i=0; i<cellList.size(); i++) {
				ArrayList<Double> temp = null;
				this.cellVisitingTimes.put(cellList.get(i), temp);
			}
		}
		
		// updating visiting time for cells beginning
		Boolean inCellFlag = false;
		
		for(Cast key : cellVisitingTimes.keySet()) {
			
			if (key.checkThePoint(this.getGeoHost().getLocation())) {
				inCellFlag = true;
			}
			
			if (key.checkThePoint(this.getGeoHost().getLocation()) && key != currentCell) {
				
				ArrayList<Double> times = new ArrayList<Double>();
				if (this.cellVisitingTimes.get(key) != null){
					times = this.cellVisitingTimes.get(key);
				}
				double simTime = (Double)SimClock.getTime();
				times.add(simTime);
				this.cellVisitingTimes.put(key, times);
				this.currentCell = key;
			}
		}
		
		if(!inCellFlag) {
			this.currentCell = null;
		}
		// update visiting time for cells end
		
		// update EVR rate for the messages
		updateEvrRateForAllGeoMessage();
		
		// update in recipient cast flag
		updateEvrArrivedInDestenitionFlag();
		
		
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
	private void updateEvrArrivedInDestenitionFlag() {
		
		for (GeoMessage m : getGeoMessageCollection()) {
			boolean insideRecipient = m.getTo().checkThePoint(this.getGeoHost().getLocation());
			m.updateProperty(MSG_EVRF_PROPERTY, insideRecipient);
		}		
	}

	/**
	 * Updates the EVR Rate for each GeoMessage inside the buffer
	 */
	protected void updateEvrRateForAllGeoMessage() {

		for (GeoMessage m : getGeoMessageCollection()) {
			double evrRate = getEvrRate(m.getTo());
			m.updateProperty(MSG_EVR_PROPERTY, evrRate);
		}
	}

	
	/**
	 * Calculating Evr (expected visiting rate) Rate of
	 * Cast x (message's destination cast) based on the cells
	 */
	public double getEvrRate(Cast x) {
		
		List<Cast> cellsInTheCast = new ArrayList<Cast>();
		
		// This for loop finds which cell is located in the cast
		for (Cast currentCell : cellList) {
			
			currentCellInLoop: for (Equation ec : currentCell.getEquList()) {
				
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
		
		//Calculating the EVR Rate based on the Lambda rate of the involved cells
		double EvrRate = 0;
		for (Cast cell : cellsInTheCast) {
			double temp = getLambda(cell);
			if (temp > EvrRate) {
				EvrRate = temp;
			}
		}
		
		return EvrRate;
	}
	
	/**
	 * Calculating Lambda i for the cell (x,y)
	 */
	public double getLambda(Cast cell) {
		
		ArrayList<Double> timeList = new ArrayList<Double>();
		
		if(this.cellVisitingTimes.get(cell) !=null) {
			timeList = this.cellVisitingTimes.get(cell);
		}
		
		ArrayList<Double> tIs = new ArrayList<Double>();
		
		if (timeList.size() < 2 ) {return 0;}
		
		else {
		
			for (int i = (timeList.size()); i>1; i--) {
				double tI = timeList.get(i-1) - timeList.get(i-2);
				tIs.add(tI);
			}
		
			double temp = 0;
		
			for (int i=0; i<tIs.size(); i++){
				temp += tIs.get(i);
			}
		
			double eTI = (temp / tIs.size());
		
			return (1 / eTI);
		}
	}
	
	@Override
	public EvrRouter replicate() {
		return new EvrRouter(this);
	}
}
