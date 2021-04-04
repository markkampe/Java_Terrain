package worldBuilder;

/*
 * This is probably the most computationally complex and expensive
 * code in the entire program, and embodies a considerable amount 
 * of (badly approximated) physics, which has been (shamelessly)
 * tweaked until it produced results that "looked reasonable".
 */
public class WaterFlow {
	
	private Map map;
	private Mesh mesh;
	private Drainage drainage;
	private Parameters parms;
	
	// maps that we import/export from/to Map
	private double heightMap[];		// Z value of each MeshPoint (from Map)
	private double erodeMap[];		// Z erosion of each MeshPoint (from Map)
	private int downHill[];			// down-hill neighbor of each MeshPoint
	private double fluxMap[];		// water flow through MeshPoint
	
	// maps we create, that will be pushed into the Map
	
	private double hydrationMap[];	// FIX 86 hydrationMap
	private double waterLevel[];	// water level at each MeshPoint
	
	// maps we create and use to compute water flow, erosion and deposition
	private double removal[];		// M^3 of removed soil
	protected double suspended[]; 	// M^3 of suspended sediment per second
	protected double velocityMap[]; // water velocity at MeshPoint
	
	/** how much water can different types of soil hold (m^3/m^3) */
	public static double saturation[] = {
		0.30,	// max water content of sedimentary soil
		0.15,	// max water content of metamorphic soil
		0.10,	// max water content of igneous soil
		0.40	// max water content of alluvial soil
	};
	
	/** relative erosion resistances for various bed rocks */
	private static double resistance[] = {
		1.0,	// sedimentary erosion resistance
		4.0,	// metamorphic erosion resistance
		2.5,	// igneous erosion resistance
		0.5		// alluvial erosion resistance
	};
	
	protected static final int UNKNOWN = -666;	// sinkMap: sink point not yet found
	protected static final int OCEAN = -1;		// sinkMap: drains to ocean
	protected static final int OFF_MAP = -2;	// sinkMap: drains off map
	
	private static final int TOO_BIG = 666;			// an impossibly large number
	private static final int TOO_SMALL = -666;		// an impossibly negative number

	// a few useful conversion constants
	private static final double YEAR = 365.25 * 24 * 60 *  60;
	private double area;			// square meters per MeshPoint
	private double rain_to_flow;	// mapping from rain(cm) to flow(M^3/s)
	
	public static final int HYDRO_DEBUG = 2;	// 3 enables painful tracing
	private static final String DEBUG_LOG_FILE = "/tmp/erosion_debug.log";
	private DebugLog debug_log;
	
	private static final double EXIT_DEPTH = 1.0;	// presumed depth at lake exit
	
	/**
	 * compute water flow, water depth, and soil hydration
	 *
	 *	Assertion: drainage has been called: 
	 *			   oceanic and downHill are up-to-date
	 */
	public WaterFlow(Map map) {
		// obtain copies of the needed resources
		parms = Parameters.getInstance();
		this.map = map;
		this.mesh = map.getMesh();
		this.drainage = map.getDrainage();
		this.heightMap = map.getHeightMap();
		this.erodeMap = map.getErodeMap();
		this.downHill = drainage.downHill;
		
		// many square meters per (average) mesh point
		area = 1000000 * (parms.xy_range * parms.xy_range) / mesh.vertices.length;
		// how many M^3/s is 1cm of annual rainfall
		rain_to_flow = .01 * area / YEAR;
		
		// see if we are producing a debug log
		if (parms.debug_level > HYDRO_DEBUG)
			debug_log = new DebugLog("WaterFlow", DEBUG_LOG_FILE);
		else
			debug_log = null;
		
		recompute();
	}
	
