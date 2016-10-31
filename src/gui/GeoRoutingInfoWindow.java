/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

import routing.util.RoutingInfo;
import core.DTNHost;
import core.GeoDTNHost;
import core.SimClock;

/**
 * A window for displaying routing information
 */
public class GeoRoutingInfoWindow extends JFrame implements ActionListener {
	private DTNHost host;
	private JButton refreshButton;
	private JCheckBox autoRefresh;
	private JScrollPane treePane;
	private JScrollPane gTreePane;
	private JTree tree;
	private JTree gTree;
	private Timer refreshTimer;
	/** how often auto refresh is performed */
	private static final int AUTO_REFRESH_DELAY = 1000;
	
	public GeoRoutingInfoWindow(DTNHost host) {
		Container cp = this.getContentPane();
		JPanel refreshPanel = new JPanel();
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);		
		this.host = host;
		this.setLayout(new BorderLayout());
		refreshPanel.setLayout(new BorderLayout());
		this.autoRefresh = new JCheckBox("Auto refresh");
		this.autoRefresh.addActionListener(this);
		this.treePane = new JScrollPane();
		this.gTreePane = new JScrollPane();
		updateTree();
		
		cp.add(treePane, BorderLayout.WEST);
		cp.add(gTreePane, BorderLayout.EAST);
		cp.add(refreshPanel, BorderLayout.SOUTH);
		
		this.refreshButton = new JButton("refresh");
		this.refreshButton.addActionListener(this);
		refreshPanel.add(refreshButton, BorderLayout.EAST);
		refreshPanel.add(autoRefresh, BorderLayout.WEST);
		
		this.pack();		
		this.setVisible(true);
	}

	
	private void updateTree() {	
		super.setTitle("Routing Info of " + host + " at " + 
				SimClock.getFormattedTime(2));
		RoutingInfo ri = host.getRoutingInfo();
		RoutingInfo gri = ((GeoDTNHost) host).getGeoRoutingInfo();
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(ri);
		DefaultMutableTreeNode gTop = new DefaultMutableTreeNode(gri);
		Vector<Integer> expanded = new Vector<Integer>();
		
		addChildren(top, ri);
		addGeoChildren(gTop, gri);

		if (this.tree != null) { /* store expanded state */
			for (int i=0; i < this.tree.getRowCount(); i++) {
				if (this.tree.isExpanded(i)) {
					expanded.add(i);
				}
			}
		}
		
		if (this.gTree != null) { /* store expanded state */
			for (int i=0; i < this.gTree.getRowCount(); i++) {
				if (this.gTree.isExpanded(i)) {
					expanded.add(i);
				}
			}
		}
		
		this.tree = new JTree(top);
		this.gTree = new JTree(gTop);
		
		for (int i=0; i < this.tree.getRowCount(); i++) { /* restore expanded */
			if (expanded.size() > 0 && expanded.firstElement() == i) {
				this.tree.expandRow(i);
				expanded.remove(0);
			}
		}
		
		for (int i=0; i < this.gTree.getRowCount(); i++) { /* restore expanded */
			if (expanded.size() > 0 && expanded.firstElement() == i) {
				this.gTree.expandRow(i);
				expanded.remove(0);
			}
		}
		
		this.treePane.setViewportView(this.tree);
		this.treePane.revalidate();
		
		this.gTreePane.setViewportView(this.gTree);
		this.gTreePane.revalidate();
	}
	
	
	private void addChildren(DefaultMutableTreeNode node, RoutingInfo info) {
		for (RoutingInfo ri : info.getMoreInfo()) {
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(ri);
			node.add(child);
			// recursively add children of this info
			addChildren(child, ri);
		}
	}
	
	private void addGeoChildren(DefaultMutableTreeNode node, RoutingInfo info) {
		for (RoutingInfo gri : info.getMoreInfo()) {
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(gri);
			node.add(child);
			// recursively add children of this info
			addChildren(child, gri);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object s = e.getSource();
		if (s == this.refreshButton || s == this.refreshTimer) {
			updateTree();
		}
		if (e.getSource() == this.autoRefresh) {
			if (this.autoRefresh.isSelected()) {
				this.refreshTimer = new Timer(AUTO_REFRESH_DELAY, this);
				this.refreshTimer.start();
			} else {
				this.refreshTimer.stop();
			}
		}
	}
	
}