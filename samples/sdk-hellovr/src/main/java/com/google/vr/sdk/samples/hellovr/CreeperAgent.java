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
		EntityRenderer.ExprBuilder.Expr speedx=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr speedy=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr speedz=builder.nextParam();
		EntityRenderer.ExprBuilder.Expr headTheta=builder.nextParam();
		EntityRenderer.Model.Rectangle[]rectangles=new EntityRenderer.Model.Rectangle[36];
		EntityRenderer.ExprBuilder.Expr t=builder.sub(tabs,t0);
		EntityRenderer.ExprBuilder.Expr x=builder.add(x0,builder.mul(t,speedx));
		EntityRenderer.ExprBuilder.Expr y=builder.add(y0,builder.mul(t,speedy));
		EntityRenderer.ExprBuilder.Expr z=builder.add(z0,builder.mul(t,speedz));
		
		EntityRenderer.Model.Point bodyLLL=new EntityRenderer.Model.Point(builder.constant(-0.175f),builder.constant(-0.175f),builder.constant(0.325f));
		EntityRenderer.Model.Point bodyLLH=new EntityRenderer.Model.Point(builder.constant(-0.175f),builder.constant(-0.175f),builder.constant(1.25f));
		EntityRenderer.Model.Point bodyLHL=new EntityRenderer.Model.Point(builder.constant(-0.175f),builder.constant( 0.175f),builder.constant(0.325f));
		EntityRenderer.Model.Point bodyLHH=new EntityRenderer.Model.Point(builder.constant(-0.175f),builder.constant( 0.175f),builder.constant(1.25f));
		EntityRenderer.Model.Point bodyHLL=new EntityRenderer.Model.Point(builder.constant( 0.175f),builder.constant(-0.175f),builder.constant(0.325f));
		EntityRenderer.Model.Point bodyHLH=new EntityRenderer.Model.Point(builder.constant( 0.175f),builder.constant(-0.175f),builder.constant(1.25f));
		EntityRenderer.Model.Point bodyHHL=new EntityRenderer.Model.Point(builder.constant( 0.175f),builder.constant( 0.175f),builder.constant(0.325f));
		EntityRenderer.Model.Point bodyHHH=new EntityRenderer.Model.Point(builder.constant( 0.175f),builder.constant( 0.175f),builder.constant(1.25f));
		rectangles[0]=new EntityRenderer.Model.Rectangle(bodyLLH,bodyHLH,bodyHHH,bodyLHH, 0,16,0,16);
		rectangles[1]=new EntityRenderer.Model.Rectangle(bodyLLL,bodyHLL,bodyHLH,bodyLLH,16,16,0,16);
		rectangles[2]=new EntityRenderer.Model.Rectangle(bodyHLL,bodyHHL,bodyHHH,bodyHLH,32,16,0,16);
		rectangles[3]=new EntityRenderer.Model.Rectangle(bodyHHL,bodyLHL,bodyLHH,bodyHHH,48,16,0,16);
		rectangles[4]=new EntityRenderer.Model.Rectangle(bodyLHL,bodyLLL,bodyLLH,bodyLHH,64,16,0,16);
		rectangles[5]=new EntityRenderer.Model.Rectangle(bodyHLL,bodyLLL,bodyLHL,bodyHHL,80,16,0,16);
		
		EntityRenderer.ExprBuilder.Expr danglingTheta=builder.mul(builder.constant(0.2f),builder.cos(builder.mul(builder.mul(t,builder.constant(13.33f)),builder.sqrt(builder.add(builder.exp2(speedx),builder.exp2(speedy))))));
		EntityRenderer.ExprBuilder.Expr zbot=builder.sub(builder.constant(0.375f),builder.mul(builder.constant(0.375f),builder.cos(danglingTheta)));
		for(int xoff=-1;xoff<2;xoff+=2)for(int yoff=-1;yoff<2;yoff+=2)
		{
			EntityRenderer.ExprBuilder.Expr xmin=builder.add(builder.constant(0.175f*xoff-0.125f),builder.mul(builder.constant(0.325f*xoff*yoff),builder.sin(danglingTheta)));
			EntityRenderer.ExprBuilder.Expr xmax=builder.add(builder.constant(0.175f*xoff+0.125f),builder.mul(builder.constant(0.325f*xoff*yoff),builder.sin(danglingTheta)));
			EntityRenderer.ExprBuilder.Expr xumin=builder.constant(0.175f*xoff-0.125f);
			EntityRenderer.ExprBuilder.Expr xumax=builder.constant(0.175f*xoff+0.125f);
			EntityRenderer.ExprBuilder.Expr ymin=builder.constant(0.175f*yoff-0.125f);
			EntityRenderer.ExprBuilder.Expr ymax=builder.constant(0.175f*yoff+0.125f);
			EntityRenderer.Model.Point legLLL=new EntityRenderer.Model.Point(xmin,ymin,zbot);
			EntityRenderer.Model.Point legLLH=new EntityRenderer.Model.Point(xumin,ymin,builder.constant(0.375f));
			EntityRenderer.Model.Point legLHL=new EntityRenderer.Model.Point(xmin,ymax,zbot);
			EntityRenderer.Model.Point legLHH=new EntityRenderer.Model.Point(xumin,ymax,builder.constant(0.375f));
			EntityRenderer.Model.Point legHLL=new EntityRenderer.Model.Point(xmax,ymin,zbot);
			EntityRenderer.Model.Point legHLH=new EntityRenderer.Model.Point(xumax,ymin,builder.constant(0.375f));
			EntityRenderer.Model.Point legHHL=new EntityRenderer.Model.Point(xmax,ymax,zbot);
			EntityRenderer.Model.Point legHHH=new EntityRenderer.Model.Point(xumax,ymax,builder.constant(0.375f));
			int rectoff=12+((xoff+1)+(yoff+1)/2)*6;
			rectangles[rectoff+0]=new EntityRenderer.Model.Rectangle(legLLH,legHLH,legHHH,legLHH, 0,16,32,16);
			rectangles[rectoff+1]=new EntityRenderer.Model.Rectangle(legLLL,legHLL,legHLH,legLLH,16,16,32,16);
			rectangles[rectoff+2]=new EntityRenderer.Model.Rectangle(legHLL,legHHL,legHHH,legHLH,32,16,32,16);
			rectangles[rectoff+3]=new EntityRenderer.Model.Rectangle(legHHL,legLHL,legLHH,legHHH,48,16,32,16);
			rectangles[rectoff+4]=new EntityRenderer.Model.Rectangle(legLHL,legLLL,legLLH,legLHH,64,16,32,16);
			rectangles[rectoff+5]=new EntityRenderer.Model.Rectangle(legHLL,legLLL,legLHL,legHHL,80,16,32,16);
		}
		
		EntityRenderer.ExprBuilder.Expr cosHeadTheta=builder.cos(headTheta),sinHeadTheta=builder.sin(headTheta);
		EntityRenderer.ExprBuilder.Expr n04=builder.constant(-0.225f),p04=builder.constant(0.225f);
		EntityRenderer.Model.Point headLLL=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.25f));
		EntityRenderer.Model.Point headLLH=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.7f));
		EntityRenderer.Model.Point headLHL=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.25f));
		EntityRenderer.Model.Point headLHH=new EntityRenderer.Model.Point(builder.sub(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.constant(1.7f));
		EntityRenderer.Model.Point headHLL=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.25f));
		EntityRenderer.Model.Point headHLH=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(n04,sinHeadTheta)),builder.add(builder.mul(n04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.7f));
		EntityRenderer.Model.Point headHHL=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.25f));
		EntityRenderer.Model.Point headHHH=new EntityRenderer.Model.Point(builder.sub(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.add(builder.mul(p04,cosHeadTheta),builder.mul(p04,sinHeadTheta)),builder.constant(1.7f));
		rectangles[ 6]=new EntityRenderer.Model.Rectangle(headLLH,headHLH,headHHH,headLHH, 0,16,16,16);
		rectangles[ 7]=new EntityRenderer.Model.Rectangle(headLLL,headHLL,headHLH,headLLH,16,16,16,16);
		rectangles[ 8]=new EntityRenderer.Model.Rectangle(headHLL,headHHL,headHHH,headHLH,32,16,16,16);
		rectangles[ 9]=new EntityRenderer.Model.Rectangle(headHHL,headLHL,headLHH,headHHH,48,16,16,16);
		rectangles[10]=new EntityRenderer.Model.Rectangle(headLHL,headLLL,headLLH,headLHH,64,16,16,16);
		rectangles[11]=new EntityRenderer.Model.Rectangle(headHLL,headLLL,headLHL,headHHL,80,16,16,16);
		
		model=new EntityRenderer.Model(builder,rectangles,x,y,z,builder.atan2(speedy, speedx));
	}
	public static EntityRenderer init(Supplier<Location> location,int maxInstanceCount,byte[]texture)
	{
		if(renderer!=null)throw new IllegalStateException("re-initialize of CreeperAgent");
		CreeperAgent.maxInstanceCount=maxInstanceCount;
		return renderer=new EntityRenderer(HelloVrActivity.WEST_LIMIT,HelloVrActivity.EAST_LIMIT,HelloVrActivity.SOUTH_LIMIT,HelloVrActivity.NORTH_LIMIT,HelloVrActivity.BOTTOM_LIMIT,HelloVrActivity.TOP_LIMIT,location,model,maxInstanceCount,texture);
	}
	private static AtomicInteger instanceCount=new AtomicInteger();
	private final int instanceID;
	private float x,y,z,speedx, speedy, speedz;
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
		float x=this.x+speedx*timeElapsed;
		float y=this.y+speedy*timeElapsed;
		float z=this.z+speedz*timeElapsed;
		return new Location(x,y,z);
	}
	public void show(){renderer.enableEntity(instanceID);}
	public void hide(){renderer.disableEntity(instanceID);}
	public void setLocAndSpeed(float x,float y,float z,float speedx,float speedy,float speedz)
	{
		speedx*=1e-9;speedy*=1e-9;speedz*=1e-9;
		float headTheta=(float)Math.random()*0.5f-0.25f;//this.theta-theta;
		this.x=x;this.y=y;this.z=z;this.speedx=speedx;this.speedy=speedy;this.speedz=speedz;
		renderer.setEntityAttrib(instanceID,new float[]{
				startTime=System.nanoTime(),
				this.x,
				this.y,
				this.z,
				this.speedx,
				this.speedy,
				this.speedz,
				headTheta});
	}
	public void setIllumination(float illumination){renderer.setEntityIllumination(instanceID,illumination);}
}