	public void recompute() {
		// import the rainfall and arterial river influx
		double[] rainMap = map.getRainMap();
		double[] incoming = map.getIncoming();
		double[] soilMap = map.getSoilMap();
		double[] fluxMap = map.getFluxMap();
		final int ALLUVIAL = map.getSoilType("Alluvial");
		
		// allocate our internal maps
		removal = new double[mesh.vertices.length];
		suspended = new double[mesh.vertices.length];
		velocityMap = new double[mesh.vertices.length];
				
		// 0. initialize our output maps to no flowing water or lakes
		hydrationMap = map.getHydrationMap();	// FIX 86 hydrationMap
		waterLevel = map.getWaterLevel();
		for(int i = 0; i < mesh.vertices.length; i++) {
			fluxMap[i] = 0.0;
			removal[i] = 0.0;
			suspended[i] = 0.0;
			velocityMap[i] = 0.0;
			hydrationMap[i] = drainage.oceanic[i] ? heightMap[i] - parms.sea_level : 0.0;	// FIX 86 hydrationMap
			waterLevel[i] = drainage.oceanic[i] ? parms.sea_level : UNKNOWN;
		}
		
		// if no incoming rivers or rain, we are done
		if (incoming == null && rainMap == null)
			return;
		
		// pick up the erosion parameters
		double Ve = parms.Ve;		// erosion/deposition threshold
		double Vmin = parms.Vmin;	// minimum velocity to carry sediment
		double Smax = parms.Smax;	// maximum sediment per M^3 of water
		
		// calculate the incoming flux, erosion, deposition, and water depth
		map.min_flux = TOO_BIG;
		map.max_flux = TOO_SMALL;
		map.min_velocity = TOO_BIG;
		map.max_velocity = TOO_SMALL;
		map.min_rain = TOO_BIG;
		map.max_rain = TOO_SMALL;
		for(int i = 0; i < drainage.landPoints; i++) {
			int x = drainage.byFlow[i];

			// keep track of min/max rainfall
			if (rainMap[x] < map.min_rain)
				map.min_rain = rainMap[x];
			if (rainMap[x] > map.max_rain)
				map.max_rain = rainMap[x];

			// add incoming off-map rivers and rainfall to this point's flux
			fluxMap[x] += incoming[x] + (rain_to_flow * rainMap[x]);
		
			// compute the soil absorbtion and evaporative loss
			int soilType = erodeMap[x] < 0 ? ALLUVIAL : (int) soilMap[x];
			double absorbed = saturation[soilType] * parms.Dp * area;
			double lost = absorbed * evaporation();

			
			// if loss exceeds incoming, net flux is zero
			if (fluxMap[x] * YEAR <= lost) {
				hydrationMap[x] = fluxMap[x] * YEAR / (parms.Dp * area); 
				fluxMap[x] = 0;
				continue;
			}
				
			// net incoming is reduced by evaporative loss
			hydrationMap[x] = saturation[soilType];
			fluxMap[x] -= lost / YEAR;
			
			// figure out what happens to the excess water
			String msg = null;	// debug message string
			int d = downHill[x];
			if (d >= 0) {
				// my net incoming flows to my downhill neightbor
				fluxMap[d] += fluxMap[x];
				
				// calculate the down-hill water velocity
				double v = velocity(drainage.slopeMap[x]);
				if (velocityMap[d] < v)
					velocityMap[d] = v;
				
				// take average of my incoming and outgoing
				v = (velocityMap[x] + velocityMap[d]) / 2;
				if (v > map.max_velocity)
					map.max_velocity = v;
				if (v >= Vmin && v < map.min_velocity)
					map.min_velocity = v;
				
				// we might be constructing a debug log entry
				msg = (debug_log == null) ? null :
					String.format("x=%4d, v=%6.3f, f=%6.3f", x, v, fluxMap[x]);
				
				if (v >= Ve) {	
					// erosion doesn't happen in lakes, and only up to max capacity
					double can_hold = Smax * fluxMap[x];
					double taken = 0.0;
					if (drainage.outlet[x] == UNKNOWN && suspended[x] < can_hold) {
						double can_take = erosion((int) soilMap[x], v) * fluxMap[x];
						taken = Math.min(can_take, can_hold - suspended[x]);
						removal[x] += taken;
					}
					
					// downhill gets our incoming plus our erosion
					suspended[d] += suspended[x] + taken;
					
					// see if this is the worst erosion on the map
					double delta_z = parms.z(erosion(x));
					if (erodeMap[x] + delta_z > map.max_erosion)
						map.max_erosion = erodeMap[x] + delta_z;
					
					if (debug_log != null) {
						msg += String.format(", e=%6.4f, susp[%4d]=%6.4f", taken, d, suspended[d]);
						if (!drainage.oceanic[d])
							msg += String.format(", vin=%6.3f, vout=%6.3f", velocityMap[x], velocityMap[d]);
						else	
							msg += " (flows into the ocean)";
					}
				} else if (suspended[x] > 0) {
					double dropped = precipitation(v) * suspended[x];
					removal[x] -= dropped;
					suspended[d] += suspended[x] - dropped;
					if (debug_log != null)
						msg += String.format(", d=%6.4f, susp[%4d]=%6.4f", dropped, d, suspended[d]);
					
					// see if this is the deepest sedimentation on the map
					double delta_z = parms.z(sedimentation(x));
					if (erodeMap[x] - delta_z < -map.max_deposition)
						map.max_deposition = -(erodeMap[x] - delta_z);
				} else
					msg = null;	// no deposition or erosion
			}
			
			// if this point is under water, figure out how deep
			double outlet = drainage.outlet[x];
			if (outlet != UNKNOWN)
				if (heightMap[x] - erodeMap[x] < outlet) {
					hydrationMap[x] = (heightMap[x] - erodeMap[x]) - outlet;	// FIX 86 hydrationMap
					waterLevel[x] = outlet;
					if (debug_log != null)
						msg += String.format("\n\tflood %d (at %.1fMSL) %.1f%s u/w",
								x, parms.altitude(heightMap[x] - erodeMap[x]),
								parms.height(-hydrationMap[x]), Parameters.unit_z);
				} else {	// escape point is trivially under water
					// FIX water depth at exit point determined by flow
					hydrationMap[x] = -parms.z(EXIT_DEPTH);
					waterLevel[x] = heightMap[x] - erodeMap[x] + parms.z(EXIT_DEPTH);
					if (debug_log != null)
						msg += String.format("\n\tflood exit point %d %.2f%s u/w",
											x, parms.height(-hydrationMap[x]), Parameters.unit_z);
				}
				
			// debug logging
			if (debug_log != null && msg != null)
				debug_log.write(msg + "\n");
			
			// update minimum/maximum flux values
			if (fluxMap[x] > map.max_flux)
				map.max_flux = fluxMap[x];
			if (fluxMap[x] >= parms.stream_flux/10 && fluxMap[x] < map.min_flux)
				map.min_flux = fluxMap[x];
		}
		
		/*
		 * compute the water level for all above-water points to be that of the
		 * highest under-water point below them.
		 */
		for(int i = 0; i < drainage.landPoints; i++) {
			int point = drainage.byHeight[i];	// work highest-to-lowest
			if (waterLevel[point] != UNKNOWN)
				pushWaterLevel(point, waterLevel[point]);
		}
		
		// flush out any debugging info
		if (debug_log != null)
			debug_log.flush();
		
		// if there was no water flow, fix the Map min/max values
		if (map.max_flux == TOO_SMALL)
			map.max_flux = 0;
		if (map.min_flux == TOO_BIG)
			map.min_flux = 0;
		if (map.min_velocity == TOO_BIG)
			map.min_velocity = 0;
		if (map.max_velocity == TOO_SMALL)
			map.max_velocity = 0;
		if (map.max_rain == TOO_SMALL)
			map.max_rain = 0;
		if (map.min_rain == TOO_BIG)
			map.min_rain = 0;
	}
	
