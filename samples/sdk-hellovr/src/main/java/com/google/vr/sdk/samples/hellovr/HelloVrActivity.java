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

public class HelloVrActivity extends GvrActivity implements GvrView.StereoRenderer {
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

  private static final String[] OBJECT_VERTEX_SHADER_CODE =
      new String[] {
        "uniform mat4 u_MVP;",
        "attribute vec4 a_Position;",
        "attribute vec2 a_UV;",
        "varying vec2 v_UV;",
        "",
        "void main() {",
        "  v_UV = a_UV;",
        "  gl_Position = u_MVP * a_Position;",
        "}",
      };
  private static final String[] OBJECT_FRAGMENT_SHADER_CODE =
      new String[] {
        "precision mediump float;",
        "varying vec2 v_UV;",
        "uniform sampler2D u_Texture;",
        "",
        "void main() {",
        "  // The y coordinate of this sample's textures is reversed compared to",
        "  // what OpenGL expects, so we invert the y coordinate.",
        "  gl_FragColor = texture2D(u_Texture, vec2(v_UV.x, 1.0 - v_UV.y));",
        "}",
      };

  private int objectProgram;

  private int objectPositionParam;
  private int objectUvParam;
  private int objectModelViewProjectionParam;

  private TexturedMesh[] block; //object and textures of all blocks
  private Texture[] blockTex;
  private TexturedMesh targetObjectMesh;
  private Texture targetObjectNotSelectedTexture;
  private Texture targetObjectSelectedTexture;

  private float[] camera;
  private float[] view;
  private float[] modelViewProjection;
  private float[] modelView;

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

  private float X;    //location of camera(player)
  private float Y;
  private float Z;
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

