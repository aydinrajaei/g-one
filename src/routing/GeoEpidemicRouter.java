/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package routing;

import core.Settings;

/**
 * Epidemic geo-message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class GeoEpidemicRouter extends GeoActiveRouter {
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public GeoEpidemicRouter(Settings s) {
		super(s);
		//TODO: read&use epidemic router specific settings (if any)
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GeoEpidemicRouter(GeoEpidemicRouter r) {
		super(r);
		//TODO: copy epidemic settings here (if any)
	}
			
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableGeoMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		
		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}
	
	
	@Override
	public GeoEpidemicRouter replicate() {
		return new GeoEpidemicRouter(this);
	}

}