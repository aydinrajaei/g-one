package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import movement.map.MapNode;
import movement.map.SimMap;
import core.CastSim;
import core.Cast;
import core.Coord;

/**
 * PlayfieldGraphic for SimMap visualization
 *
 */
public class CastGraphic extends PlayFieldGraphic {
	private List<Coord> coords;
	private CastSim simCast;
	private List<Cast> Casts;
	private final Color PATH_COLOR = Color.ORANGE;
		
	public CastGraphic(CastSim simCast) {
		this.simCast = simCast;
	}	
	
	// TODO: draw only once and store to buffer
	@Override
	public void draw(Graphics2D g2) {
		
		if (simCast == null) {
			return;
		}
		
		Casts = simCast.getCastList();
		
		for (int i=0; i<Casts.size(); i++){
			
			coords = Casts.get(i).getTheCast();
			
			if (coords == null) {
				return;
			}
			
			g2.setColor(PATH_COLOR);
			Coord prev = coords.get(0);
			
			for (int j=1, n=coords.size(); j < n; j++) {
				Coord next = coords.get(j);
				g2.drawLine(scale(prev.getX()), scale(prev.getY()),
						scale(next.getX()), scale(next.getY()));
				prev = next;
			}
		}
		
	}	

}
