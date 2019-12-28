package cc.lym.Renderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;

import cc.lym.util.Location;
import cc.lym.util.RandomAccessModel;
import cc.lym.util.Supplier;
import cc.lym.util.Util;

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
	private final static boolean DEBUG=false;
	
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
		this.sceneSize[0]=(int)(xMax-xMin);this.sceneSize[1]=(int)(yMax-yMin);this.sceneSize[2]=(int)(zMax-zMin);
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
		boolean allOpaque=true;
		for(int i=1;i<blockIDLimit;i++)//id=0 -> air
		{
			opaque[i]=true;
			for(int j=0;j<blockTextureSize*6;j++)
				for(int k=blockTextureSize*i;k<blockTextureSize*(i+1);k++)
					if(((this.texture.getPixel(j,k)>>24)&0xff)!=0xff)
						allOpaque=opaque[i]=false;
			if(!opaque[i])
				Log.i(LOG_TAG,"block "+i+" is transparent");
		}
		if(allOpaque)
			Log.i(LOG_TAG,"all opaque");
		else
			Log.i(LOG_TAG,"not all opaque");
		(this.daemon=new DaemonThread()).start();
		PositionBuffer=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asIntBuffer();
		BlockPositionBuffer=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asIntBuffer();
		UVBuffer=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer();
		illuBuffer=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer();
		indexBuffer=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asIntBuffer();
	}
	public void init(RandomAccessModel model,RandomAccessModel illumination,int xRange,int yRange,int zRange)
	{
		Log.w(LOG_TAG,"init");
		synchronized (daemon)
		{
			pendingOperationCount.drainPermits();
			pendingOperations.clear();
			exposedFaces.clear();
			Log.w(LOG_TAG,"init begin");
			for(int i=0;i<xRange-1;i++)
				for(int j=0;j<yRange-1;j++)
					for(int k=0;k<zRange-1;k++)
					{
						for(Direction thisDir:new Direction[]{Direction.EAST,Direction.NORTH,Direction.UP})
						{
							Direction neighborDir=null;
							switch(thisDir)
							{
								case UP:	neighborDir=Direction.DOWN;		break;
								case EAST:	neighborDir=Direction.WEST;		break;
								case NORTH:	neighborDir=Direction.SOUTH;	break;
							}
							int neighbourID=-1,thisID=model.lookup(i,j,k);
							switch(thisDir)
							{
								case UP:	neighbourID=model.lookup(i,j,k+1);break;
								case EAST:	neighbourID=model.lookup(i+1,j,k);break;
								case NORTH:	neighbourID=model.lookup(i,j+1,k);break;
							}
							int neighbourIllu=illuMax,thisIllu=illumination.lookup(i,j,k);
							switch(thisDir)
							{
								case EAST:	neighbourIllu=illumination.lookup(i+1,j,k);break;
								case NORTH:	neighbourIllu=illumination.lookup(i,j+1,k);break;
								case UP:	neighbourIllu=illumination.lookup(i,j,k+1);break;
							}
							long neighbourX=i,neighbourY=j,neibhbourZ=k,thisX=i,thisY=j,thisZ=k;
							switch(neighborDir)
							{
								case WEST:	neighbourX=neighbourX+1;if(neighbourX==xMax)neighbourX=xMin;break;
								case SOUTH:	neighbourY=neighbourY+1;if(neighbourY==yMax)neighbourY=yMin;break;
								case DOWN:	neibhbourZ=neibhbourZ+1;if(neibhbourZ==zMax)neibhbourZ=zMin;break;
							}
							FaceLocation thisLoc=new FaceLocation(thisX,thisY,thisZ,thisDir);
							FaceLocation neighbourLoc=new FaceLocation(neighbourX,neighbourY,neibhbourZ,neighborDir);
							if((thisID!=0&&neighbourID==0)||(opaque[thisID]&&!opaque[neighbourID]))
							{
								exposedFaces.put(thisLoc,new FaceAttr(neighbourIllu,thisID,!opaque[thisID],thisIllu));
							}
							if((neighbourID!=0&&thisID==0)||(opaque[neighbourID]&&!opaque[thisID]))
							{
								exposedFaces.put(neighbourLoc,new FaceAttr(thisIllu,neighbourID,!opaque[neighbourID],neighbourIllu));
							}
						}
					}
			Log.w(LOG_TAG,"init OK");
			updateIllumination(xMin,yMin,zMin,illumination.lookup(xMin,yMin,zMin));//to restart daemon
		}
	}
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
		pendingOperations.add(new UpdateBlock(Util.fold(x,xMin,xMax), Util.fold(y,yMin,yMax), Util.fold(z,zMin,zMax),newBlockID,surroundingBlocks,illumination,illuMin,illuMax));
		pendingOperationCount.release();
		if(DEBUG)
		{
			StringBuilder neighbourBuilder=new StringBuilder();
			neighbourBuilder.append("[");
			for(int i=0;i<6;i++)
				neighbourBuilder.append(surroundingBlocks[i]).append(i==5?"]":",");
			StringBuilder illuBuilder=new StringBuilder();
			illuBuilder.append("[");
			for(int i=0;i<3;i++)
				for(int j=0;j<3;j++)
					for(int k=0;k<3;k++)
						illuBuilder.append(illumination[i][j][k]).append((i==2&&j==2&&k==2)?"]":",");
			Log.i(LOG_TAG,String.format("updblock %d %d %d %d "+neighbourBuilder+illuBuilder,x,y,z,newBlockID));
		}
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
		pendingOperations.add(new UpdateIllumination(Util.fold(x,xMin,xMax), Util.fold(y,yMin,yMax), Util.fold(z,zMin,zMax),newIllumination));
		pendingOperationCount.release();
		if(DEBUG)Log.i(LOG_TAG,String.format("updillum %d %d %d %d",x,y,z,newIllumination));
	}
	
	private IntBuffer PositionBuffer;
	private IntBuffer BlockPositionBuffer;
	private FloatBuffer UVBuffer;
	private FloatBuffer illuBuffer;
	private IntBuffer indexBuffer;
	private final Object bufferLock=new Object();
	
	private class DaemonThread extends Thread
	{
		int counter=0,total=0;
		@Override public void run()
		{
			while(true)
			{
				int counter=0;
				pendingOperationCount.acquireUninterruptibly();
				synchronized (this)
				{
					try
					{
						long deadline=System.nanoTime()+15000000;
						do
						{
							Op _tmp_op=pendingOperations.poll();
							counter++;
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
										if((thisID!=0&&neighbourID==0)||(opaque[thisID]&&!opaque[neighbourID]))
										{
											FaceAttr obj=exposedFaces.get(thisLoc);
											if(obj!=null)obj.blockID=thisID;
											else exposedFaces.put(thisLoc,new FaceAttr(neighbourIllu,thisID,!opaque[thisID],thisIllu));
										}
										else
										{
											exposedFaces.remove(thisLoc);
										}
										if((neighbourID!=0&&thisID==0)||(opaque[neighbourID]&&!opaque[thisID]))
										{
											FaceAttr obj=exposedFaces.get(neighbourLoc);
											if(obj!=null)obj.blockID=neighbourID;
											else exposedFaces.put(neighbourLoc,new FaceAttr(thisIllu,neighbourID,!opaque[neighbourID],neighbourIllu));
										}
										else
										{
											exposedFaces.remove(neighbourLoc);
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
				}
				Log.i(LOG_TAG,"batch "+(this.counter++)+": "+counter+" ops, "+(total+=counter)+" ops total, currently "+exposedFaces.size()+" surfaces");
				try
				{
					List<Integer>position=new ArrayList<>();
					List<Integer>blockPosition=new ArrayList<>();
					List<Float>uv=new ArrayList<>();
					List<Float>illu=new ArrayList<>();
					List<Integer>index=new ArrayList<>();
					for(Map.Entry<FaceLocation,FaceAttr>_$item:exposedFaces.entrySet())
					{
						FaceLocation loc=_$item.getKey();
						FaceAttr attr=_$item.getValue();
						//points:
						//4-3
						//| |
						//1-2
						int x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,faceoff=-1;
						x0=x1=x2=x3=x4=(int)loc.x;
						y0=y1=y2=y3=y4=(int)loc.y;
						z0=z1=z2=z3=z4=(int)loc.z;
						switch(loc.dir)
						{
							case UP:z1++;z2++;z3++;z4++;	x2++;x3++;y3++;y4++;faceoff=0;break;
							case SOUTH:						x2++;x3++;z3++;z4++;faceoff=1;break;
							case EAST:x1++;x2++;x3++;x4++;	y2++;y3++;z3++;z4++;faceoff=2;break;
							case NORTH:y1++;y2++;y3++;y4++;	x1++;x4++;z4++;z3++;faceoff=3;break;
							case WEST:						y1++;y4++;z4++;z3++;faceoff=4;break;
							case DOWN:						x1++;x4++;y4++;y3++;faceoff=5;break;
						}
						float u1,v1,u2,v2,u3,v3,u4,v4;
						u1=u4=faceoff/6.0f;u2=u3=(faceoff+1)/6.0f;
						v1=v2=(attr.blockID+1)/(float)blockIDLimit;v3=v4=(attr.blockID)/(float)blockIDLimit;
						float illuOut=(attr.illumination-illuMin)/(float)(illuMax-illuMin);
						//triangle 1-2-3
						position.add(x1);position.add(y1);position.add(z1);
						position.add(x2);position.add(y2);position.add(z2);
						position.add(x3);position.add(y3);position.add(z3);
						blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
						blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
						blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
						uv.add(u1);uv.add(v1);
						uv.add(u2);uv.add(v2);
						uv.add(u3);uv.add(v3);
						illu.add(illuOut);
						illu.add(illuOut);
						illu.add(illuOut);
						index.add(index.size());
						index.add(index.size());
						index.add(index.size());
						//triangle 1-3-4
						position.add(x1);position.add(y1);position.add(z1);
						position.add(x3);position.add(y3);position.add(z3);
						position.add(x4);position.add(y4);position.add(z4);
						blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
						blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
						blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
						uv.add(u1);uv.add(v1);
						uv.add(u3);uv.add(v3);
						uv.add(u4);uv.add(v4);
						illu.add(illuOut);
						illu.add(illuOut);
						illu.add(illuOut);
						index.add(index.size());
						index.add(index.size());
						index.add(index.size());
						if(attr.dispInner)
						{
							float illuIn=(attr.innerIllumination-illuMin)/(float)(illuMax-illuMin);
							//triangle 1-3-2
							position.add(x1);position.add(y1);position.add(z1);
							position.add(x3);position.add(y3);position.add(z3);
							position.add(x2);position.add(y2);position.add(z2);
							blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
							blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
							blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
							uv.add(u1);uv.add(v1);
							uv.add(u3);uv.add(v3);
							uv.add(u2);uv.add(v2);
							illu.add(illuOut);
							illu.add(illuOut);
							illu.add(illuOut);
							index.add(index.size());
							index.add(index.size());
							index.add(index.size());
							//triangle 1-4-3
							position.add(x1);position.add(y1);position.add(z1);
							position.add(x4);position.add(y4);position.add(z4);
							position.add(x3);position.add(y3);position.add(z3);
							blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
							blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
							blockPosition.add(x0);blockPosition.add(y0);blockPosition.add(z0);
							uv.add(u1);uv.add(v1);
							uv.add(u4);uv.add(v4);
							uv.add(u3);uv.add(v3);
							illu.add(illuOut);
							illu.add(illuOut);
							illu.add(illuOut);
							index.add(index.size());
							index.add(index.size());
							index.add(index.size());
						}
					}
					
					IntBuffer PositionBuffer=ByteBuffer.allocateDirect(4*position.size()).order(ByteOrder.nativeOrder()).asIntBuffer();
					IntBuffer BlockPositionBuffer=ByteBuffer.allocateDirect(4*blockPosition.size()).order(ByteOrder.nativeOrder()).asIntBuffer();
					FloatBuffer UVBuffer=ByteBuffer.allocateDirect(4*uv.size()).order(ByteOrder.nativeOrder()).asFloatBuffer();
					FloatBuffer illuBuffer=ByteBuffer.allocateDirect(4*illu.size()).order(ByteOrder.nativeOrder()).asFloatBuffer();
					IntBuffer indexBuffer=ByteBuffer.allocateDirect(4*index.size()).order(ByteOrder.nativeOrder()).asIntBuffer();
					
					int[]_position_=new int[position.size()];
					for(int i=0;i<_position_.length;i++)
						_position_[i]=position.get(i);
					PositionBuffer.put(_position_);PositionBuffer.rewind();

					int[]_blockPosition_=new int[blockPosition.size()];
					for(int i=0;i<_blockPosition_.length;i++)
						_blockPosition_[i]=blockPosition.get(i);
					BlockPositionBuffer.put(_blockPosition_);BlockPositionBuffer.rewind();

					float[]_uv_=new float[uv.size()];
					for(int i=0;i<_uv_.length;i++)
						_uv_[i]=uv.get(i);
					UVBuffer.put(_uv_);UVBuffer.rewind();

					float[]_illu_=new float[illu.size()];
					for(int i=0;i<_illu_.length;i++)
						_illu_[i]=illu.get(i);
					illuBuffer.put(_illu_);illuBuffer.rewind();

					int[]_index_=new int[index.size()];
					for(int i=0;i<_index_.length;i++)
						_index_[i]=index.get(i);
					indexBuffer.put(_index_);indexBuffer.rewind();

					synchronized(bufferLock)
					{
						BlockRenderer.this.PositionBuffer=PositionBuffer;
						BlockRenderer.this.BlockPositionBuffer=BlockPositionBuffer;
						BlockRenderer.this.UVBuffer=UVBuffer;
						BlockRenderer.this.illuBuffer=illuBuffer;
						BlockRenderer.this.indexBuffer=indexBuffer;
					}
					Log.i(LOG_TAG,"scene recreated");
				}
				catch(RuntimeException e){Log.e(LOG_TAG,"internal error: cannot rebuild render buffer",e);}
			}
		}
	}
	
	private static final String vertexShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"" +
			"precision highp int;" +
			"precision highp float;" +
			"" +
			"uniform mat4 u_transform;" +
			"uniform ivec3 u_UpperBound;" +
			"uniform ivec3 u_LowerBound;" +
			"uniform ivec3 u_SceneSize;" +
			"in ivec3 a_Position;" +
			"in ivec3 a_BlockPosition;" +
			"in vec2 a_UV;" +
			"out vec2 v_UV;" +
			"in float a_illu;" +
			"out float v_illu;" +
			"" +
			"void main()" +
			"{" +
			"	ivec3 Position;" +
			"" +
			"	if(a_BlockPosition.x>u_UpperBound.x)Position.x=a_Position.x-u_SceneSize.x;" +
			"	else if(a_BlockPosition.x<u_LowerBound.x)Position.x=a_Position.x+u_SceneSize.x;" +
			"	else Position.x=a_Position.x;" +
			"" +
			"	if(a_BlockPosition.y>u_UpperBound.y)Position.y=a_Position.y-u_SceneSize.y;" +
			"	else if(a_BlockPosition.y<u_LowerBound.y)Position.y=a_Position.y+u_SceneSize.y;" +
			"	else Position.y=a_Position.y;" +
			"" +
			"	if(a_BlockPosition.z>u_UpperBound.z)Position.z=a_Position.z-u_SceneSize.z;" +
			"	else if(a_BlockPosition.z<u_LowerBound.z)Position.z=a_Position.z+u_SceneSize.z;" +
			"	else Position.z=a_Position.z;" +
			"" +
			"	gl_Position=u_transform*vec4(Position,1.0);" +
			"	v_UV=a_UV;" +
			"	v_illu=a_illu;" +
			"}" +
			"";
	private static final String fragmentShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"" +
			"precision highp int;" +
			"precision highp float;" +
			"precision highp sampler2D;" +
			"" +
			"in vec2 v_UV;" +
			"in float v_illu;" +
			"uniform sampler2D u_Texture;" +
			"out vec4 out_FragColor;" +
			"" +
			"void main()" +
			"{" +
			"	vec4 color=texture(u_Texture,vec2(v_UV.x,v_UV.y));" +
			"	if(color.a<0.5)discard;" +
			"	out_FragColor=vec4(color.rgb*v_illu,1.0);" +
			"}" +
			"";
	private Location location=new Location(0,0,0);
	private final float[]headTrans=new float[16];
	private final int[]upperBound=new int[3];
	private final int[]lowerBound=new int[3];
	private final int[]sceneSize=new int[3];
	private int glProgram;
	private int glParam_u_transform;//mat4
	private int glParam_u_UpperBound;//ivec3
	private int glParam_u_LowerBound;//ivec3
	private int glParam_u_SceneSize;//ivec3
	private int glParam_a_Position;//ivec3
	private int glParam_a_BlockPosition;//ivec3
	private int glParam_a_UV;//vec2
	private int glParam_a_illu;//float
	private int[]textureId=new int[1];
	
	@Override
	public void onNewFrame(HeadTransform headTransform)
	{
		Location tmp=locationSupplier.get();
		location=new Location(Util.fold(tmp.x,xMin,xMax), Util.fold(tmp.y,yMin,yMax), Util.fold(tmp.z,zMin,zMax));//fold
		upperBound[0]=(int)(location.x+sceneSize[0]/2);upperBound[1]=(int)(location.y+sceneSize[1]/2);upperBound[2]=(int)(location.z+sceneSize[2]/2);
		lowerBound[0]=(int)(location.x-sceneSize[0]/2);lowerBound[1]=(int)(location.y-sceneSize[1]/2);lowerBound[2]=(int)(location.z-sceneSize[2]/2);
		headTransform.getHeadView(headTrans,0);
	}
	@Override
	public void onDrawEye(Eye eye)
	{
		GLES31.glUseProgram(glProgram);
		Renderer.checkGlError();
		
		float[]head2eye=new float[16];
		Matrix.multiplyMM(head2eye,0,eye.getEyeView(),0,new float[]{0,0,-1,0,-1,0,0,0,0,1,0,0,(float)location.y,-(float)location.z,(float)location.x,1},0);
		float[]perspective=new float[16];
		Matrix.multiplyMM(perspective,0,eye.getPerspective(0.05f,250.0f),0,head2eye,0);
		
		GLES31.glUniformMatrix4fv(glParam_u_transform,1,false,perspective,0);
		GLES31.glUniform3iv(glParam_u_UpperBound,1,upperBound,0);
		GLES31.glUniform3iv(glParam_u_LowerBound,1,lowerBound,0);
		GLES31.glUniform3iv(glParam_u_SceneSize,1,sceneSize,0);
		Renderer.checkGlError();
		GLES31.glEnableVertexAttribArray(glParam_a_Position);
		GLES31.glEnableVertexAttribArray(glParam_a_BlockPosition);
		GLES31.glEnableVertexAttribArray(glParam_a_UV);
		GLES31.glEnableVertexAttribArray(glParam_a_illu);
		Renderer.checkGlError();
		
		IntBuffer PositionBuffer;
		IntBuffer BlockPositionBuffer;
		FloatBuffer UVBuffer;
		FloatBuffer illuBuffer;
		IntBuffer indexBuffer;
		synchronized(bufferLock)
		{
			PositionBuffer=this.PositionBuffer;
			BlockPositionBuffer=this.BlockPositionBuffer;
			UVBuffer=this.UVBuffer;
			illuBuffer=this.illuBuffer;
			indexBuffer=this.indexBuffer;
		}
		
		GLES31.glVertexAttribIPointer(glParam_a_Position,3, GLES31.GL_INT,0,PositionBuffer);
		GLES31.glVertexAttribIPointer(glParam_a_BlockPosition,3, GLES31.GL_INT,0,BlockPositionBuffer);
		GLES31.glVertexAttribPointer(glParam_a_UV,2, GLES31.GL_FLOAT,false,0,UVBuffer);
		GLES31.glVertexAttribPointer(glParam_a_illu,1, GLES31.GL_FLOAT,false,0,illuBuffer);
		Renderer.checkGlError();
		GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
		GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId[0]);
		Renderer.checkGlError();
		GLES31.glDrawElements(GLES31.GL_TRIANGLES,indexBuffer.limit(),GLES31.GL_UNSIGNED_INT,indexBuffer);
		Renderer.checkGlError();
	}
	@Override
	public void onSurfaceCreated(EGLConfig eglConfig)
	{
		Log.i(LOG_TAG,"onSurfaceCreated");
		int vs=Renderer.loadShader(GLES31.GL_VERTEX_SHADER,vertexShaderSourceCode);
		int fs=Renderer.loadShader(GLES31.GL_FRAGMENT_SHADER,fragmentShaderSourceCode);
		glProgram = GLES31.glCreateProgram();
		GLES31.glAttachShader(glProgram,vs);
		Renderer.checkGlError();
		GLES31.glAttachShader(glProgram,fs);
		Renderer.checkGlError();
		GLES31.glLinkProgram(glProgram);
		int[] linkStatus = new int[1];
		GLES31.glGetProgramiv(glProgram, GLES31.GL_LINK_STATUS, linkStatus, 0);
		if(linkStatus[0]!=GLES31.GL_TRUE)
		{
			Log.e("opengl","link error: "+GLES31.glGetProgramInfoLog(glProgram));
			throw new RuntimeException(GLES31.glGetProgramInfoLog(glProgram));
		}
		Renderer.checkGlError();
		glParam_u_transform=GLES31.glGetUniformLocation(glProgram,"u_transform");
		glParam_u_UpperBound=GLES31.glGetUniformLocation(glProgram,"u_UpperBound");
		glParam_u_LowerBound=GLES31.glGetUniformLocation(glProgram,"u_LowerBound");
		glParam_u_SceneSize=GLES31.glGetUniformLocation(glProgram,"u_SceneSize");
		glParam_a_Position=GLES31.glGetAttribLocation(glProgram,"a_Position");
		glParam_a_BlockPosition=GLES31.glGetAttribLocation(glProgram,"a_BlockPosition");
		glParam_a_UV=GLES31.glGetAttribLocation(glProgram,"a_UV");
		glParam_a_illu=GLES31.glGetAttribLocation(glProgram,"a_illu");
		Renderer.checkGlError();
		GLES31.glGenTextures(1,textureId,0);
		GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
		GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId[0]);
		Renderer.checkGlError();
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_WRAP_S,GLES31.GL_CLAMP_TO_EDGE);
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_WRAP_T,GLES31.GL_CLAMP_TO_EDGE);
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_MIN_FILTER,GLES31.GL_LINEAR_MIPMAP_NEAREST);
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_MAG_FILTER,GLES31.GL_NEAREST);
		Renderer.checkGlError();
		GLUtils.texImage2D(GLES31.GL_TEXTURE_2D,0,texture,0);
		GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D);
		Renderer.checkGlError();
	}
	@Override public void onFinishFrame(Viewport viewport) {}
	@Override public void onSurfaceChanged(int i, int i1) {}
	@Override public void onRendererShutdown() {}
}
