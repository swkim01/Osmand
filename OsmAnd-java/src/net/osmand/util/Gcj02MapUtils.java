package net.osmand.util;



/**
 * This utility class includes : 
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 * 
 *  Original from https://github.com/googollee/eviltransform
 *
 */
public class Gcj02MapUtils extends MapUtils {
	
	//
    // Krasovsky 1940
    //
    // a = 6378245.0, 1/f = 298.3
    // b = a * (1 - f)
    // ee = (a^2 - b^2) / a^2;
    static final double a = 6378245.0;
    static final double ee = 0.00669342162296594323;
	static final double earthR = 6371000;
	
	boolean outOfChina(double lat, double lon) {
		if (lon < 72.004 || lon > 137.8347) {
			return true;
		}
		if (lat < 0.8293 || lat > 55.8271) {
			return true;
		}
		return false;
	}
	
	double transformLat(double x, double y) {
		double ret = -100.0 + 2.0*x + 3.0*y + 0.2*y*y + 0.1*x*y + 0.2*Math.sqrt(Math.abs(x));
		ret += (20.0*Math.sin(6.0*x*Math.PI) + 20.0*Math.sin(2.0*x*Math.PI)) * 2.0 / 3.0;
		ret += (20.0*Math.sin(y*Math.PI) + 40.0*Math.sin(y/3.0*Math.PI)) * 2.0 / 3.0;
		ret += (160.0*Math.sin(y/12.0*Math.PI) + 320*Math.sin(y*Math.PI/30.0)) * 2.0 / 3.0;
		return ret;
	}
	
	double transformLon(double x, double y) {
		double ret = 300.0 + x + 2.0*y + 0.1*x*x + 0.1*x*y + 0.1*Math.sqrt(Math.abs(x));
		ret += (20.0*Math.sin(6.0*x*Math.PI) + 20.0*Math.sin(2.0*x*Math.PI)) * 2.0 / 3.0;
		ret += (20.0*Math.sin(x*Math.PI) + 40.0*Math.sin(x/3.0*Math.PI)) * 2.0 / 3.0;
		ret += (150.0*Math.sin(x/12.0*Math.PI) + 300.0*Math.sin(x/30.0*Math.PI)) * 2.0 / 3.0;
		return ret;
	}
	
	GeoPoint delta(double lat, double lon) {
		double dlat = transformLat(lon-105.0, lat-35.0);
		double dlon = transformLon(lon-105.0, lat-35.0);
		double radLat = lat / 180.0 * Math.PI;
		double magic = Math.sin(radLat);
		magic = 1 - ee*magic*magic;
		double sqrtMagic = Math.sqrt(magic);
		GeoPoint d_pt = new GeoPoint(dlon, dlat);
		d_pt.y = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
		d_pt.x = (dlon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
		return d_pt;
	}
	
	GeoPoint wgs2gcj(double wgsLat, double wgsLon) {
		GeoPoint out_pt = new GeoPoint(wgsLon, wgsLat);
		if (outOfChina(wgsLat, wgsLon)) {
			out_pt.y = wgsLat;
			out_pt.x = wgsLon;
			return out_pt;
		}
		GeoPoint d_pt = delta(wgsLat, wgsLon);
		out_pt.y += d_pt.getY();
		out_pt.x += d_pt.getX();
		return out_pt;
	}
	
	GeoPoint gcj2wgs(double gcjLat, double gcjLon) {
		GeoPoint out_pt = new GeoPoint(gcjLon, gcjLat);
		if (outOfChina(gcjLat, gcjLon)) {
			out_pt.y = gcjLat;
			out_pt.x = gcjLon;
			return out_pt;
		}
		GeoPoint d_pt = delta(gcjLat, gcjLon);
		out_pt.y -= d_pt.getY();
		out_pt.x -= d_pt.getX();
		return out_pt;
	}
	
	double distance(double latA, double lngA, double latB, double lngB) {
		double x = Math.cos(latA*Math.PI/180) * Math.cos(latB*Math.PI/180) * Math.cos((lngA-lngB)*Math.PI/180);
		double y = Math.sin(latA*Math.PI/180) * Math.sin(latB*Math.PI/180);
		double s = x + y;
		if (s > 1) {
			s = 1;
		}
		if (s < -1) {
			s = -1;
		}
		double alpha = Math.acos(s);
		double distance = alpha * earthR;
		return distance;
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberX(float, double, double)
	 */
	@Override
	public double getTileNumberX(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		GeoPoint gcj_pt = wgs2gcj(latitude, longitude);
		return super.getTileNumberX(zoom, gcj_pt.getX(), gcj_pt.getY());
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberY(float, double, double)
	 */
	@Override
	public double getTileNumberY(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		GeoPoint gcj_pt = wgs2gcj(latitude, longitude);
		return super.getTileNumberY(zoom, gcj_pt.getX(), gcj_pt.getY());
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLongitudeFromTile(float, double, double)
	 */
	@Override
	public double getLongitudeFromTile(float zoom, double x, double y) {
		double gcjLon = super.getLongitudeFromTile(zoom, x, y);
		double gcjLat = super.getLatitudeFromTile(zoom, x, y);
		GeoPoint wgs_pt = gcj2wgs(gcjLat, gcjLon);
		return wgs_pt.getX();
	}

	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLatitudeFromTile(float, double, double)
	 */
	@Override
	public double getLatitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		double gcjLon = super.getLongitudeFromTile(zoom, x, y);
		double gcjLat = super.getLatitudeFromTile(zoom, x, y);
		GeoPoint wgs_pt = gcj2wgs(gcjLat, gcjLon);
		return wgs_pt.getY();
	}
}
