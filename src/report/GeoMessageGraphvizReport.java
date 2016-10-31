/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package report;

import java.util.List;
import java.util.Vector;

import core.DTNHost;
import core.GeoDTNHost;
import core.Message;
import core.GeoMessage;
import core.MessageListener;
import core.GeoMessageListener;

/**
 * Creates a graphviz compatible graph of messages that were passed.
 * Messages created during the warm up period are ignored.
 */
public class GeoMessageGraphvizReport extends Report implements MessageListener, GeoMessageListener {
	/** Name of the graphviz report ({@value})*/
	public static final String GRAPH_NAME = "msggraph";
	private Vector<Message> deliveredMessages;
	private Vector<GeoMessage> deliveredGeoMessages;
	
	/**
	 * Constructor.
	 */
	public GeoMessageGraphvizReport() {
		init();
	}

	protected void init() {
		super.init();
		this.deliveredMessages = new Vector<Message>();
		this.deliveredGeoMessages = new Vector<GeoMessage>();
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
	
	public void messageTransferred(Message m, DTNHost from,
			DTNHost to,	boolean firstDelivery) {
		if (firstDelivery && !isWarmupID(m.getId())) {
			newEvent();
			this.deliveredMessages.add(m);
		}
	}
	
	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from,
			GeoDTNHost to,	boolean firstDelivery) {
		if (firstDelivery && !isWarmupID(m.getId())) {
			newEvent();
			this.deliveredGeoMessages.add(m);
		}
	}

	/* nothing to implement for these */
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {	}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
	public void geoMessageDeleted(GeoMessage m, GeoDTNHost where, boolean dropped) {	}
	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}
	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {}

	@Override
	public void done() {
		write("/* scenario " + getScenarioName() + "\n" +
				deliveredMessages.size() + " messages delivered at " + 
				"sim time " + getSimTime() + "\n" + deliveredGeoMessages.size() + " geomessages delivered at " + 
				"sim time " + getSimTime() + " */") ;
		write("digraph " + GRAPH_NAME + " {");
		setPrefix("\t"); // indent following lines by one tab
		
		for (Message m : deliveredMessages) {
			List<DTNHost> path = m.getHops();
			String pathString = path.remove(0).toString(); // start node

			for (DTNHost next : path) {
				pathString += "->" + next.toString();
			}
			
			write (pathString + ";");
		}
		
		for (GeoMessage m : deliveredGeoMessages) {
			List<DTNHost> path = m.getHops();
			String pathString = path.remove(0).toString(); // start node

			for (DTNHost next : path) {
				pathString += "->" + next.toString();
			}
			
			write (pathString + ";");
		}
		
		setPrefix(""); // don't indent anymore
		write("}");
		
		super.done();
	}

}
