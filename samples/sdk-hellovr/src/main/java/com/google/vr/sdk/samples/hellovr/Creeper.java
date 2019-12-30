package com.google.vr.sdk.samples.hellovr;

import android.util.Log;

import cc.lym.util.Location;

public class Creeper extends Entity {

    CreeperAgent creeperAgent;

    public Creeper(float box_x_half, float box_y_half, float box_z_half_down, float box_z_half_up , float[] center_pos, Scene scene) {
        super(box_x_half, box_y_half, box_z_half_down, box_z_half_up ,center_pos, scene);
        creeperAgent=new CreeperAgent();
        creeperAgent.setIllumination(1);
        creeperAgent.show();
        creeperAgent.setLocAndSpeed(center_pos[0],center_pos[1],center_pos[2]-1.5f,0,0,0,0);
    }

    int speed_cnt=0;
    @Override
    public void set_next_action() {
        Location location=creeperAgent.getLocation();
        Log.i("creeper_prev", "x: "+center_pos[0]+", y: "+center_pos[1]+", z: "+center_pos[2]);
        center_pos[0]=(float)location.x;
        center_pos[1]=(float)location.y;
        center_pos[2]=(float)location.z+1f;
        speed_cnt+=1;
        if(collide_wall){
            creeperAgent.setLocAndSpeed(center_pos[0],center_pos[1],center_pos[2]-1f,speed[0],speed[1],speed[2],(float) Math.random() - 0.5f);
            Log.i("creeper_suc", "x: "+center_pos[0]+", y: "+center_pos[1]+", z: "+center_pos[2]);
            Log.i("creeper", "speedx: "+speed[0]+", speedy: "+speed[1]);
        }
        else if(speed_cnt%300 == 0){
            speed[0]=Math.min(1.5f*((float)Math.random()-0.5f),0.4f);        //乱走
            speed[1]=Math.min(1.5f*((float)Math.random()-0.5f),0.4f);        //乱走
            creeperAgent.setLocAndSpeed(center_pos[0],center_pos[1],center_pos[2]-1f,speed[0],speed[1],speed[2],(float) Math.random() - 0.5f);
            //speed_cnt=0;
        }
//        if(Math.abs(speed[2])<0.00001){                //乱跳
//            speed[2]+=Math.max(Math.random()-0.8,0);
//        }
    }
}
