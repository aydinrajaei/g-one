/* 
 * Copyright 2014 Sussex University, Aydin Rajaei
 *  
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * World contains all the nodes and is responsible for updating their
 * location and connections.
 */
public class GeoWorld extends World{

	public GeoWorld(List<GeoDTNHost> hosts, int sizeX, int sizeY,
			double updateInterval, List<UpdateListener> updateListeners,
			boolean simulateConnections, List<EventQueue> eventQueues) {
		
		super(hosts, sizeX, sizeY, updateInterval, updateListeners,
				simulateConnections, eventQueues);
	
	}

	/**
	 * Returns a node from the world by its address
	 * @param address The address of the node
	 * @return The requested node or null if it wasn't found
	 */
	
	@Override
	public GeoDTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Address " +
					"range of 0-" + (hosts.size()-1) + " is valid");
		}

		GeoDTNHost node = (GeoDTNHost) this.hosts.get(address);
		assert node.getAddress() == address : "Node indexing failed. " + 
			"Node " + node + " in index " + address;

		return node; 
	}


}
