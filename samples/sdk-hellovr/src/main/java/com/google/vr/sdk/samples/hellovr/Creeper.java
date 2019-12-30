package com.google.vr.sdk.samples.hellovr;

public class Creeper extends Entity {

    public Creeper(float box_x_half, float box_y_half, float box_z_half_down, float box_z_half_up , float[] center_pos, Scene scene) {
        super(box_x_half, box_y_half, box_z_half_down, box_z_half_up ,center_pos, scene);
    }

    int speed_cnt=0;
    @Override
    public void set_next_action() {
        speed_cnt+=1;
        if(speed_cnt==1000)
        speed[0]=(float)Math.random()-0.5f;        //乱走
        speed[1]=(float)Math.random()-0.5f;
//        if(Math.abs(speed[2])<0.00001){                //乱跳
//            speed[2]+=Math.max(Math.random()-0.8,0);
//        }
    }
}
