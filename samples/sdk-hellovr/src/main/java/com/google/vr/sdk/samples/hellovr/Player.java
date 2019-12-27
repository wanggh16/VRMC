package com.google.vr.sdk.samples.hellovr;

import android.util.Log;

import cc.lym.Renderer.BlockRenderer;
import cc.lym.Renderer.HeadTransformProvider;

public class Player extends Entity{

    private float[] headRPY;
    private HeadTransformProvider head;
    private BlockRenderer blockRenderer;
    private boolean[] move; //记录移动方向的数组，长为4，每一位分别代表向前后左右移动
    private float MOVE_SPEED=0.04f;
    public enum Direction{
        FORWARD,BACKWARD,LEFTWARD,RIGHTWARD
    }

    public Player(float box_x_half, float box_y_half, float box_z_half_down, float box_z_half_up, float[] center_pos, HeadTransformProvider head, BlockRenderer blockRenderer,Scene scene) {
        super(box_x_half, box_y_half, box_z_half_down, box_z_half_up, center_pos, scene);
        this.move=new boolean[]{false,false,false,false};
        this.head=head;
        this.blockRenderer=blockRenderer;
        this.headRPY=new float[3];
    }

    @Override
    public void set_next_action() {
        head.getForwardVector(headRPY,0);
        Log.i("hhh", "xxxx: "+headRPY[0]+", yyyy: "+headRPY[1]+", zzzz: "+headRPY[2]);
        Log.i("hhh", "posx: "+center_pos[0]+", posy: "+center_pos[1]+", posz: "+center_pos[2]);
        if(move[0]){
            speed[0] = -MOVE_SPEED * headRPY[2];
            speed[1] = -MOVE_SPEED * headRPY[0];
        }
        if(move[1]){
            speed[0] += MOVE_SPEED * headRPY[0];
            speed[1] += -MOVE_SPEED * headRPY[2];
        }
        if(move[2]){
            speed[0] += MOVE_SPEED * headRPY[2];
            speed[1] += MOVE_SPEED * headRPY[0];
        }
        if(move[3]){
            speed[0] += -MOVE_SPEED * headRPY[0];
            speed[1] += MOVE_SPEED * headRPY[2];
        }
    }

    public void set_move_toward(Direction direction){
        switch (direction){
            case FORWARD:
                move[0]=true;
                break;
            case LEFTWARD:
                move[1]=true;
                break;
            case BACKWARD:
                move[2]=true;
                break;
            case RIGHTWARD:
                move[3]=true;
                break;
        }
    }

    public void stop_move_toward(Direction direction){
        switch (direction){
            case FORWARD:
                move[0]=false;
                break;
            case LEFTWARD:
                move[1]=false;
                break;
            case BACKWARD:
                move[2]=false;
                break;
            case RIGHTWARD:
                move[3]=false;
                break;
        }
    }

    public void jump(){
        if(collide_z()==-1)
            speed[2]+=0.15;
    }

    public void destroy_block(){
        Scene.Point block_center_pos_render=get_facing_block();
        Scene.Point block_center_pos_array=scene.transform_render_to_array(block_center_pos_render.x,block_center_pos_render.y,block_center_pos_render.z);
        int i=Math.round(block_center_pos_array.x);
        int j=Math.round(block_center_pos_array.y);
        int k=Math.round(block_center_pos_array.z);
        int up =scene.get_neighbor_block_id(i,j,k, Scene.Position.UP);
        int down =scene.get_neighbor_block_id(i,j,k, Scene.Position.DOWN);
        int east =scene.get_neighbor_block_id(i,j,k, Scene.Position.EAST);
        int west =scene.get_neighbor_block_id(i,j,k, Scene.Position.WEST);
        int north =scene.get_neighbor_block_id(i,j,k, Scene.Position.NORTH);
        int south =scene.get_neighbor_block_id(i,j,k, Scene.Position.SOUTH);
        blockRenderer.updateBlock((int)Math.round(block_center_pos_render.x),(int)Math.round(block_center_pos_render.y),(int)Math.round(block_center_pos_render.z),0, new int[]{up,south,east,north,west,down}, new int[][][]{{{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}, {{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}, {{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}});
        scene.scene[i][j][k]=0;
        Log.i("xyz", "x: "+block_center_pos_render.x+"y: "+block_center_pos_render.y+"z: "+block_center_pos_render.z);
    }

    private Scene.Point get_facing_block(){
        Scene.Point forward_v = scene.transform_sdk_to_render(headRPY[0],headRPY[1],headRPY[2]);
        Log.i("forward_v", "x: "+forward_v.x+"y: "+forward_v.y+"z: "+forward_v.z);
        return scene.new Point(1,2,3);
    }
}
