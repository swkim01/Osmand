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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * A 2D rectangular mesh. Can be drawn textured or untextured.
 * This version is modified from the original Grid.java (found in
 * the SpriteText package in the APIDemos Android sample) to support hardware
 * vertex buffers.
 */
class Grid {
    private FloatBuffer mFloatVertexBuffer;
    private FloatBuffer mFloatTexCoordBuffer;
    private FloatBuffer mFloatColorBuffer;

    private CharBuffer mIndexBuffer;
    
    private Buffer mVertexBuffer;
    private Buffer mTexCoordBuffer;
    private Buffer mColorBuffer;
    private int mCoordinateType;

    private int mW;
    private int mH;
    private int mIndexCount;
    private int mVertBufferIndex;
    private int mIndexBufferIndex;
    private int mTextureCoordBufferIndex;
    private int mColorBufferIndex;
    
    public Grid(int vertsAcross, int vertsDown) {
        if (vertsAcross < 0 || vertsAcross >= 65536) {
            throw new IllegalArgumentException("vertsAcross");
        }
        if (vertsDown < 0 || vertsDown >= 65536) {
            throw new IllegalArgumentException("vertsDown");
        }
        if (vertsAcross * vertsDown >= 65536) {
            throw new IllegalArgumentException("vertsAcross * vertsDown >= 65536");
        }

        
        mW = vertsAcross;
        mH = vertsDown;
        int size = vertsAcross * vertsDown;
        final int FLOAT_SIZE = 4;
        final int CHAR_SIZE = 2;
        
       	mFloatVertexBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 3)
           	.order(ByteOrder.nativeOrder()).asFloatBuffer();
       	mFloatTexCoordBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 2)
           	.order(ByteOrder.nativeOrder()).asFloatBuffer();
       	mFloatColorBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE * size * 4)
       	.order(ByteOrder.nativeOrder()).asFloatBuffer();
        	
        	
       	mVertexBuffer = mFloatVertexBuffer;
       	mTexCoordBuffer = mFloatTexCoordBuffer;
       	mColorBuffer = mFloatColorBuffer;
       	mCoordinateType = GL10.GL_FLOAT;

        int quadW = mW - 1;
        int quadH = mH - 1;
        int quadCount = quadW * quadH;
        int indexCount = quadCount * 6;
        mIndexCount = indexCount;
        mIndexBuffer = ByteBuffer.allocateDirect(CHAR_SIZE * indexCount)
            .order(ByteOrder.nativeOrder()).asCharBuffer();

        /*
         * Initialize triangle list mesh.
         *
         *     [0]-----[  1] ...
         *      |    /   |
         *      |   /    |
         *      |  /     |
         *     [w]-----[w+1] ...
         *      |       |
         *
         */

        {
            int i = 0;
            for (int y = 0; y < quadH; y++) {
                for (int x = 0; x < quadW; x++) {
                    char a = (char) (y * mW + x);
                    char b = (char) (y * mW + x + 1);
                    char c = (char) ((y + 1) * mW + x);
                    char d = (char) ((y + 1) * mW + x + 1);

                    mIndexBuffer.put(i++, a);
                    mIndexBuffer.put(i++, b);
                    mIndexBuffer.put(i++, c);

                    mIndexBuffer.put(i++, b);
                    mIndexBuffer.put(i++, c);
                    mIndexBuffer.put(i++, d);
                }
            }
        }
        
        mVertBufferIndex = 0;
    }

    void set(int i, int j, float x, float y, float z, float u, float v, float[] color) {
        if (i < 0 || i >= mW) {
            throw new IllegalArgumentException("i");
        }
        if (j < 0 || j >= mH) {
            throw new IllegalArgumentException("j");
        }

        final int index = mW * j + i;

        final int posIndex = index * 3;
        final int texIndex = index * 2;
        final int colorIndex = index * 4;
        
       	mFloatVertexBuffer.put(posIndex, x);
       	mFloatVertexBuffer.put(posIndex + 1, y);
       	mFloatVertexBuffer.put(posIndex + 2, z);
	
       	mFloatTexCoordBuffer.put(texIndex, u);
       	mFloatTexCoordBuffer.put(texIndex + 1, v);
        	
       	if (color != null) {
       		mFloatColorBuffer.put(colorIndex, color[0]);
       		mFloatColorBuffer.put(colorIndex + 1, color[1]);
       		mFloatColorBuffer.put(colorIndex + 2, color[2]);
       		mFloatColorBuffer.put(colorIndex + 3, color[3]);
       	}
    }

    public static void beginDrawing(GL10 gl, boolean useTexture, boolean useBlend) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        
        if (useTexture) {
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glEnable(GL10.GL_TEXTURE_2D);
        } else {
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glDisable(GL10.GL_TEXTURE_2D);
        }
        if (useBlend) {
        	gl.glDisable(GL10.GL_DEPTH_TEST);
        	gl.glEnable(GL10.GL_BLEND);
        	gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        	//gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE);
        }
        /*
        if (useColor) {
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        } else {
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }
        */
    }
    
    
    public void draw(GL10 gl, boolean useTexture, boolean useColor) {
        gl.glVertexPointer(3, mCoordinateType, 0, mVertexBuffer);
    
        if (useTexture) {
            gl.glTexCoordPointer(2, mCoordinateType, 0, mTexCoordBuffer);
        }
            
        if (useColor) {
            gl.glColorPointer(4, mCoordinateType, 0, mColorBuffer);
        }
    
        gl.glDrawElements(GL10.GL_TRIANGLES, mIndexCount,
                GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
    }
    
    public static void endDrawing(GL10 gl, boolean useBlend) {
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        if (useBlend) {
        	gl.glDisable(GL10.GL_BLEND);
        	gl.glEnable(GL10.GL_DEPTH_TEST);
        }
    }
  
    // These functions exposed to patch Grid info into native code.
    public final int getVertexBuffer() {
    	return mVertBufferIndex;
    }
    
    public final int getTextureBuffer() {
    	return mTextureCoordBufferIndex;
    }
    
    public final int getIndexBuffer() {
    	return mIndexBufferIndex;
    }
    
    public final int getColorBuffer() {
    	return mColorBufferIndex;
    }

	public final int getIndexCount() {
		return mIndexCount;
	}

	public long getVertexCount() {
		return mW * mH;
	}

}
