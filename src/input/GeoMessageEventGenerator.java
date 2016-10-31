package input;

import java.util.List;

import core.Cast;
import core.CastSim;
import core.GeoSimScenario;
import core.Settings;

public class GeoMessageEventGenerator extends MessageEventGenerator {

	public GeoMessageEventGenerator(Settings s) {
		super(s);
	}

	protected Cast drawToAddress() {
		CastSim CSE = GeoSimScenario.getInstance().getCasts();
		List<Cast> ExistedCasts = CSE.getCastList();
		int index = rng.nextInt(ExistedCasts.size());
		
		return ExistedCasts.get(index);
	}
	
	@Override
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* zero stands for one way messages */
		int msgSize;
		int interval;
		int from;
		Cast to;
		
		/* Get two *different* nodes randomly from the host ranges */
		from = drawHostAddress(this.hostRange);	
		to = drawToAddress();
		
		msgSize = drawMessageSize();
		interval = drawNextEventTimeDiff();
		
		/* Create event and advance to next event */
		GeoMessageCreateEvent mce = new GeoMessageCreateEvent(from, to, this.getID(), 
				msgSize, responseSize, this.nextEventsTime);
		this.nextEventsTime += interval;	
		
		if (this.msgTime != null && this.nextEventsTime > this.msgTime[1]) {
			/* next event would be later than the end time */
			this.nextEventsTime = Double.MAX_VALUE;
		}
		
		return mce;
	}
}
