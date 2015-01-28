package net.osmand.map;

import java.io.IOException;

import net.osmand.util.MapUtils;

public interface ITileSource {

	public int getMaximumZoomSupported();

	public String getName();

	public int getTileSize();

	public String getUrlToLoad(int x, int y, int zoom);

	public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException;

	public int getMinimumZoomSupported();

	public String getTileFormat();

	public int getBitDensity();

	public boolean isEllipticYTile();

	public boolean couldBeDownloadedFromInternet();

	public MapUtils getMapUtils();

	public int getExpirationTimeMillis();

	public int getExpirationTimeMinutes();

}
