/* 
 * Copyright 2014 Aydin Rajaei, University of Sussex.
 * The Geo-One Simulator Project. 
 */
package input;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import core.Coord;

public class WKTCastReader extends WKTReader {
	
	private List<List<Coord>> casts = new ArrayList <List<Coord>>();

	public WKTCastReader() {}
	
	/**
	 * Read the file of Casts information.
	 * @param input Reader where the WKT data is read from
	 * @throws IOException if something went wrong with reading from the input
	 */
	public void readFile(File file) throws IOException {
		readFile(new FileReader(file));
	}
	
	
	/**
	 * Read the file of Casts information.
	 * @param input Reader where the WKT data is read from
	 * @throws IOException if something went wrong with reading from the input
	 */
	public void readFile(Reader input) throws IOException {
		
		String type;
		String contents;
		
		init(input);
		
		while((type = nextType()) != null) {
			
			if (type.equals(LINESTRING)) {
				contents = readNestedContents();
				casts.add(parseLineString(contents));
				
			}
			
			else {
				// known type but not interesting -> skip
				readNestedContents();
			}
		}
	}

	/**
	 * Return the Cast List that read from file.
	 * @return List<List<Coord>>
	 */
	public List<List<Coord>> returnCasts() {
		return casts;
	}

	
}
