package com.google.vr.sdk.samples.hellovr;

abstract public class Entity {
    public double box_x_half;   //实体三个方向的包围盒半宽度
    public double box_y_half;
    public double box_z_half;

    public float[] center_pos;
    public float[] speed;
    private char[][][] scene;   //场景三维数组
    private double g=0.0098;

    public Entity(double box_x_half,double box_y_half,double box_z_half, float[] center_pos){
        this.box_x_half=box_x_half;
        this.box_y_half=box_y_half;
        this.box_z_half=box_z_half;
        this.center_pos=center_pos;
        this.speed=new float[]{0,0,0};
    }

    //根据实体当前的位置和速度获取下一个坐标
    public void update_pos(){
        update_speed();
        center_pos[0]+=speed[0];
        center_pos[1]+=speed[1];
        center_pos[2]+=speed[2];
        speed[0]=0;
        speed[2]=0;
    }

    //更新物体速度（如碰撞情况等）：
    private void update_speed(){
        set_next_action();
        //处理x和z方向的碰撞：
        if(collide_x()==1 && speed[0]>0){
            speed[0]=0;
        }else if(collide_x()==-1 && speed[0]<0){
            speed[0]=0;
        }
        if(collide_z()==1 && speed[2]>0){
            speed[2]=0;
        }else if(collide_z()==-1 && speed[2]<0){
            speed[2]=0;
        }
        //处理y方向的碰撞，需要考虑重力：
        if(collide_y()==1 && speed[1]>0){
            speed[1]=0;
        }else if(collide_y()==-1 && speed[1]<=0){    //y负方向有东西
            speed[1]=0;
            return;
        }

        speed[1]-=g;    //受到重力加速度影响
    }

    //1：正方向碰撞；-1：负方向碰撞；0：不碰撞。下同
    public int collide_x(){
        return 0;
    }

    public int collide_y(){
        if(center_pos[1]>0)return 0;
        else return -1;
    }

    public int collide_z(){
        return 0;
    }

    //设置该实体的下一个动作：
    abstract public void set_next_action();
}
