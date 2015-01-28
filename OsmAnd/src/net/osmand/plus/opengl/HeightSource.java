package net.osmand.plus.opengl;

import android.content.Context;

public class HeightSource {
	//private Aeo mA;
	//private int mC;
	private Context mContext;
	private SrtmFile mSrtmFile;
	private double[] mE;

	public HeightSource(Context context) {
		super();
		
		this.mContext = context;
		mE = new double[]{0, 0};
		SrtmFile srtmFile = new SrtmFile(mContext);
		if (srtmFile.scanDir() == true)
			mSrtmFile = srtmFile;
		
	}
	
	public int getHeight(float longitude, float latitude) {
		int height = -9999;
		if (mSrtmFile != null) {
			try {
				//Log.d("getHeight1", "longitude="+longitude+", latitude"+latitude);
				height = mSrtmFile.getAltitude((double)longitude, (double)latitude);
			} catch (Exception e) {}
		}
		if (height == -9999)
			height = 0;
		return height;
	}

	/*
	public void a(Handler handler) {
		if (mA != null) {
			mA.a(handler);
	    }
	}
	*/

	public int[] getHeight(float[] longitude, float[] latitude) {
		double[] longitudes = new double[longitude.length];
		double[] latitudes = new double[latitude.length];
		for (int i=0; i < longitude.length; i++) {
			//double[] position = mMapUtils.getLocationFromTile((float)zoom, (double)positionX[i]/512, (double)positionY[i]/512);
			//Log.d("getHeight", "longitude="+longitude[i]+", latitude"+latitude[i]);
			longitudes[i] = (double)longitude[i];
			latitudes[i] = (double)latitude[i];
		}
		int[] heights;
		if (mSrtmFile != null)
			heights = mSrtmFile.getAltitude(longitudes, latitudes);
		else {
			heights = new int[longitude.length];
			for (int i=0; i< longitude.length; i++)
				heights[i] = 0;
		}
		return heights;
	}

	/*
	public Aji a(int[][] p1) {
		return mA.a(mC, p1, p1[0].length);
	}
	*/
}
