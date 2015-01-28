package net.osmand.util;



/**
 * This utility class includes : 
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 *   
 *
 */
public class ForestOnMapUtils extends MapUtils {
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberX(float, double, double)
	 */
	@Override
	public double getTileNumberX(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		longitude = checkLongitude(longitude);
		if (zoom < 6)
			return 0;
		
		GeoPoint in_pt = new GeoPoint(longitude, latitude);
		GeoPoint out_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.GRS80, in_pt);
		return (out_pt.getX())/(1<<(25-(int)zoom));
	}

	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberY(float, double, double)
	 */
	@Override
	public double getTileNumberY(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		latitude = checkLatitude(latitude);
		if (zoom < 6)
			return 0;
		
		GeoPoint in_pt = new GeoPoint(longitude, latitude);
		GeoPoint out_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.GRS80, in_pt);
		return (out_pt.getY())/(1<<(25-(int)zoom));
	}
		
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLongitudeFromTile(float, double, double)
	 */
	@Override
	public double getLongitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		if (zoom < 6)
			return 0;
		double z = (1<<(25-(int)zoom));
		GeoPoint in_pt = new GeoPoint(x*z, y*z);
		GeoPoint out_pt = GeoTrans.convert(GeoTrans.GRS80, GeoTrans.GEO, in_pt);
		return out_pt.getX();
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLatitudeFromTile(float, double, double)
	 */
	@Override
	public double getLatitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		if (zoom < 6)
			return 0;
		double z = (1<<(25-(int)zoom));
		GeoPoint in_pt = new GeoPoint(x*z, y*z);
		GeoPoint out_pt = GeoTrans.convert(GeoTrans.GRS80, GeoTrans.GEO, in_pt);
		return out_pt.getY();
	}
	
	@Override
	public int getPixelShiftX(float zoom, double long1, double long2, double tileSize){
		return (int) ((getTileNumberX(zoom, long1, 37.) - getTileNumberX(zoom, long2, 37.)) * tileSize);
	}

	@Override
	public int getPixelShiftY(float zoom, double lat1, double lat2, double tileSize){
		return (int) (-(getTileNumberY(zoom, 127., lat1) - getTileNumberY(zoom, 127., lat2)) * tileSize);
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
