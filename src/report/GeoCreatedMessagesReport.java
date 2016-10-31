/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package report;

import core.DTNHost;
import core.GeoDTNHost;
import core.Message;
import core.GeoMessage;
import core.MessageListener;
import core.GeoMessageListener;

/**
 * Reports information about all created messages. Messages created during
 * the warm up period are ignored.
 * For output syntax, see {@link #HEADER}.
 */
public class GeoCreatedMessagesReport extends Report implements MessageListener, GeoMessageListener {
	public static String HEADER = "# time  ID  size  fromHost  toHost/toCast  TTL  " + 
		"isResponse";

	/**
	 * Constructor.
	 */
	public GeoCreatedMessagesReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		write(HEADER);
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			return;
		}
		
		int ttl = m.getTtl();
		write(format(getSimTime()) + " " + m.getId() + " " + 
				m.getSize() + " " + m.getFrom() + " " + m.getTo() + " " +
				(ttl != Integer.MAX_VALUE ? ttl : "n/a") +  
				(m.isResponse() ? " Y " : " N "));
	}
	
	public void newGeoMessage(GeoMessage m) {
		if (isWarmup()) {
			return;
		}
		
		int ttl = m.getTtl();
		write(format(getSimTime()) + " " + m.getId() + " " + 
				m.getSize() + " " + m.getFrom() + " " + m.getTo() + " " +
				(ttl != Integer.MAX_VALUE ? ttl : "n/a") +  
				(m.isResponse() ? " Y " : " N "));
	}
	
	// nothing to implement for the rest
	public void messageTransferred(Message m, DTNHost f, DTNHost t,boolean b) {}
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
	public void geoMessageTransferred(GeoMessage m, GeoDTNHost f, GeoDTNHost t,boolean b) {}
	public void geoMessageDeleted(GeoMessage m, GeoDTNHost where, boolean dropped) {}
	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}
	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}

	@Override
	public void done() {
		super.done();
	}
}
