/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.vr.sdk.samples.hellovr;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;                //detect screen press and release
import android.view.KeyEvent;
import android.media.MediaPlayer;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.audio.GvrAudioEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lym.Renderer.BlockRenderer;
import cc.lym.Renderer.HandRenderer;
import cc.lym.Renderer.HeadTransformProvider;
import cc.lym.Renderer.OverlayRenderer;
import cc.lym.Renderer.Renderer;
import cc.lym.leap.LeapReceiver;
import cc.lym.util.Location;

/**
 * A Google VR sample application.
 *
 * <p>This app presents a scene consisting of a room and a floating object. When the user finds the
 * object, they can invoke the trigger action, and a new object will be randomly spawned. When in
 * Cardboard mode, the user must gaze at the object and use the Cardboard trigger button. When in
 * Daydream mode, the user can use the controller to position the cursor, and use the controller
 * buttons to invoke the trigger action.
 */

public class HelloVrActivity extends GvrActivity {
    public static final int WEST_LIMIT=0,EAST_LIMIT=20,SOUTH_LIMIT=0,NORTH_LIMIT=50,BOTTOM_LIMIT=0,TOP_LIMIT=30;
    public static int wrapWE(int val){return ((val-WEST_LIMIT)%(EAST_LIMIT-WEST_LIMIT)+(EAST_LIMIT-WEST_LIMIT))%(EAST_LIMIT-WEST_LIMIT)+WEST_LIMIT;}
    public static int wrapSN(int val){return ((val-SOUTH_LIMIT)%(NORTH_LIMIT-SOUTH_LIMIT)+(NORTH_LIMIT-SOUTH_LIMIT))%(NORTH_LIMIT-SOUTH_LIMIT)+SOUTH_LIMIT;}
    public static int wrapDU(int val){return ((val-BOTTOM_LIMIT)%(TOP_LIMIT-BOTTOM_LIMIT)+(TOP_LIMIT-BOTTOM_LIMIT))%(TOP_LIMIT-BOTTOM_LIMIT)+BOTTOM_LIMIT;}
    private static final String TAG = "HelloVrActivity";
	private static final int[]AVAILABLE_BLOCKS={1,2,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30};
	private static final List<Integer>AVAILABLE_BLOCKS_LIST=new ArrayList<>();
	static
	{
		for(int type:AVAILABLE_BLOCKS)
			AVAILABLE_BLOCKS_LIST.add(type);
	}

    private Scene scene;
    private Player player;        //玩家

    BlockRenderer blockRenderer;
    HeadTransformProvider headTransformProvider;
    HandRenderer handRenderer;
    OverlayRenderer overlayRenderer;
    LeapReceiver leapReceiver;
    Bitmap overlay;

    private MediaPlayer mp;

