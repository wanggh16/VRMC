package cc.lym.Renderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import cc.lym.util.Supplier;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Renderer that renders blocks.
 *
 * The coordinate system used here: (East(x), North(y), Up(z))
 * Block at position (x,y,z) has its west-south-bottom corner at (x,y,z) and east-north-top corner at (x+1,y+1,z+1)
 * Block ID 0 represents air (or any invisible block)
 *
 * Structure of the texture image:
 * Assume ID 1, 2 and 3 represents block A,B and C, then the texture image should look like:
 * <style>.texture_struct tr td{border:1px solid #000000;}</style>
 * <table class="texture_struct" cellspacing="0">
 *     <tr><td>whatever(8*8)</td><td>whatever(8*8)</td><td>whatever(8*8)</td><td>whatever(8*8)</td><td>whatever(8*8)</td><td>whatever(8*8)</td></tr>
 *     <tr><td>A_TOP(8*8)</td><td>A_SOUTH(8*8)</td><td>A_EAST(8*8)</td><td>A_NORTH(8*8)</td><td>A_WEST(8*8)</td><td>A_BOTTOM(8*8)</td></tr>
 *     <tr><td>B_TOP(8*8)</td><td>B_SOUTH(8*8)</td><td>B_EAST(8*8)</td><td>B_NORTH(8*8)</td><td>B_WEST(8*8)</td><td>B_BOTTOM(8*8)</td></tr>
 *     <tr><td>C_TOP(8*8)</td><td>C_SOUTH(8*8)</td><td>C_EAST(8*8)</td><td>C_NORTH(8*8)</td><td>C_WEST(8*8)</td><td>C_BOTTOM(8*8)</td></tr>
 * </table>
 * 48 pixels wide and 32 pixels high
 */
public class BlockRenderer implements HeadlessRenderer {
	private final static String LOG_TAG="BlockRenderer";
	
	private enum Direction{UP,SOUTH,EAST,NORTH,WEST,DOWN}
	private static class FaceLocation
	{
		final long x,y,z;final Direction dir;final int hashcode;
		FaceLocation(long x,long y,long z,Direction dir)
		{
			this.x=x;this.y=y;this.z=z;this.dir=dir;
			int param;
			switch(dir)
			{
				case UP:param=0;break;
				case SOUTH:param=1;break;
				case EAST:param=2;break;
				case NORTH:param=3;break;
				case WEST:param=4;break;
				case DOWN:param=5;break;
				default:throw new RuntimeException();
			}
			long tmp=param+x+(x<<17|x>>47)+y+(y<<29|x>>35)+z+(z<<41|z>>23);
			hashcode=(int)(tmp^(tmp>>32));
		}
		@Override public int hashCode(){return hashcode;}
		@Override public boolean equals(Object obj)
		{
			if(!(obj instanceof FaceLocation))return false;
			FaceLocation var=(FaceLocation)obj;
			return var.x==x&&var.y==y&&var.z==z&&var.dir==dir;
		}
	}
	private static class FaceAttr{int illumination;int blockID;boolean dispInner;int innerIllumination;FaceAttr(int illu,int ID,boolean dispIn,int inIllu){illumination=illu;blockID=ID;dispInner=dispIn;innerIllumination=inIllu;}}
	private static abstract class Op
	{
		final long x,y,z;
		Op(long x,long y,long z)
		{
			this.x=x;this.y=y;this.z=z;
		}
		static long fold(long val,long min,long max)
		{
			val=(val-min)%(max-min);
			if(val<0)val+=(max-min);
			return val+min;
		}
	}
	private static class UpdateBlock extends Op
	{
		final int newBlockID;
		final int[][][]illumination;
		final int[]surroundingBlocks;
		@Override @NonNull public String toString()
		{
			StringBuilder builder=new StringBuilder(String.format("UpdateBlock@(%d,%d,%d), %d ",x,y,z,newBlockID));
			builder.append("(");
			if(surroundingBlocks!=null)
				for(int i=0;i<6;i++)
					builder.append(i==0?"":",").append(surroundingBlocks[i]);
			builder.append(")(");
			for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)
				builder.append((i==0&&j==0&&k==0)?"":",").append(illumination[i][j][k]);
			builder.append(")");
			return builder.toString();
		}
		UpdateBlock(long x,long y,long z,int newBlockID,int[]surroundingBlocks,int[][][]illumination,int illuMin,int illuMax)
		{
			super(x,y,z);
			this.newBlockID=newBlockID;
			this.surroundingBlocks=new int[6];
			for(int i=0;i<6;i++)
				this.surroundingBlocks[i]=surroundingBlocks[i];
			int[][][] thisIllumination=new int[3][3][3];
			if(illumination.length!=3)
			{
				Log.e(LOG_TAG,"illumination.length "+illumination.length);
				thisIllumination=null;
			}
			if(thisIllumination!=null)
			for(int i=0;i<3;i++)
			{
				if(illumination[i].length!=3)
				{
					Log.e(LOG_TAG,"illumination["+i+"].length "+illumination[i].length);
					thisIllumination=null;
				}
				if(thisIllumination!=null)
				for(int j=0;j<3;j++)
				{
					if(illumination[i][j].length!=3)
					{
						Log.e(LOG_TAG,"illumination["+i+"]["+j+"].length "+illumination[i][j].length);
						thisIllumination=null;
					}
					if(thisIllumination!=null)
					for(int k=0;k<3;k++)
					{
						if(illumination[i][j][k]<illuMin||illumination[i][j][k]>illuMax)
						{
							Log.w(LOG_TAG,"illegal illumination["+i+"]["+j+"]["+k+"] "+illumination[i][j][k]);
							thisIllumination[i][j][k]=Math.max(illuMin,Math.min(illuMax,illumination[i][j][k]));
						}
						else thisIllumination[i][j][k]=illumination[i][j][k];
					}
				}
			}
			if(thisIllumination==null)
				throw new IllegalArgumentException("illumination array is invalid");
			this.illumination=thisIllumination;
		}
	}
	private static class UpdateIllumination extends Op
	{
		final int newIllumination;
		@Override @NonNull public String toString()
		{
			return String.format("UpdateIllumination@(%d,%d,%d), %d ",x,y,z,newIllumination);
		}
		UpdateIllumination(long x,long y,long z,int newIllumination)
		{
			super(x,y,z);
			this.newIllumination=newIllumination;
		}
	}
	
	/**
	 * An object that holds the player's viewpoint location.
	 */
	public static class Location{final double x,y,z;public Location(double x,double y,double z){this.x=x;this.y=y;this.z=z;}}
	
	private final long xMin,xMax,yMin,yMax,zMin,zMax;
	private final int illuMin,illuMax;
	private final int blockIDLimit,blockTextureSize;
	private final Supplier<Location> locationSupplier;
	private final Bitmap texture;
	private final boolean[]opaque;
	private final Map<FaceLocation,FaceAttr> exposedFaces=new LinkedHashMap<>();
	private final ConcurrentLinkedQueue<Op> pendingOperations=new ConcurrentLinkedQueue<>();
	private Semaphore pendingOperationCount=new Semaphore(0);
	private final DaemonThread daemon;
	
	/**
	 * Constructs a scene with no blocks.
	 * The west-south-bottom corner of the scene is at (xMin,yMin,zMin) and the east-north-top corner is at (xMax,yMax,zMax).
	 * A legal position (x,y,z) of a block should hold <b>xMin <= x < xMax</b>, <b>yMin <= y < yMax</b> and <b>zMin <= z < zMax</b>
	 * A legal illumination level val should hold <b>illuMin <= val <= illuMax</b>
	 * @param xMin Small enough when spacial folding is not needed on the west-east dimension.
	 * @param xMax Large enough when spacial folding is not needed on the west-east dimension.
	 * @param yMin Small enough when spacial folding is not needed on the south-north dimension.
	 * @param yMax Large enough when spacial folding is not needed on the south-north dimension.
	 * @param zMin Small enough when spacial folding is not needed on the bottom-up dimension.
	 * @param zMax Large enough when spacial folding is not needed on the bottom-up dimension.
	 * @param illuMin Illumination level where blocks will be totally dark.
	 * @param illuMax Illumination level where blocks will be rendered as bright as the original texture.
	 * @param locationSupplier A function that can provide location of the player's viewpoint location whenever needed.
	 * @param texture Content of the image file that holds all the textures, acceptable formats: PNG JPEG WEBP.
	 */
	public BlockRenderer(long xMin, long xMax, long yMin, long yMax, long zMin, long zMax, int illuMin, int illuMax,
						 Supplier<Location>locationSupplier, byte[] texture)
	{
		this.xMin=xMin;this.xMax=xMax;this.yMin=yMin;this.yMax=yMax;this.zMin=zMin;this.zMax=zMax;
		this.illuMin=illuMin;this.illuMax=illuMax;
		if(xMin>xMax||yMin>yMax||zMin>zMax||illuMin>illuMax)
		{
			Log.e(LOG_TAG,String.format("illegal range x%d~%d y%d~%d z%d~%d illumination%d~%d",xMin,xMax,yMin,yMax,zMin,zMax,illuMin,illuMax));
			throw new IllegalArgumentException();
		}
		this.locationSupplier=locationSupplier;
		BitmapFactory.Options options=new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeByteArray(texture,0,texture.length,options);
		blockTextureSize=options.outWidth/6;
		blockIDLimit=options.outHeight/blockTextureSize;
		if(blockTextureSize*6!=options.outWidth)
			Log.w(LOG_TAG,"texture width is "+options.outWidth+", cutting to "+(blockTextureSize*6));
		if(blockIDLimit*blockTextureSize!=options.outHeight)
			Log.w(LOG_TAG,"texture height is "+options.outHeight+", cutting to "+(blockIDLimit*blockTextureSize));
		Bitmap full=BitmapFactory.decodeByteArray(texture,0,texture.length);
		this.texture=Bitmap.createBitmap(full,0,0,blockTextureSize*6,blockIDLimit*blockTextureSize);
		opaque=new boolean[blockIDLimit];
		opaque[0]=false;
		for(int i=1;i<blockIDLimit;i++)//id=0 -> air
		{
			opaque[i]=true;
			for(int j=0;j<blockTextureSize*6;j++)
				for(int k=blockTextureSize*i;k<blockTextureSize*(i+1);k++)
					if(((this.texture.getPixel(j,k)>>24)&0xff)!=0xff)
						opaque[i]=false;
			if(!opaque[i])
				Log.v(LOG_TAG,"block "+i+" is transparent");
		}
		(this.daemon=new DaemonThread()).start();
	}
//	public BlockRenderer(long xMin, long xMax, long yMin, long yMax, long zMin, long zMax, int illuMin, int illuMax,
//						 Supplier<Location>locationSupplier, byte[] texture, int[][][]blocks)
//	{
//		this(xMin,xMax,yMin,yMax,zMin,zMax,illuMin,illuMax,locationSupplier,texture);
//		if(xMin<0||yMin<0||zMin<0)
//			Log.e(LOG_TAG,"illegal west-south-bottom corner for array init: "+xMin+" "+yMin+" "+zMin);
//		for(long i=xMin;i<xMax;i++)
//			for(long j=yMin;j<yMax;j++)
//				for(long k=zMin;k<zMax;k++)
//				{
//
//				}
//	}
	/**
	 * Update a block
	 * @param x Position of the updated block
	 * @param y
	 * @param z
	 * @param newBlockID ID of the block after updating
	 * @param surroundingBlocks ID of blocks to the {UP,SOUTH,EAST,NORTH,WEST,DOWN} of the updated block.<br>
	 *                          Note: If spacial folding is used, when a block at the EAST-most position is updated, the block to its east should be some block at the WEST-most position.
	 * @param illumination illumination[i][j][k] represents illumination level at position (x-1+i,y-1+j,z-1+k)
	 */
	public void updateBlock(long x,long y,long z,int newBlockID,int[]surroundingBlocks,int[][][]illumination)
	{
		if(x<xMin||x>=xMax||y<yMin||y>=yMax||z<zMin||z>=zMax)
		{
			Log.w(LOG_TAG,"illegal coordinate "+x+" "+y+" "+z+", folded");
		}
		if(newBlockID<0||newBlockID>=blockIDLimit)
		{
			Log.e(LOG_TAG,"illegal block ID "+newBlockID);
			throw new IllegalArgumentException("illegal block ID "+newBlockID);
		}
		if(surroundingBlocks==null)
		{
			Log.e(LOG_TAG,"surrounding blocks missing");
			throw new IllegalArgumentException("surrounding blocks missing");
		}
		if(surroundingBlocks.length!=6)
		{
			Log.e(LOG_TAG,"invalid surrounding array");
			throw new IllegalArgumentException("invalid surrounding array");
		}
		for(int i=0;i<6;i++)
			if(surroundingBlocks[i]<0||surroundingBlocks[i]>=blockIDLimit)
			{
				Log.e(LOG_TAG,"illegal surrounding block ID "+newBlockID);
				throw new IllegalArgumentException("illegal surrounding block ID "+newBlockID);
			}
		pendingOperations.add(new UpdateBlock(Op.fold(x,xMin,xMax),Op.fold(y,yMin,yMax),Op.fold(z,zMin,zMax),newBlockID,surroundingBlocks,illumination,illuMin,illuMax));
		pendingOperationCount.release();
	}
	/**
	 * Update the illumination level at a position
	 * @param x Position of the updated location
	 * @param y
	 * @param z
	 * @param newIllumination Illumination level after updating
	 */
	public void updateIllumination(long x,long y,long z,int newIllumination)
	{
		if(x<xMin||x>=xMax||y<yMin||y>=yMax||z<zMin||z>=zMax)
		{
			Log.w(LOG_TAG,"illegal coordinate "+x+" "+y+" "+z+", folded");
		}
		if(newIllumination<illuMin||newIllumination>illuMax)
		{
			Log.w(LOG_TAG,"illegal illumination "+newIllumination);
			newIllumination=Math.max(illuMin,Math.min(illuMax,newIllumination));
		}
		pendingOperations.add(new UpdateIllumination(Op.fold(x,xMin,xMax),Op.fold(y,yMin,yMax),Op.fold(z,zMin,zMax),newIllumination));
		pendingOperationCount.release();
	}
	
	class DaemonThread extends Thread
	{
		@Override public void run()
		{
			while(true)
			{
				pendingOperationCount.acquireUninterruptibly();
				try
				{
					long deadline=System.nanoTime()+3000000;
					do
					{
						Op _tmp_op=pendingOperations.poll();
						try
						{
							if(_tmp_op instanceof UpdateBlock)
							{
								UpdateBlock op=(UpdateBlock)_tmp_op;
								for(Direction thisDir:Direction.values())
								{
									Direction neighborDir=null;
									switch(thisDir)
									{
										case UP:	neighborDir=Direction.DOWN;		break;
										case SOUTH:	neighborDir=Direction.NORTH;	break;
										case EAST:	neighborDir=Direction.WEST;		break;
										case NORTH:	neighborDir=Direction.SOUTH;	break;
										case WEST:	neighborDir=Direction.EAST;		break;
										case DOWN:	neighborDir=Direction.UP;		break;
									}
									int neighbourID=-1,thisID=op.newBlockID;
									switch(thisDir)
									{
										case UP:	neighbourID=op.surroundingBlocks[0];break;
										case SOUTH:	neighbourID=op.surroundingBlocks[1];break;
										case EAST:	neighbourID=op.surroundingBlocks[2];break;
										case NORTH:	neighbourID=op.surroundingBlocks[3];break;
										case WEST:	neighbourID=op.surroundingBlocks[4];break;
										case DOWN:	neighbourID=op.surroundingBlocks[5];break;
									}
									int neighbourIllu=illuMax,thisIllu=op.illumination[1][1][1];
									switch(thisDir)
									{
										case WEST:	neighbourIllu=op.illumination[0][1][1];break;
										case EAST:	neighbourIllu=op.illumination[2][1][1];break;
										case SOUTH:	neighbourIllu=op.illumination[1][0][1];break;
										case NORTH:	neighbourIllu=op.illumination[1][2][1];break;
										case DOWN:	neighbourIllu=op.illumination[1][1][0];break;
										case UP:	neighbourIllu=op.illumination[1][1][2];break;
									}
									long neighbourX=op.x,neighbourY=op.y,neibhbourZ=op.z,thisX=op.x,thisY=op.y,thisZ=op.z;
									switch(neighborDir)
									{
										case WEST:	neighbourX=neighbourX+1;if(neighbourX==xMax)neighbourX=xMin;break;
										case EAST:	if(neighbourX==xMin)neighbourX=xMax;neighbourX=neighbourX-1;break;
										case SOUTH:	neighbourY=neighbourY+1;if(neighbourY==yMax)neighbourY=yMin;break;
										case NORTH:	if(neighbourY==yMin)neighbourY=yMax;neighbourY=neighbourY-1;break;
										case DOWN:	neibhbourZ=neibhbourZ+1;if(neibhbourZ==zMax)neibhbourZ=zMin;break;
										case UP:	if(neibhbourZ==zMin)neibhbourZ=zMax;neibhbourZ=neibhbourZ-1;break;
									}
									FaceLocation thisLoc=new FaceLocation(thisX,thisY,thisZ,thisDir);
									FaceLocation neighbourLoc=new FaceLocation(neighbourX,neighbourY,neibhbourZ,neighborDir);
									if(thisID==0||opaque[neighbourID])
									{
										exposedFaces.remove(thisLoc);
									}
									else
									{
										FaceAttr obj=exposedFaces.get(thisLoc);
										if(obj!=null)obj.blockID=thisID;
										else exposedFaces.put(thisLoc,new FaceAttr(neighbourIllu,thisID,!opaque[thisID],thisIllu));
									}
									if(neighbourID==0||opaque[thisID])
									{
										exposedFaces.remove(neighbourLoc);
									}
									else
									{
										FaceAttr obj=exposedFaces.get(neighbourLoc);
										if(obj!=null)obj.blockID=neighbourID;
										else exposedFaces.put(neighbourLoc,new FaceAttr(thisIllu,neighbourID,!opaque[neighbourID],neighbourIllu));
									}
								}
							}
							else if(_tmp_op instanceof UpdateIllumination)
							{
								UpdateIllumination op=(UpdateIllumination)_tmp_op;
								for(Direction dir:Direction.values())
								{
									FaceAttr obj=exposedFaces.get(new FaceLocation(op.x,op.y,op.z,dir));
									if(obj!=null)obj.innerIllumination=op.newIllumination;
								}
								for(Direction dir:Direction.values())
								{
									long x=op.x,y=op.y,z=op.z;
									switch(dir)
									{
										case WEST:	x=x+1;if(x==xMax)x=xMin;break;
										case EAST:	if(x==xMin)x=xMax;x=x-1;break;
										case SOUTH:	y=y+1;if(y==yMax)y=yMin;break;
										case NORTH:	if(y==yMin)y=yMax;y=y-1;break;
										case DOWN:	z=z+1;if(z==zMax)z=zMin;break;
										case UP:	if(z==zMin)z=zMax;z=z-1;break;
									}
									FaceAttr obj=exposedFaces.get(new FaceLocation(x,y,z,dir));
									if(obj!=null)obj.illumination=op.newIllumination;
								}
							}
							else{Log.e(LOG_TAG,"internal error: unknown op "+_tmp_op);}
						}
						catch(RuntimeException e){Log.e(LOG_TAG,"internal error: cannot update face set with "+_tmp_op,e);}
					}while(System.nanoTime()<deadline&&pendingOperationCount.tryAcquire(300,TimeUnit.MICROSECONDS));
				}catch(InterruptedException ignored){}
				try
				{
				
				}
				catch(RuntimeException e){Log.e(LOG_TAG,"internal error: cannot rebuild render buffer",e);}
			}
		}
	}
	
	private Location location=new Location(0,0,0);
	private float[]headTrans=new float[16];
	
	@Override
	public void onNewFrame(HeadTransform headTransform)
	{
		location=locationSupplier.get();
		headTransform.getHeadView(headTrans,0);
	}
	@Override
	public void onDrawEye(Eye eye)
	{
	
	}
	@Override
	public void onSurfaceCreated(EGLConfig eglConfig)
	{
	
	}
	@Override public void onFinishFrame(Viewport viewport) {}
	@Override public void onSurfaceChanged(int i, int i1) {}
	@Override public void onRendererShutdown() {}
}
