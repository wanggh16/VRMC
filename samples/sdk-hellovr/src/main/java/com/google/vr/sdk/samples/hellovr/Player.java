package com.google.vr.sdk.samples.hellovr;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import cc.lym.Renderer.BlockRenderer;
import cc.lym.Renderer.HandRenderer;
import cc.lym.Renderer.HeadTransformProvider;

public class Player extends Entity{

    private float[] headRPY,headRight,headUp;
    private HeadTransformProvider head;
    private BlockRenderer blockRenderer;
    private final HandRenderer handRenderer;
    private boolean[] move; //记录移动方向的数组，长为4，每一位分别代表向前后左右移动
    public float MOVE_SPEED=0.05f;
    private float HAND_REACH=4;
    public enum Direction{
        FORWARD,BACKWARD,LEFTWARD,RIGHTWARD
    }

    public Player(float box_x_half, float box_y_half, float box_z_half_down, float box_z_half_up, float[] center_pos, HeadTransformProvider head, BlockRenderer blockRenderer, HandRenderer handRenderer, Scene scene) {
        super(box_x_half, box_y_half, box_z_half_down, box_z_half_up, center_pos, scene);
        this.move=new boolean[]{false,false,false,false};
        this.head=head;
        this.blockRenderer=blockRenderer;
        this.handRenderer=handRenderer;
        this.headRPY=new float[3];
        this.headRight=new float[3];
        this.headUp=new float[3];
    }

