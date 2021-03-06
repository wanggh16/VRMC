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
import android.graphics.Matrix;
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
import cc.lym.Renderer.EntityRenderer;
import cc.lym.Renderer.HandRenderer;
import cc.lym.Renderer.HeadTransformProvider;
import cc.lym.Renderer.OverlayRenderer;
import cc.lym.Renderer.Renderer;
import cc.lym.leap.LeapReceiver;
import cc.lym.util.Location;
import cc.lym.util.Supplier;

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
    private Player player;          //玩家
    private int currentBlockIndex=0;
    private int currentBlockId=1;

    BlockRenderer blockRenderer;
    HeadTransformProvider headTransformProvider;
    HandRenderer handRenderer;
    OverlayRenderer overlayRenderer;
    OverlayRenderer optionalBlockRender;
    LeapReceiver leapReceiver;
    EntityRenderer creeperRenderer;
    Bitmap overlay;
    private boolean leapHandUpMode=false;
    
    private final Creeper[]creepers=new Creeper[1];

    private MediaPlayer mp;

    private int blockCD = 0;
    private int explodei = 0, explodej = 0, explodek = 0;
    private float speed_2_old = 0;
    private float[] headRotation;
    private static GvrAudioEngine gvrAudioEngine;
    private volatile int walkGrassId = GvrAudioEngine.INVALID_ID;
    private volatile int walkStoneId = GvrAudioEngine.INVALID_ID;
    private volatile int dropId = GvrAudioEngine.INVALID_ID;
    private volatile int digId = GvrAudioEngine.INVALID_ID;
    private volatile int fuseId = GvrAudioEngine.INVALID_ID;
    private volatile int explodeId = GvrAudioEngine.INVALID_ID;
    private volatile int creeperId = GvrAudioEngine.INVALID_ID;
    private static final String WALK_GRASS_SOUND_FILE = "audio/grass2.ogg";
    private static final String WALK_STONE_SOUND_FILE = "audio/stone6.ogg";
    private static final String DROP_SOUND_FILE = "audio/gravel1.ogg";
    private static final String DIG_GRASS_SOUND_FILE = "audio/diggrass2.ogg";
    private static final String DIG_STONE_SOUND_FILE = "audio/digstone4.ogg";
    private static final String FUSE_SOUND_FILE = "audio/fuse.ogg";
    private static final String EXPLODE_SOUND_FILE = "audio/explode2.ogg";
    public static final String CREEPER_SOUND_FILE = "audio/creeper1.ogg";
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

                        gvrAudioEngine.preloadSoundFile(CREEPER_SOUND_FILE);
                        creeperId = gvrAudioEngine.createSoundObject(CREEPER_SOUND_FILE);
                        gvrAudioEngine.setSoundVolume(creeperId, 1.0f);
                        gvrAudioEngine.playSound(creeperId, true /* looped playback */);

                        gvrAudioEngine.preloadSoundFile(WALK_GRASS_SOUND_FILE);
                        walkGrassId = gvrAudioEngine.createSoundObject(WALK_GRASS_SOUND_FILE);
                        gvrAudioEngine.setSoundVolume(walkGrassId, 0.5f);
                        gvrAudioEngine.playSound(walkGrassId, true /* looped playback */);
                        gvrAudioEngine.pauseSound(walkGrassId);

                        gvrAudioEngine.preloadSoundFile(WALK_STONE_SOUND_FILE);
                        walkStoneId = gvrAudioEngine.createSoundObject(WALK_STONE_SOUND_FILE);
                        gvrAudioEngine.setSoundVolume(walkStoneId, 1.0f);
                        gvrAudioEngine.playSound(walkStoneId, true /* looped playback */);
                        gvrAudioEngine.pauseSound(walkStoneId);

                        gvrAudioEngine.preloadSoundFile(DROP_SOUND_FILE);
                        gvrAudioEngine.preloadSoundFile(DIG_GRASS_SOUND_FILE);
                        gvrAudioEngine.preloadSoundFile(DIG_STONE_SOUND_FILE);
                        gvrAudioEngine.preloadSoundFile(FUSE_SOUND_FILE);
                        gvrAudioEngine.preloadSoundFile(EXPLODE_SOUND_FILE);
                    }
                })
                .start();

        GvrView gvrView = findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
	
		Supplier<Location> locationSupplier=()->new Location(player.center_pos[0]+0.5,player.center_pos[1]+0.5,player.center_pos[2]+0.5);
        
        byte[]texture=null;
		try{
			AssetFileDescriptor tex=getAssets().openFd("texture.png");
			int length=(int)tex.getLength();
			texture=new byte[length];
			int res=tex.createInputStream().read(texture);
			if(res!=length)
				Log.w("texture loader",String.format("%d bytes expected, %d read",length,res));
		}catch(IOException e){throw new RuntimeException("IOException",e);}
		creeperRenderer=CreeperAgent.init(locationSupplier,creepers.length,texture);

		texture=null;
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
                    .andThen(blockRenderer=new BlockRenderer(WEST_LIMIT,EAST_LIMIT,SOUTH_LIMIT,NORTH_LIMIT,BOTTOM_LIMIT,TOP_LIMIT,0,15,locationSupplier,texture))
                    .andThen(creeperRenderer)
                    .andThen(headTransformProvider=new HeadTransformProvider())
                    .andThen(handRenderer=new HandRenderer())
                    .andThen(overlayRenderer=new OverlayRenderer(overlay= BitmapFactory.decodeStream(getAssets().open("overlay.png")),0.5f,0.7f,0.7f))//;
                    .andThen(optionalBlockRender=new OverlayRenderer(BitmapFactory.decodeStream(getAssets().open(currentBlockId+".png")),0.499f,0.7f,0.7f));
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

        player=new Player(0.3f,0.3f,1.5f,0.2f,new float[]{10,41,6}, headTransformProvider, blockRenderer, handRenderer, scene);
        for(int i=0;i<creepers.length;i++)creepers[i]=new Creeper(0.3f,0.3f,1.5f,0.2f,new float[]{10.6f,41,6f},scene);
        leapReceiver=new LeapReceiver(this::deleteBlock,this::setBlock,()->{leapHandUpMode=true;updateItemBar();updateMainOverlay();},()->{leapHandUpMode=false;updateItemBar();updateMainOverlay();});

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
                //player.jump();
                deleteBlock();
                player.stop_move_toward(Player.Direction.FORWARD);
                break;
            default:
                Log.i(TAG, "DN");
                //deleteBlock();
                player.set_move_toward(Player.Direction.FORWARD);
        }
        return true;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(leapHandUpMode)
        {
            if(keyCode==KeyEvent.KEYCODE_A)
                keyCode=KeyEvent.KEYCODE_1;
            if(keyCode==KeyEvent.KEYCODE_D)
                keyCode=KeyEvent.KEYCODE_2;
            if(keyCode==KeyEvent.KEYCODE_W)
                keyCode=KeyEvent.KEYCODE_3;
            if(keyCode==KeyEvent.KEYCODE_S)
                keyCode=KeyEvent.KEYCODE_4;
        }
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
                if (blockCD == 0) deleteBlock();
                break;
            case KeyEvent.KEYCODE_C:
                if (blockCD == 0) setBlock();
                break;
            case KeyEvent.KEYCODE_Q:
                player.MOVE_SPEED = 0.10f;
                //Scene.Point block_curr = scene.transform_render_to_array(player.center_pos[0],player.center_pos[1],player.center_pos[2] - 1);
                //player.explode((int)block_curr.x, (int)block_curr.y, (int)block_curr.z, 3);
                break;
            case KeyEvent.KEYCODE_1:    //选择物品栏中左边的方块
                currentBlockIndex-=1;
                if(currentBlockIndex<0)currentBlockIndex=AVAILABLE_BLOCKS_LIST.size()-1;
                currentBlockId=AVAILABLE_BLOCKS_LIST.get(currentBlockIndex);
                updateItemBar();
                break;
            case KeyEvent.KEYCODE_2:    //选择物品栏中右边的方块
                currentBlockIndex+=1;
                if(currentBlockIndex>=AVAILABLE_BLOCKS_LIST.size())currentBlockIndex=0;
                currentBlockId=AVAILABLE_BLOCKS_LIST.get(currentBlockIndex);
                updateItemBar();
                break;
            case KeyEvent.KEYCODE_3:    //选择物品栏中最左边的方块
                for(int i=0;i<4;i++)
                {
                    currentBlockIndex-=1;
                    if(currentBlockIndex<0)currentBlockIndex=AVAILABLE_BLOCKS_LIST.size()-1;
                    currentBlockId=AVAILABLE_BLOCKS_LIST.get(currentBlockIndex);
                }
                updateItemBar();
                break;
            case KeyEvent.KEYCODE_4:    //选择物品栏中最右边的方块
                for(int i=0;i<4;i++)
                {
                    currentBlockIndex+=1;
                    if(currentBlockIndex>=AVAILABLE_BLOCKS_LIST.size())currentBlockIndex=0;
                    currentBlockId=AVAILABLE_BLOCKS_LIST.get(currentBlockIndex);
                }
                updateItemBar();
                break;
            default:
        }
        return true;
    }

	private Iterator<Integer>nextBlockType=AVAILABLE_BLOCKS_LIST.iterator();
    private void setBlock() {
        CrossPoint cross = player.get_facing_block();
        if (cross != null) {
            boolean placesuccess = false;
            int placedblocki = cross.nextblocki;
            int placedblockj = cross.nextblockj;
            int placedblockk = cross.nextblockk;
            if (cross.type == 0 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj + 1, cross.nextblockk)) {
                placedblockj++;
                placesuccess = true;
            }
            else if (cross.type == 1 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj - 1, cross.nextblockk)) {
                placedblockj--;
                placesuccess = true;
            }
            else if (cross.type == 2 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj, cross.nextblockk + 1)) {
                placedblockk++;
                placesuccess = true;
            }
            else if (cross.type == 3 && player.canPlaceBlock(cross.nextblocki, cross.nextblockj, cross.nextblockk - 1)) {
                placedblockk--;
                placesuccess = true;
            }
            else if (cross.type == 4 && player.canPlaceBlock(cross.nextblocki - 1, cross.nextblockj, cross.nextblockk)) {
                placedblocki--;
                placesuccess = true;
            }
            else if (cross.type == 5 && player.canPlaceBlock(cross.nextblocki + 1, cross.nextblockj, cross.nextblockk)) {
                placedblocki++;
                placesuccess = true;
            }

            if (placesuccess) {
                player.set_block(placedblocki, placedblockj, placedblockk, (char)currentBlockId);
                if (currentBlockId == 2 || currentBlockId == 5 || currentBlockId == 8 || currentBlockId == 28) digId = gvrAudioEngine.createSoundObject(DIG_GRASS_SOUND_FILE);
                else digId = gvrAudioEngine.createSoundObject(DIG_STONE_SOUND_FILE);
                Scene.Point block_center_pos_render = scene.transform_array_to_render(placedblocki, placedblockj, placedblockk);
                Scene.Point block_pos_gvr = scene.transform_render_to_sdk(block_center_pos_render.x, block_center_pos_render.y, block_center_pos_render.z);
                Scene.Point player_pos_gvr = scene.transform_render_to_sdk(player.center_pos[0], player.center_pos[1], player.center_pos[2]);
                headTransformProvider.getQuaternion(headRotation, 0);
                gvrAudioEngine.setHeadRotation(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
                gvrAudioEngine.setHeadPosition((float)player_pos_gvr.x,(float)player_pos_gvr.y,(float)player_pos_gvr.z);
                gvrAudioEngine.setSoundObjectPosition(digId, (float)block_pos_gvr.x, (float)block_pos_gvr.y, (float)block_pos_gvr.z);
                gvrAudioEngine.setSoundVolume(digId, Math.min(1.0f, (float)(1/cross.dist)));
                gvrAudioEngine.playSound(digId, false);

                if (currentBlockId == 8){
                    blockCD = 300;
                    fuseId = gvrAudioEngine.createSoundObject(FUSE_SOUND_FILE);
                    explodeId = gvrAudioEngine.createSoundObject(EXPLODE_SOUND_FILE);
                    gvrAudioEngine.setSoundObjectPosition(fuseId, (float)block_pos_gvr.x, (float)block_pos_gvr.y, (float)block_pos_gvr.z);
                    gvrAudioEngine.setSoundVolume(fuseId, 1.0f);
                    gvrAudioEngine.setSoundObjectPosition(explodeId, (float)block_pos_gvr.x, (float)block_pos_gvr.y, (float)block_pos_gvr.z);
                    gvrAudioEngine.setSoundVolume(explodeId, 1.0f);
                    gvrAudioEngine.playSound(fuseId, false);
                    explodei = placedblocki;
                    explodej = placedblockj;
                    explodek = placedblockk;
                }
                else blockCD = 5;
            }
        }
    }

    private void deleteBlock() {
        CrossPoint cross = player.get_facing_block();
        if (cross != null){
            char deletedblock = scene.scene[cross.nextblocki][cross.nextblockj][cross.nextblockk];
            if (deletedblock != 3 && deletedblock != 31) {
                player.set_block(cross.nextblocki, cross.nextblockj, cross.nextblockk, (char) 0);
                if (deletedblock == 2 || deletedblock == 5 || deletedblock == 8 || deletedblock == 28) digId = gvrAudioEngine.createSoundObject(DIG_GRASS_SOUND_FILE);
                else digId = gvrAudioEngine.createSoundObject(DIG_STONE_SOUND_FILE);
                Scene.Point block_center_pos_render = scene.transform_array_to_render(cross.nextblocki, cross.nextblockj, cross.nextblockk);
                Scene.Point block_pos_gvr = scene.transform_render_to_sdk(block_center_pos_render.x, block_center_pos_render.y, block_center_pos_render.z);
                Scene.Point player_pos_gvr = scene.transform_render_to_sdk(player.center_pos[0], player.center_pos[1], player.center_pos[2]);
                headTransformProvider.getQuaternion(headRotation, 0);
                gvrAudioEngine.setHeadRotation(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
                gvrAudioEngine.setHeadPosition((float)player_pos_gvr.x,(float)player_pos_gvr.y,(float)player_pos_gvr.z);
                gvrAudioEngine.setSoundObjectPosition(digId, (float)block_pos_gvr.x, (float)block_pos_gvr.y, (float)block_pos_gvr.z);
                gvrAudioEngine.setSoundVolume(digId, Math.min(1.0f, (float)(1/cross.dist)));
                gvrAudioEngine.playSound(digId, false);
                //挖去某一种方块时更新物品栏
                currentBlockId=deletedblock;
                currentBlockIndex=AVAILABLE_BLOCKS_LIST.indexOf((int)deletedblock);
                updateItemBar();
                //冷却时间
                blockCD = 5;
            }
        }
    }

    private void updateItemBar(){
        try{
            Log.i("itembar",""+currentBlockId);
            if(leapHandUpMode)
				optionalBlockRender.changeContent(moveUp(BitmapFactory.decodeStream(getAssets().open(currentBlockId+".png"))));
            else
            	optionalBlockRender.changeContent(BitmapFactory.decodeStream(getAssets().open(currentBlockId+".png")));
        }catch(IOException ignored){}
    }
    private void updateMainOverlay(){
    	if(leapHandUpMode)
    		overlayRenderer.changeContent(moveUp(overlay));
    	else
	    	overlayRenderer.changeContent(overlay);
	}
	private static Bitmap moveUp(Bitmap bitmap)
	{Log.w("move pic","up");
	
    	return Bitmap.createBitmap(bitmap,0,bitmap.getHeight()/2,bitmap.getWidth(),bitmap.getHeight()/2,null,false);
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
                // Regular update call to GVR audio engine.
                gvrAudioEngine.update();
                headTransformProvider.getQuaternion(headRotation, 0);
                gvrAudioEngine.setHeadRotation(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
                for(Creeper creeper:creepers){
                    creeper.update_pos();
                    gvrAudioEngine.setSoundVolume(creeperId, Math.min(1.0f, 1/
                            (Math.abs(player.center_pos[0] - creeper.center_pos[0]) + Math.abs(player.center_pos[1] - creeper.center_pos[1]) + Math.abs(player.center_pos[2] - creeper.center_pos[2]))
                    ));
                    Scene.Point creeper_pos_gvr = scene.transform_render_to_sdk(creeper.center_pos[0], creeper.center_pos[1], creeper.center_pos[2]);
                    gvrAudioEngine.setSoundObjectPosition(creeperId, (float)creeper_pos_gvr.x, (float)creeper_pos_gvr.y, (float)creeper_pos_gvr.z);
                }

                if (blockCD > 0) blockCD--;
                if (blockCD == 100){
                    gvrAudioEngine.playSound(explodeId, false);
                    player.explode(explodei, explodej, explodek, 3);
                }
                //check play walk sound or not
                int bottom = player.getBottomBlock();
                if (player.isMoving()){
                    Scene.Point block_pos_gvr = scene.transform_render_to_sdk(player.center_pos[0], player.center_pos[1], player.center_pos[2]-1.5);
                    Scene.Point player_pos_gvr = scene.transform_render_to_sdk(player.center_pos[0], player.center_pos[1], player.center_pos[2]);
                    gvrAudioEngine.setHeadPosition((float)player_pos_gvr.x,(float)player_pos_gvr.y,(float)player_pos_gvr.z);
                    if (bottom == 0 || bottom == 31){   //走在空气或空气墙上，不放走路声音
                        gvrAudioEngine.pauseSound(walkGrassId);
                        gvrAudioEngine.pauseSound(walkStoneId);
                    }else {
                        if (bottom == 2 || bottom == 5 || bottom == 8 || bottom == 28) {
                            gvrAudioEngine.setSoundObjectPosition(walkGrassId, (float)block_pos_gvr.x, (float)block_pos_gvr.y, (float)block_pos_gvr.z);
                            gvrAudioEngine.resumeSound(walkGrassId);
                            gvrAudioEngine.pauseSound(walkStoneId);
                        } else {
                            gvrAudioEngine.setSoundObjectPosition(walkStoneId, (float)block_pos_gvr.x, (float)block_pos_gvr.y, (float)block_pos_gvr.z);
                            gvrAudioEngine.pauseSound(walkGrassId);
                            gvrAudioEngine.resumeSound(walkStoneId);
                        }
                    }
                }
                else{
                    gvrAudioEngine.pauseSound(walkGrassId);
                    gvrAudioEngine.pauseSound(walkStoneId);
                }
                if (player.speed[2] == 0 && speed_2_old < 0){
                    dropId = gvrAudioEngine.createSoundObject(DROP_SOUND_FILE);
                    Scene.Point sound_pos_gvr = scene.transform_render_to_sdk(player.center_pos[0], player.center_pos[1], player.center_pos[2]-1.5f);
                    Scene.Point player_pos_gvr = scene.transform_render_to_sdk(player.center_pos[0], player.center_pos[1], player.center_pos[2]);
                    headTransformProvider.getQuaternion(headRotation, 0);
                    gvrAudioEngine.setHeadRotation(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
                    gvrAudioEngine.setHeadPosition((float)player_pos_gvr.x,(float)player_pos_gvr.y,(float)player_pos_gvr.z);
                    gvrAudioEngine.setSoundObjectPosition(dropId, (float)sound_pos_gvr.x, (float)sound_pos_gvr.y, (float)sound_pos_gvr.z);
                    gvrAudioEngine.setSoundVolume(dropId, Math.min(1.0f, 8*speed_2_old*speed_2_old));
                    gvrAudioEngine.playSound(dropId, false);
                }
                speed_2_old = player.speed[2];
            }
        }
    }
}
