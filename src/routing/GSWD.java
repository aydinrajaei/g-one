/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package routing;

import java.util.ArrayList;
import java.util.List;

import util.Tuple;
import core.Cast;
import core.Connection;
import core.Coord;
import core.GeoDTNHost;
import core.GeoMessage;
import core.Settings;

/**
 * Implementation of Geocasting Spray with Direction Router (GSWD)
 * It distribute the copies if copyticket left and the direction check is passed.
 *
 */
public class GSWD extends GeoActiveRouter {
	
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES = "copyticket";
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String GSAF_NS = "GSAFRouter";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = "GSWDRouter" + "." +
		"copies";
	/** Message recipient direction key */
	public static final String MSG_GSWD_DIRECTION_PROPERTY = "GSWDRouterDirection" + "." +
		"copies";
	
	/** Message delete flag */
	//public static final String MSG_GSWD_DELETE_PROPERTY = "GSWDDeleteFlag" + "." +
	//	"copies";
	
	protected int initialNrofCopies;
	
	public GSWD(Settings s) {
		super(s);
		Settings gsafSettings = new Settings(GSAF_NS);
		
		initialNrofCopies = gsafSettings.getInt(NROF_COPIES);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GSWD(GSWD r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
	}
	
	@Override
	public int receiveGeoMessage(GeoMessage m, GeoDTNHost from) {
		return super.receiveGeoMessage(m, from);
	}
	
	@Override
	//receiverSide
	public GeoMessage geoMessageTransferred(String id, GeoDTNHost from) {
		GeoMessage msg = super.geoMessageTransferred(id, from);
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		
		assert nrofCopies != null : "Not a GSAF message: " + msg;
		
		if (nrofCopies > 0) {
			nrofCopies --;
		}
		else {
			nrofCopies = 0;
		}
		
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		//msg.updateProperty(MSG_GSWD_DELETE_PROPERTY, false);
		return msg;
	}
	
	@Override 
	public boolean createNewGeoMessage(GeoMessage msg) {
		makeRoomForNewGeoMessage(msg.getSize());
		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
		msg.addProperty(MSG_GSWD_DIRECTION_PROPERTY, new Integer(0));
		//msg.addProperty(MSG_GSWD_DELETE_PROPERTY, new Boolean(false));
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
		
		for (GeoMessage m : getGeoMessageCollection()) {
			for (Connection con : getConnections()) {
				GeoDTNHost to = (GeoDTNHost) con.getOtherNode(getGeoHost());
				
				if (m.getTo().checkThePoint(to.getLocation())) {
					forTuples.add(new Tuple<GeoMessage, Connection>(m,con));
					continue;
				}
			}
		}
		
		for (GeoMessage m : getGeoMessageCollection()) {
			for (Connection con : getConnections()) {
				GeoDTNHost to = (GeoDTNHost) con.getOtherNode(getGeoHost());
				int recipientDirection = (Integer) m.getProperty(MSG_GSWD_DIRECTION_PROPERTY);
				int copyTicket = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
				
				//First Phase of Routing procedure
				if ((copyTicket > 0) && (this.getGeoHost().getDirection() != recipientDirection) && (to.getDirection() == recipientDirection)) {
					forTuples.add(new Tuple<GeoMessage, Connection>(m,con));
					//m.updateProperty(MSG_GSWD_DELETE_PROPERTY, true);
				}
			}
		}
		
		return forTuples;
	}
	
	/**
	 * Method is called just before a transfer is aborted at {@link #update()} 
	 * due connection going down. This happens on the sending host. 
	 * Subclasses that are interested of the event may want to override this. 
	 * @param con The connection whose transfer was aborted
	 */
	@Override
	protected void transferAborted(Connection con) {
		super.transferAborted(con);
		//con.getGeoMessage().updateProperty(MSG_GSWD_DELETE_PROPERTY, false);
	}
	
	@Override
	public void update() {
		super.update();
		
		updateRecipientDirection();
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableGeoMessages() != null) {
			return;
		}
		
	}
	
	/**
	 * Called just before a transfer is finalized (by 
	 * {@link ActiveRouter#update()}).
	 * Reduces the number of copies we have left for a message. 
	 */
	@Override
	//senderSide
	protected void transferDone(Connection con) {
		Integer nrofCopies;
		String msgId = con.getGeoMessage().getId();
		/* get this router's copy of the message */
		GeoMessage msg = getGeoMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}
		
		/* reduce the amount of copies left */
		nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		if (nrofCopies > 0) {
			nrofCopies --;
		}
		else {
			nrofCopies = 0;
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		
		//Boolean DeleteFlag = (Boolean) con.getGeoMessage().getProperty(MSG_GSWD_DELETE_PROPERTY);
		//if (DeleteFlag) {
		//	this.removeFromGeoMessages(msgId);
		//}
	
	}
	
	protected void updateRecipientDirection() {
		
		for (GeoMessage m : getGeoMessageCollection()) {
			int direction = 0;
			Coord centerPoint = m.getTo().getCenter();
			
			double cX = centerPoint.getX(); //futureX
			double cY = centerPoint.getY(); //futureY
			double pX = this.getGeoHost().getLocation().getX(); //currentX 
			double pY = this.getGeoHost().getLocation().getY(); //currentY 
			
			//StandStill
			if (pY == cY && pX == cX) {
				direction = 0;
			}
			
			//North
			else if (pY >= cY) {
				
				//NorthEast
				if(pX <= cX){ direction = 1; }
				//NorthWest
				else if(pX > cX) { direction = 4; }
			}
			
			//South
			else if(pY < cY) {
				//SoutEast
				if(pX <= cX) { direction = 3; }
				//SouthWest
				else if (pX > cX) { direction = 2; }
			}
			
			m.updateProperty(MSG_GSWD_DIRECTION_PROPERTY, direction);
		}
	}
	
	@Override
	public GSWD replicate() {
		return new GSWD(this);
	}
}
