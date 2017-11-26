package net.osmand.util;

/**
 * This utility class includes : 
 * 1. finding center for array of nodes
 * 2. tile evaluation algorithms
 * 
 *  Original from http://blog.csdn.net/coolypf/article/details/8569813
 *
 */
public class Bd09MapUtils extends Gcj02MapUtils {
	
	static final double x_pi = Math.PI /*3.14159265358979324*/ * 3000.0 / 180.0; 
	
	GeoPoint gcj2bd(double gcjLat, double gcjLon) {
		double x = gcjLon, y = gcjLat;
		double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
		double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
		GeoPoint bd_pt = new GeoPoint(z * Math.cos(theta) + 0.0065, z * Math.sin(theta) + 0.006);
		return bd_pt;
	}
	
	GeoPoint bd2gcj(double bdLat, double bdLon) {
		double x = bdLon - 0.0065, y = bdLat - 0.006;
		double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
		double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
		GeoPoint gcj_pt = new GeoPoint(z * Math.cos(theta), z * Math.sin(theta));
		return gcj_pt;
	}
	
	static final double[] LLBAND = {75,60,45,30,15,0};

	static final double[][] LL2MC = {
	       {-0.0015702102444,111320.7020616939,1704480524535203.,-10338987376042340.,26112667856603880.,-35149669176653700.,26595700718403920.,-10725012454188240.,1800819912950474.,82.5},
	       {0.0008277824516172526,111320.7020463578,647795574.6671607,-4082003173.641316,10774905663.51142,-15171875531.51559,12053065338.62167,-5124939663.577472,913311935.9512032,67.5},
	       {0.00337398766765,111320.7020202162,4481351.045890365,-23393751.19931662,79682215.47186455,-115964993.2797253,97236711.15602145,-43661946.33752821,8477230.501135234,52.5},
	       {0.00220636496208,111320.7020209128,51751.86112841131,3796837.749470245,992013.7397791013,-1221952.21711287,1340652.697009075,-620943.6990984312,144416.9293806241,37.5},
	       {-0.0003441963504368392,111320.7020576856,278.2353980772752,2485758.690035394,6070.750963243378,54821.18345352118,9540.606633304236,-2710.55326746645,1405.483844121726,22.5},
	       {-0.0003218135878613132,111320.7020701615,0.00369383431289,823725.6402795718,0.46104986909093,2351.343141331292,1.58060784298199,8.77738589078284,0.37238884252424,7.45}
	};
	
	static final double[] MCBAND = {12890594.86,8362377.87,5591021,3481989.83,1678043.12,0};
	
	static final double[][] MC2LL = {
		{1.410526172116255e-8,0.00000898305509648872,-1.9939833816331,200.9824383106796,-187.2403703815547,91.6087516669843,-23.38765649603339,2.57121317296198,-0.03801003308653,17337981.2},
		{-7.435856389565537e-9,0.000008983055097726239,-0.78625201886289,96.32687599759846,-1.85204757529826,-59.36935905485877,47.40033549296737,-16.50741931063887,2.28786674699375,10260144.86},
		{-3.030883460898826e-8,0.00000898305509983578,0.30071316287616,59.74293618442277,7.357984074871,-25.38371002664745,13.45380521110908,-3.29883767235584,0.32710905363475,6856817.37},
		{-1.981981304930552e-8,0.000008983055099779535,0.03278182852591,40.31678527705744,0.65659298677277,-4.44255534477492,0.85341911805263,0.12923347998204,-0.04625736007561,4482777.06},
		{3.09191371068437e-9,0.000008983055096812155,0.00006995724062,23.10934304144901,-0.00023663490511,-0.6321817810242,-0.00663494467273,0.03430082397953,-0.00466043876332,2555164.4},
		{2.890871144776878e-9,0.000008983055095805407,-3.068298e-8,7.47137025468032,-0.00000353937994,-0.02145144861037,-0.00001234426596,0.00010322952773,-0.00000323890364,826088.5}
	};
	
