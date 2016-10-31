/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package routing;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import routing.util.RoutingInfo;

import util.Tuple;

import core.Application;
import core.Connection;
import core.GeoDTNHost; //#changed
import core.GeoMessage; //#changed
import core.GeoMessageListener; //#changed
import core.Settings;
import core.SettingsError;
import core.SimClock;
import core.SimError;

/**
 * Superclass for geomessage routers.
 */
public abstract class GeoMessageRouter {
	/** GeoMessage buffer size -setting id ({@value}). Integer value in bytes.*/
	public static final String B_SIZE_S = "bufferSize";
	/**
	 * GeoMessage TTL -setting id ({@value}). Value is in minutes and must be
	 * an integer. 
	 */ 
	public static final String MSG_TTL_S = "msgTtl";
	/**
	 * GeoMessage/fragment sending queue type -setting id ({@value}). 
	 * This setting affects the order the geomessages and fragments are sent if the
	 * routing protocol doesn't define any particular order (e.g, if more than 
	 * one message can be sent directly to the final recipient). 
	 * Valid values are<BR>
	 * <UL>
	 * <LI/> 1 : random (message order is randomized every time; default option)
	 * <LI/> 2 : FIFO (most recently received messages are sent last)
	 * </UL>
	 */ 
	public static final String SEND_QUEUE_MODE_S = "sendQueue";
	
	/** Setting value for random queue mode */
	public static final int Q_MODE_RANDOM = 1;
	/** Setting value for FIFO queue mode */
	public static final int Q_MODE_FIFO = 2;
	/** Setting value for LIFO queue mode */
	public static final int Q_MODE_LIFO = 3;
	/** Setting value for HTFO queue mode */
	public static final int Q_MODE_HTFO = 4;
	/** Setting value for LTFO queue mode */
	public static final int Q_MODE_LTFO = 5;
	
	/* Return values when asking to start a transmission:
	 * RCV_OK (0) means that the host accepts the message and transfer started, 
	 * values < 0 mean that the  receiving host will not accept this 
	 * particular message (right now), 
	 * values > 0 mean the host will not right now accept any message. 
	 * Values in the range [-100, 100] are reserved for general return values
	 * (and specified here), values beyond that are free for use in 
	 * implementation specific cases */
	/** Receive return value for OK */
	public static final int RCV_OK = 0;
	/** Receive return value for busy receiver */
	public static final int TRY_LATER_BUSY = 1;
	/** Receive return value for an old (already received) message */
	public static final int DENIED_OLD = -1;
	/** Receive return value for not enough space in the buffer for the msg */
	public static final int DENIED_NO_SPACE = -2;
	/** Receive return value for messages whose TTL has expired */
	public static final int DENIED_TTL = -3;
	/** Receive return value for a node low on some resource(s) */
	public static final int DENIED_LOW_RESOURCES = -4;
	/** Receive return value for a node low on some resource(s) */
	public static final int DENIED_POLICY = -5;
	/** Receive return value for unspecified reason */
	public static final int DENIED_UNSPECIFIED = -99;
	
	private List<GeoMessageListener> gmListeners;
	/** The geomessages being transferred with msgID_hostName keys */
	private HashMap<String, GeoMessage> incomingGeoMessages;
	/** The geomessages this router is carrying */
	private HashMap<String, GeoMessage> geomessages; 
	/** The geomessages this router has received as the final recipient */
	private HashMap<String, GeoMessage> deliveredGeoMessages;
	/** The geomessages that Applications on this router have blacklisted */
	private HashMap<String, Object> blacklistedGeoMessages;
	/** Host where this router belongs to */
	private GeoDTNHost geohost;
	/** size of the buffer */
	private int bufferSize;
	/** TTL for all messages */
	protected int msgTtl;
	/** Queue mode for sending geomessages */
	private int sendQueueMode;

	/** applications attached to the host */
	private HashMap<String, Collection<Application>> applications = null;
	
