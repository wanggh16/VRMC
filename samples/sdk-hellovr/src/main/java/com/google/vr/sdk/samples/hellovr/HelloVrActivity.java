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

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;                //detect screen press and release
import android.view.KeyEvent;
import android.media.MediaPlayer;
import android.net.Uri;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import java.io.IOException;
import javax.microedition.khronos.egl.EGLConfig;

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
  private static final float pi = 3.1415927f;
  private static final float Z_NEAR = 0.01f;
  private static final float Z_FAR = 60.0f;   //maximum visibility

  private static final int MAZE_SIZE = 9;          //size of maze(odd only)
  private static final int MAZE_Y = 0;              //for debug
  private static final float MOVE_SPEED = 0.06f;    //distance per frame
  private static final float TARGET_SPEED = 0.03f;
  private static final float DISTANCE_LIMIT = 1;    //distance to catch target
  private static final float PLAYER_RADIUS = 0.4f;  //collision case
  private static final float TARGET_RADIUS = 0.2f;
  private static final float ABSORB = 0.9f;

  private float[] modelTarget;
  private float[][] modelBlock; //transform matrices of all blocks
  private char[][] maze = {
          {1,1,1,1,1,1,1,1,1},
          {1,0,0,0,0,0,0,0,1},
          {1,0,1,0,1,1,1,0,1},
          {1,0,1,0,0,0,0,0,1},
          {1,0,1,0,0,0,1,0,1},
          {1,0,0,0,0,0,1,0,1},
          {1,0,1,1,1,0,1,0,1},
          {1,0,0,0,0,0,0,0,1},
          {1,1,1,1,1,1,1,1,1}
  };
  private int block_cnt;  //count of all blocks including floor and ceiling
  private int wall_cnt;   //count of wall blocks

//  private float X;    //location of camera(player)
//  private float Y;
//  private float Z;
  private Player player;        //玩家
  private float target_x = 0;  //location of target
  private float target_z = 0;
  private float targetMoveAngle = pi/4; //direction of target speed
  private int targetDirX = 1;
  private int targetDirZ = 1;
  private float[] headRPY;

  private boolean moving; //true when screen pressed continuously
  private boolean caught; //true when player catch the target
  private int dir = 1;
  private float x_inc,z_inc;

  /**
   * Sets the view to our GvrView and initializes the transformation matrices we will use
   * to render our scene.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initializeGvrView();
    
    headRPY = new float[3];
    modelTarget = new float[16];
    wall_cnt = 0;
    for (int i=0;i<MAZE_SIZE;i++) {
        for (int j=0;j<MAZE_SIZE;j++) {
            if (maze[i][j]==1) wall_cnt+=1;
        }
    }
    block_cnt = wall_cnt + 2*MAZE_SIZE*MAZE_SIZE;
    modelBlock = new float[block_cnt][16];


  }

  public void initializeGvrView() {
    setContentView(R.layout.common_ui);

    GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

    gvrView.setRenderer((GvrView.StereoRenderer)null);
    gvrView.setTransitionViewEnabled(true);

    // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
    // Daydream controller input for basic interactions using the existing Cardboard trigger API.
    gvrView.enableCardboardTriggerEmulation();

    if (gvrView.setAsyncReprojectionEnabled(true)) {
      // Async reprojection decouples the app framerate from the display framerate,
      // allowing immersive interaction even at the throttled clockrates set by
      // sustained performance mode.
      AndroidCompat.setSustainedPerformanceMode(this, true);
    }

    setGvrView(gvrView);
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
   * Called when screen touched
   */
  @Override
  public void onCardboardTrigger() {
    Log.i(TAG, "onCardboardTrigger");
      player.set_move_toward(Player.Direction.FORWARD);
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
              player.stop_move_toward(Player.Direction.FORWARD);
              break;
          default:
              Log.i(TAG, "DN");
              player.set_move_toward(Player.Direction.FORWARD);
              player.jump();
      }
      return true;
  }
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
      switch (keyCode){
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
}