	/**
	 * set all of my up-hill neighbors, who do not already know their water level
	 *     to have the same water level I do.
	 * @param point	index of neighbor to be checked
	 * @param level to set his water level to
	 */
	void pushWaterLevel(int point, double level) {
		double myHeight = heightMap[point] - erodeMap[point];
		
		for(int n = 0; n < mesh.vertices[point].neighbors; n++) {
			int x = mesh.vertices[point].neighbor[n].index;
			if (heightMap[x] - erodeMap[x] > myHeight && waterLevel[x] == UNKNOWN) {
				waterLevel[x] = level;
				pushWaterLevel(x, waterLevel[point]);
			}
		}
	}
	
	/**
	 * estimated erosion
	 * @param index of point being checked
	 * @return meters of erosion
	 */
	public double erosion(int index) {
		if (removal[index] <= 0)
			return 0.0;
		return removal[index] * YEAR / area;
	}
	
	/**
	 * estimated sediment deposition
	 * @param index of the point being checked
	 * @return meters of deposited sediment
	 */
	public double sedimentation(int index) {
		/*
		 * I do two strange things here:
		 * 
		 *   - even tho the calcualations leading to removal[index] seem
		 *     reasonable, I always felt that maps showed more erosion
		 *     than sedimentation.  And since this is more about expectations
		 *     than physics I added an EXAGGERATE factor to goose 
		 *     sedimentation to yield more satisfactory results.
		 *     
		 *   - sedimentation was tracking the erosion of riverbeds (which
		 *     settles out pretty quickly) but in the real world it probably
		 *     has more to do with topsoil that washes into the rivers and
		 *     settles out in much slower water.  Rather than trying to 
		 *     compute this from erosion, I simply said that slow water
		 *     always deposits silt in proportion to flux (and inverse
		 *     proportion to speed).
		 */
		final double EXAGGERATE = 4.0;	// this looks good :-)
		
		double sediment = 0.0;
		if (removal[index] < 0)
			sediment = EXAGGERATE * -removal[index] * YEAR / area;
		else if (velocityMap[index] <= parms.Vd) {
			double sloth = (parms.Vd - velocityMap[index]) / (parms.Vd - parms.Vmin);
			sediment = sloth * fluxMap[index] * parms.Cs * YEAR / area;
		}
		return sediment;
	}
	
