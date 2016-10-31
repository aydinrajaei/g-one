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
 * Reports delivered messages
 * report: 
 *  message_id creation_time deliver_time (duplicate)
 */
public class GeoMessageReport extends Report implements MessageListener, GeoMessageListener {
	public static final String HEADER =
	    "# messages: ID, start time, end time";
	/** all message delays */
	
	/**
	 * Constructor.
	 */
	public GeoMessageReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		write(HEADER);
	}
	
	public void newMessage(Message m) {}
	
	public void newGeoMessage(GeoMessage m) {}
	
	public void messageTransferred(Message m, DTNHost from, DTNHost to, 
			boolean firstDelivery) {
		if (firstDelivery) {
			write(m.getId() + " " 
					+ format(m.getCreationTime()) + " "  
					+ format(getSimTime()));
		} else {
			if (to.getAddress() == m.getTo().getAddress()) {
				write(m.getId() + " " 
						+ format(m.getCreationTime()) + " "  
						+ format(getSimTime()) + " duplicate");
			}
		}
	}
	
	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, GeoDTNHost to, 
			boolean firstDelivery) {
		if (firstDelivery) {
			write(m.getId() + " " 
					+ format(m.getCreationTime()) + " "  
					+ format(getSimTime()));
		} else {
			if (m.getTo().checkThePoint(to.getLocation())) { 
				write(m.getId() + " " 
						+ format(m.getCreationTime()) + " "  
						+ format(getSimTime()) + " duplicate");
			}
		}
	}

	@Override
	public void done() {
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