  private MediaPlayer mp1, mp2; //play in turn to avoid delay
  private int sound_direction = 0; //50 horizontal points of CIPIC HRTF database
  private float sound_volume = 1;
  private int[] sound_files = {R.raw.seg1, R.raw.seg2, R.raw.seg3, R.raw.seg4, R.raw.seg5, R.raw.seg6, R.raw.seg7, R.raw.seg8, R.raw.seg9, R.raw.seg10,
          R.raw.seg11, R.raw.seg12, R.raw.seg13, R.raw.seg14, R.raw.seg15, R.raw.seg16, R.raw.seg17, R.raw.seg18, R.raw.seg19, R.raw.seg20,
          R.raw.seg21, R.raw.seg22, R.raw.seg23, R.raw.seg24, R.raw.seg25, R.raw.seg26, R.raw.seg27, R.raw.seg28, R.raw.seg29, R.raw.seg30,
          R.raw.seg31, R.raw.seg32, R.raw.seg33, R.raw.seg34, R.raw.seg35, R.raw.seg36, R.raw.seg37, R.raw.seg38, R.raw.seg39, R.raw.seg40,
          R.raw.seg41, R.raw.seg42, R.raw.seg43, R.raw.seg44, R.raw.seg45, R.raw.seg46, R.raw.seg47, R.raw.seg48, R.raw.seg49, R.raw.seg50};
  /**
   * Sets the view to our GvrView and initializes the transformation matrices we will use
   * to render our scene.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initializeGvrView();

    camera = new float[16];
    view = new float[16];
    modelViewProjection = new float[16];
    modelView = new float[16];
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
    block = new TexturedMesh[block_cnt];
    blockTex = new Texture[block_cnt];

    //init media players here
    mp1 = MediaPlayer.create(HelloVrActivity.this, R.raw.seg1);
    mp2 = MediaPlayer.create(HelloVrActivity.this, R.raw.seg1);
    mp1.setNextMediaPlayer(mp2);

    mp1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        // Override onCompletion method to apply desired operations.
        @Override
        public void onCompletion(MediaPlayer mediaPlayer){
            // Whatever you want to do when the audio playback is done...
            Log.i(TAG,"mp1");
            mp1_init();
        }
    } );
    mp2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        // Override onCompletion method to apply desired operations.
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            // Whatever you want to do when the audio playback is done...
            Log.i(TAG, "mp2");
            mp2_init();
        }
    } );
  }

  public void initializeGvrView() {
    setContentView(R.layout.common_ui);

    GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

    gvrView.setRenderer(this);
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

  @Override
  public void onRendererShutdown() {
    Log.i(TAG, "onRendererShutdown");
  }

  @Override
  public void onSurfaceChanged(int width, int height) {
    Log.i(TAG, "onSurfaceChanged");
  }

  /**
   * Creates the buffers we use to store information about the 3D world.
   *
   * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
   * Hence we use ByteBuffers.
   *
   * @param config The EGL configuration used when creating the surface.
   */
  @Override
  public void onSurfaceCreated(EGLConfig config) {
    Log.i(TAG, "onSurfaceCreated");

    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    objectProgram = Util.compileProgram(OBJECT_VERTEX_SHADER_CODE, OBJECT_FRAGMENT_SHADER_CODE);
    objectPositionParam = GLES20.glGetAttribLocation(objectProgram, "a_Position");
    objectUvParam = GLES20.glGetAttribLocation(objectProgram, "a_UV");
    objectModelViewProjectionParam = GLES20.glGetUniformLocation(objectProgram, "u_MVP");

    Util.checkGlError("Object program params");
    X=0;Y=0;Z=0;
    resetTargetPosition();
    moving=false;caught = false;
    //build all of the blocks' transform matrices
    int current_block = 0;
    //build the walls according to the maze matrix
    for (int i=0;i<MAZE_SIZE;i++) {
          for (int j=0;j<MAZE_SIZE;j++) {
              if (maze[i][j]==1) {
                  Matrix.setIdentityM(modelBlock[current_block], 0);
                  Matrix.translateM(modelBlock[current_block], 0,
                          (i-(MAZE_SIZE>>1))*3,
                          MAZE_Y,
                          (j-(MAZE_SIZE>>1))*3);
                  current_block++;
              }
          }
      }
    //build the floor
    for (int i=0;i<MAZE_SIZE;i++) {
      for (int j=0;j<MAZE_SIZE;j++) {
        Matrix.setIdentityM(modelBlock[current_block], 0);
        Matrix.translateM(modelBlock[current_block], 0,
                (i-(MAZE_SIZE>>1))*3,
                MAZE_Y - 3,
                (j-(MAZE_SIZE>>1))*3);
        current_block++;
      }
    }
    //build the ceiling
    for (int i=0;i<MAZE_SIZE;i++) {
      for (int j=0;j<MAZE_SIZE;j++) {
        Matrix.setIdentityM(modelBlock[current_block], 0);
        Matrix.translateM(modelBlock[current_block], 0,
                (i-(MAZE_SIZE>>1))*3,
                MAZE_Y + 3,
                (j-(MAZE_SIZE>>1))*3);
        current_block++;
      }
    }
    //place the target
    Matrix.setIdentityM(modelTarget, 0);
    Matrix.translateM(modelTarget, 0, target_x, 0, target_z);

    Util.checkGlError("onSurfaceCreated");
    //load all the objects ans textures
    try {
      for (int i = 0;i < block_cnt;i++) {
        block[i] = new TexturedMesh(this, "big_cube.obj", objectPositionParam, objectUvParam);
        if (i < wall_cnt) blockTex[i] = new Texture(this, "wall.png");
        else if (i < (wall_cnt + block_cnt)>>1) blockTex[i] = new Texture(this, "bottom.png");
        else blockTex[i] = new Texture(this, "top.png");
      }
      targetObjectMesh = new TexturedMesh(this, "TriSphere.obj", objectPositionParam, objectUvParam);
      targetObjectSelectedTexture = new Texture(this, "TriSphere_Pink_BakedDiffuse.png");
      targetObjectNotSelectedTexture = new Texture(this, "TriSphere_Blue_BakedDiffuse.png");
    } catch (IOException e) {
      Log.e(TAG, "Unable to initialize objects", e);
    }
    //start music playing
    mp1.start();
  }