	/**
	 * estimated water flow velocity
	 * @param slope ... dZ/dX
	 * @return flow speed (meters/second)
	 */
	public double velocity(double slope) {
		/*
		 * I tried using the Manning formula for open channel flow
		 * 		V = Rh^.65 * S^.5 / n
		 * I complemented this with real river data:
		 * 		Lower Mississippi: speed 0.5M/s, slope  1cm/km
		 * 		Western Columbia:  speed 3.1M/s, slope 10cm/km
		 * But when I applied these to a couple of test maps, I found
		 * that Fantasy maps seem to be much steeper than real landscapes,
		 * so the water never slowed down enough for sedimentation.
		 * 
		 * Fantasy maps seem to have gradients from .001 to 1.0
		 * (100-1000 steeper than real world), so I decided to
		 * curve fit a reasonable range of river speeds (0.005 to 0.5)
		 * to that range of steeper gradients:
		 * 
		 * 		slope	 M/s
		 * 		-----	-----
		 * 		0.001	0.005	dead slow
		 * 		0.010	0.050	mountain stream
		 * 		0.100	0.500	river
		 * 		0.500	3.000	white water
		 */
		double v = slope * 5.0;
		if (v < parms.Vmin)
			return parms.Vmin;
		if (v > parms.Vmax)
			return parms.Vmax;
		return v;
	}
	
