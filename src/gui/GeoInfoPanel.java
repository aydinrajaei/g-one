/* 
 * Copyright 2014 Sussex University, Aydin Rajaei
 *  
 */
package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import movement.Path;
import core.DTNHost;
import core.GeoDTNHost;
import core.GeoMessage;
import core.Message;

/**
 * Information panel that shows data of selected messages and nodes.
 */
public class GeoInfoPanel extends JPanel implements ActionListener{
	private JComboBox msgChooser;
	private JComboBox gMsgChooser;
	private JLabel info;
	private JButton infoButton;
	private JButton routingInfoButton;
	private Message selectedMessage;
	private GeoMessage selectedGeoMessage;
	private DTNHost selectedHost;
	private GeoDTNSimGUI gui;
	
	public GeoInfoPanel(GeoDTNSimGUI gui) {
		this.gui = gui;
		reset();
	}
	
	private void reset() {
		this.removeAll();
		this.repaint();
		this.info = null;
		this.infoButton = null;
		this.selectedMessage = null;
		this.selectedGeoMessage = null;
	}
	
	/**
	 * Show information about a host
	 * @param host Host to show the information of
	 */
	public void showInfo(DTNHost host) {
		Vector<Message> messages = 
			new Vector<Message>(host.getMessageCollection());
		Vector<GeoMessage> geomessages = 
				new Vector<GeoMessage>(((GeoDTNHost) host).getGeoMessageCollection());
		Collections.sort(messages);
		//Collections.sort(geomessages);
		reset();
		this.selectedHost = host;
		String text = (host.isMovementActive() ? "" : "INACTIVE ") + host + 
			" at " + host.getLocation();
		
		msgChooser = new JComboBox(messages);
		msgChooser.insertItemAt(messages.size() + " messages", 0);
		msgChooser.setSelectedIndex(0);
		msgChooser.addActionListener(this);
		
		gMsgChooser = new JComboBox(geomessages);
		gMsgChooser.insertItemAt(geomessages.size() + " geomessages", 0);
		gMsgChooser.setSelectedIndex(0);
		gMsgChooser.addActionListener(this);
		
		routingInfoButton = new JButton("routing info");
		routingInfoButton.addActionListener(this);
		
		this.add(new JLabel(text));
		this.add(msgChooser);
		this.add(gMsgChooser);
		this.add(routingInfoButton);
		this.revalidate();
	}
	
	/**
	 * Show information about a message
	 * @param message Message to show the information of
	 */
	public void showInfo(Message message) {
		reset();
		this.add(new JLabel(message.toString()));
		setMessageInfo(message);
		this.revalidate();
	}

	private void setMessageInfo(Message m) {
		int ttl = m.getTtl();
		String txt = " [" + m.getFrom() + "->" + m.getTo() + "] " +   
				"size:" + m.getSize() + ", UI:" + m.getUniqueId() +  
				", received @ " + String.format("%.2f", m.getReceiveTime());
		if (ttl != Integer.MAX_VALUE) {
			txt += " TTL: " + ttl;
		}
		
		String butTxt = "path: " + (m.getHops().size()-1) + " hops";
		
		if (this.info == null) {
			this.info = new JLabel(txt);
			this.infoButton = new JButton(butTxt);
			this.add(info);
			this.add(infoButton);
			infoButton.addActionListener(this);
		}
		else {
			this.info.setText(txt);
			this.infoButton.setText(butTxt);
		}
		
		this.selectedMessage = m;
		infoButton.setToolTipText("path:" + m.getHops());
		
		this.revalidate();
	}

	/**
	 * Show information about a message
	 * @param message Message to show the information of
	 */
	public void showInfo(GeoMessage geomessage) {
		reset();
		this.add(new JLabel(geomessage.toString()));
		setGeoMessageInfo(geomessage);
		this.revalidate();
	}

	private void setGeoMessageInfo(GeoMessage m) {
		int ttl = m.getTtl();
		String txt = " [" + m.getFrom() + "->" + m.getTo() + "] " +   
				"size:" + m.getSize() + ", UI:" + m.getUniqueId() +  
				", received @ " + String.format("%.2f", m.getReceiveTime());
		if (ttl != Integer.MAX_VALUE) {
			txt += " TTL: " + ttl;
		}
		
		String butTxt = "path: " + (m.getHops().size()-1) + " hops";
		
		if (this.info == null) {
			this.info = new JLabel(txt);
			this.infoButton = new JButton(butTxt);
			this.add(info);
			this.add(infoButton);
			infoButton.addActionListener(this);
		}
		else {
			this.info.setText(txt);
			this.infoButton.setText(butTxt);
		}
		
		this.selectedGeoMessage = m;
		infoButton.setToolTipText("path:" + m.getHops());
		
		this.revalidate();
	}

	public void actionPerformed(ActionEvent e) {
		if (selectedMessage != null){	
			if (e.getSource() == msgChooser) {
				if (msgChooser.getSelectedIndex() == 0) { // title text selected
					return; 
				}
				Message m = (Message)msgChooser.getSelectedItem();
				setMessageInfo(m);
			}
			else if (e.getSource() == this.infoButton) {
				Path p = new Path();
				for (DTNHost h : this.selectedMessage.getHops()) {
					p.addWaypoint(h.getLocation());
				}
				
				this.gui.showPath(p);
			}
			else if (e.getSource() ==  this.routingInfoButton) {
				new GeoRoutingInfoWindow(this.selectedHost);
			}
		}
		else{
			if (e.getSource() == gMsgChooser) {
				if (gMsgChooser.getSelectedIndex() == 0) { // title text selected
					return; 
				}
				GeoMessage m = (GeoMessage)gMsgChooser.getSelectedItem();
				setGeoMessageInfo(m);
			}
			else if (e.getSource() == this.infoButton) {
				Path p = new Path();
				for (DTNHost h : this.selectedGeoMessage.getHops()) {
					p.addWaypoint(h.getLocation());
				}
					
				this.gui.showPath(p);
			}
			else if (e.getSource() ==  this.routingInfoButton) {
				new GeoRoutingInfoWindow(this.selectedHost);
			}
		}
	}
	
}