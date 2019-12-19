package com.google.vr.sdk.samples.hellovr;

public class Animal extends Entity {

    public Animal(float box_x_half, float box_y_half, float box_z_half_down, float box_z_half_up ,float[] center_pos, char[][][] scene) {
        super(box_x_half, box_y_half, box_z_half_down, box_z_half_up ,center_pos, scene);
    }

    @Override
    public void set_next_action() {
        speed[0]+=Math.random();        //乱走
        speed[1]+=Math.random();
        if(Math.abs(speed[2])<0.00001){                //乱跳
            speed[2]+=Math.max(Math.random()-0.8,0);
        }
    }
}