    private float[] headRotation;
    private float speed_2_old = 0;
    private float soundRelativex;
    private float soundRelativey;
    private float soundRelativez;
    private GvrAudioEngine gvrAudioEngine;
    private volatile int walkGrassId = GvrAudioEngine.INVALID_ID;
    private volatile int walkStoneId = GvrAudioEngine.INVALID_ID;
    private volatile int dropId = GvrAudioEngine.INVALID_ID;
    private static final String WALK_GRASS_SOUND_FILE = "audio/grass2.ogg";
    private static final String WALK_STONE_SOUND_FILE = "audio/stone6.ogg";
    private static final String DROP_SOUND_FILE = "audio/gravel1.ogg";
    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((t,e) -> Log.e("uncaught exception","",e));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);

        headRotation = new float[4];
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        // Avoid any delays during start-up due to decoding of sound files.
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        mp = MediaPlayer.create(HelloVrActivity.this, R.raw.creative6);
                        mp.setVolume(0.03f, 0.03f);
                        mp.setLooping(true);
                        mp.start();

                        gvrAudioEngine.preloadSoundFile(WALK_GRASS_SOUND_FILE);
                        walkGrassId = gvrAudioEngine.createSoundObject(WALK_GRASS_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(walkGrassId, 0, -1.5f, 0);
                        gvrAudioEngine.setSoundVolume(walkGrassId, 0.5f);
                        gvrAudioEngine.playSound(walkGrassId, true /* looped playback */);
                        gvrAudioEngine.pauseSound(walkGrassId);

                        gvrAudioEngine.preloadSoundFile(WALK_STONE_SOUND_FILE);
                        walkStoneId = gvrAudioEngine.createSoundObject(WALK_STONE_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(walkStoneId, 0, -1.5f, 0);
                        gvrAudioEngine.setSoundVolume(walkStoneId, 1.0f);
                        gvrAudioEngine.playSound(walkStoneId, true /* looped playback */);
                        gvrAudioEngine.pauseSound(walkStoneId);

                        gvrAudioEngine.preloadSoundFile(DROP_SOUND_FILE);
                    }
                })
                .start();

        GvrView gvrView = findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        byte[]texture=null;
        try{
            AssetFileDescriptor tex=getAssets().openFd("wall.png");
            int length=(int)tex.getLength();
            texture=new byte[length];
            int res=tex.createInputStream().read(texture);
            if(res!=length)
                Log.w("texture loader",String.format("%d bytes expected, %d read",length,res));
        }catch(IOException e){throw new RuntimeException("IOException",e);}
        try{
            GvrView.StereoRenderer renderer= Renderer.base()
                    .andThen(blockRenderer=new BlockRenderer(WEST_LIMIT,EAST_LIMIT,SOUTH_LIMIT,NORTH_LIMIT,BOTTOM_LIMIT,TOP_LIMIT,0,15,()->new Location(player.center_pos[0]+0.5,player.center_pos[1]+0.5,player.center_pos[2]+0.5),texture))
                    .andThen(headTransformProvider=new HeadTransformProvider())
                    .andThen(handRenderer=new HandRenderer())
                    .andThen(overlayRenderer=new OverlayRenderer(overlay= BitmapFactory.decodeStream(getAssets().open("overlay.png"))));
            gvrView.setRenderer(renderer);
        }catch(IOException ignored){}
        gvrView.setTransitionViewEnabled(true);
        if (gvrView.setAsyncReprojectionEnabled(true))
        {
            Log.w("init","Sustained performance mode");
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
        setGvrView(gvrView);
        try
        {
            scene=new Scene(getAssets().open("scene_data.txt"));
        }catch(IOException e){throw new RuntimeException(e);}
        new SceneModifier().start();

        player=new Player(0.25f,0.25f,1.5f,0.2f,new float[]{10,41,7}, headTransformProvider, blockRenderer, handRenderer, scene);

        leapReceiver=new LeapReceiver(this::deleteBlock,this::setBlock,()->{},()->{});
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Called when there's a screen action
     * Mainly to detect trigger release and stop moving
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_UP:
                Log.i(TAG, "UP");
                player.jump();
                //setBlock();
                //player.stop_move_toward(Player.Direction.FORWARD);
                break;
            default:
                Log.i(TAG, "DN");
                deleteBlock();
                //player.set_move_toward(Player.Direction.FORWARD);
        }
        return true;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("keycode", String.valueOf(keyCode));
        switch (keyCode){
            case KeyEvent.KEYCODE_X:
                player.jump();
                break;
            case KeyEvent.KEYCODE_W:
                player.set_move_toward(Player.Direction.FORWARD);
                break;
            case KeyEvent.KEYCODE_A:
                player.set_move_toward(Player.Direction.LEFTWARD);
                break;
            case KeyEvent.KEYCODE_S:
                player.set_move_toward(Player.Direction.BACKWARD);
                break;
            case KeyEvent.KEYCODE_D:
                player.set_move_toward(Player.Direction.RIGHTWARD);
                break;
            case KeyEvent.KEYCODE_Z:
                deleteBlock();
                break;
            case KeyEvent.KEYCODE_C:
                setBlock();
                break;
            case KeyEvent.KEYCODE_Q:
                player.MOVE_SPEED = 0.10f;
                break;
            default:
        }
        return true;
    }
    
	private Iterator<Integer>nextBlockType=AVAILABLE_BLOCKS_LIST.iterator();
    private char blockInHand=(char)(int)nextBlockType.next();
    private void setBlock() {
        CrossPoint cross = player.get_facing_block();
        if (cross != null) {
            if (cross.type == 0 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj + 1, cross.nextblockk))
                player.set_block(cross.nextblocki, cross.nextblockj + 1, cross.nextblockk, blockInHand);
            else if (cross.type == 1 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj - 1, cross.nextblockk))
                player.set_block(cross.nextblocki, cross.nextblockj - 1, cross.nextblockk, blockInHand);
            else if (cross.type == 2 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj, cross.nextblockk + 1))
                player.set_block(cross.nextblocki, cross.nextblockj, cross.nextblockk + 1, blockInHand);
            else if (cross.type == 3 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj, cross.nextblockk - 1))
                player.set_block(cross.nextblocki, cross.nextblockj, cross.nextblockk - 1, blockInHand);
            else if (cross.type == 4 && player.canPlaceBlock(cross.nextblocki - 1, cross.nextblockj, cross.nextblockk))
                player.set_block(cross.nextblocki - 1, cross.nextblockj, cross.nextblockk, blockInHand);
            else if (cross.type == 5 && player.canPlaceBlock(cross.nextblocki + 1, cross.nextblockj, cross.nextblockk))
                player.set_block(cross.nextblocki + 1, cross.nextblockj, cross.nextblockk, blockInHand);
            blockInHand=(char)(int)nextBlockType.next();
            if(!nextBlockType.hasNext())
            	nextBlockType=AVAILABLE_BLOCKS_LIST.iterator();
        }
    }
    
    private void deleteBlock() {
        CrossPoint cross = player.get_facing_block();
        if (cross != null && scene.scene[cross.nextblocki][cross.nextblockj][cross.nextblockk] != 3
                && scene.scene[cross.nextblocki][cross.nextblockj][cross.nextblockk] != 31){
            player.set_block(cross.nextblocki, cross.nextblockj, cross.nextblockk, (char)0);
        }
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_W:
                player.stop_move_toward(Player.Direction.FORWARD);
                break;
            case KeyEvent.KEYCODE_A:
                player.stop_move_toward(Player.Direction.LEFTWARD);
                break;
            case KeyEvent.KEYCODE_S:
                player.stop_move_toward(Player.Direction.BACKWARD);
                break;
            case KeyEvent.KEYCODE_D:
                player.stop_move_toward(Player.Direction.RIGHTWARD);
                break;
            case KeyEvent.KEYCODE_Q:
                player.MOVE_SPEED = 0.05f;
                break;
            default:
        }
        return true;
    }


    class SceneModifier extends Thread
    {
        void sleep_(long millis)
        {
            try{sleep(millis);}catch(InterruptedException ignored){}
        }
        @Override public void run()
        {
            setName("scene modifier");
            setUncaughtExceptionHandler((thread,exception)->{Log.e(TAG,"uncaught",exception);throw new RuntimeException(exception);});
            sleep_(1000);
            int ns_1=scene.get_scene_width_ns()-1;
            int we_1=scene.get_scene_width_we()-1;
            blockRenderer.init((x,y,z)->scene.scene[(int)z][(int)(ns_1-x)][(int)(we_1-y)],(x,y,z)->15,EAST_LIMIT-WEST_LIMIT,NORTH_LIMIT-SOUTH_LIMIT,TOP_LIMIT-BOTTOM_LIMIT);

            Log.w(TAG,"scene inited");

            //			blockRenderer.updateBlock(0,0,-10,2,new int[]{0,0,2,0,0,2},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
//			blockRenderer.updateBlock(0,0,10,1,new int[]{0,0,2,0,0,2},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
//			blockRenderer.updateBlock(0,-10,0,2,new int[]{0,0,2,0,0,2},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
//			blockRenderer.updateBlock(0,10,0,1,new int[]{0,0,2,0,0,2},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
//			blockRenderer.updateBlock(-10,0,0,2,new int[]{0,0,2,0,0,2},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
//			blockRenderer.updateBlock(10,0,0,2,new int[]{0,0,2,0,0,2},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
            int mark=0;
//            while(true)
//            {
//                sleep_(5000);
//                blockRenderer.updateBlock(0,0,-10,(mark++)%3,new int[]{0,0,0,0,0,0},new int[][][]{{{15,15,15},{15,5,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}},{{15,15,15},{15,15,15},{15,15,15}}});
//
//            }
            while(true){
                //Log.i("hhh1", "x: "+player.center_pos[0]+", y: "+player.center_pos[1]+", z: "+player.center_pos[2]);
                sleep_(20);
                player.update_pos();
                // Update the 3d audio engine with the most recent head rotation.
                player.head.getQuaternion(headRotation, 0);
                gvrAudioEngine.setHeadRotation(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
                gvrAudioEngine.update();
                //check play walk sound or not
                int bottom = player.getBottomBlock();
                if (player.isMoving()){
                    if (bottom == 0 || bottom == 31){
                        gvrAudioEngine.pauseSound(walkGrassId);
                        gvrAudioEngine.pauseSound(walkStoneId);
                    }
                    else if (bottom == 2 || bottom == 5){
                        gvrAudioEngine.resumeSound(walkGrassId);
                        gvrAudioEngine.pauseSound(walkStoneId);
                    }
                    else{
                        gvrAudioEngine.pauseSound(walkGrassId);
                        gvrAudioEngine.resumeSound(walkStoneId);
                    }
                }
                else{
                    gvrAudioEngine.pauseSound(walkGrassId);
                    gvrAudioEngine.pauseSound(walkStoneId);
                }
                if (player.speed[2] == 0 && speed_2_old < 0){
                    dropId = gvrAudioEngine.createSoundObject(DROP_SOUND_FILE);
                    gvrAudioEngine.setSoundObjectPosition(dropId, 0, -1.5f, 0);
                    gvrAudioEngine.setSoundVolume(dropId, Math.min(1.0f, 8*speed_2_old*speed_2_old));
                    gvrAudioEngine.playSound(dropId, false);
                }
                speed_2_old = player.speed[2];
                Log.i("hhh", "bottom:"+speed_2_old);
                //Log.i("hhh", "x: "+player.center_pos[0]+", y: "+player.center_pos[1]+", z: "+player.center_pos[2]);
            }
        }
    }
}
