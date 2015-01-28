package net.osmand.plus.opengl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import android.content.Context;

public class SrtmFile {

	private File mFile;
	private int mLongitude = 0x7fffffff;
	private int mLatitude;
	private File mHeightDir; // "/sdcard/HeightMap/";
	private final String FILEFORMAT = "%c%02d%c%03d.HGT";
	private boolean mF;
	private boolean mG;
	
	public SrtmFile(Context context) {
		mHeightDir = ((OsmandApplication) context.getApplicationContext()).getAppPath(IndexConstants.HEIGHTS_INDEX_DIR);
	}
	
	public int getAltitude(double x, double y) throws Exception {
		
		File file = null;
		RandomAccessFile rFile = null;
		int longitude = (int)x - (x >= 0 ? 0 : 1);
		int latitude = (int)y - (y >= 0 ? 0 : 1);
		int altitude = -9999;
		
		if (longitude == mLongitude && latitude == mLatitude) {
			file = mFile;
		}
		else {
			String fileName = String.format(FILEFORMAT,
					(y >= 0 ? 'N' : 'S'),
					(int)Math.abs(latitude),
					(x > 0 ? 'E' : 'W'),
					(int)Math.abs(longitude));
			file = new File(mHeightDir, fileName);
			mFile = file;
			mLongitude = longitude;
			mLatitude = latitude;
		}
		
		// you can get srtm files from  http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/Eurasia/
		
		if (file.exists()) {
			try {
				rFile = new RandomAccessFile(file, "r");

				int row = (int) Math.sqrt((double)(file.length() / 2)) - 1; /* 1200 */
				int value = 3600 / row;
				int offsetY = (int)(((double)(latitude + 1) - y) * 3600) / value;
				int offsetX = (int)((x - (double)longitude) * 3600) / value;
				long offset = (long)(offsetX * 2 + offsetY * 2 * (row + 1));
				rFile.seek(offset);
				
				altitude = rFile.readShort();
			}
			catch (FileNotFoundException e) {}
			catch (IOException e) {}
			finally {
				if (rFile != null) {
					try {
						rFile.close();
					}
					catch (IOException ex) { }
				}
			}
		}
		
		return altitude;
	}
	
	public boolean scanDir() {
		File dir = null;
		File[] files = null;
		
		if (mG == false) {
			dir = mHeightDir;
			
			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name){
					return name.endsWith(".hgt");
				}
			} );
			
			if (files != null) {
				if (files.length != 0) {
					mF = true;
				}
			}
		}
		return mF;
	}
	
	public int[] getAltitude(double[] x, double[] y) {
		File file = null;
		RandomAccessFile rFile = null;
		int readFlag = 0;
		
		int[] altitudes = new int[Math.min(x.length, y.length)];
		
		for (int i=0; i < altitudes.length; i++) {
			int longitude = (int)x[i] - (x[i] >= 0 ? 0 : 1);
			int latitude = (int)y[i] - (y[i] >= 0 ? 0 : 1);
			
			if (longitude == mLongitude && latitude == mLatitude) {
				file = mFile;
			}
		    else {
				String fileName = String.format(FILEFORMAT,
						(y[i] >= 0 ? 'N' : 'S'),
						(int)Math.abs(latitude),
						(x[i] > 0 ? 'E' : 'W'),
						(int)Math.abs(longitude));
				//Log.e("srtmFile", "fileName="+fileName);
				file = new File(mHeightDir, fileName);
				mFile = file;
				mLongitude = longitude;
				mLatitude = latitude;
				
				if (rFile != null) {
					try {
						rFile.close();
					}
					catch (IOException e) { }
					rFile = null;
				}
			}

			if (file.exists()) {
				try {
					if (rFile == null) {
						rFile = new RandomAccessFile(file, "r");
					}
					int row = (int) Math.sqrt((double)(file.length() / 2)) - 1;
					int value = 3600 / row;
					int offsetY = (int)(((double)(latitude + 1) - y[i]) * 3600.) / value;
					int offsetX = (int)((x[i] - (double)longitude) * 3600.) / value;
					long offset = (long)(offsetX * 2 + offsetY * 2 * (row + 1));
					//Log.d("srtmFile", "row="+row+",offsetX="+offsetX+",offsetY="+offsetY);
					rFile.seek(offset);
					altitudes[i] = rFile.readShort();
					readFlag = 1;
				}
				catch (FileNotFoundException e) { }
				catch (IOException e) {}
			}
		}
		
		if (rFile != null) {
			try {
				rFile.close();
			}
			catch (IOException e) { }
		}	
		if (readFlag != 0)
			return altitudes;
		else
			return null;
	}
}
