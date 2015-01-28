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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/**
 * An OpenGL ES renderer based on the GLSurfaceView rendering framework.  This
 * class is responsible for drawing a list of renderables to the screen every
 * frame.  It also manages loading of textures and (when VBOs are used) the
 * allocation of vertex buffer objects.
 */
public class SimpleGLRenderer implements GLSurfaceView.Renderer {
	// A reference to the application context.
	private Context mContext;
	
	private LandTileMap mTileMap;
	private LandTileMap mOverlayMap;
	
	private Vector3 mCameraPosition = new Vector3();
	private Vector3 mLookAtPosition = new Vector3();
	private Object mCameraLock = new Object();
	private boolean mCameraDirty;

	public SimpleGLRenderer(Context context, LandTileMap tileMap, LandTileMap overlayMap) {
		mContext = context;
		
		mTileMap = tileMap;
		mOverlayMap = overlayMap;
	}
	
	/** Draws the landscape. */
	@Override
	public void onDrawFrame(GL10 gl) {
		//ProfileRecorder.sSingleton.stop(ProfileRecorder.PROFILE_FRAME);
		//ProfileRecorder.sSingleton.start(ProfileRecorder.PROFILE_FRAME);
		
		if (mTileMap != null) {
			// Clear the screen.  Note that a real application probably would only clear the depth buffer
			// (or maybe not even that).
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			
			// If the camera has moved since the last frame, rebuild our projection matrix.
			if (mCameraDirty){
				synchronized (mCameraLock) {
					gl.glMatrixMode(GL10.GL_MODELVIEW);
					gl.glLoadIdentity();
					GLU.gluLookAt(gl, mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, 
						mLookAtPosition.x, mLookAtPosition.y, mLookAtPosition.z, 
						0.0f, 1.0f, 0.0f);
					mCameraDirty = false;
				}
			}
			
			//ProfileRecorder.sSingleton.start(ProfileRecorder.PROFILE_DRAW);
			// Draw the landscape.
			if (mTileMap.getMap() != null)
				mTileMap.draw(gl, mCameraPosition);
			if (mOverlayMap.getMap() != null)
				mOverlayMap.draw(gl, mCameraPosition);
			//ProfileRecorder.sSingleton.stop(ProfileRecorder.PROFILE_DRAW);
		}
		
		//ProfileRecorder.sSingleton.endFrame();
	}

	/* Called when the size of the window changes. */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);

		/*
		 * Set our projection matrix. This doesn't have to be done each time we
		 * draw, but usually a new projection needs to be set when the viewport
		 * is resized.
		 */
		float ratio = (float)width / height;
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		GLU.gluPerspective(gl, 60.0f, ratio, 2.0f, 3000.0f);

		mCameraDirty = true;
	}
	
	public void setCameraPosition(float x, float y, float z) {
		synchronized (mCameraLock) {
			mCameraPosition.set(x, y, z);
			mCameraDirty = true;
		}
	}
	
	public void setCameraLookAtPosition(float x, float y, float z) {
		synchronized (mCameraLock) {
			mLookAtPosition.set(x, y, z);
			mCameraDirty = true;
		}
	}
      
	/**
	 * Called whenever the surface is created.  This happens at startup, and
	 * may be called again at runtime if the device context is lost (the screen
	 * goes to sleep, etc).  This function must fill the contents of vram with
	 * texture data and (when using VBOs) hardware vertex arrays.
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		/*
		 * Some one-time OpenGL initialization can be made here probably based
		 * on features of this particular context
		 */
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(0.6117f, 0.6941f, 0.89f, 1);
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_CULL_FACE);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		//gl.glEnable(GL10.GL_BLEND);
		//gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		mTileMap.loadBitmap(mContext, gl);
	}
}
