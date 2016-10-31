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
 * Report for of amount of messages delivered vs. time. A new report line
 * is created every time when either a message is created or delivered.
 * Messages created during the warm up period are ignored.
 * For output syntax, see {@link #HEADER}.
 */
public class GeoMessageDeliveryReport extends Report implements MessageListener, GeoMessageListener {
	public static String HEADER="# time  created  delivered  delivered/created";
	private int created;
	private int delivered;

	/**
	 * Constructor.
	 */
	public GeoMessageDeliveryReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		created = 0;
		delivered = 0;
		write(HEADER);
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to, 
			boolean firstDelivery) {
		if (firstDelivery && !isWarmup() && !isWarmupID(m.getId())) {
			delivered++;
			reportValues();
		}
	}
	
	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, GeoDTNHost to, 
			boolean firstDelivery) {
		if (firstDelivery && !isWarmup() && !isWarmupID(m.getId())) {
			delivered++;
			reportValues();
		}
	}

	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		created++;
		reportValues();
	}
	
	public void newGeoMessage(GeoMessage m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		created++;
		reportValues();
	}
	
	/**
	 * Writes the current values to report file
	 */
	private void reportValues() {
		double prob = (1.0 * delivered) / created;
		write(format(getSimTime()) + " " + created + " " + delivered + 
				" " + format(prob));
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
