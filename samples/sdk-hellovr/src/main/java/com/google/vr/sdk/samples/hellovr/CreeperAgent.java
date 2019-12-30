package com.google.vr.sdk.samples.hellovr;

import java.util.concurrent.atomic.AtomicInteger;

import cc.lym.Renderer.EntityRenderer;
import cc.lym.util.Location;
import cc.lym.util.Supplier;

final class CreeperAgent
{
	private static EntityRenderer renderer;
	private static EntityRenderer.Model model;
	private static int maxInstanceCount;
	static
	{
		EntityRenderer.ExprBuilder builder=new EntityRenderer.ExprBuilder();
		EntityRenderer.ExprBuilder.Expr tabs=builder.time();
		EntityRenderer.ExprBuilder.Expr t0=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr x0=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr y0=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr z0=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr theta=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr speed=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr headTheta=builder.nextParam();
		EntityRenderer.Model.Rectangle[]rectangles=new EntityRenderer.Model.Rectangle[12];
		EntityRenderer.ExprBuilder.Expr t=builder.sub(tabs,t0);
		EntityRenderer.ExprBuilder.Expr x=builder.add(x0,builder.mul(builder.mul(t,speed),builder.cos(theta)));
		EntityRenderer.ExprBuilder.Expr y=builder.add(y0,builder.mul(builder.mul(t,speed),builder.sin(theta)));
		
		EntityRenderer.Model.Point bodyLLL=new EntityRenderer.Model.Point(builder.constant(-0.4f),builder.constant(-0.4f),builder.constant(0.6f));
		EntityRenderer.Model.Point bodyLLH=new EntityRenderer.Model.Point(builder.constant(-0.4f),builder.constant(-0.4f),builder.constant(1.2f));
		EntityRenderer.Model.Point bodyLHL=new EntityRenderer.Model.Point(builder.constant(-0.4f),builder.constant( 0.4f),builder.constant(0.6f));
		EntityRenderer.Model.Point bodyLHH=new EntityRenderer.Model.Point(builder.constant(-0.4f),builder.constant( 0.4f),builder.constant(1.2f));
		EntityRenderer.Model.Point bodyHLL=new EntityRenderer.Model.Point(builder.constant( 0.4f),builder.constant(-0.4f),builder.constant(0.6f));
		EntityRenderer.Model.Point bodyHLH=new EntityRenderer.Model.Point(builder.constant( 0.4f),builder.constant(-0.4f),builder.constant(1.2f));
		EntityRenderer.Model.Point bodyHHL=new EntityRenderer.Model.Point(builder.constant( 0.4f),builder.constant( 0.4f),builder.constant(0.6f));
		EntityRenderer.Model.Point bodyHHH=new EntityRenderer.Model.Point(builder.constant( 0.4f),builder.constant( 0.4f),builder.constant(1.2f));
		rectangles[0]=new EntityRenderer.Model.Rectangle(bodyLLH,bodyHLH,bodyHHH,bodyLHH, 0,16,0,16);
		rectangles[1]=new EntityRenderer.Model.Rectangle(bodyLLL,bodyHLL,bodyHLH,bodyLLH,16,16,0,16);
		rectangles[2]=new EntityRenderer.Model.Rectangle(bodyHLL,bodyHHL,bodyHHH,bodyHLH,32,16,0,16);
		rectangles[3]=new EntityRenderer.Model.Rectangle(bodyHHL,bodyLHL,bodyLHH,bodyHHH,48,16,0,16);
		rectangles[4]=new EntityRenderer.Model.Rectangle(bodyLHL,bodyLLL,bodyLLH,bodyLHH,64,16,0,16);
		rectangles[5]=new EntityRenderer.Model.Rectangle(bodyHLL,bodyLLL,bodyLHL,bodyHHL,80,16,0,16);
		
		EntityRenderer.ExprBuilder.Expr cosHeadTheta=builder.cos(headTheta),sinHeadTheta=builder.sin(headTheta);
		EntityRenderer.ExprBuilder.Expr n04=builder.constant(-0.2f),p04=builder.constant(0.2f);
		EntityRenderer.Model.Point headLLL=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.2f));
		EntityRenderer.Model.Point headLLH=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.6f));
		EntityRenderer.Model.Point headLHL=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.2f));
		EntityRenderer.Model.Point headLHH=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.6f));
		EntityRenderer.Model.Point headHLL=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.2f));
		EntityRenderer.Model.Point headHLH=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.6f));
		EntityRenderer.Model.Point headHHL=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.2f));
		EntityRenderer.Model.Point headHHH=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.6f));
		rectangles[ 6]=new EntityRenderer.Model.Rectangle(headLLH,headHLH,headHHH,headLHH, 0,16,16,16);
		rectangles[ 7]=new EntityRenderer.Model.Rectangle(headLLL,headHLL,headHLH,headLLH,16,16,16,16);
		rectangles[ 8]=new EntityRenderer.Model.Rectangle(headHLL,headHHL,headHHH,headHLH,32,16,16,16);
		rectangles[ 9]=new EntityRenderer.Model.Rectangle(headHHL,headLHL,headLHH,headHHH,48,16,16,16);
		rectangles[10]=new EntityRenderer.Model.Rectangle(headLHL,headLLL,headLLH,headLHH,64,16,16,16);
		rectangles[11]=new EntityRenderer.Model.Rectangle(headHLL,headLLL,headLHL,headHHL,80,16,16,16);
		
		model=new EntityRenderer.Model(builder,rectangles,x,y,z0,theta);
	}
	public static EntityRenderer init(Supplier<Location> location,int maxInstanceCount,byte[]texture)
	{
		if(renderer!=null)throw new IllegalStateException("re-initialize of CreeperAgent");
		CreeperAgent.maxInstanceCount=maxInstanceCount;
		return renderer=new EntityRenderer(HelloVrActivity.WEST_LIMIT,HelloVrActivity.EAST_LIMIT,HelloVrActivity.SOUTH_LIMIT,HelloVrActivity.NORTH_LIMIT,HelloVrActivity.BOTTOM_LIMIT,HelloVrActivity.TOP_LIMIT,location,model,maxInstanceCount,texture);
	}
	private static AtomicInteger instanceCount=new AtomicInteger();
	private final int instanceID;
	private float x,y,z,speed,theta;
	private long startTime;
	public CreeperAgent()
	{
		if(renderer==null)throw new IllegalStateException("CreeperAgent not initialized");
		instanceID=instanceCount.getAndAdd(1);
		if(instanceID>=maxInstanceCount)
			throw new IllegalStateException("too many creepers");
	}
	public Location getLocation()
	{
		float timeElapsed=System.nanoTime()-startTime;
		float x=this.x+speed*(float)Math.cos(theta)*timeElapsed;
		float y=this.x+speed*(float)Math.sin(theta)*timeElapsed;
		return new Location(x,y,z);
	}
	public void show(){renderer.enableEntity(instanceID);}
	public void hide(){renderer.disableEntity(instanceID);}
	public void setLocAndSpeed(float x,float y,float z,float speed,float theta)
	{
		speed*=1e-9;
		float headTheta=this.theta-theta;
		this.x=x;this.y=y;this.z=z;this.speed=speed;this.theta=theta;
		renderer.setEntityAttrib(instanceID,new float[]{
				System.nanoTime(),
				this.x,
				this.y,
				this.z,
				this.theta,
				this.speed,
				headTheta});
	}
	public void setIllumination(float illumination){renderer.setEntityIllumination(instanceID,illumination);}
}
