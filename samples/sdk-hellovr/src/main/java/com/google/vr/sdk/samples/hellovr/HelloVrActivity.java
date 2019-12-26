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
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;

import java.io.IOException;

import cc.lym.Renderer.BlockRenderer;
import cc.lym.Renderer.HeadTransformProvider;
import cc.lym.Renderer.OverlayRenderer;
import cc.lym.Renderer.Renderer;

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
    private static final String TAG = "HelloVrActivity";
    private static final int MAZE_WIDTH = 9;          //size of maze(odd only)

    private Scene scene;

    private Player player;        //玩家
    private float[] headRPY;

    BlockRenderer blockRenderer;
    HeadTransformProvider headTransformProvider;
    OverlayRenderer overlayRenderer;
    Bitmap overlay;


    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((t,e) -> Log.e("uncaught exception","",e));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);

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
                    .andThen(blockRenderer=new BlockRenderer(-15,15,-15,15,-15,15,0,15,()->new BlockRenderer.Location(player.center_pos[0]+0.5,player.center_pos[1]+0.5,player.center_pos[2]+0.5),texture))
                    .andThen(headTransformProvider=new HeadTransformProvider())
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
        new SceneModifier().start();

        headRPY = new float[3];
        scene=new Scene();
        player=new Player(0.25f,0.25f,0.9f, 0.3f,new float[]{4,4,5}, headTransformProvider, blockRenderer, scene.scene);

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
                player.stop_move_toward(Player.Direction.FORWARD);
                break;
            default:
                Log.i(TAG, "DN");
                player.set_move_toward(Player.Direction.FORWARD);
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
            default:
        }
        return true;
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
            sleep_(1000);
            for(int i=0;i<scene.get_scene_height();i++){     //上下
                for(int j=0;j<scene.get_scene_width_ns();j++){  //南北
                    for(int k=0;k<scene.get_scene_width_we();k++) {  //东西
                        int up =scene.get_neighbor_block_id(i,j,k, Scene.Position.UP);
                        int down =scene.get_neighbor_block_id(i,j,k, Scene.Position.DOWN);
                        int east =scene.get_neighbor_block_id(i,j,k, Scene.Position.EAST);
                        int west =scene.get_neighbor_block_id(i,j,k, Scene.Position.WEST);
                        int north =scene.get_neighbor_block_id(i,j,k, Scene.Position.NORTH);
                        int south =scene.get_neighbor_block_id(i,j,k, Scene.Position.SOUTH);
                        Scene.Point point=scene.transform_to_render(i,j,k);
                        blockRenderer.updateBlock(point.x, point.y, point.z, scene.get_id(i,j,k), new int[]{up,south,east,north,west,down}, new int[][][]{{{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}, {{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}, {{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}});
                    }
                }
            }

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
                Log.i("hhh", "x: "+player.center_pos[0]+", y: "+player.center_pos[1]+", z: "+player.center_pos[2]);
            }
        }
    }
}
