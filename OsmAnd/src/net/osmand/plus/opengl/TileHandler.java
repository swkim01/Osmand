package net.osmand.plus.opengl;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

public class TileHandler extends Handler {
	private final WeakReference mWeak;
	
	public TileHandler(LandTileMap tileMap) {
		mWeak = new WeakReference(tileMap);
	}
	
	@Override
	public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
		LandTileMap tileMap = (LandTileMap) mWeak.get();
		
		if (tileMap != null) {
			/*
			Aji aji = msg.obj;
			if (aji != null) {
				synchronized (LandTileMap.getLock(tileMap)) {
					try {
						if (LandTileMap.getClearFlag(tileMap) == false) {
							LandTile[] tiles = LandTileMap.getTiles(tileMap);
							for (int i=0; i < tiles.length; i++) {
								LandTile tile = tiles[i];
								if (tile != null &&
										tile.getLodLevels() == aji.a && tile.getMaxSubdivisions() == aji.b)
									tile.setBitmap(aji.f);
							}
						}
					} catch (Exception e) { }
				}
			}
			*/
		}
	}
}