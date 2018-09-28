package net.osmand.util;

/**
 * This utility class includes : 
 * 1. finding center for array of nodes
 * 2. tile evaluation algorithms
 * 
 *  Original from http://blog.csdn.net/coolypf/article/details/8569813
 *
 */
public class SogouMapUtils extends Bd09MapUtils {

	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberX(float, double, double)
	 */
	@Override
	public double getTileNumberX(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		GeoPoint pixel_pt = lonLatToPoint(longitude, latitude);
		return pixel_pt.getX() * 256 / 250 /(1<<(25-(int)zoom));
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberY(float, double, double)
	 */
	@Override
	public double getTileNumberY(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		GeoPoint pixel_pt = lonLatToPoint(longitude, latitude);
		return pixel_pt.getY() * 256 / 250 /(1<<(25-(int)zoom));
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLongitudeFromTile(float, double, double)
	 */
	@Override
	public double getLongitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		GeoPoint wgs_pt = pointToLonLat(x * (1<<(25-(int)zoom)) * 250 / 256, y * (1<<(25-(int)zoom)) * 250 / 256);
		return wgs_pt.getX();
	}

	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLatitudeFromTile(float, double, double)
	 */
	@Override
	public double getLatitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		GeoPoint wgs_pt = pointToLonLat(x * (1<<(25-(int)zoom)) * 250 / 256, y * (1<<(25-(int)zoom)) * 250 / 256);
		return wgs_pt.getY();
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
	
	public static void main(String[] args) throws Exception {
		float zoom = 3;
		//double lon = 116.404;
		//double lat = 39.915;
		//double lon = 113.68444;
		//double lat = 34.78545;
		double lon = 40.0;
		double lat = 37.5;
		SogouMapUtils mapUtils = new SogouMapUtils();
		GeoPoint js_pt = mapUtils.lonLatToPoint(lon, lat);
		System.out.println("px="+js_pt.getX()+", py="+js_pt.getY());
		System.out.println("tx="+(js_pt.getX()/(1<<(25-(int)zoom)))+", ty="+(js_pt.getY()/(1<<(25-(int)zoom))));
		GeoPoint ll_pt = mapUtils.pointToLonLat(js_pt.getX(), js_pt.getY());
		System.out.println("lon="+ll_pt.getX()+", lat="+ll_pt.getY());
		double x = mapUtils.getTileNumberX(zoom, lon, lat);
		double y = mapUtils.getTileNumberY(zoom, lon, lat);
		System.out.println("tx2="+x+", ty2="+y);
		double tlon = mapUtils.getLongitudeFromTile(zoom, x, y);
		double tlat = mapUtils.getLatitudeFromTile(zoom, x, y);
		System.out.println("lon="+tlon+", lat="+tlat);
	}
}
