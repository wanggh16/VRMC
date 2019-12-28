package com.google.vr.sdk.samples.hellovr;

public class CrossPoint implements Comparable{
    public double dist;
    public int type;
    //jnxp, jpxn, knyp, kpyn, ipzp, inzn, stuck
    public int nextblocki;
    public int nextblockj;
    public int nextblockk;
    public CrossPoint(double dist, int type, int nextblocki, int nextblockj, int nextblockk){
        this.dist=dist;this.type=type;this.nextblocki=nextblocki;this.nextblockj=nextblockj;this.nextblockk=nextblockk;
    }
    @Override
    public int compareTo(Object o) {
        CrossPoint s = (CrossPoint) o;
        if (this.dist > s.dist) {
            return 1;
        }
        else {
            return -1;
        }
    }
}
