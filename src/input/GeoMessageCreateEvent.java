/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package input;

import core.Cast;
import core.DTNHost;
import core.GeoDTNHost;
import core.GeoMessage;
import core.GeoWorld;
import core.World;

/**
 * External event for creating a  geo_message.
 */
public class GeoMessageCreateEvent extends GeoMessageEvent {
	private int size;
	private int responseSize;
	
	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Which Cast the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public GeoMessageCreateEvent(int from, Cast to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
	}

	
	/**
	 * Creates the geo_message this event represents. 
	 */
	@Override
	public void processEvent(World world) {
		Cast to = this.toAddr;
		GeoDTNHost from = (GeoDTNHost) world.getNodeByAddress(this.fromAddr);			
		
		GeoMessage m = new GeoMessage(from, to, this.id, this.size);
		m.setResponseSize(this.responseSize);
		from.createNewGeoMessage(m);
	}
	
	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}
}