	/**
	 * Constructor. Creates a new geomessage router based on the settings in
	 * the given Settings object. Size of the geomessage buffer is read from
	 * {@link #B_SIZE_S} setting. Default value is Integer.MAX_VALUE.
	 * @param s The settings object
	 */
	public GeoMessageRouter(Settings s) {
		this.bufferSize = Integer.MAX_VALUE; // defaults to rather large buffer	
		this.msgTtl = GeoMessage.INFINITE_TTL;
		this.applications = new HashMap<String, Collection<Application>>();
		
		if (s.contains(B_SIZE_S)) {
			this.bufferSize = s.getInt(B_SIZE_S);
		}
		if (s.contains(MSG_TTL_S)) {
			this.msgTtl = s.getInt(MSG_TTL_S);
		}
		if (s.contains(SEND_QUEUE_MODE_S)) {
			this.sendQueueMode = s.getInt(SEND_QUEUE_MODE_S);
			if (sendQueueMode < 1 || sendQueueMode > 5) {
				throw new SettingsError("Invalid value for " + 
						s.getFullPropertyName(SEND_QUEUE_MODE_S));
			}
		}
		else {
			sendQueueMode = Q_MODE_RANDOM;
		}
		
	}
	
	/**
	 * Initializes the router; i.e. sets the host this router is in and
	 * geomessage listeners that need to be informed about geomessage related
	 * events etc.
	 * @param host The host this router is in
	 * @param mListeners The message listeners
	 */
	public void init(GeoDTNHost geohost, List<GeoMessageListener> gmListeners) {
		this.incomingGeoMessages = new HashMap<String, GeoMessage>();
		this.geomessages = new HashMap<String, GeoMessage>();
		this.deliveredGeoMessages = new HashMap<String, GeoMessage>();
		this.blacklistedGeoMessages = new HashMap<String, Object>();
		this.gmListeners = gmListeners;
		this.geohost = geohost;
	}
	
	/**
	 * Copy-constructor.
	 * @param r Router to copy the settings from.
	 */
	protected GeoMessageRouter(GeoMessageRouter r) {
		this.bufferSize = r.bufferSize;
		this.msgTtl = r.msgTtl;
		this.sendQueueMode = r.sendQueueMode;

		this.applications = new HashMap<String, Collection<Application>>();
		for (Collection<Application> apps : r.applications.values()) {
			for (Application app : apps) {
				addApplication(app.replicate());
			}
		}
	}
	
	/**
	 * Updates router.
	 * This method should be called (at least once) on every simulation
	 * interval to update the status of transfer(s). 
	 */
	public void update(){
		for (Collection<Application> apps : this.applications.values()) {
			for (Application app : apps) {
				app.update(this.geohost);
			}
		}
	}
	
	/**
	 * Informs the router about change in connections state.
	 * @param con The connection that changed
	 */
	public abstract void changedConnection(Connection con);	
	
	/**
	 * Returns a geomessage by ID.
	 * @param id ID of the geomessage
	 * @return The geomessage
	 */
	protected GeoMessage getGeoMessage(String id) {
		return this.geomessages.get(id);
	}
	
	/**
	 * Checks if this router has a geomessage with certain id buffered.
	 * @param id Identifier of the geomessage
	 * @return True if the router has geomessage with this id, false if not
	 */
	public boolean hasGeoMessage(String id) {
		return this.geomessages.containsKey(id);
	}
	
	/**
	 * Returns true if a full geomessage with same ID as the given geomessage has been
	 * received by this geohost as the <strong>final</strong> recipient 
	 * (at least once).
	 * @param m geomessage we're interested of
	 * @return true if a geomessage with the same ID has been received by 
	 * this host as the final recipient.
	 */
	public boolean isDeliveredGeoMessage(GeoMessage m) {
		return (this.deliveredGeoMessages.containsKey(m.getId()));
	}
	
	/** 
	 * Returns <code>true</code> if the geomessage has been blacklisted. GeoMessages
	 * get blacklisted when an application running on the node wants to drop it.
	 * This ensures the peer doesn't try to constantly send the same geomessage to
	 * this node, just to get dropped by an application every time.
	 * 
	 * @param id	id of the geomessage
	 * @return <code>true</code> if blacklisted, <code>false</code> otherwise.
	 */
	protected boolean isBlacklistedGeoMessage(String id) {
		return this.blacklistedGeoMessages.containsKey(id);
	}
	
	/**
	 * Returns a reference to the geomessages of this router in collection.
	 * <b>Note:</b> If there's a chance that some geomessage(s) from the collection
	 * could be deleted (or added) while iterating through the collection, a
	 * copy of the collection should be made to avoid concurrent modification
	 * exceptions. 
	 * @return a reference to the geomessages of this router in collection
	 */
	public Collection<GeoMessage> getGeoMessageCollection() {
		return this.geomessages.values();
	}
	
	/**
	 * Returns the number of geomessages this router has
	 * @return How many geomessages this router has
	 */
	public int getNrofGeoMessages() {
		return this.geomessages.size();
	}
	
