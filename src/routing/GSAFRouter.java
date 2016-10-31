/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package routing;

import java.util.ArrayList;
import java.util.List;

import core.Connection;
import core.GeoDTNHost;
import core.GeoMessage;
import core.Settings;

/**
 * Implementation of Geocasting Spray And Flood Router (GSAF)
 *
 */
public class GSAFRouter extends GeoActiveRouter {
	
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES = "copyticket";
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String GSAF_NS = "GSAFRouter";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = "GSAFRouter" + "." +
		"copies";
	
	protected int initialNrofCopies;
	
	public GSAFRouter(Settings s) {
		super(s);
		Settings gsafSettings = new Settings(GSAF_NS);
		
		initialNrofCopies = gsafSettings.getInt(NROF_COPIES);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GSAFRouter(GSAFRouter r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
		//this.isBinary = r.isBinary;
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
		return msg;
	}
	
	@Override 
	public boolean createNewGeoMessage(GeoMessage msg) {
		makeRoomForNewGeoMessage(msg.getSize());
		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
		addToGeoMessages(msg, true);
		return true;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableGeoMessages() != null) {
			return;
		}
		
		/* create a list of SAWMessages that have copies left to distribute */
		@SuppressWarnings(value = "unchecked")
		List<GeoMessage> copiesLeft = sortByQueueMode(getGeoMessagesWithCopiesLeft());
		
		if (copiesLeft.size() > 0) {
			/* try to send those messages */
			this.tryMessagesToConnections(copiesLeft, getConnections());
		}
	}
	
	/**
	 * Creates and returns a list of messages this router is currently
	 * carrying and still has copies left to distribute (nrof copies > 1).
	 * @return A list of messages that have copies left
	 */
	protected List<GeoMessage> getGeoMessagesWithCopiesLeft() {
		List<GeoMessage> list = new ArrayList<GeoMessage>();

		for (GeoMessage m : getGeoMessageCollection()) {
			Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
			assert nrofCopies != null : "GSAF message " + m + " didn't have " + 
				"nrof copies property!";
			if (nrofCopies > 0) {
				list.add(m);
			}
		}
		
		return list;
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
	}
	
	@Override
	public GSAFRouter replicate() {
		return new GSAFRouter(this);
	}
}
