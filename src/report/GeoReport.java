/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo1 Simulator Project. 
 */
package report;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import core.DTNHost;
import core.GeoDTNHost;
import core.GeoSimScenario;
import core.Message;
import core.GeoMessage;
import core.MessageListener;
import core.GeoMessageListener;
import core.UpdateListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class GeoReport extends Report implements MessageListener, GeoMessageListener, UpdateListener {
	
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
	
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
	
	
	//for GeoMessages: 
	private Map<String, Double> geoCreationTimes;
	private List<Double> geoLatencies; 
	private List<Integer> geoHopCounts;
	private List<Double> geoMsgBufferTime;
	private List<Double> geoRtt; 
	// round trip times
	//	private Map<String, List<GeoDTNHost>> geoDestination;
	private Map<String, List<Pair>> geoDestination;
	private Map<String, List<Double>> perCast;
	private Map<String, List<Double>> perCastLatencies;
	private Map<String, List<Double>> perGroup;
	private Map<Integer, Integer> quantity;
	private Map<String, Double> ttlTable;
	private List<GeoMessage> createdGeoMessages;
	private List<GeoMessage> existedGeoMessages;
	
	private int nrofGeoDropped;
	private int nrofGeoRemoved;
	private int nrofGeoStarted;
	private int nrofGeoAborted;
	private int nrofGeoRelayed;
	private int nrofGeoCreated;
	private int nrofGeoResponseReqCreated;
	private int nrofGeoResponseDelivered;
	private int nrofGeoDelivered;
	private int totalRecipients;
	
	/**
	 * Constructor.
	 */
	public GeoReport() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
		
		this.geoCreationTimes = new HashMap<String, Double>();
		this.geoLatencies = new ArrayList<Double>();
		this.geoMsgBufferTime = new ArrayList<Double>();
		this.geoHopCounts = new ArrayList<Integer>();
		this.geoRtt = new ArrayList<Double>();
		this.geoDestination = new HashMap<String, List<Pair>>();
		this.perCast = new HashMap<String, List<Double>>();
		this.perCastLatencies = new HashMap<String, List<Double>>();
		this.perGroup = new HashMap<String, List<Double>>();
		this.quantity = new HashMap<Integer, Integer>();
		this.ttlTable = new HashMap<String, Double>();
		this.createdGeoMessages = new ArrayList<GeoMessage>();
		this.existedGeoMessages = new ArrayList<GeoMessage>();
		
		this.nrofGeoDropped = 0;
		this.nrofGeoRemoved = 0;
		this.nrofGeoStarted = 0;
		this.nrofGeoAborted = 0;
		this.nrofGeoRelayed = 0;
		this.nrofGeoCreated = 0;
		this.nrofGeoResponseReqCreated = 0;
		this.nrofGeoResponseDelivered = 0;
		this.nrofGeoDelivered = 0;
		this.totalRecipients = 0;
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}

	public void geoMessageDeleted(GeoMessage m, GeoDTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofGeoDropped++;
		}
		else {
			this.nrofGeoRemoved++;
		}
		
		this.geoMsgBufferTime.add(getSimTime() - m.getReceiveTime());
	}
	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}
		
		this.nrofAborted++;
	}
	
	public void geoMessageTransferAborted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}
		
		this.nrofGeoAborted++;
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			this.latencies.add(getSimTime() - 
				this.creationTimes.get(m.getId()) );
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);
			
			if (m.isResponse()) {
				this.rtt.add(getSimTime() -	m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
	}

	public void geoMessageTransferred(GeoMessage m, GeoDTNHost from, GeoDTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}

		if (isCooldownID(m.getId())) {
			return;
		}
		
		this.nrofGeoRelayed++;
		if (finalTarget) {
			this.geoLatencies.add(getSimTime() - 
			this.geoCreationTimes.get(m.getId()) );
			this.nrofGeoDelivered++;
			this.geoHopCounts.add(m.getHops().size() - 1);
			
			if(perCastLatencies.get(m.getTo().toString()) != null) {
				List<Double> geolates = perCastLatencies.get(m.getTo().toString());
				geolates.add(getSimTime() - this.geoCreationTimes.get(m.getId()));
				perCastLatencies.put(m.getTo().toString(), geolates);
			}
			
			if(perCastLatencies.get(m.getTo().toString()) == null) {
				List<Double> geolates = new ArrayList<Double>();
				geolates.add(getSimTime() - this.geoCreationTimes.get(m.getId()));
				perCastLatencies.put(m.getTo().toString(), geolates);
			}
			
			if (m.isResponse()) {
				this.geoRtt.add(getSimTime() -	m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
	}
	
	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		if (isCooldown()) {
			addCooldownID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
	}
	
	public void newGeoMessage(GeoMessage m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}

		if (isCooldown()) {
			addCooldownID(m.getId());
			return;
		}
		
		this.geoCreationTimes.put(m.getId(), getSimTime());
		this.nrofGeoCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
		
		String temp = m.getId();
		List <Pair> Nodes = new ArrayList<Pair>();
		this.geoDestination.put(temp, Nodes);
		this.createdGeoMessages.add(m.replicate());
		this.existedGeoMessages.add(m.replicate());
		
		double time = this.getSimTime() + (60.0 * m.getTtl()); //ttl returns as minutes, but simTime is based on seconds
		this.ttlTable.put(temp, time);
		
	}
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}
	
	public void geoMessageTransferStarted(GeoMessage m, GeoDTNHost from, GeoDTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (isCooldownID(m.getId())) {
			return;
		}

		this.nrofGeoStarted++;
	}
	
	public void updated(List<? extends DTNHost> hosts){
		dropExpired();
		updatePairs();
		List<GeoDTNHost> Hosts = GeoSimScenario.getInstance().getHosts();
		for(int i=0; i<Hosts.size(); i++)
		{
			for(int j=0; j<existedGeoMessages.size(); j++)
			{
				Boolean check = existedGeoMessages.get(j).getTo().checkThePoint(Hosts.get(i).getLocation());
				if (check)
				{
					Boolean existed = false;
					List<Pair> Destinations = this.geoDestination.get(existedGeoMessages.get(j).getId());
					for(int k=0; k<Destinations.size(); k++)
					{
						if(Destinations.get(k).getGeoHost().toString() == Hosts.get(i).toString())
						{
							existed = true;
						}
					}
					if (!existed)// && (this.getSimTime() <= (this.ttlTable.get(existedGeoMessages.get(j).toString())-(1200.0)))) //:D
					{
						Pair temp = new Pair(Hosts.get(i), this.getSimTime());
						Destinations.add(temp);
						this.geoDestination.put(existedGeoMessages.get(j).getId(), Destinations);
					}
				}				
			}
		}
	}

	private void updatePairs() {
		for(int i=0; i<existedGeoMessages.size(); i++){
			List<Pair> temp = this.geoDestination.get(existedGeoMessages.get(i).getId());
			for(int j=0; j<temp.size(); j++){
				Boolean check = existedGeoMessages.get(i).getTo().checkThePoint(temp.get(j).getGeoHost().getLocation());
				if (!check){
					temp.get(j).setPair(this.getSimTime());
				}
			}
		}
		
	}

	private void dropExpired() {
		for (Map.Entry<String, Double> entry : ttlTable.entrySet()) {
		    String key = entry.getKey();
		    double value = entry.getValue();
		    if (value <= this.getSimTime()){
		    	for (int i = 0; i < existedGeoMessages.size(); i++) {
		    		if (key == existedGeoMessages.get(i).getId()){
		    		existedGeoMessages.remove(i);
		    		}
		    	}
		    }
		}
		
	}

	@Override
	public void done() {
		write("Message stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		double deliveryProb = 0; // delivery probability
		double responseProb = 0; // request-response success probability
		double overHead = Double.NaN;	// overhead ratio
		
		if (this.nrofCreated > 0) {
			deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		}
		if (this.nrofDelivered > 0) {
			overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
				this.nrofDelivered;
		}
		if (this.nrofResponseReqCreated > 0) {
			responseProb = (1.0* this.nrofResponseDelivered) / 
				this.nrofResponseReqCreated;
		}
		
		String statsText = "created: " + this.nrofCreated + 
			"\nstarted: " + this.nrofStarted + 
			"\nrelayed: " + this.nrofRelayed +
			"\naborted: " + this.nrofAborted +
			"\ndropped: " + this.nrofDropped +
			"\nremoved: " + this.nrofRemoved +
			"\ndelivered: " + this.nrofDelivered +
			"\ndelivery_prob: " + format(deliveryProb) +
			"\nresponse_prob: " + format(responseProb) + 
			"\noverhead_ratio: " + format(overHead) + 
			"\nlatency_avg: " + getAverage(this.latencies) +
			"\nlatency_med: " + getMedian(this.latencies) + 
			"\nhopcount_avg: " + getIntAverage(this.hopCounts) +
			"\nhopcount_med: " + getIntMedian(this.hopCounts) + 
			"\nbuffertime_avg: " + getAverage(this.msgBufferTime) +
			"\nbuffertime_med: " + getMedian(this.msgBufferTime) +
			"\nrtt_avg: " + getAverage(this.rtt) +
			"\nrtt_med: " + getMedian(this.rtt)
			;
		
		write(statsText);
		
		//Geo Scenario
		
		//Delivery ratio raw data
		List<Double> deliveryRatios = new ArrayList<Double>();
		
		
		
		
		write("\n\nGeoMessage stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		double geoDeliveryProb = 0; // delivery probability
		double geoResponseProb = 0; // request-response success probability
		double geoOverHead = Double.NaN;	// overhead ratio
		
		if (this.nrofGeoCreated > 0) {
			
			List <Double> deliProbs = new ArrayList<Double>();
			double sum = 0;
			
			for(int i=0; i<createdGeoMessages.size(); i++)
			{
				int nrofdeli = 0;
				List<Pair> dList = this.geoDestination.get(createdGeoMessages.get(i).getId());
				for(int j=0; j<dList.size(); j++)
				{
					boolean check = dList.get(j).getGeoHost().getGeoRouter().isDeliveredGeoMessage(createdGeoMessages.get(i));
					if (check)
					{
						nrofdeli++;
					}
				}
				
				double deliRatio = 0;
				if (nrofdeli != 0 && dList.size() != 0)
				{
				deliRatio = (1.0 * nrofdeli) / dList.size();
				}
				
				deliProbs.add(deliRatio);
				//for delivery ratio raw data
				deliveryRatios.add(deliRatio);
				
			}
			
			for (int k=0; k<deliProbs.size(); k++)
			{
				sum += deliProbs.get(k);
			}
			geoDeliveryProb = (1.0 * sum) / deliProbs.size();
			
		}
		if (this.nrofGeoDelivered > 0) {
			geoOverHead = (1.0 * (this.nrofGeoRelayed - this.nrofGeoDelivered)) /
				this.nrofGeoDelivered;
		}
		if (this.nrofGeoResponseReqCreated > 0) {
			geoResponseProb = (1.0* this.nrofGeoResponseDelivered) / 
				this.nrofGeoResponseReqCreated;
		}
		
		String geoStatsText = "created: " + this.nrofGeoCreated + 
			"\nstarted: " + this.nrofGeoStarted + 
			"\nrelayed: " + this.nrofGeoRelayed +
			"\naborted: " + this.nrofGeoAborted +
			"\ndropped: " + this.nrofGeoDropped +
			"\nremoved: " + this.nrofGeoRemoved +
			"\ndelivered: " + this.nrofGeoDelivered +
			"\ndelivery_prob: " + format(geoDeliveryProb) +
			"\nresponse_prob: " + format(geoResponseProb) + 
			"\noverhead_ratio: " + format(geoOverHead) + 
			"\nlatency_avg: " + getAverage(this.geoLatencies) +
			"\nlatency_med: " + getMedian(this.geoLatencies) + 
			"\nhopcount_avg: " + getIntAverage(this.geoHopCounts) +
			"\nhopcount_med: " + getIntMedian(this.geoHopCounts) + 
			"\nbuffertime_avg: " + getAverage(this.geoMsgBufferTime) +
			"\nbuffertime_med: " + getMedian(this.geoMsgBufferTime) +
			"\nrtt_avg: " + getAverage(this.geoRtt) +
			"\nrtt_med: " + getMedian(this.geoRtt)
			;
		
		write(geoStatsText);
		
		
		
		
		//detailed report section
		for(int i=0; i<createdGeoMessages.size(); i++ ) {
			int nrofdeli = 0;
			boolean isDelivered = false;
			//write ("\n----------------------------\n"+"GeoMessage ID:  " + createdGeoMessages.get(i).toString());
			//write ("Sender Host:    " + createdGeoMessages.get(i).getFrom().toString());
			//write ("Addressed Cast: " + createdGeoMessages.get(i).getTo().toString() + "\n");
			
			//get perCast List
			List<Double> currentCast = new ArrayList<Double>();
			if (perCast.containsKey(createdGeoMessages.get(i).getTo().toString())) {
				currentCast = this.perCast.get(createdGeoMessages.get(i).getTo().toString());
				}
			//
			
			//get perGroup List
			String groupIndex = Character.toString(createdGeoMessages.get(i).getFrom().toString().charAt(0));// + "";
			List<Double> currentGroup = new ArrayList<Double>();
			if (perGroup.containsKey(groupIndex)) {
				currentGroup = this.perGroup.get(groupIndex);
				}
			//
			
			List<Pair> dList = this.geoDestination.get(createdGeoMessages.get(i).getId());
			for (int j=0; j<dList.size(); j++)
			{
				
				boolean check = dList.get(j).getGeoHost().getGeoRouter().isDeliveredGeoMessage(createdGeoMessages.get(i));
				if (check)
				{
					nrofdeli++;
					isDelivered = true;
				}
				else{isDelivered = false;}
				
				//out.printf("%-8s %15s %15s %-10s %n", dList.get(j).getGeoHost().toString(), format(dList.get(j).getInTime()), format(dList.get(j).getOutTime()), isDelivered);
				
			}
			//write("\n" + nrofdeli + "  out of  " + dList.size() + "  Hosts have received the GeoMessage.");
			
			//totalRecipients
			this.totalRecipients += dList.size(); 
			
			//perCast
			double prob = ((nrofdeli*1.0)/(dList.size()*1.0))*100;
			currentCast.add(prob);
			perCast.put(createdGeoMessages.get(i).getTo().toString(), currentCast);
			//
			
			//perGroup
			currentGroup.add(prob);
			perGroup.put(groupIndex, currentGroup);
			//
			
			//Quantity List
			int wprob = (int) prob;
			if (quantity.containsKey(wprob)){
				int count = quantity.get(wprob);
				count ++;
				quantity.put(wprob, count);
			}
			else{
				quantity.put(wprob, 1);
			}
			//
			
			//write("Delivery ratio for this GeoMessage is: "+ format(prob) +" %");
			
		}
		
		//total recipients
		write ("\n----------------------------\n----------------------------\n");
		write ("Total number of recipients: " + this.totalRecipients);
		
		
		//cast report
		write ("\n----------------------------\n----------------------------\n" + "Delivery rate per Cast:\n");
		Map<String, Double> castAvg = new HashMap<String, Double>();
		for (Map.Entry<String, List<Double>> entry : perCast.entrySet()) {
		    String key = entry.getKey();
		    List<Double> value = entry.getValue();
		    
		    double sum = 0;
		    for (int i = 0; i < value.size(); i++) {
		    		sum += value.get(i);
		    	}
		    
		    double avg = (sum/value.size());
		    
		    castAvg.put(key, avg);
		    
		    write (key + ":  " + format(avg) + " %  from " + value.size()+ " GeoMessages." );
		    
		}
		
		List<String> sortedKeys = new ArrayList<String>();
		
		while (!castAvg.isEmpty()) {
			String tempKey = "";
			double tempValue = 0;
			for (Map.Entry<String, Double> entry: castAvg.entrySet()) {
				String key = entry.getKey();
				Double value = entry.getValue();
				if (value >= tempValue) {
					tempKey = key;
					tempValue = value;
				}
			}
			
			sortedKeys.add(tempKey);
			castAvg.remove(tempKey);
		}
		
		List<String> upper10 = sortedKeys;
		List<String> lower10 = sortedKeys;
		
		upper10 = upper10.subList(0, (int) Math.ceil(upper10.size()*0.1));
		lower10 = lower10.subList((int) Math.floor(lower10.size()*0.9), (lower10.size()));
		
		List<Double> avgUpper = new ArrayList<Double>();
		String upper10Cast = "";
		for (String i: upper10) {
			upper10Cast = upper10Cast + i + ",";
			avgUpper.addAll(perCast.get(i));
		}
		
		List<Double> avgLower = new ArrayList<Double>();
		String lower10Cast = "";
		for (String j: lower10) {
			lower10Cast = lower10Cast + j + ",";
			avgLower.addAll(perCast.get(j));
		}
		
		double sumPP = 0;
	    for (int i = 0; i < avgUpper.size(); i++) {
	    		sumPP += avgUpper.get(i);
	    	}
	    
	    double avgPP = (sumPP/avgUpper.size());
	    
	    write ("\nUpper 10% Casts:  (" + upper10Cast + ")  " + format(avgPP) + " %  from " + avgUpper.size()+ " GeoMessages." );
		
	    sumPP = 0;
	    for (int i = 0; i < avgLower.size(); i++) {
	    		sumPP += avgLower.get(i);
	    	}
	    
	    avgPP = (sumPP/avgLower.size());
	    
	    write ("Lower 10% Casts:  (" + lower10Cast + ")  " + format(avgPP) + " %  from " + avgLower.size()+ " GeoMessages." );
		
		//group report
		write ("\n----------------------------\n----------------------------\n" + "Delivery rate per Group of Sender:\n");
		for (Map.Entry<String, List<Double>> entry : perGroup.entrySet()) {
		    String key = entry.getKey();
		    List<Double> value = entry.getValue();
		    
		    double sum = 0;
		    for (int i = 0; i < value.size(); i++) {
		    		sum += value.get(i);
		    	}
		    
		    double avg = (sum/value.size());
		    
		    write (key + ":  " + format(avg) + " %  from " + value.size()+ " GeoMessages." );
		    
		  }
		
		//quantity report
		write ("\n----------------------------\n----------------------------\n" + "Result Quantity Report:\n");
		write ("Total number of created Geomessages: " + this.nrofGeoCreated + "\n");
	    write ("Delivery percentage : Quantity");
	    Map<Integer, Integer> sortedQuantity = new TreeMap<Integer, Integer>(quantity);
		for (Map.Entry<Integer, Integer> entry : sortedQuantity.entrySet()) {
		    int key = entry.getKey();
		    int value = entry.getValue();
		    
		    write (key + "% :  " + value + " GeoMessages." );	    
		  }
		
		//Detailed Delivery Ratio
		deliveryRatioRawtxt(deliveryRatios);
		//write ("\n----------------------------\n----------------------------\n" + "Delivery Ratio Raw Data Report:\n");
		//String printDelivery = "";
		//for(int i=0; i<deliveryRatios.size(); i++ ) {
		//	printDelivery = printDelivery + format(deliveryRatios.get(i)) + ",";
		//}
		//write (printDelivery);
		
		//Detailed Delivery Latency
		deliveryLatencyRawtxt();
		//write ("\n----------------------------\n----------------------------\n" + "Delivery Latency Raw Data Report:\n");
		//String printLatency = "";
		//for(int j=0; j<geoLatencies.size(); j++ ) {
		//	printLatency = printLatency + format(geoLatencies.get(j)) + ",";
		//}
		//write (printLatency);
		
		//upper and lower 10% Delivery
		write ("\n----------------------------\n----------------------------\n" + "Upper and Lower 10% Delivery Ratio Report:\n");
		Collections.sort(deliveryRatios);
		List<Double> upper = deliveryRatios;
		//Collections.reverse(deliveryRatios);
		//List<Double> lower = deliveryRatios;
		
		List<Double> lower = upper.subList((int) (upper.size()*0.9), (upper.size()));
		upper = upper.subList(0, (int) (upper.size()*0.1));
		
		double sum = 0;
		for (Double key: upper) {
			sum += key;
		}
		write ("Lower 10% : "+ (sum/upper.size()));
		txtDelivery(upper, "Lower");
		
		double sump = 0;
		for (Double key: lower) {
			sump += key;
		}
		write ("Upper 10% : "+ (sump/lower.size()));
		txtDelivery(lower, "Upper");
		
		//upper and lower 10% Latency
		write ("\n----------------------------\n----------------------------\n" + "Upper and Lower 10% Delivery Latency Report:\n");
		Collections.sort(geoLatencies);
		List<Double> upperL = geoLatencies;
		//Collections.reverse(geoLatencies);
		//List<Double> lowerL = geoLatencies;
		
		List<Double> lowerL = upperL.subList((int) (upperL.size()*0.9), (upperL.size()));
		upperL = upperL.subList(0, (int) (upperL.size()*0.1));
		
		double sumL = 0;
		for (Double key: upperL) {
			sumL += key;
		}
		write ("Upper 10% : "+ (sumL/upperL.size()));
		txtLatency(upperL, "Upper");
		
		double sumLp = 0;
		for (Double key: lowerL) {
			sumLp += key;
		}
		write ("Lower 10% : "+ (sumLp/lowerL.size()));
		txtLatency(lowerL, "Lower");
		
		// delivery raw data based on casts
		castDeliveryRawtxt();
		//write ("\n----------------------------\n----------------------------\n" + "Delivery raw data based on casts");
		//for (Map.Entry<String, List<Double>> entry : perCast.entrySet()) {
		//	String printCastDelivery = "";
		//    String key = entry.getKey();
		//    write ("---\n" + key + "\n\n");
		//    List<Double> value = entry.getValue();
		//    for (Double i : value) {
		//    	printCastDelivery = printCastDelivery + format(i) + ",";
		//    }
		//    write(printCastDelivery+"\n");
		//}
		
		
		// latency raw data based on casts
		castLatencyRawtxt();
		//write ("\n----------------------------\n----------------------------\n" + "Latency raw data based on casts");
		//for (Map.Entry<String, List<Double>> entry : perCastLatencies.entrySet()) {
		//	String printCastLatency = "";
		//    String key = entry.getKey();
		//    write ("---\n" + key + "\n\n");
		//    List<Double> value = entry.getValue();
		//    for (Double i : value) {
		//    	printCastLatency = printCastLatency + format(i) + ",";
		//    }
		//    write(printCastLatency+"\n");
		//}
		
		//extra GeoMessage raw data
		Writer writer1 = null;

		try {
		    writer1 = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/messages.txt"), "utf-8"));
		    for(GeoMessage m: createdGeoMessages) {
		    	writer1.write(m.getId().toString() + ',' + m.getTo().toString() + ',' + m.getCreationTime() + '\n');
		    }
		} catch (IOException ex) {
		  // report
		} finally {
		   try {writer1.close();} catch (Exception ex) {/*ignore*/}
		}
		
		//extra Recipients raw data
		Writer writer2 = null;
						
		try {
		    writer2 = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/recipients.txt"), "utf-8"));
		    for(Entry<String, List<Pair>> entry : geoDestination.entrySet()) {
		    	String key = entry.getKey();
			    List<Pair> value = entry.getValue();
			    String buffer = key;
			    for(Pair i: value) {
			    	buffer = buffer + ',' + i.getGeoHost().toString();
			    }
		    	writer2.write(buffer + '\n');
		    }
		} catch (IOException ex) {
		  // report
		} finally {
		   try {writer2.close();} catch (Exception ex) {/*ignore*/}
		}
		
		//extra Host delivered raw data
				Writer writer3 = null;
								
				try {
				    writer3 = new BufferedWriter(new OutputStreamWriter(
				          new FileOutputStream("reports/isdelivered.txt"), "utf-8"));
				    
				    HashSet<GeoDTNHost> hosts = new HashSet<GeoDTNHost>();
				    
				    for(Entry<String, List<Pair>> entry : geoDestination.entrySet()) {
					    List<Pair> value = entry.getValue();
					    for(Pair i: value) {
					    	hosts.add(i.getGeoHost());
					    }
					}
				    
				    
				    for(GeoDTNHost h: hosts) {
				    
				    	String buffer = h.toString();
				    	
				    	for (GeoMessage m: createdGeoMessages) {
				    		
				    		if(h.getGeoRouter().isDeliveredGeoMessage(m)){
				    			buffer = buffer + ',' + m.getId().toString();
				    		}
				    	}
				    	
				    writer3.write(buffer + '\n');
				    }
				    
				} catch (IOException ex) {
				  // report
				} finally {
				   try {writer3.close();} catch (Exception ex) {/*ignore*/}
				}
				
				
		// main.txt
		maintxt(format(geoDeliveryProb), getAverage(this.geoLatencies), this.nrofGeoRelayed);		
						
		
		
		super.done();
	}
	
	private void maintxt(String m, String l, int r){
		
		Writer maintxt = null;

		try {
		    maintxt = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/main.txt"), "utf-8"));
		    
		    	maintxt.write(m + '\n' + l + '\n' + r);
		    
		} catch (IOException ex) {
		  // report
		} finally {
		   try {maintxt.close();} catch (Exception ex) {/*ignore*/}
		}
		
	}
	
	private void deliveryRatioRawtxt(List<Double> deliveryRatios) {
		
		Writer txt = null;

		try {
		    txt = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/deliveryRatioRawData.txt"), "utf-8"));
		    String printDelivery = "";
			for(int i=0; i<deliveryRatios.size(); i++ ) {
				printDelivery = printDelivery + format(deliveryRatios.get(i)) + ",";
			}
			txt.write (printDelivery);
		    
		} catch (IOException ex) {
		  // report
		} finally {
		   try {txt.close();} catch (Exception ex) {/*ignore*/}
		}
		
		
	}
	
	private void deliveryLatencyRawtxt() {
		
		Writer txt = null;

		try {
		    txt = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/deliveryLatencyRawData.txt"), "utf-8"));
		    String printLatency = "";
			for(int j=0; j<geoLatencies.size(); j++ ) {
				printLatency = printLatency + geoLatencies.get(j).intValue() + ",";
			}
			txt.write (printLatency);
		    
		} catch (IOException ex) {
		  // report
		} finally {
		   try {txt.close();} catch (Exception ex) {/*ignore*/}
		}
		
		
	}
	
	private void castDeliveryRawtxt() {
		
		Writer txt = null;
		
		for (Map.Entry<String, List<Double>> entry : perCast.entrySet()) {

			try {
				
			    String key = entry.getKey();
			    List<Double> value = entry.getValue();
			    
			    txt = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("reports/delivery" + key + ".txt"), "utf-8"));
			    
			    String printCastDelivery = "";
			    
			    for (Double i : value) {
			    	printCastDelivery = printCastDelivery + format(i) + ",";
			    }
			    txt.write(printCastDelivery);
		    
			} catch (IOException ex) {
				// report
			} finally {
				try {txt.close();} catch (Exception ex) {/*ignore*/}
			}
		
		}
		
	}
	
	
	private void castLatencyRawtxt() {
		
		Writer txt = null;
		
		for (Map.Entry<String, List<Double>> entry : perCastLatencies.entrySet()) {

			try {
				
			    String key = entry.getKey();
			    List<Double> value = entry.getValue();
			    
			    txt = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("reports/latency" + key + ".txt"), "utf-8"));
			    
			    String printCastLatency = "";
			    
			    for (Double i : value) {
			    	printCastLatency = printCastLatency + i.intValue() + ",";
			    }
			    txt.write(printCastLatency);
		    
			} catch (IOException ex) {
				// report
			} finally {
				try {txt.close();} catch (Exception ex) {/*ignore*/}
			}
		
		}
		
	}
	
	private void txtDelivery(List<Double> data, String name) {
		
		Writer txt = null;

		try {
		    txt = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/cast" + name + "10Delivery.txt"), "utf-8"));
		    String printDelivery = "";
			for(Double i: data) {
				printDelivery = printDelivery + format(i) + ",";
			}
			txt.write (printDelivery);
		    
		} catch (IOException ex) {
		  // report
		} finally {
		   try {txt.close();} catch (Exception ex) {/*ignore*/}
		}
		
	}
	
	private void txtLatency(List<Double> data, String name) {
		
		Writer txt = null;

		try {
		    txt = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("reports/cast" + name + "10Latency.txt"), "utf-8"));
		    String printLatency = "";
			for(Double i: data) {
				printLatency = printLatency + i.intValue() + ",";
			}
			txt.write (printLatency);
		    
		} catch (IOException ex) {
		  // report
		} finally {
		   try {txt.close();} catch (Exception ex) {/*ignore*/}
		}
		
	}
	
	
	
}
