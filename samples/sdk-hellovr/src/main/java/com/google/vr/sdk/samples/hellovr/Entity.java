package com.google.vr.sdk.samples.hellovr;

import android.util.Log;

abstract public class Entity {
    float BOX_HALF_WIDTH=0.5f;
    float ALLOWANCE=0.1f;       //碰撞余量
    float HEAD_LEN=0.3f;
    public float box_x_half;   //实体三个方向的包围盒半宽度
    public float box_y_half;
    public float box_z_half_down,box_z_half_up;

    public float[] center_pos;
    public float[] speed;
    protected Scene scene;   //场景三维数组
    private float g=0.0098f;

    public Entity(float box_x_half,float box_y_half,float box_z_half_down,float box_z_half_up, float[] center_pos, Scene scene){
        this.box_x_half=box_x_half;
        this.box_y_half=box_y_half;
        this.box_z_half_down=box_z_half_down;
        this.box_z_half_up=box_z_half_up;
        this.center_pos=center_pos;
        this.speed=new float[]{0,0,0};
        this.scene=scene;
    }

    //根据实体当前的位置和速度获取下一个坐标
    public void update_pos(){
        update_speed();
        center_pos[0]+=speed[0];
        center_pos[1]+=speed[1];
        center_pos[2]+=speed[2];
        speed[0]=0;
        speed[1]=0;
    }

    //更新物体速度（如碰撞情况等）：
    private void update_speed(){
        set_next_action();
        //处理x和y方向的碰撞：
        if(collide_x()==1 && speed[0]>0){
            speed[0]=0;
        }else if(collide_x()==-1 && speed[0]<0){
            speed[0]=0;
        }
        if(collide_y()==1 && speed[1]>0){
            speed[1]=0;
        }else if(collide_y()==-1 && speed[1]<0){
            speed[1]=0;
        }
        //处理z方向的碰撞，需要考虑重力：
        if(collide_z()==1 && speed[2]>0){
            speed[2]=0;
        }else if(collide_z()==-1 && speed[2]<=0){    //y负方向有东西
            speed[2]=0;
            return;
        }

        speed[2]-=g;    //受到重力加速度影响
    }

    //1：正方向碰撞；-1：负方向碰撞；0：不碰撞。下同
    public int collide_x(){
        for(int zz=(int)Math.ceil(center_pos[2]-box_z_half_down-BOX_HALF_WIDTH-0.001);zz<center_pos[2]+box_z_half_up+BOX_HALF_WIDTH;zz++) {
//            Log.i("collide", "zz:"+zz);
            if (zz < 0 || zz >= scene.get_scene_height()) return 0;
            for (int yy = (int) Math.ceil(center_pos[1] - box_y_half - BOX_HALF_WIDTH - 0.001); yy < center_pos[1] + box_y_half + BOX_HALF_WIDTH; yy++) {
                if (yy < 0 || yy >= scene.get_scene_width_we()) continue;
                int first = (int) Math.ceil(center_pos[0] - box_x_half - BOX_HALF_WIDTH - ALLOWANCE);
                for (int i = first; i <= center_pos[0]; i++) {
                    if (i < 0 || i >= scene.get_scene_width_ns()) return 0;
                    if (scene.scene[zz][scene.get_scene_width_ns() - i - 1][scene.get_scene_width_we() - yy - 1] != 0) {
//                        Log.i("collide", "collide:x-");
                        //center_pos[0]=i+box_x_half+BOX_HALF_WIDTH+ALLOWANCE;
                        return -1;
                    }
                }
                first = (int) Math.floor(center_pos[0] + box_x_half + BOX_HALF_WIDTH + ALLOWANCE);
                for (int i = first; i >= center_pos[0]; i--) {
                    if (i < 0 || i >= scene.get_scene_width_ns()) return 0;
                    if (scene.scene[zz][scene.get_scene_width_ns() - i - 1][scene.get_scene_width_we() - yy - 1] != 0) {
//                        Log.i("collide", "collide:x+");
                        //center_pos[0]=i-box_x_half-BOX_HALF_WIDTH-ALLOWANCE;
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    public int collide_y(){
        for(int zz=(int)Math.ceil(center_pos[2]-box_z_half_down-BOX_HALF_WIDTH-0.001);zz<center_pos[2]+box_z_half_up+BOX_HALF_WIDTH;zz++) {
            if(zz<0||zz>=scene.get_scene_height())return 0;
            for (int xx = (int) Math.ceil(center_pos[0] - box_x_half - BOX_HALF_WIDTH - 0.001); xx < center_pos[0] + box_x_half + BOX_HALF_WIDTH; xx++) {
                if (xx < 0 || xx >= scene.get_scene_width_ns()) continue;
                int first = (int) Math.ceil(center_pos[1] - box_y_half - BOX_HALF_WIDTH - ALLOWANCE);
                for (int i = first; i <= center_pos[1]; i++) {
                    if (i < 0 || i >= scene.get_scene_width_we()) return 0;
                    if (scene.scene[zz][scene.get_scene_width_ns() - xx - 1][scene.get_scene_width_we() - i - 1] != 0) {
//                        Log.i("collide", "collide:y-");
                        //center_pos[1]=i+box_y_half+BOX_HALF_WIDTH+ALLOWANCE;
                        return -1;
                    }
                }
                first = (int) Math.floor(center_pos[1] + box_y_half + BOX_HALF_WIDTH + ALLOWANCE);
                for (int i = first; i >= center_pos[1]; i--) {
                    if (i < 0 || i >= scene.get_scene_width_we()) return 0;
                    if (scene.scene[zz][scene.get_scene_width_ns() - xx - 1][scene.get_scene_width_we() - i - 1] != 0) {
//                        Log.i("collide", "collide:y+");
                        //center_pos[1]=i-box_y_half-BOX_HALF_WIDTH-ALLOWANCE;
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    public int collide_z(){
        for(int yy=(int)Math.ceil(center_pos[1]-box_y_half-BOX_HALF_WIDTH-0.001);yy<center_pos[1]+box_y_half+BOX_HALF_WIDTH;yy++) {
            if(yy<0||yy>=scene.get_scene_width_we())continue;
            for (int xx = (int) Math.ceil(center_pos[0] - box_x_half - BOX_HALF_WIDTH - 0.001); xx < center_pos[0] + box_x_half + BOX_HALF_WIDTH; xx++) {
                if (xx < 0 || xx >= scene.get_scene_width_ns()) continue;

                int first = (int)Math.ceil(center_pos[2]-box_z_half_down-BOX_HALF_WIDTH-0.2);
                for (int i = first; i <= center_pos[2]; i++) {
                    if (i < 0 || i >= scene.get_scene_height()) return 0;
                    if (scene.scene[i][scene.get_scene_width_ns() - xx - 1][scene.get_scene_width_we() - yy - 1] != 0) {
                        //center_pos[2]=i+1;
                        return -1;
                    }
                }
                first = (int)Math.floor(center_pos[2]+box_z_half_up+BOX_HALF_WIDTH+ALLOWANCE);
                for (int i = first; i >= center_pos[2]; i--) {
                    if (i < 0 || i >= scene.get_scene_height()) return 0;
                    if (scene.scene[i][scene.get_scene_width_ns() - xx - 1][scene.get_scene_width_we() - yy - 1] != 0) {
                        //center_pos[2]=i+1;
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    //设置该实体的下一个动作：
    abstract public void set_next_action();
}