	/*
	 * Erosion/Deposition Modeling
	 * 
	 * The Hjulstrom curve says that different sized particles are 
	 * eroded and deposited differently, but ...
	 * 	- most erosion starts somewhere between 0.2 and 1.0 M/s
	 *  - most deposition happens between .005 and 1.0 M/s
	 *  
	 * Suspended concentration studies suggest that a liter of
	 * moving water can carry between 4 and 400 mg (2-200cc)
	 * of suspended soil.  As expected the total transported 
	 * soil is nearly linear with the flow rate, but the 
	 * suspended solids per liter did not seem to be any simple
	 * function of the total flow rate.  Hence, my decision
	 * to base concentration entirely on flow velocity.
	 * 
	 * My over-simplified erosion model:
	 * 	- erosion happens at velocities between Ve and Vmax
	 * 	      w/solids concentration rising linearly w/speed
	 * 
	 * My over-simplfied deposition model:
	 * 	- deposition happens at velocities between Ve and Vmin,
	 * 	      w/between 0 and 1/2 (a linear function of speed)
	 * 		  of the suspended solids settling out per MeshPoint.
	 *     below Vd, we start seeing silt deposition (and there is
	 *     	   more silt in water than was eroded from river bed
	 */
	/**
	 * Compute the erosive power of fast moving water
	 * 
	 * @param mineral composition of river bed
	 * @param velocity of the water
	 * @return M^3 of eroded material per M^3 of water
	 */
	private double erosion(int mineral, double velocity) {
		
		if (velocity <= parms.Ve)
			return 0.0;			// slow water contains no suspended soil
		
		// simplest model is absorbtion proportional to speed
		double suspended = parms.Smax;
		if (velocity < parms.Vmax)
			suspended *= velocity/parms.Vmax;
		return parms.Ce * suspended / resistance[mineral];
	}
	
	/**
	 * compute the amount of sedimentation from slow water
	 * @param velocity of the water
	 * @return fraction of suspended load that will fall out
	 */
	private double precipitation(double velocity) {
		if (velocity >= parms.Ve)
			return 0.0;			// fast water drops nothing
		double precip = parms.Cd;
		if (velocity > parms.Vmin)
			precip *= (parms.Ve - velocity)/parms.Ve;
		return precip;
	}
	
	/**
	 * estimated river width and depth
	 * 
	 * @param flow ... rate (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * 
	 * NOTE: W/D ranges from 2-20 ... call it 6/V
	 * 	1. Area = Flow/Velocity
	 * 	2. Area = W x D
	 * 	3. R = W / D (~6/V)
	 * 
	 *  from (3):
	 *  	(3a) W=RD or (3b) D=W/R
	 *  combining (3a) with (2)
	 *  	W = RD = RA/W = sqrt(AR)
	 *  combining (3b) with (2)
	 *  	D = W/R = A/DR = sqrt(A/R) 
	 */
	private static double widthToDepth(double velocity) {
		final double MAX_RATIO = 20;	// max river W/D
		double ratio = 6/velocity;
		return (ratio > MAX_RATIO) ? MAX_RATIO : ratio;
	}
	
	/**
	 * estimated river width
	 * @param flow speed (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * @return estimated width (meters)
	 */
	public double width(double flow, double velocity) {
		double area = flow / velocity;
		double ratio = widthToDepth(velocity);
		return Math.sqrt(area * ratio);
	}

	/**
	 * estimated river depth
	 * @param flow speed (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * @return estimated depth (meters)
	 */
	public double depth(double flow, double velocity) {
		double area = flow / velocity;
		double ratio = widthToDepth(velocity);
		return Math.sqrt(area / ratio);
	}
	
	/*
	 * returns fraction of soil-water lost over a year
	 * 
	 * 	Note: I read a few articles and, finding them quite complex
	 * 		  made up a formula that I fit to a few data
	 * 		  points based on two plausible assumptions:
	 * 			1. there is a half-time, during which 1/2 of the water evaporates
	 * 			2. the half time decreases exponentially as the temperature rises
	 */
	private double evaporation() {
		double degC = parms.meanTemp();
		double half_time = parms.E35C * Math.pow(2, (35-degC)/parms.Edeg);
		return 1 - Math.pow(0.5, 365.25/half_time);
	}
}

