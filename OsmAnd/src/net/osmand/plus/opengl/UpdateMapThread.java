package net.osmand.plus.opengl;


public class UpdateMapThread extends Thread {
	final LandTileMap mTileMap;
	private final float mWidth;
	private final float mHeight;
	private final LandTileMap.Direction mD;
	
	/**
	 * @param mTileMap
	 * @param mWeak
	 */
	public UpdateMapThread(LandTileMap tileMap, float width, float height, LandTileMap.Direction direction) {
		super();
		this.mTileMap = tileMap;
		this.mWidth = width;
		this.mHeight = height;
		this.mD = direction;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		LandTileMap.setUpdateFlag(mTileMap, false);
		if (mTileMap.getMap() != null)
			LandTileMap.buildTile(mTileMap, mWidth, mHeight, mD);
		LandTileMap.setUpdateFlag(mTileMap, true);	
	}
}
