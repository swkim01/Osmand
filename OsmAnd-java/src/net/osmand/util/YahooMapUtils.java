package net.osmand.util;



/**
 * This utility class includes : 
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 *   
 *
 */
public class YahooMapUtils extends MapUtils {
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberY(float, double, double)
	 */
	@Override
	public double getTileNumberY(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		latitude = checkLatitude(latitude);
		double eval = Math.log( Math.tan(Math.toRadians(latitude)) + 1/Math.cos(Math.toRadians(latitude)) );
		if (Double.isInfinite(eval) || Double.isNaN(eval)) {
			latitude = latitude < 0 ? - 89.9 : 89.9;
			eval = Math.log( Math.tan(Math.toRadians(latitude)) + 1/Math.cos(Math.toRadians(latitude)) );
		}
		return  eval / Math.PI / 2 * getPowZoom(zoom);
	}

	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLatitudeFromTile(float, double, double)
	 */
	@Override
	public double getLatitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		double result = Math.atan(Math.sinh(Math.PI * 2 * y / getPowZoom(zoom))) * 180d / Math.PI;
		return result;
	}
	
	@Override
	public int getPixelShiftY(float zoom, double lat1, double lat2, double tileSize){
		return (int) (-(getTileNumberY(zoom, 0., lat1) - getTileNumberY(zoom, 0., lat2)) * tileSize);
	}
	
	/**
	 * Get vertical index order of the map
	 * in case of increasing index from arctic to equator like wgs84 osm map, return 1,
	 * or otherwise return -1
	 * @return
	 */
	@Override
	public int getVIndexOrder() {
		return -1;
	}

}