  /**
   * Prepares OpenGL ES before we draw a frame.
   *
   * @param headTransform The head transformation in the new frame.
   */
  @Override
  public void onNewFrame(HeadTransform headTransform) {
    // Build the camera matrix and apply it to the ModelView.
    Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);
    randomMoveTarget();
    // Update the media players with HRTF files according to the most recent head rotation and distance.
    headTransform.getEulerAngles(headRPY, 0);
    //mapping:  X+ == Z-  Z+ == X-
    sound_direction = angleToDirection(getAngleDiff());
    //inverse-square law
    sound_volume = Math.min(1, 1/getDistanceSquare());
    //sound absorption of walls
    sound_volume = sound_volume * (float)Math.pow(ABSORB, getBlockPoints());
    //calculate the move in X and Z directions using yaw angle
    //also prevent the player from touching the wall
    if (moving) {
      int blockidx = (int)(MAZE_SIZE*1.5+X)/3;
      int blockidz = (int)(MAZE_SIZE*1.5+Z)/3;
      float xinblock = X - (blockidx-(MAZE_SIZE>>1))*3;
      float zinblock = Z - (blockidz-(MAZE_SIZE>>1))*3;
      switch (dir) {
          case 1:
              x_inc = -(float) (MOVE_SPEED * Math.sin(headRPY[1]));
              z_inc = -(float) (MOVE_SPEED * Math.cos(headRPY[1]));
              break;
          case 2:
              x_inc = -(float) (MOVE_SPEED * Math.cos(headRPY[1]));
              z_inc = (float) (MOVE_SPEED * Math.sin(headRPY[1]));
              break;
          case 3:
              x_inc = (float) (MOVE_SPEED * Math.sin(headRPY[1]));
              z_inc = (float) (MOVE_SPEED * Math.cos(headRPY[1]));
              break;
          case 4:
              x_inc = (float) (MOVE_SPEED * Math.cos(headRPY[1]));
              z_inc = -(float) (MOVE_SPEED * Math.sin(headRPY[1]));
              break;
          default:
      }
      //check the nearest 4 blocks respectively
      //if player is close to a solid block, set speed of that direction to zero
      if (xinblock > 1.5 - PLAYER_RADIUS && maze[blockidx+1][blockidz] == 1 && x_inc > 0) x_inc = 0;
      else if (xinblock < -1.5 + PLAYER_RADIUS && maze[blockidx-1][blockidz] == 1 && x_inc < 0) x_inc = 0;
      if (zinblock > 1.5 - PLAYER_RADIUS && maze[blockidx][blockidz+1] == 1 && z_inc > 0) z_inc = 0;
      else if (zinblock < -1.5 + PLAYER_RADIUS && maze[blockidx][blockidz-1] == 1 && z_inc < 0) z_inc = 0;
      X += x_inc;
      Z += z_inc;
    }
    Util.checkGlError("onNewFrame");
  }

  /**
   * Draws a frame for an eye.
   *
   * @param eye The eye to render. Includes all required transformations.
   */
  @Override
  public void onDrawEye(Eye eye) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    // The clear color doesn't matter here because it's completely obscured by
    // the room. However, the color buffer is still cleared because it may
    // improve performance.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    // Apply the eye transformation (translate ans rotate) to the camera.
    Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);
    Matrix.translateM(view, 0, -X, -Y, -Z);

    // Build the ModelView and ModelViewProjection matrices
    // and display all the objects
    float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
    for (int i = 0;i < block_cnt;i++) {
      Matrix.multiplyMM(modelView, 0, view, 0, modelBlock[i], 0);
      Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
      GLES20.glUseProgram(objectProgram);
      GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0);
      blockTex[i].bind();
      block[i].draw();
    }
    Matrix.multiplyMM(modelView, 0, view, 0, modelTarget, 0);
    Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
    GLES20.glUseProgram(objectProgram);
    GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0);
    if (canCatchTarget()) targetObjectSelectedTexture.bind();
    else targetObjectNotSelectedTexture.bind();
    targetObjectMesh.draw();
  }

  @Override
  public void onFinishFrame(Viewport viewport) {}

  /**
   * Called when screen touched
   */
  @Override
  public void onCardboardTrigger() {
    Log.i(TAG, "onCardboardTrigger");
    moving = true;
    if (canCatchTarget()) {
      caught = true;
      resetTargetPosition();
    }
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
              moving = false;
              break;
          default:
              Log.i(TAG, "DN");
              moving = true;
      }
      return true;
  }
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
      switch (keyCode){
          case KeyEvent.KEYCODE_W:
              moving = true;
              dir = 1;
              break;
          case KeyEvent.KEYCODE_A:
              moving = true;
              dir = 2;
              break;
          case KeyEvent.KEYCODE_S:
              moving = true;
              dir = 3;
              break;
          case KeyEvent.KEYCODE_D:
              moving = true;
              dir = 4;
              break;
          default:
      }
      return true;
  }
  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
      switch (keyCode){
          case KeyEvent.KEYCODE_W:
              moving = false;
              break;
          case KeyEvent.KEYCODE_A:
              moving = false;
              break;
          case KeyEvent.KEYCODE_S:
              moving = false;
              break;
          case KeyEvent.KEYCODE_D:
              moving = false;
              break;
          default:
      }
      return true;
  }

  private float getDistanceSquare() {
    return (target_x - X)*(target_x - X) + (target_z - Z)*(target_z - Z);
  }
  //target's head related direction
  private float getAngleDiff() {
      return (float)Math.atan2(X - target_x, Z - target_z) - headRPY[1];
  }

  //calculate how much the sound is blocked by obstacles
  //about 10% volume off per 0.2m
  private int getBlockPoints() {
      int blocked_points = 0;
      int total_points = (int)(5*Math.sqrt(getDistanceSquare()));
      for (int current_point = 1;current_point < total_points;current_point++) {
          float point_x = (current_point*X + (total_points - current_point)*target_x)/total_points;
          float point_z = (current_point*Z + (total_points - current_point)*target_z)/total_points;
          if (maze[(int)(MAZE_SIZE*1.5+point_x)/3][(int)(MAZE_SIZE*1.5+point_z)/3] == 1) blocked_points++;
      }
      return blocked_points;
  }
  //can catch only with close distance and right direction
  private boolean canCatchTarget() {
      return getDistanceSquare() < DISTANCE_LIMIT*DISTANCE_LIMIT && Math.abs(getAngleDiff()) < 0.5;
  }
  //random set target position and avoid obstacles
  private void resetTargetPosition() {
      do {
          target_x = (float)(2 * (MAZE_SIZE + 1) * Math.random() - MAZE_SIZE - 1);
          target_z = (float)(2 * (MAZE_SIZE + 1) * Math.random() - MAZE_SIZE - 1);
      } while (maze[(int)(MAZE_SIZE*1.5+target_x)/3][(int)(MAZE_SIZE*1.5+target_z)/3] == 1);
      Matrix.setIdentityM(modelTarget, 0);
      Matrix.translateM(modelTarget, 0, target_x, 0, target_z);
  }
  //target moves every frame
  private void randomMoveTarget() {
      int blockidx = (int)(MAZE_SIZE*1.5+target_x)/3;
      int blockidz = (int)(MAZE_SIZE*1.5+target_z)/3;
      float xinblock = target_x - (blockidx-(MAZE_SIZE>>1))*3;
      float zinblock = target_z - (blockidz-(MAZE_SIZE>>1))*3;
      //change direction randomly
      targetMoveAngle += 0.1*(Math.random() - 0.5);
      float x_inc = (float)(targetDirX*TARGET_SPEED*Math.sin(targetMoveAngle));
      float z_inc = (float)(targetDirZ*TARGET_SPEED*Math.cos(targetMoveAngle));
      //bounce off walls
      if (xinblock > 1.5 - TARGET_RADIUS && maze[blockidx+1][blockidz] == 1 && x_inc > 0) {x_inc = 0; targetDirX = - targetDirX;}
      else if (xinblock < -1.5 + TARGET_RADIUS && maze[blockidx-1][blockidz] == 1 && x_inc < 0) {x_inc = 0; targetDirX = - targetDirX;}
      if (zinblock > 1.5 - TARGET_RADIUS && maze[blockidx][blockidz+1] == 1 && z_inc > 0) {z_inc = 0; targetDirZ = - targetDirZ;}
      else if (zinblock < -1.5 + TARGET_RADIUS && maze[blockidx][blockidz-1] == 1 && z_inc < 0) {z_inc = 0; targetDirZ = - targetDirZ;}
      target_x += x_inc;
      target_z += z_inc;
      Matrix.setIdentityM(modelTarget, 0);
      Matrix.translateM(modelTarget, 0, target_x, 0, target_z);
  }
  //init media players in a new thread
  private void mp1_init() {
    new Thread(new Runnable() {@Override public void run() {
      mp1.reset();
      Uri uri;
      if (!caught) {
          uri = Uri.parse("android.resource://com.google.vr.sdk.samples.hellovr/" + sound_files[sound_direction]);
          mp1.setVolume(sound_volume, sound_volume);
      }
      else {
          uri = Uri.parse("android.resource://com.google.vr.sdk.samples.hellovr/" + R.raw.caught);
          caught = false;
          mp1.setVolume(1.0f, 1.0f);
      }
      try {
        mp1.setDataSource(HelloVrActivity.this, uri);
        mp1.prepare();
      } catch (IOException e) {
        e.printStackTrace();
      }
      mp2.setNextMediaPlayer(mp1);
    }}).start();
  }

  private void mp2_init() {
    new Thread(new Runnable() {@Override public void run() {
      mp2.reset();
      Uri uri;
      if (!caught) {
          uri = Uri.parse("android.resource://com.google.vr.sdk.samples.hellovr/" + sound_files[sound_direction]);
          mp2.setVolume(sound_volume, sound_volume);
      }
      else {
          uri = Uri.parse("android.resource://com.google.vr.sdk.samples.hellovr/" + R.raw.caught);
          caught = false;
          mp2.setVolume(1.0f, 1.0f);
      }
      try {
        mp2.setDataSource(HelloVrActivity.this, uri);
        mp2.prepare();
      } catch (IOException e) {
        e.printStackTrace();
      }
      mp1.setNextMediaPlayer(mp2);
    }}).start();
  }
  //choose HRTF files according to target's relative direction
  private int angleToDirection(float angle) {
      angle = angle * 180 / pi;
      if (angle > 180) angle -= 360;
      else if (angle <= -180) angle += 360;
      if (90 < angle && angle <= 180) {
          if (177.5 < angle) return 37;
          else if (172.5 < angle && angle <= 177.5) return 38;
          else if (167.5 < angle && angle <= 172.5) return 39;
          else if (162.5 < angle && angle <= 167.5) return 40;
          else if (157.5 < angle && angle <= 162.5) return 41;
          else if (152.5 < angle && angle <= 157.5) return 42;
          else if (147.5 < angle && angle <= 152.5) return 43;
          else if (142.5 < angle && angle <= 147.5) return 44;
          else if (137.5 < angle && angle <= 142.5) return 45;
          else if (130 < angle && angle <= 137.5) return 46;
          else if (120 < angle && angle <= 130) return 47;
          else if (107.5 < angle && angle <= 120) return 48;
          else return 49;
      }
      else if (0 < angle && angle <= 90) {
          if (72.5 < angle) return 0;
          else if (60 < angle && angle <= 72.5) return 1;
          else if (50 < angle && angle <= 60) return 2;
          else if (42.5 < angle && angle <= 50) return 3;
          else if (37.5 < angle && angle <= 42.5) return 4;
          else if (32.5 < angle && angle <= 37.5) return 5;
          else if (27.5 < angle && angle <= 32.5) return 6;
          else if (22.5 < angle && angle <= 27.5) return 7;
          else if (17.5 < angle && angle <= 22.5) return 8;
          else if (12.5 < angle && angle <= 17.5) return 9;
          else if (7.5 < angle && angle <= 12.5) return 10;
          else if (2.5 < angle && angle <= 7.5) return 11;
          else return 12;
      }
      else if (-90 < angle && angle <= 0) {
          if (-2.5 < angle) return 12;
          else if (-7.5 < angle && angle <= -2.5) return 13;
          else if (-12.5 < angle && angle <= -7.5) return 14;
          else if (-17.5 < angle && angle <= -12.5) return 15;
          else if (-22.5 < angle && angle <= -17.5) return 16;
          else if (-27.5 < angle && angle <= -22.5) return 17;
          else if (-32.5 < angle && angle <= -27.5) return 18;
          else if (-37.5 < angle && angle <= -32.5) return 19;
          else if (-42.5 < angle && angle <= -37.5) return 20;
          else if (-50 < angle && angle <= -42.5) return 21;
          else if (-60 < angle && angle <= -50) return 22;
          else if (-72.5 < angle && angle <= -60) return 23;
          else return 24;
      }
      else {
          if (-107.5 < angle) return 25;
          else if (-120 < angle && angle <= -107.5) return 26;
          else if (-130 < angle && angle <= -120) return 27;
          else if (-137.5 < angle && angle <= -130) return 28;
          else if (-142.5 < angle && angle <= -137.5) return 29;
          else if (-147.5 < angle && angle <= -142.5) return 30;
          else if (-152.5 < angle && angle <= -147.5) return 31;
          else if (-157.5 < angle && angle <= -152.5) return 32;
          else if (-162.5 < angle && angle <= -157.5) return 33;
          else if (-167.5 < angle && angle <= -162.5) return 34;
          else if (-172.5 < angle && angle <= -167.5) return 35;
          else if (-177.5 < angle && angle <= -172.5) return 36;
          else return 37;
      }
  }
}
