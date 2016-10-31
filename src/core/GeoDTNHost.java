/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 * 
 * Modified by: Aydin Rajaei, University of Sussex.
 * The Geo1 Simulator Project.
 *  
 */
package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import movement.MovementModel;
import movement.Path;
import routing.GeoMessageRouter;
import routing.MessageRouter;
import routing.util.RoutingInfo;

/**
 * A "unicast and geocast" DTN capable host.
 * 
 * Modified by: @author Aydin Rajaei
 */
public class GeoDTNHost extends DTNHost {

	private List<GeoMessageListener> gmsgListeners;
	private GeoMessageRouter georouter;
	
	public GeoDTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs, String groupId,
			List<NetworkInterface> interf, ModuleCommunicationBus comBus,
			MovementModel mmProto, MessageRouter mRouterProto, GeoMessageRouter gmRouterProto,
			List<GeoMessageListener> gMsgLs) {
		super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);
		
		this.gmsgListeners = gMsgLs;
		setGeoRouter(gmRouterProto.replicate());
		
		}
	

	/**
	 * Set a georouter for this geohost
	 * @param router The router to set
	 */
	private void setGeoRouter(GeoMessageRouter georouter) {
		georouter.init(this, gmsgListeners);
		this.georouter = georouter;
	}
	
	/**
	 * Returns the georouter of this geohost
	 * @return the georouter of this geohost
	 */
	public GeoMessageRouter getGeoRouter() {
		return this.georouter;
	}

	/**
	 * Returns a copy of the "list of connections", this host has with other hosts
	 * @return a copy of the "list of connections", this host has with other hosts
	 */
	public List<Connection> getConnections() {
		List<Connection> listOfConnections = new ArrayList<Connection>();

		for (NetworkInterface i : net) {
			listOfConnections.addAll(i.getConnections());
		}

		return listOfConnections;
	}

    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	@Override
	public void connectionUp(Connection con) {
		this.router.changedConnection(con);
		this.georouter.changedConnection(con);
	}

    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	@Override
	public void connectionDown(Connection con) {
		this.router.changedConnection(con);
		this.georouter.changedConnection(con);
	}
	
	/**
	 * Returns the geomessages in a collection.
	 * @return GeoMessages in a collection
	 */
	public Collection<GeoMessage> getGeoMessageCollection() {
		return this.georouter.getGeoMessageCollection();
	}

	/**
	 * Returns the number of geomessages this node is carrying.
	 * @return How many geomessages the node is carrying currently.
	 */
	public int getNrofGeoMessages() {
		return this.georouter.getNrofGeoMessages();
	}
	
	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty
	 * buffer but can be over 100 if a created message is bigger than buffer 
	 * space that could be freed.
	 * @return Buffer occupancy percentage
	 */
	public double getGeoBufferOccupancy() {
		double bSize = georouter.getBufferSize();
		double freeBuffer = georouter.getFreeBufferSize();
		return 100*((bSize-freeBuffer)/bSize);
	}

	/**
	 * Returns routing info of this host's router.
	 * @return The routing info.
	 */
	public RoutingInfo getGeoRoutingInfo() {
		return this.georouter.getGeoRoutingInfo();
	}
	
	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	@Override
	public void update(boolean simulateConnections) {
		super.updateDirection();
		
		if (!isRadioActive()) {
			// Make sure inactive nodes don't have connections
			tearDownAllConnections();
			return;
		}
		
		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
		this.router.update();
		this.georouter.update();
	}
	

	/**
	 * Sends a message from this host to another host
	 * @param id Identifier of the message
	 * @param to Host the message should be sent to
	 */
	public void sendGeoMessage(String id, GeoDTNHost to) {
		this.georouter.sendGeoMessage(id, to);
	}

	/**
	 * Start receiving a message from another host
	 * @param m The message
	 * @param from Who the message is from
	 * @return The value returned by 
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int receiveGeoMessage(GeoMessage m, GeoDTNHost from) {
		int retVal = this.georouter.receiveGeoMessage(m, from); 

		if (retVal == GeoMessageRouter.RCV_OK) {
			m.addNodeOnPath(this);	// add this node on the messages path
		}

		return retVal;	
	}

	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableGeoMessages(Connection con) {
		return this.georouter.requestDeliverableGeoMessages(con);
	}

	/**
	 * Informs the host that a message was successfully transferred.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 */
	public void geoMessageTransferred(String id, GeoDTNHost from) {
		this.georouter.geoMessageTransferred(id, from);
	}

	/**
	 * Informs the host that a message transfer was aborted.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void geoMessageAborted(String id, GeoDTNHost from, int bytesRemaining) {
		this.georouter.geoMessageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new geomessage to this host's router
	 * @param m The geomessage to create
	 */
	public void createNewGeoMessage(GeoMessage m) {
		this.georouter.createNewGeoMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * @param id Identifier of the message
	 * @param drop True if the message is deleted because of "dropping"
	 * (e.g. buffer is full) or false if it was deleted for some other reason
	 * (e.g. the message got delivered to final destination). This effects the
	 * way the removing is reported to the message listeners.
	 */
	public void deleteGeoMessage(String id, boolean drop) {
		this.georouter.deleteGeoMessage(id, drop);
	}

}