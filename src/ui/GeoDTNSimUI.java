/* 
 * Copyright 2014 Sussex University, Aydin Rajaei
 *  
 */
package ui;

import report.Report;
import core.ApplicationListener;
import core.ConnectionListener;
import core.MessageListener;
import core.GeoMessageListener;
import core.MovementListener;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import core.SimError;
import core.GeoSimScenario;
import core.GeoWorld;
import core.UpdateListener;

/**
 * Abstract subclass for Geo_One user interfaces; contains also some simulation
 * settings.
 */
public abstract class GeoDTNSimUI extends DTNSimUI {
	
	/** Scenario of the current simulation */
	protected GeoSimScenario scen;
	
	/** report class' package name */
	private static final String REPORT_PAC = "report.";

	/** The World where all actors of the simulator are */
	protected GeoWorld world;

	/*
	public GeoDTNSimUI() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void runSim() {
		// TODO Auto-generated method stub

	}
	*/

	/**
	 * Starts the simulation.
	 */
	@Override
	public void start() {
		initModel();
		runSim();
	}

	/**
	 * Initializes the simulator model.
	 */
	private void initModel() {
		Settings settings = null;
				
		try {
			settings = new Settings();
			this.scen = GeoSimScenario.getInstance();

			// add reports
			for (int i=1, n = settings.getInt(NROF_REPORT_S); i<=n; i++){
				String reportClass = settings.getSetting(REPORT_S + i);
				addReport((Report)settings.createObject(REPORT_PAC + 
						reportClass));	
			}

			double warmupTime = 0;
			if (settings.contains(MM_WARMUP_S)) {
				warmupTime = settings.getDouble(MM_WARMUP_S);
				if (warmupTime > 0) {
					SimClock c = SimClock.getInstance();
					c.setTime(-warmupTime);
				}
			}

			this.world = this.scen.getWorld();
			world.warmupMovementModel(warmupTime);
		}
		catch (SettingsError se) {
			System.err.println("Can't start: error in configuration file(s)");
			System.err.println(se.getMessage());
			System.exit(-1);			
		}
		catch (SimError er) {
			System.err.println("Can't start: " + er.getMessage());
			System.err.println("Caught at " + er.getStackTrace()[0]);
			System.exit(-1);
		}		
	}

	/**
	 * Adds a new report for simulator
	 * @param r Report to add
	 */
	protected void addReport(Report r) {
		if (r instanceof MessageListener) {
			scen.addMessageListener((MessageListener)r);
		}
		if (r instanceof GeoMessageListener) {
			scen.addGeoMessageListener((GeoMessageListener)r);
		}
		if (r instanceof ConnectionListener) {
			scen.addConnectionListener((ConnectionListener)r);
		}
		if (r instanceof MovementListener) {
			scen.addMovementListener((MovementListener)r);
		}
		if (r instanceof UpdateListener) {
			scen.addUpdateListener((UpdateListener)r);
		}
		if (r instanceof ApplicationListener) {
			scen.addApplicationListener((ApplicationListener)r);
		}

		this.reports.add(r);
	}
	
}