	static double getLoop(double cK, double cJ, double T) {
		while(cK>T){cK-=T-cJ;}
		while(cK<cJ){cK+=T-cJ;}
		return cK;
	}
	static double getRange(double cK, double cJ, double T){
		cK=Math.max(cK,cJ);
		cK=Math.min(cK,T);
		return cK;
	}
	
	GeoPoint convertor(GeoPoint cK, double[] cL) {
		if(cK == null ||cL == null){return null;}
		double x=cL[0]+cL[1]*Math.abs(cK.x);
		double cJ=Math.abs(cK.y)/cL[9];
		double y=cL[2]+cL[3]*cJ+cL[4]*cJ*cJ+cL[5]*cJ*cJ*cJ+cL[6]*cJ*cJ*cJ*cJ+cL[7]*cJ*cJ*cJ*cJ*cJ+cL[8]*cJ*cJ*cJ*cJ*cJ*cJ;
		x *= (cK.x<0?-1:1);
		y *=(cK.y<0?-1:1);
		return new GeoPoint(x, y);
	}
	
	GeoPoint lonLatToPoint(double lon, double lat) {
		double[] cL = null;
		double Tlon=getLoop(lon,-180,180);
		double Tlat=getRange(lat,-74,74);
		GeoPoint ll=new GeoPoint(Tlon,Tlat);
		for(int i=0;i<LLBAND.length;i++){
			if(ll.y>=LLBAND[i]){
				cL=LL2MC[i];
				break;
			}
		}
		if(cL == null){
			for(int i=LLBAND.length-1;i>=0;i--){
				if(ll.y<=-LLBAND[i]){
					cL=LL2MC[i];
					break;
				}
			}
		}
		return convertor(ll,cL);
	}
	
	GeoPoint pointToLonLat(double x, double y){
		double[] cM = null;
		GeoPoint pt=new GeoPoint(x,y);
		for(int i=0;i<MCBAND.length;i++){
			if(Math.abs(pt.y)>=MCBAND[i]){
				cM=MC2LL[i];
				break;
			}
		}
		return convertor(pt,cM);
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberX(float, double, double)
	 */
	@Override
	public double getTileNumberX(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		GeoPoint gcj_pt = wgs2gcj(latitude, longitude);
		GeoPoint bd_pt = gcj2bd(gcj_pt.getY(), gcj_pt.getX());
		GeoPoint pixel_pt = lonLatToPoint(bd_pt.getX(), bd_pt.getY());
		return pixel_pt.getX()/(1<<(25-(int)zoom));
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getTileNumberY(float, double, double)
	 */
	@Override
	public double getTileNumberY(float zoom, double longitude, double latitude) {
		// TODO Auto-generated method stub
		GeoPoint gcj_pt = wgs2gcj(latitude, longitude);
		GeoPoint bd_pt = gcj2bd(gcj_pt.getY(), gcj_pt.getX());
		GeoPoint pixel_pt = lonLatToPoint(bd_pt.getX(), bd_pt.getY());
		return pixel_pt.getY()/(1<<(25-(int)zoom));
	}
	
	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLongitudeFromTile(float, double, double)
	 */
	@Override
	public double getLongitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		GeoPoint ll_pt = pointToLonLat(x * (1<<(25-(int)zoom)), y * (1<<(25-(int)zoom)));
		GeoPoint gcj_pt = bd2gcj(ll_pt.getY(), ll_pt.getX());
		GeoPoint wgs_pt = gcj2wgs(gcj_pt.getY(), gcj_pt.getX());
		return wgs_pt.getX();
	}

	/* (non-Javadoc)
	 * @see net.osmand.osm.MapUtils#getLatitudeFromTile(float, double, double)
	 */
	@Override
	public double getLatitudeFromTile(float zoom, double x, double y) {
		// TODO Auto-generated method stub
		GeoPoint ll_pt = pointToLonLat(x * (1<<(25-(int)zoom)), y * (1<<(25-(int)zoom)));
		GeoPoint gcj_pt = bd2gcj(ll_pt.getY(), ll_pt.getX());
		GeoPoint wgs_pt = gcj2wgs(gcj_pt.getY(), gcj_pt.getX());
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
		Bd02MapUtils mapUtils = new Bd02MapUtils();
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
