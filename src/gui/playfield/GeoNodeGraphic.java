/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;

import core.Coord;
import core.DTNHost;
import core.GeoDTNHost;

/**
 * Visualization of a DTN Node
 *
 */
public class GeoNodeGraphic extends NodeGraphic {
	private static Color msgColor1 = Color.BLUE;
	private static Color msgColor2 = Color.GREEN;
	private static Color msgColor3 = Color.RED;
	private static Color gMsgColor1 = Color.MAGENTA;
	private static Color gMsgColor2 = Color.ORANGE;
	private static Color gMsgColor3 = Color.YELLOW;


	public GeoNodeGraphic(DTNHost node) {
		super(node);
	}


	/**
	 * Visualize the messages this node is carrying
	 * @param g2 The graphic context to draw to
	 */
	@Override
	protected void drawMessages(Graphics2D g2) {
		int nrofMessages = node.getNrofMessages();
		int nrofGeoMessages = ((GeoDTNHost) node).getNrofGeoMessages();
		Coord loc = node.getLocation();

		drawBar(g2,loc, nrofMessages % 10, 1, 1);
		drawBar(g2,loc, nrofMessages / 10, 2, 1);
		drawBar(g2,loc, nrofGeoMessages % 10, 1, 2);
		drawBar(g2,loc, nrofGeoMessages / 10, 2, 2);
	}

	/**
	 * Draws a bar (stack of squares) next to a location
	 * @param g2 The graphic context to draw to
	 * @param loc The location where to draw
	 * @param nrof How many squares in the stack
	 * @param col Which column
	 */
	private void drawBar(Graphics2D g2, Coord loc, int nrof, int col, int type) {
		final int BAR_HEIGHT = 5;
		final int BAR_WIDTH = 5;
		final int BAR_DISPLACEMENT = 2;

		// draws a stack of squares next loc
		for (int i=1; i <= nrof; i++) {
			if (i%2 == 0) { // use different color for every other msg
				if (type == 1){
					g2.setColor(msgColor1);
				}
				if (type == 2){
					g2.setColor(gMsgColor1);
				}
			}
			else {
				if (col > 1) {
					if (type == 1){
						g2.setColor(msgColor3);
					}
					if (type == 2){
						g2.setColor(gMsgColor3);
					}
				}
				else {
					if (type == 1){
						g2.setColor(msgColor2);
					}
					if (type == 2){
						g2.setColor(gMsgColor2);
					}
				}
			}

			if(type==1){
				g2.fillRect(scale(loc.getX()-BAR_DISPLACEMENT-(BAR_WIDTH*col)),
						scale(loc.getY()- BAR_DISPLACEMENT- i* BAR_HEIGHT),
						scale(BAR_WIDTH), scale(BAR_HEIGHT));
			}
			if(type==2){
				g2.fillRect(scale(loc.getX()+(BAR_WIDTH*col)),
						scale(loc.getY()- BAR_DISPLACEMENT- i* BAR_HEIGHT),
						scale(BAR_WIDTH), scale(BAR_HEIGHT));
			}
		}

	}

}
