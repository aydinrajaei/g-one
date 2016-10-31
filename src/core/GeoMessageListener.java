/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package core;

/**
 * Interface for classes that want to be informed about geomessages
 * between geohosts
 *
 */
public interface GeoMessageListener {
	
	/**
	 * Method is called when a new geomessage is created
	 * @param m GeoMessage that was created
	 */
	public void newGeoMessage(GeoMessage m);
	
	/**
	 * Method is called when a geomessage's transfer is started
	 * @param m The geomessage that is going to be transferred
	 * @param from Node where the geomessage is transferred from 
	 * @param to Cast where the message is transferred to
	 */
//	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, Cast to);
	
	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, GeoDTNHost to);
	
	/**
	 * Method is called when a geomessage is deleted
	 * @param m The geomessage that was deleted
	 * @param where The host where the geomessage was deleted
	 * @param dropped True if the geomessage was dropped, false if removed
	 */
	public void geoMessageDeleted(GeoMessage m, GeoDTNHost where, boolean dropped);
	
	/**
	 * Method is called when a geomessage's transfer was aborted before 
	 * it finished
	 * @param m The geomessage that was being transferred
	 * @param from Node where the geomessage was being transferred from 
	 * @param to Cast where the geomessage was being transferred to
	 */
//	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, Cast to);
	
	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, GeoDTNHost to);
	
	/**
	 * Method is called when a geomessage is successfully transferred from
	 * a node to another.
	 * @param m The geomessage that was transferred
	 * @param from Node where the geomessage was transferred from
	 * @param to Cast where the geomessage was transferred to
	 * @param firstDelivery Was the target node final destination of the geomessage
	 * and received this geomessage for the first time.
	 */
//	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, Cast to,
//			boolean firstDelivery);
	
	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, GeoDTNHost to,
			boolean firstDelivery);
}
