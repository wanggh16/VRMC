package com.google.vr.sdk.samples.hellovr;

public class Player extends Entity{

    private float[] headRPY;
    private boolean[] move; //记录移动方向的数组，长为4，每一位分别代表向前后左右移动
    private float MOVE_SPEED=0.005f;
    public enum Direction{
        FORWARD,BACKWARD,LEFTWARD,RIGHTWARD
    }

    public Player(double box_x_half, double box_y_half, double box_z_half, float[] center_pos, float[] headRPY) {
        super(box_x_half, box_y_half, box_z_half, center_pos);
        this.move=new boolean[]{false,false,false,false};
        this.headRPY = headRPY;
    }

    @Override
    public void set_next_action() {
        if(move[0]){
            speed[0] = -(float) (MOVE_SPEED * Math.sin(headRPY[1]));
            speed[2] = -(float) (MOVE_SPEED * Math.cos(headRPY[1]));
        }
        if(move[1]){
            speed[0] += -(float) (MOVE_SPEED * Math.cos(headRPY[1]));
            speed[2] += (float) (MOVE_SPEED * Math.sin(headRPY[1]));
        }
        if(move[2]){
            speed[0] += (float) (MOVE_SPEED * Math.sin(headRPY[1]));
            speed[2] += (float) (MOVE_SPEED * Math.cos(headRPY[1]));
        }
        if(move[3]){
            speed[0] += (float) (MOVE_SPEED * Math.cos(headRPY[1]));
            speed[2] += -(float) (MOVE_SPEED * Math.sin(headRPY[1]));
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
        if(collide_y()==-1)
            speed[1]+=0.2;
    }
}
