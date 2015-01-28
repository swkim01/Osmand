package net.osmand.plus.opengl;

import java.lang.ref.WeakReference;

public class TileMapThread extends Thread {
	final LandTileMap mTileMap;
	final LandTileMap mOverlayMap;
	final WeakReference mWeak;
	
	/**
	 * @param mTileMap
	 * @param mWeak
	 */
	public TileMapThread(LandTileMap tileMap, LandTileMap overlayMap, WeakReference mWeak) {
		super();
		this.mTileMap = tileMap;
		this.mOverlayMap = overlayMap;
		this.mWeak = mWeak;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (mTileMap.getMap() != null)
			LandTileMap.buildTile(mTileMap, LandTileMap.getWorldWidth(mTileMap), LandTileMap.getWorldHeight(mTileMap), LandTileMap.Direction.DOWN);
		LandTileMap.setDrawFlag(mTileMap, true);
		if (mOverlayMap.getMap() != null)
			LandTileMap.buildTile(mOverlayMap, LandTileMap.getWorldWidth(mOverlayMap), LandTileMap.getWorldHeight(mOverlayMap), LandTileMap.Direction.DOWN);
		LandTileMap.setDrawFlag(mOverlayMap, true);
	}
}
