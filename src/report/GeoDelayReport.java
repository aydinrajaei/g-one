/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package report;

import java.util.ArrayList;
import java.util.List;

import core.DTNHost;
import core.GeoDTNHost;
import core.Message;
import core.GeoMessage;
import core.MessageListener;
import core.GeoMessageListener;

/**
 * Reports delivered messages' delays (one line per delivered message)
 * and cumulative delivery probability sorted by message delays.
 * Ignores the messages that were created during the warm up period.
 */
public class GeoDelayReport extends Report implements MessageListener, GeoMessageListener {
	public static final String HEADER =
	    "#messageDelay";
	/** all message delays */
	private List<Double> delays;
	private int nrofCreated;
	
	/**
	 * Constructor.
	 */
	public GeoDelayReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		write(HEADER);
		this.delays = new ArrayList<Double>();
		this.nrofCreated = 0;
	}
	
	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
		}
		if (isCooldown()) {
			addCooldownID(m.getId());
		}
		else {
			this.nrofCreated++;
		}
	}
	
	public void newGeoMessage(GeoMessage m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
		}
		if (isCooldown()) {
			addCooldownID(m.getId());
		}
		else {
			this.nrofCreated++;
		}
	}
	
	public void messageTransferred(Message m, DTNHost from, DTNHost to, 
			boolean firstDelivery) {
		if (firstDelivery && !isWarmupID(m.getId()) && !isCooldownID(m.getId())) {
			this.delays.add(getSimTime() - m.getCreationTime());
		}
		
	}
	
	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, GeoDTNHost to, 
			boolean firstDelivery) {
		if (firstDelivery && !isWarmupID(m.getId()) && !isCooldownID(m.getId())) {
			this.delays.add(getSimTime() - m.getCreationTime());
		}
		
	}

	@Override
	public void done() {
		if (delays.size() == 0) {
			write("# no messages delivered in sim time "+format(getSimTime()));
			super.done();
			return;
		}
		
		write(format(nrofCreated));
		write("Delays");
		
		java.util.Collections.sort(delays);
		
		for (int i=0; i < delays.size(); i++) {
			write(format(delays.get(i)));
		}
		super.done();
	}
	
	// nothing to implement for the rest
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
	public void geoMessageDeleted(GeoMessage m, GeoDTNHost where, boolean dropped) {}
	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}
	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}

}
