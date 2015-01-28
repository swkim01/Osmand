/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.osmand.plus.opengl;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;

// This class defines a single land tile.  It is built from
// a height map image and may contain several meshes defining different levels
// of detail.
public class LandTile {
	public final static float TILE_SIZE = 512;
	private final static float HALF_TILE_SIZE = TILE_SIZE / 2;
	
	public boolean mClearFlag;
	private Grid mLODMeshes[];
	private int mLODTextures[];
	private Vector3 mPosition = new Vector3();
	private Vector3 mCenterPoint = new Vector3();
	private Bitmap mHeightMap;
	private int mPositionX;
	private int mPositionZ;
	private float mHalfTileSize = HALF_TILE_SIZE;
	
	public LandTile() {
	}
	
	public LandTile(float sizeX, int positionX, int positionZ, float size) {
		mPositionX = positionX;
		mPositionZ = positionZ;
		mHalfTileSize = size / 2.0f;
	}
	
	public int getPositionX() {
		return mPositionX;
	}
	
	public int getPositionZ() {
		return mPositionZ;
	}
	
	public void setLods(Grid[] lodMeshes) {
		mLODMeshes = lodMeshes;
	}
	
	public void setLODTextures( int  LODTextures[], boolean clearFlag) {
		mLODTextures = LODTextures;
		mClearFlag = clearFlag;
		if (clearFlag == true) {
			mHeightMap = null;
		}
	}
	
	public final void setPosition(float x, float y, float z) {
		mPosition.set(x, y, z);
		mCenterPoint.set(x + mHalfTileSize, y, z + mHalfTileSize);
	}
	 
	public final void setPosition(Vector3 position) {
		mPosition.set(position);
		mCenterPoint.set(position.x + mHalfTileSize, position.y, position.z + mHalfTileSize);
	}
	
	public final Vector3 getPosition() {
		return mPosition;
	}
	
	public final Vector3 getCenterPoint() {
		return mCenterPoint;
	}
	
	public final Grid[] getLods() {
		return mLODMeshes;
	}
	
	public Bitmap getBitmap() {
		return mHeightMap;
	}
	
	public void setBitmap(Bitmap bitmap) {
		mHeightMap = bitmap;
	}
	
	public void deleteBitmap() {
		mHeightMap = null;
	}
		
	public void draw(GL10 gl, Vector3 cameraPosition) {	
		gl.glPushMatrix();
		gl.glTranslatef(mPosition.x, mPosition.y, mPosition.z);
		
		// TODO  - should add some code to keep state of current Texture and only set it if a new texture is needed - 
		// may be taken care of natively by OpenGL lib.
		if( mLODTextures != null ) {
			// Check to see if we have different LODs to choose from (i.e. Text LOD feature is turned on).  If not then
			// just select the default texture
			gl.glBindTexture(GL10.GL_TEXTURE_2D,  mLODTextures[0]);
		}
		
		mLODMeshes[0].draw(gl, true, true);
		
		gl.glPopMatrix();
	}

}
