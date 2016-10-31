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
 * might be the implementation of second chance after cast!!!
 * Implementation of Spray and wait router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class GeoCastPlusRouter extends GeoActiveRouter {
//	
//	/** identifier for the initial number of copies setting ({@value})*/ 
//	public static final String NROF_COPIES = "nrofCopies";
//	/** identifier for the binary-mode setting ({@value})*/ 
//	public static final String BINARY_MODE = "binaryMode";
//	/** SprayAndWait router's settings name space ({@value})*/ 
//	public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
//	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = "GeoCastFinderRouter" + "." +
		"copies";
//	
//	/** Message property key */
	public static final String MSG_CAST_FLAG = "GeoCastPlusRouter" + "." +
		"flag";
//
	protected int initialNrofCopies;
//	protected boolean isBinary;
	
	protected int NROF_COPIES = 3;
	protected boolean Cast_FLAG = false;
	
	public GeoCastPlusRouter(Settings s) {
		super(s);
		//Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
		
		initialNrofCopies = this.NROF_COPIES;
		//isBinary = snwSettings.getBoolean( BINARY_MODE);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GeoCastPlusRouter(GeoCastPlusRouter r) {
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
		Boolean flag = (Boolean)msg.getProperty(MSG_CAST_FLAG);
		
		assert nrofCopies != null : "Not a CastPlus message: " + msg;
		
		if (nrofCopies > 0) {
			nrofCopies --;
		}
		else {
			nrofCopies = 0;
			flag = true;
		}
		
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		msg.updateProperty(MSG_CAST_FLAG, flag);
		return msg;
	}
	
	@Override 
	public boolean createNewGeoMessage(GeoMessage msg) {
		makeRoomForNewGeoMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
		msg.addProperty(MSG_CAST_FLAG, new Boolean(Cast_FLAG));
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
			assert nrofCopies != null : "CastFinder message " + m + " didn't have " + 
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
	 * In binary Spray and Wait, sending host is left with floor(n/2) copies,
	 * but in standard mode, nrof copies left is reduced by one. 
	 */
	@Override
	//senderSide
	protected void transferDone(Connection con) {
		Integer nrofCopies;
		Boolean flag;
		String msgId = con.getGeoMessage().getId();
		/* get this router's copy of the message */
		GeoMessage msg = getGeoMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}
		
		/* reduce the amount of copies left */
		nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		flag = (Boolean)msg.getProperty(MSG_CAST_FLAG); 
		if (nrofCopies > 0) {
			nrofCopies --;
		}
		else {
			nrofCopies = 0;
			flag = true;
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		msg.updateProperty(MSG_CAST_FLAG, flag);
	}
	
	@Override
	protected int startTransfer(GeoMessage m, Connection con) {
		
		int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
		boolean flag = (Boolean)m.getProperty(MSG_CAST_FLAG);
		if (flag == true && !m.getTo().checkThePoint(getGeoHost().getLocation())) 
		{
			flag = false;
			nrofCopies ++;
			m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
			m.updateProperty(MSG_CAST_FLAG, flag);
		}
		
		//if (flag true && !m.getto checkThePoint (theOther Host.getLocation ))
		//then: remove The Message from Buffer

		return super.startTransfer(m, con);	
	}
	
	@Override
	public GeoCastPlusRouter replicate() {
		return new GeoCastPlusRouter(this);
	}
}
