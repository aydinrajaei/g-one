/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Implementation of Direction Based router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class DirectionBasedRoutingProtocol extends ActiveRouter {
	/** identifier for the initial number of copies setting ({@value})*/ 
	//public static final String NROF_COPIES = "nrofCopies";
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String DBRP_NS = "DBRSP";
	/** Message property key */
	public static final String MSG_DIRECTION_PROPERTY = DBRP_NS + "." +
		"directions";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = DBRP_NS + "." +
		"copies";
	protected HashSet<Integer> initialDirections;
	protected int initialNrofCopies;

	public DirectionBasedRoutingProtocol(Settings s) {
		super(s);
		//Settings snwSettings = new Settings(DBRP_NS);
		//HashSet<Integer> initialDirections = new HashSet<Integer>();
		//initialDirections.add(1);
		//initialDirections.add(2);
		//initialDirections.add(3);
		//initialDirections.add(4);
		
		initialNrofCopies = 3;
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected DirectionBasedRoutingProtocol(DirectionBasedRoutingProtocol r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
	}
	
	@Override
	public int receiveMessage(Message m, DTNHost from) {
		return super.receiveMessage(m, from);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		
		HashSet<Integer> directions = new HashSet<Integer>();
		directions = (HashSet<Integer>) msg.getProperty(MSG_DIRECTION_PROPERTY);
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);

		assert directions != null : "Not a DBRP message: " + msg;
		assert nrofCopies != null : "Not a DBRP message: " + msg;		

		/* if n is more than one: reduce nrofCopies */
		if (nrofCopies != 0) { nrofCopies--; }
		
		/* if n=1: no more copies need to be forwarded */
		if (nrofCopies != 0) {
			directions.add(1);
			directions.add(2);
			directions.add(3);
			directions.add(4);
			directions.remove(this.getHost().getDirection());
		}
		else{ directions.clear(); }
		
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		msg.updateProperty(MSG_DIRECTION_PROPERTY, directions);
		return msg;
	}
	
	@Override 
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
		HashSet<Integer> directions = new HashSet<Integer>();
		directions.add(1);
		directions.add(2);
		directions.add(3);
		directions.add(4);
		//directions = initialDirections;
		//directions.remove(this.getHost().getDirection());
		msg.addProperty(MSG_DIRECTION_PROPERTY, directions);
		addToMessages(msg, true);
		return true;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		/* create a list of SAWMessages that have copies left to distribute */
		@SuppressWarnings(value = "unchecked")
		List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());
		
		if (copiesLeft.size() > 0) {
			/* try to send those messages */
			this.tryMessagesToConnections(copiesLeft, getConnections());
		}
	}
	
	/**
	 * Tries to send all given messages to all given connections. Connections
	 * are first iterated in the order they are in the list and for every
	 * connection, the messages are tried in the order they are in the list.
	 * Once an accepting connection is found, no other connections or messages
	 * are tried.
	 * @param messages The list of Messages to try
	 * @param connections The list of Connections to try
	 * @return The connections that started a transfer or null if no connection
	 * accepted a message.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Connection tryMessagesToConnections(List<Message> messages,
			List<Connection> connections) {
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			List<Message> readyMessages = messages;
			for (int j=0; j<readyMessages.size(); j++){
				
				HashSet<Integer> directions = new HashSet<Integer>();
				directions = (HashSet<Integer>) readyMessages.get(j).getProperty(MSG_DIRECTION_PROPERTY);
				
				if(!directions.contains(con.getOtherNode(getHost()).getDirection()))
				{
				readyMessages.remove(j);
				}
			}
				
			Message started = tryAllMessages(con, readyMessages); 
			if (started != null) { 
				return con;
			}		
		}
		
		return null;
	}
	
	/**
	 * Creates and returns a list of messages this router is currently
	 * carrying and still has copies left to distribute (!directions.isEmpty();).
	 * @return A list of messages that have copies left
	 */
	@SuppressWarnings("unchecked")
	protected List<Message> getMessagesWithCopiesLeft() {
		List<Message> list = new ArrayList<Message>();

		for (Message m : getMessageCollection()) {
			HashSet<Integer> directions = new HashSet<Integer>();
			directions = (HashSet<Integer>) m.getProperty(MSG_DIRECTION_PROPERTY);
			assert directions != null : "DBRP message " + m + " didn't have " + 
				"any directions!";
			if (!directions.isEmpty()) {
				list.add(m);
			}
		}
		
		return list;
	}
	
	/**
	 * Called just before a transfer is finalized (by 
	 * {@link ActiveRouter#update()}).
	 * Reduces the number of copies we have left for a message. 
	 * In binary Spray and Wait, sending host is left with floor(n/2) copies,
	 * but in standard mode, nrof copies left is reduced by one. 
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void transferDone(Connection con) {
		
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}
		
		/* reduce the amount of copies left */
		HashSet<Integer> directions = new HashSet<Integer>();
		directions = (HashSet<Integer>)msg.getProperty(MSG_DIRECTION_PROPERTY);
		directions.remove(con.getOtherNode(getHost()).getDirection());
		
		msg.updateProperty(MSG_DIRECTION_PROPERTY, directions);
	}
	
	@Override
	public DirectionBasedRoutingProtocol replicate() {
		return new DirectionBasedRoutingProtocol(this);
	}
}
