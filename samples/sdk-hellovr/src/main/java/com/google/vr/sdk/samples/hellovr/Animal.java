package com.google.vr.sdk.samples.hellovr;

public class Animal extends Entity {

    public Animal(double box_x_half, double box_y_half, double box_z_half, float[] center_pos) {
        super(box_x_half, box_y_half, box_z_half, center_pos);
    }

    @Override
    public void set_next_action() {
        speed[0]+=Math.random();        //乱走
        speed[2]+=Math.random();
        if(Math.abs(speed[1])<0.00001){                //乱跳
            speed[1]+=Math.max(Math.random()-0.8,0);
        }
    }
}
