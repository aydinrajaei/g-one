/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package report;

import java.util.List;

import core.DTNHost;
import core.GeoDTNHost;
import core.Message;
import core.GeoMessage;
import core.MessageListener;
import core.GeoMessageListener;

/**
 * Report information about all delivered messages. Messages created during
 * the warm up period are ignored.
 * For output syntax, see {@link #HEADER}.
 */
public class GeoDeliveredMessagesReport extends Report implements MessageListener, GeoMessageListener {
	public static String HEADER = "# time  ID  size  hopcount  deliveryTime  " +
		"fromHost  toHost  remainingTtl  isResponse  path";

	/**
	 * Constructor.
	 */
	public GeoDeliveredMessagesReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		write(HEADER);
	}

	/** 
	 * Returns the given messages hop path as a string
	 * @param m The message
	 * @return hop path as a string
	 */
	private String getPathString(Message m) {
		List<DTNHost> hops = m.getHops();
		String str = m.getFrom().toString();
		
		for (int i=1; i<hops.size(); i++) {
			str += "->" + hops.get(i); 
		}
		
		return str;
	}
	
	/** 
	 * Returns the given geomessages hop path as a string
	 * @param m The geomessage
	 * @return hop path as a string
	 */
	private String getPathString(GeoMessage m) {
		List<DTNHost> hops = m.getHops();
		String str = m.getFrom().toString();
		
		for (int i=1; i<hops.size(); i++) {
			str += "->" + hops.get(i); 
		}
		
		return str;
	}
	
	public void messageTransferred(Message m, DTNHost from, DTNHost to, 
			boolean firstDelivery) {
		if (!isWarmupID(m.getId()) && firstDelivery) {
			int ttl = m.getTtl();
			write(format(getSimTime()) + " " + m.getId() + " " + 
					m.getSize() + " " + m.getHopCount() + " " + 
					format(getSimTime() - m.getCreationTime()) + " " + 
					m.getFrom() + " " + m.getTo() + " " +
					(ttl != Integer.MAX_VALUE ? ttl : "n/a") +  
					(m.isResponse() ? " Y " : " N ") + getPathString(m));
		}
	}

	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, GeoDTNHost to, 
			boolean firstDelivery) {
		if (!isWarmupID(m.getId()) && firstDelivery) {
			int ttl = m.getTtl();
			write(format(getSimTime()) + " " + m.getId() + " " + 
					m.getSize() + " " + m.getHopCount() + " " + 
					format(getSimTime() - m.getCreationTime()) + " " + 
					m.getFrom() + " " + m.getTo() + " " +
					(ttl != Integer.MAX_VALUE ? ttl : "n/a") +  
					(m.isResponse() ? " Y " : " N ") + getPathString(m));
		}
	}
	
	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
		}
	}
	
	public void newGeoMessage(GeoMessage m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
		}
	}
	
	// nothing to implement for the rest
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
	public void geoMessageDeleted(GeoMessage m, GeoDTNHost where, boolean dropped) {}
	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}
	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}

	@Override
	public void done() {
		super.done();
	}
}