    @Override
    public void set_next_action() {
        head.getForwardVector(headRPY,0);
        head.getRightVector(headRight,0);
        head.getUpVector(headUp,0);
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

    public void set_block(int i, int j, int k, char blockid){
        //Scene.Point block_center_pos_array=scene.transform_render_to_array(block_center_pos_render.x,block_center_pos_render.y,block_center_pos_render.z);
        //int i=Math.round(block_center_pos_array.x);
        //int j=Math.round(block_center_pos_array.y);
        //int k=Math.round(block_center_pos_array.z);
        int up =scene.get_neighbor_block_id(i,j,k, Scene.Position.UP);
        int down =scene.get_neighbor_block_id(i,j,k, Scene.Position.DOWN);
        int east =scene.get_neighbor_block_id(i,j,k, Scene.Position.EAST);
        int west =scene.get_neighbor_block_id(i,j,k, Scene.Position.WEST);
        int north =scene.get_neighbor_block_id(i,j,k, Scene.Position.NORTH);
        int south =scene.get_neighbor_block_id(i,j,k, Scene.Position.SOUTH);
        Scene.Point block_center_pos_render=scene.transform_array_to_render(i, j, k);
        blockRenderer.updateBlock(Math.round(block_center_pos_render.x),Math.round(block_center_pos_render.y),Math.round(block_center_pos_render.z),blockid, new int[]{up,south,east,north,west,down}, new int[][][]{{{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}, {{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}, {{15, 15, 15}, {15, 15, 15}, {15, 15, 15}}});
        scene.scene[HelloVrActivity.wrapDU(i)][HelloVrActivity.wrapWE(j)][HelloVrActivity.wrapSN(k)]=blockid;
        Log.i("xyz", "x: "+block_center_pos_render.x+"y: "+block_center_pos_render.y+"z: "+block_center_pos_render.z);
    }

    public CrossPoint get_facing_block(){
        float[]alphaBeta=handRenderer.getAlphaBeta();
        float[]pointingDirection=new float[3];
        for(int i=0;i<3;i++)
            pointingDirection[i]=headRPY[i]+headRight[i]*alphaBeta[0]+headUp[i]*alphaBeta[1];
        Scene.Point forward_v = scene.transform_sdk_to_render(pointingDirection[0],pointingDirection[1],pointingDirection[2]);
        Log.i("forward_v", "x: "+forward_v.x+"y: "+forward_v.y+"z: "+forward_v.z);
        if (forward_v.x == 0) forward_v.x = 1e-6f;
        if (forward_v.y == 0) forward_v.y = 1e-6f;
        if (forward_v.z == 0) forward_v.z = 1e-6f;
        List<CrossPoint> P = new ArrayList<>();
        Scene.Point block_curr = scene.transform_render_to_array(center_pos[0],center_pos[1],center_pos[2]);
        int i=(int)Math.round(block_curr.x);
        int j=(int)Math.round(block_curr.y);
        int k=(int)Math.round(block_curr.z);
        if (scene.scene[HelloVrActivity.wrapDU(i)][HelloVrActivity.wrapWE(j)][HelloVrActivity.wrapSN(k)] != 0) return new CrossPoint(0, 6, i, j, k);
        Log.i("cross", "i"+i+"j"+j+"k"+k);
        if (forward_v.x > 0){
            double dist = (Math.round(center_pos[0]) + 0.5f - center_pos[0])/forward_v.x;
            while (dist < HAND_REACH){
                block_curr = scene.transform_render_to_array(
                        center_pos[0] + (dist + 1e-8) * forward_v.x,
                        center_pos[1] + (dist + 1e-8) * forward_v.y,
                        center_pos[2] + (dist + 1e-8) * forward_v.z);
                Log.i("cross", "xxx"+block_curr.x+"yyy"+block_curr.y+"zzz"+block_curr.z);
                P.add(new CrossPoint(dist, 0, (int)Math.round(block_curr.x), (int)Math.round(block_curr.y), (int)Math.round(block_curr.z)));
                dist += 1/forward_v.x;
            }
        }
        else{
            double dist = (center_pos[0] + 0.5f - Math.round(center_pos[0]))/(-forward_v.x);
            while (dist < HAND_REACH){
                block_curr = scene.transform_render_to_array(
                        center_pos[0] + (dist + 1e-8) * forward_v.x,
                        center_pos[1] + (dist + 1e-8) * forward_v.y,
                        center_pos[2] + (dist + 1e-8) * forward_v.z);
                P.add(new CrossPoint(dist, 1, (int)Math.round(block_curr.x), (int)Math.round(block_curr.y), (int)Math.round(block_curr.z)));
                dist += 1/(-forward_v.x);
            }
        }
        if (forward_v.y > 0){
            double dist = (Math.round(center_pos[1]) + 0.5f - center_pos[1])/forward_v.y;
            while (dist < HAND_REACH){
                block_curr = scene.transform_render_to_array(
                        center_pos[0] + (dist + 1e-8) * forward_v.x,
                        center_pos[1] + (dist + 1e-8) * forward_v.y,
                        center_pos[2] + (dist + 1e-8) * forward_v.z);
                P.add(new CrossPoint(dist, 2, (int)Math.round(block_curr.x), (int)Math.round(block_curr.y), (int)Math.round(block_curr.z)));
                dist += 1/forward_v.y;
            }
        }
        else{
            double dist = (center_pos[1] + 0.5f - Math.round(center_pos[1]))/(-forward_v.y);
            while (dist < HAND_REACH){
                block_curr = scene.transform_render_to_array(
                        center_pos[0] + (dist + 1e-8) * forward_v.x,
                        center_pos[1] + (dist + 1e-8) * forward_v.y,
                        center_pos[2] + (dist + 1e-8) * forward_v.z);
                P.add(new CrossPoint(dist, 3, (int)Math.round(block_curr.x), (int)Math.round(block_curr.y), (int)Math.round(block_curr.z)));
                dist += 1/(-forward_v.y);
            }
        }
        if (forward_v.z > 0){
            double dist = (Math.round(center_pos[2]) + 0.5f - center_pos[2])/forward_v.z;
            while (dist < HAND_REACH){
                block_curr = scene.transform_render_to_array(
                        center_pos[0] + (dist + 1e-8) * forward_v.x,
                        center_pos[1] + (dist + 1e-8) * forward_v.y,
                        center_pos[2] + (dist + 1e-8) * forward_v.z);
                P.add(new CrossPoint(dist, 4, (int)Math.round(block_curr.x), (int)Math.round(block_curr.y), (int)Math.round(block_curr.z)));
                dist += 1/forward_v.z;
            }
        }
        else{
            double dist = (center_pos[2] + 0.5f - Math.round(center_pos[2]))/(-forward_v.z);
            while (dist < HAND_REACH){
                block_curr = scene.transform_render_to_array(
                        center_pos[0] + (dist + 1e-8) * forward_v.x,
                        center_pos[1] + (dist + 1e-8) * forward_v.y,
                        center_pos[2] + (dist + 1e-8) * forward_v.z);
                P.add(new CrossPoint(dist, 5, (int)Math.round(block_curr.x), (int)Math.round(block_curr.y), (int)Math.round(block_curr.z)));
                dist += 1/(-forward_v.z);
            }
        }
        Collections.sort(P);
        CrossPoint cross = null;
        boolean havecross = false;
        for(int ii = 0;ii < P.size(); ii ++){
            cross = P.get(ii);
            Log.i("cross", "cross:type"+cross.type+"i"+cross.nextblocki+"j"+cross.nextblockj+"k"+cross.nextblockk);
            if (cross.nextblocki >= 0 && cross.nextblocki < scene.get_scene_height()
                    && cross.nextblockj >= 0 && cross.nextblockj < scene.get_scene_width_ns()
                    && cross.nextblockk >= 0 && cross.nextblockk < scene.get_scene_width_we()) {
                Log.i("cross", "valid");
                if (scene.scene[HelloVrActivity.wrapDU(cross.nextblocki)][HelloVrActivity.wrapWE(cross.nextblockj)][HelloVrActivity.wrapSN(cross.nextblockk)] != 0) {
                    havecross = true;
                    Log.i("cross", "dist:" + cross.dist + "i:" + cross.nextblocki + "j:" + cross.nextblockj + "k:" + cross.nextblockk + "type:" + cross.type);
                    break;
                }
            }
        }
        if (havecross) return cross;
        else return null;
    }

    public boolean canPlaceBlock(int i, int j, int k){
        Scene.Point block_center_pos_render=scene.transform_array_to_render(i, j, k);
        if (Math.abs(block_center_pos_render.x - center_pos[0]) < box_x_half + 0.5
                && Math.abs(block_center_pos_render.y - center_pos[1]) < box_y_half + 0.5
                && center_pos[2] - block_center_pos_render.z < box_z_half_down + 0.5
                && block_center_pos_render.z - center_pos[2] < box_z_half_up + 0.5)
            return false;
        return true;
    }
}