	/**
	 * Returns the size of the geomessage buffer.
	 * @return The size or Integer.MAX_VALUE if the size isn't defined.
	 */
	public int getBufferSize() {
		return this.bufferSize;
	}
	
	/**
	 * Returns the amount of free space in the buffer. May return a negative
	 * value if there are more geomessages in the buffer than should fit there
	 * (because of creating new geomessages).
	 * @return The amount of free space (Integer.MAX_VALUE if the buffer
	 * size isn't defined)
	 */
	public int getFreeBufferSize() {
		int occupancy = 0;
		
		if (this.getBufferSize() == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		
		for (GeoMessage m : getGeoMessageCollection()) {
			occupancy += m.getSize();
		}
		
		return this.getBufferSize() - occupancy;
	}
	
	/**
	 * Returns the geohost this router is in
	 * @return The geohost object
	 */
	protected GeoDTNHost getGeoHost() {
		return this.geohost;
	}
	
	/**
	 * Start sending a message to another geohost.
	 * @param id Id of the geomessage to send
	 * @param to The geohost to send the geomessage to
	 */
	public void sendGeoMessage(String id, GeoDTNHost to) {
		GeoMessage m = getGeoMessage(id);
		GeoMessage m2;
		if (m == null) throw new SimError("no geomessage for id " +
				id + " to send at " + this.geohost);
 
		m2 = m.replicate();	// send a replicate of the geomessage
		to.receiveGeoMessage(m2, this.geohost);
	}
	
	/**
	 * Requests for deliverable geomessage from this router to be sent trough a
	 * connection.
	 * @param con The connection to send the geomessages trough
	 * @return True if this router started a transfer, false if not
	 */
	public boolean requestDeliverableGeoMessages(Connection con) {
		return false; // default behavior is to not start -- subclasses override
	}
	
	/**
	 * Try to start receiving a geomessage from another host.
	 * @param m GeoMessage to put in the receiving buffer
	 * @param from Who the geomessage is from
	 * @return Value zero if the node accepted the message (RCV_OK), value less
	 * than zero if node rejected the message (e.g. DENIED_OLD), value bigger
	 * than zero if the other node should try later (e.g. TRY_LATER_BUSY).
	 */
	public int receiveGeoMessage(GeoMessage m, GeoDTNHost from) {
		GeoMessage newGeoMessage = m.replicate();
				
		this.putToIncomingBuffer(newGeoMessage, from);		
		newGeoMessage.addNodeOnPath(this.geohost);
		
		for (GeoMessageListener gml : this.gmListeners) {
			gml.geoMessageTransferStarted(newGeoMessage, from, getGeoHost());
		}
		
		return RCV_OK; // superclass always accepts messages
	}
	
	/**
	 * This method should be called (on the receiving host) after a geomessage
	 * was successfully transferred. The transferred geomessage is put to the
	 * geomessage buffer unless this host is the final recipient of the geomessage.
	 * @param id Id of the transferred geomessage
	 * @param from Host the geomessage was from (previous hop)
	 * @return The geomessage that this host received
	 */
	public GeoMessage geoMessageTransferred(String id, GeoDTNHost from) {
		GeoMessage incoming = removeFromIncomingBuffer(id, from);
		boolean isFinalRecipient;
		boolean isFirstDelivery; // is this first delivered instance of the msg
		
		
		if (incoming == null) {
			throw new SimError("No geomessage with ID " + id + " in the incoming "+
					"buffer of " + this.geohost);
		}
		
		incoming.setReceiveTime(SimClock.getTime());
		
		// Pass the geomessage to the application (if any) and get outgoing geomessage
		GeoMessage outgoing = incoming;
		for (Application app : getApplications(incoming.getAppID())) {
			// Note that the order of applications is significant
			// since the next one gets the output of the previous.
			outgoing = app.handle(outgoing, this.geohost);
			if (outgoing == null) break; // Some app wanted to drop the message
		}
		
		GeoMessage aGeoMessage = (outgoing==null)?(incoming):(outgoing);
		// If the application re-targets the message (changes 'to')
		// then the message is not considered as 'delivered' to this host.
		isFinalRecipient = aGeoMessage.getTo().checkThePoint(this.geohost.getLocation());
		isFirstDelivery = isFinalRecipient &&
		!isDeliveredGeoMessage(aGeoMessage);

		if (!isFinalRecipient && outgoing!=null) {
			// not the final recipient and app doesn't want to drop the message
			// -> put to buffer
			addToGeoMessages(aGeoMessage, false);
		} else if (isFinalRecipient) {
			if (isFirstDelivery) {
			this.deliveredGeoMessages.put(id, aGeoMessage); 
			}
			// -> put to buffer (because of the nature of the Geocasting)
			addToGeoMessages(aGeoMessage, false);
		} else if (outgoing == null) {
			// Blacklist geomessages that an app wants to drop.
			// Otherwise the peer will just try to send it back again.
			this.blacklistedGeoMessages.put(id, null);
		}
		
		for (GeoMessageListener gml : this.gmListeners) {
			gml.geoMessageTransferred(aGeoMessage, from, this.geohost,
					isFirstDelivery);
		}
		
		return aGeoMessage;
	}
	
	/**
	 * Puts a geomessage to incoming geomessages buffer. Two geomessages with the
	 * same ID are distinguished by the from geohost.
	 * @param m The geomessage to put
	 * @param from Who the geomessage was from (previous hop).
	 */
	protected void putToIncomingBuffer(GeoMessage m, GeoDTNHost from) {
		this.incomingGeoMessages.put(m.getId() + "_" + from.toString(), m);
	}
	
	/**
	 * Removes and returns a geomessage with a certain ID from the incoming 
	 * geomessages buffer or null if such geomessage wasn't found. 
	 * @param id ID of the geomessage
	 * @param from The geohost that sent this geomessage (previous hop)
	 * @return The found geomessage or null if such geomessage wasn't found
	 */
	protected GeoMessage removeFromIncomingBuffer(String id, GeoDTNHost from) {
		return this.incomingGeoMessages.remove(id + "_" + from.toString());
	}
	
	/**
	 * Returns true if a geomessage with the given ID is one of the
	 * currently incoming geomessages, false if not
	 * @param id ID of the geomessage
	 * @return True if such geomessage is incoming right now
	 */
	protected boolean isIncomingGeoMessage(String id) {
		return this.incomingGeoMessages.containsKey(id);
	}
	
	/**
	 * Adds a geomessage to the geomessage buffer and informs geomessage listeners
	 * about new geomessage (if requested).
	 * @param m The geomessage to add
	 * @param newGeoMessage If true, geomessage listeners are informed about a new
	 * geomessage, if false, nothing is informed.
	 */
	protected void addToGeoMessages(GeoMessage m, boolean newGeoMessage) {
		this.geomessages.put(m.getId(), m);
		
		if (newGeoMessage) {
			for (GeoMessageListener gml : this.gmListeners) {
				gml.newGeoMessage(m);
			}
		}
	}
	
	/**
	 * Removes and returns a geomessage from the geomessage buffer.
	 * @param id Identifier of the geomessage to remove
	 * @return The removed geomessage or null if geomessage for the ID wasn't found
	 */
	protected GeoMessage removeFromGeoMessages(String id) {
		GeoMessage m = this.geomessages.remove(id);
		return m;
	}
	
	/**
	 * This method should be called (on the receiving host) when a gomessage 
	 * transfer was aborted.
	 * @param id Id of the geomessage that was being transferred
	 * @param from Host the geomessage was from (previous hop)
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void geoMessageAborted(String id, GeoDTNHost from, int bytesRemaining) {
		GeoMessage incoming = removeFromIncomingBuffer(id, from);
		if (incoming == null) {
			throw new SimError("No incoming geomessage for id " + id + 
					" to abort in " + this.geohost);
		}		
		
		for (GeoMessageListener gml : this.gmListeners) {
			gml.geoMessageTransferAborted(incoming, from, this.geohost);
		}
	}
	
	/**
	 * Creates a new geomessage to the router.
	 * @param m The geomessage to create
	 * @return True if the creation succeeded, false if not (e.g.
	 * the message was too big for the buffer)
	 */
	public boolean createNewGeoMessage(GeoMessage m) {
		m.setTtl(this.msgTtl);
		addToGeoMessages(m, true);		
		return true;
	}
	
	/**
	 * Deletes a geomessage from the buffer and informs geomessage listeners
	 * about the event
	 * @param id Identifier of the geomessage to delete
	 * @param drop If the message is dropped (e.g. because of full buffer) this 
	 * should be set to true. False value indicates e.g. remove of geomessage
	 * because it was delivered to final destination.  
	 */
	public void deleteGeoMessage(String id, boolean drop) {
		GeoMessage removed = removeFromGeoMessages(id); 
		if (removed == null) throw new SimError("no geomessage for id " +
				id + " to remove at " + this.geohost);
		
		for (GeoMessageListener gml : this.gmListeners) {
			gml.geoMessageDeleted(removed, this.geohost, drop);
		}
	}
	
	/**
	 * Sorts/shuffles the given list according to the current sending queue 
	 * mode. The list can contain either GeoMessage or Tuple<GeoMessage, GeoConnection> 
	 * objects. Other objects cause error. 
	 * @param list The list to sort or shuffle
	 * @return The sorted/shuffled list
	 */
	@SuppressWarnings(value = "unchecked") /* ugly way to make this generic */
	protected List sortByQueueMode(List list) {
		switch (sendQueueMode) {
		case Q_MODE_RANDOM:
			Collections.shuffle(list, new Random(SimClock.getIntTime()));
			break;
		case Q_MODE_FIFO:
			Collections.sort(list, 
					new Comparator() {
				/** Compares two tuples by their geomessages' receiving time */
				public int compare(Object o1, Object o2) {
					double diff;
					GeoMessage m1, m2;
					
					if (o1 instanceof Tuple) {
						m1 = ((Tuple<GeoMessage, Connection>)o1).getKey();
						m2 = ((Tuple<GeoMessage, Connection>)o2).getKey();
					}
					else if (o1 instanceof GeoMessage) {
						m1 = (GeoMessage)o1;
						m2 = (GeoMessage)o2;
					}
					else {
						throw new SimError("Invalid type of objects in " + 
								"the list");
					}
					
					diff = m1.getReceiveTime() - m2.getReceiveTime();
					if (diff == 0) {
						return 0;
					}
					return (diff < 0 ? -1 : 1);
				}
			});
			break;
		case Q_MODE_LIFO:
			Collections.sort(list, 
					new Comparator() {
				/** Compares two tuples by their geomessages' receiving time */
				public int compare(Object o1, Object o2) {
					double diff;
					GeoMessage m1, m2;
					
					if (o1 instanceof Tuple) {
						m1 = ((Tuple<GeoMessage, Connection>)o1).getKey();
						m2 = ((Tuple<GeoMessage, Connection>)o2).getKey();
					}
					else if (o1 instanceof GeoMessage) {
						m1 = (GeoMessage)o1;
						m2 = (GeoMessage)o2;
					}
					else {
						throw new SimError("Invalid type of objects in " + 
								"the list");
					}
					
					diff = m2.getReceiveTime() - m1.getReceiveTime();
					if (diff == 0) {
						return 0;
					}
					return (diff < 0 ? -1 : 1);
				}
			});
			break;
		case Q_MODE_HTFO:
			Collections.sort(list, 
					new Comparator() {
				/** Compares two tuples by their geomessages' receiving time */
				public int compare(Object o1, Object o2) {
					double diff;
					GeoMessage m1, m2;
					
					if (o1 instanceof Tuple) {
						m1 = ((Tuple<GeoMessage, Connection>)o1).getKey();
						m2 = ((Tuple<GeoMessage, Connection>)o2).getKey();
					}
					else if (o1 instanceof GeoMessage) {
						m1 = (GeoMessage)o1;
						m2 = (GeoMessage)o2;
					}
					else {
						throw new SimError("Invalid type of objects in " + 
								"the list");
					}
					
					diff = m2.getTtl() - m1.getTtl();
					if (diff == 0) {
						return 0;
					}
					return (diff < 0 ? -1 : 1);
				}
			});
			break;
		case Q_MODE_LTFO:
			Collections.sort(list, 
					new Comparator() {
				/** Compares two tuples by their geomessages' receiving time */
				public int compare(Object o1, Object o2) {
					double diff;
					GeoMessage m1, m2;
					
					if (o1 instanceof Tuple) {
						m1 = ((Tuple<GeoMessage, Connection>)o1).getKey();
						m2 = ((Tuple<GeoMessage, Connection>)o2).getKey();
					}
					else if (o1 instanceof GeoMessage) {
						m1 = (GeoMessage)o1;
						m2 = (GeoMessage)o2;
					}
					else {
						throw new SimError("Invalid type of objects in " + 
								"the list");
					}
					
					diff = m1.getTtl() - m2.getTtl();
					if (diff == 0) {
						return 0;
					}
					return (diff < 0 ? -1 : 1);
				}
			});
			break;
		/* add more queue modes here */
		default:
			throw new SimError("Unknown queue mode " + sendQueueMode);
		}
		
		return list;
	}

	/**
	 * Gives the order of the two given geomessages as defined by the current
	 * queue mode 
	 * @param m1 The first geomessage
	 * @param m2 The second geomessage
	 * @return -1 if the first geomessage should come first, 1 if the second 
	 *          geomessage should come first, or 0 if the ordering isn't defined
	 */
	protected int compareByQueueMode(GeoMessage m1, GeoMessage m2) {
		switch (sendQueueMode) {
		case Q_MODE_RANDOM:
			/* return randomly (enough) but consistently -1, 0 or 1 */
			return (m1.hashCode()/2 + m2.hashCode()/2) % 3 - 1; 
		case Q_MODE_FIFO:
			double diff1 = m1.getReceiveTime() - m2.getReceiveTime();
			if (diff1 == 0) {
				return 0;
			}
			return (diff1 < 0 ? -1 : 1);
		case Q_MODE_LIFO:
			double diff2 = m2.getReceiveTime() - m1.getReceiveTime();
			if (diff2 == 0) {
				return 0;
			}
			return (diff2 < 0 ? -1 : 1);
		case Q_MODE_HTFO:
			double diff3 = m2.getTtl() - m1.getTtl();
			if (diff3 == 0) {
				return 0;
			}
			return (diff3 < 0 ? -1 : 1);
		case Q_MODE_LTFO:
			double diff4 = m1.getTtl() - m2.getTtl();
			if (diff4 == 0) {
				return 0;
			}
			return (diff4 < 0 ? -1 : 1);
		/* add more queue modes here */
		default:
			throw new SimError("Unknown queue mode " + sendQueueMode);
		}
	}
	
	/**
	 * Returns routing information about this router.
	 * @return The routing information.
	 */
	public RoutingInfo getGeoRoutingInfo() {
		RoutingInfo ri = new RoutingInfo(this);
		RoutingInfo incoming = new RoutingInfo(this.incomingGeoMessages.size() + 
				" incoming geomessage(s)");
		RoutingInfo delivered = new RoutingInfo(this.deliveredGeoMessages.size() +
				" delivered geomessage(s)");
		
		RoutingInfo cons = new RoutingInfo(geohost.getConnections().size() + 
			" connection(s)");
				
		ri.addMoreInfo(incoming);
		ri.addMoreInfo(delivered);
		ri.addMoreInfo(cons);
		
		for (GeoMessage m : this.incomingGeoMessages.values()) {
			incoming.addMoreInfo(new RoutingInfo(m));
		}
		
		for (GeoMessage m : this.deliveredGeoMessages.values()) {
			delivered.addMoreInfo(new RoutingInfo(m + " path:" + m.getHops()));
		}
		
		for (Connection c : geohost.getConnections()) {
			cons.addMoreInfo(new RoutingInfo(c));
		}

		return ri;
	}
	
	/** 
	 * Adds an application to the attached applications list.
	 * 
	 * @param app	The application to attach to this router.
	 */
	public void addApplication(Application app) {
		if (!this.applications.containsKey(app.getAppID())) {
			this.applications.put(app.getAppID(),
					new LinkedList<Application>());
		}
		this.applications.get(app.getAppID()).add(app);
	}
	
	/** 
	 * Returns all the applications that want to receive geomessages for the given
	 * application ID.
	 * 
	 * @param ID	The application ID or <code>null</code> for all apps.
	 * @return		A list of all applications that want to receive the message.
	 */
	public Collection<Application> getApplications(String ID) {
		LinkedList<Application>	apps = new LinkedList<Application>();
		// Applications that match
		Collection<Application> tmp = this.applications.get(ID);
		if (tmp != null) {
			apps.addAll(tmp);
		}
		// Applications that want to look at all messages
		if (ID != null) {
			tmp = this.applications.get(null);
			if (tmp != null) {
				apps.addAll(tmp);
			}
		}
		return apps;
	}

	/**
	 * Creates a replicate of this georouter. The replicate has the same
	 * settings as this router but empty buffers and georouting tables.
	 * @return The replicate
	 */
	public abstract GeoMessageRouter replicate();
	
	/**
	 * Returns a String presentation of this router
	 * @return A String presentation of this router
	 */
	public String toString() {
		return getClass().getSimpleName() + " of " + 
			this.getGeoHost().toString() + " with " + getNrofGeoMessages() 
			+ " geomessages";
	}
}
