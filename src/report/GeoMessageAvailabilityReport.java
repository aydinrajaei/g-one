/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package report;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import core.Connection;
import core.DTNHost;
import core.GeoDTNHost;
import core.Message;
import core.GeoMessage;
import core.Settings;
import core.SettingsError;

/**
 * Reports which messages are available (either in the buffer or at one
 * of the connected hosts' buffer) for certain, randomly selected,
 * tracked hosts. Supports the same settings as the 
 * {@link MessageLocationReport}
 */
public class GeoMessageAvailabilityReport extends GeoMessageLocationReport {

	/** Number of tracked hosts -setting id ({@value}). Defines how many 
	 * hosts are selected for sampling message availability */
	public static final String NROF_HOSTS_S = "nrofHosts";
	
	private int nrofHosts;
	private Set<DTNHost> trackedHosts;	
	private Random rng;
	
	public GeoMessageAvailabilityReport() {
		super();		
		Settings s = getSettings();
		nrofHosts = s.getInt(NROF_HOSTS_S, -1);
		this.rng = new Random(nrofHosts);
		
		this.trackedHosts = null;
	}

	/**
	 * Randomly selects the hosts to track
	 * @param hosts All hosts in the scenario
	 * @return The set of tracked hosts
	 */
	private Set<DTNHost> selectTrackedHosts(List<? extends DTNHost> hosts) { //#changed list<DTNHost> to list<? extends DTNHost>
		Set<DTNHost> trackedHosts = new HashSet<DTNHost>();

		if (this.nrofHosts > hosts.size()) {
			throw new SettingsError("Can't use more hosts than there are " +
					"in the simulation scenario");
		}
			
		
		for (int i=0; i<nrofHosts; i++) {
			DTNHost nextHost = hosts.get(rng.nextInt(hosts.size()));
			if (trackedHosts.contains(nextHost)) {
				i--;
			} else {
				trackedHosts.add(nextHost);
			}
		}
		
		return trackedHosts;
	}
	
	/**
	 * Creates a snapshot of message availability
	 * @param trackedHosts The list of hosts in the world
	 */
	@Override
	protected void createSnapshot(List<? extends DTNHost> hosts) { //#changed list<DTNHost> to list<? extends DTNHost>		
		write("[" + (int) getSimTime() + "]"); /* write sim time stamp */
		
		if (this.trackedHosts == null) {
			this.trackedHosts = selectTrackedHosts(hosts);
		}
		
		for (DTNHost host : hosts) {
			Set<String> msgIds = null;
			String idString = "";
			
			if (! this.trackedHosts.contains(host)) {
				continue;
			}
			
			msgIds = new HashSet<String>();
			
			/* add own messages */
			for (Message m : host.getMessageCollection()) {
				if (!isTracked(m)) {
					continue;
				}				
				msgIds.add(m.getId());
			}
			for (GeoMessage m : ((GeoDTNHost) host).getGeoMessageCollection()) {
				if (!isTracked(m)) {
					continue;
				}				
				msgIds.add(m.getId());
			}
			/* add all peer messages */
			for (Connection c : host.getConnections()) {
				DTNHost peer = c.getOtherNode(host);
				for (Message m : peer.getMessageCollection()) {
					if (!isTracked(m)) {
						continue;
					}
					msgIds.add(m.getId());					
				}
				for (GeoMessage m : ((GeoDTNHost) peer).getGeoMessageCollection()) {
					if (!isTracked(m)) {
						continue;
					}
					msgIds.add(m.getId());					
				}
			}
			
			for (String id : msgIds) {
				idString += " " + id;
			}
			
			write(host + idString);				
		}		
	}
}