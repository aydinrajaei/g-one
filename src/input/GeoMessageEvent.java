/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package input;

import core.Cast;

/**
 * A geo_message related external event
 */
public abstract class GeoMessageEvent extends ExternalEvent {
	/** address of the node the message is from */
	protected int fromAddr;
	/** address of the Cast the message is to */
	protected Cast toAddr;
	/** identifier of the message */
	protected String id;
	
	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param to Which Cast the message goes to 
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 */
	public GeoMessageEvent(int from, Cast to, String id, double time) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "MSG @" + this.time + " " + id;
	}
}
