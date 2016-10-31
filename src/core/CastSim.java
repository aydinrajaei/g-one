/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo1 Simulator Project. 
 */
package core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import input.WKTCastReader;

/**
 * The CastSim class reads the cast data from file and deploys the information into the Geo1 simulation environment.
 * Also, it takes care of calibration of the cast map with the other maps. 
 *
 * @author Aydin Rajaei
 */
public class CastSim {

	/** cast file -setting id ({@value})*/
	public static final String castMap = "castFile";
	/** ccell list file -setting id ({@value})*/
	public static final String cellMap = "cellList";
	
	public static final String OFFSETX = "offset-x";
	
	public static final String OFFSETY = "offset-y";
	
	public static final String CASTS_FROM_FILE = "CastSimModel";
	
	Settings settings = new Settings(CASTS_FROM_FILE);
	
	List<List<Coord>> castsList = new ArrayList <List<Coord>>();
	
	List<Cast> Casts = new ArrayList <Cast>();
	
	/**
	 * Constructor of the CastSim class.
	 * @throws IOException
	 */
	public CastSim() {
		run();	
	}
	
	/**
	 * Constructor with custom cell file implemented for EVR Router
	 * @param cellPath the Path of cell list from file
	 */
	public CastSim(boolean x) {
		runCell();
		defineCast();
		System.out.println("Cell List have initialized successfully!");
	}

	/**
	 * the Run method runs the file reader and calls the Cast definer.
	 * 
	 */
	public void run() {
		runCast();
		defineCast();
		System.out.println("Casts have initialized successfully!");	
	}

	/**
	 * This method finds out the name of cast information's file name from setting text file. 
	 * @param settings
	 * @return pathFile cast information's file path
	 */
	private String checkCastFile(Settings settings) {
		String pathFile = settings.getSetting(castMap);
		return pathFile;
	}
	
	/**
	 * This method finds out the name of cast information's file name from setting text file. 
	 * @param settings
	 * @return pathFile cast information's file path
	 */
	private String checkCellFile(Settings settings) {
		String pathFile = settings.getSetting(cellMap);
		return pathFile;
	}
	
	/**
	 * This method reads and calibrates the cast maps.
	 * 
	 */
	private void runCast() { 
		
		WKTCastReader c = new WKTCastReader();
		String CFile = checkCastFile(settings);
		try {
			c.readFile(new File(CFile));
		} catch (IOException e) {
			System.out.println("Casts list has not read properly from file");
			e.printStackTrace();
		}
		castsList = c.returnCasts();
		
		int offSetX = settings.getInt(OFFSETX);
		int offSetY = settings.getInt(OFFSETY);
		
		//CastSim calibrator
		for(int j=0; j<castsList.size(); j++){
			for(int k=0; k<castsList.get(j).size(); k++){
				castsList.get(j).get(k).setLocation(castsList.get(j).get(k).getX() + offSetX, -castsList.get(j).get(k).getY() + offSetY);
			}
		}
	}
	
	/**
	 * This method reads and calibrates the cell list.
	 * 
	 */
	private void runCell() { 
		
		WKTCastReader c = new WKTCastReader();
		String CFile = checkCellFile(settings);
		try {
			c.readFile(new File(CFile));
		} catch (IOException e) {
			System.out.println("Cell list has not read properly from file");
			e.printStackTrace();
		}
		castsList = c.returnCasts();
		
		int offSetX = settings.getInt(OFFSETX);
		int offSetY = settings.getInt(OFFSETY);
		
		//CastSim calibrator
		for(int j=0; j<castsList.size(); j++){
			for(int k=0; k<castsList.get(j).size(); k++){
				castsList.get(j).get(k).setLocation(castsList.get(j).get(k).getX() + offSetX, -castsList.get(j).get(k).getY() + offSetY);
			}
		}
	}

	/**
	 * This method creates new object of each cast and adds it to the casts
	 * 
	 */
	private void defineCast() {
		for (int i=0; i<castsList.size(); i++){	
			Cast temp = new Cast(castsList.get(i));
			temp.setId(i+1);
			Casts.add(temp);			
		}
	}

	/**
	 * It prints every cast that is already defined in the simulation.
	 * 
	 */
	private void printCast() {
		for(int j=0; j<Casts.size(); j++){
			Casts.get(j).printcast();
		}	
	}

	/**
	 * This method returns the list of defined casts object.
	 * @return Casts : List<object> of existed casts.
	 */
	public List<Cast> getCastList() {
		return this.Casts;
	}
	
}
