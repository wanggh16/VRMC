package com.google.vr.sdk.samples.hellovr;

import android.util.Log;

import cc.lym.Renderer.HeadTransformProvider;
import cc.lym.util.Supplier;

public class Player extends Entity{

    private float[] headRPY;
    private HeadTransformProvider head;
    private boolean[] move; //记录移动方向的数组，长为4，每一位分别代表向前后左右移动
    private float MOVE_SPEED=0.02f;
    public enum Direction{
        FORWARD,BACKWARD,LEFTWARD,RIGHTWARD
    }

    public Player(double box_x_half, double box_y_half, double box_z_half, float[] center_pos, HeadTransformProvider head, char[][][] scene) {
        super(box_x_half, box_y_half, box_z_half, center_pos, scene);
        this.move=new boolean[]{false,false,false,false};
        this.head=head;
        this.headRPY=new float[3];
    }

    @Override
    public void set_next_action() {
        head.getForwardVector(headRPY,0);
        Log.i("hhh", "xxxx: "+headRPY[0]+", yyyy: "+headRPY[1]+", zzzz: "+headRPY[2]);
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
            case BACKWARD:
                move[1]=true;
                break;
            case LEFTWARD:
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
            case BACKWARD:
                move[1]=false;
                break;
            case LEFTWARD:
                move[2]=false;
                break;
            case RIGHTWARD:
                move[3]=false;
                break;
        }
    }

    public void jump(){
        if(collide_z()==-1)
            speed[2]+=0.2;
    }
}
