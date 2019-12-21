package cc.lym.leap;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class LeapReceiver
{
	private final static String LOG_TAG="LeapReceiver";
	
	private final Runnable onGet;
	private final Runnable onPut;
	private final Runnable onOpenMenu;
	private final Runnable onCloseMenu;
	private final DaemonThread daemonThread;
	private Location loc=null;
	private Hand lasthand=null;
	public static class Location{public final double x,y,z;Location(double x,double y,double z){this.x=x;this.y=y;this.z=z;}}
	public synchronized Location queryPosition(){return loc;}
	public LeapReceiver(Runnable onGet,Runnable onPut,Runnable onOpenMenu,Runnable onCloseMenu)
	{
		this.onGet=onGet;this.onPut=onPut;this.onOpenMenu=onOpenMenu;this.onCloseMenu=onCloseMenu;
		daemonThread=new DaemonThread();
		daemonThread.start();
	}
	private class DaemonThread extends Thread
	{
		@Override public void run()
		{
			try
			{
				DatagramSocket sock=new DatagramSocket(29475);
				sock.setSoTimeout(0);
				byte[]buffer=new byte[2048];
				DatagramPacket packet=new DatagramPacket(buffer,buffer.length);
				boolean lastForward=false;
				boolean grabForward=false;
				boolean lastSelector=false;
				while(true)
				{
					try{
						int len;
						do
						{
							sock.receive(packet);
							len=packet.getLength();
							if(len!=78)
								Log.w(LOG_TAG,"corrupted packet, try again");
						}while(len!=78);
						DataInputStream in=new DataInputStream(new ByteArrayInputStream(buffer));
						Hand hand=new Hand();
						hand.seqNo=in.readLong();
						hand.isPresent=(in.readByte()!=0);
						if(hand.isPresent)
						{
							hand.id=in.readInt();
							hand.isRight=(in.readByte()!=0);
							hand.visibleTime=in.readLong();
							hand.pinchDistance=in.readFloat();
							hand.grabAngle=in.readFloat();
							hand.pinchNormalized=in.readFloat();
							hand.grabNormalized=in.readFloat();
							hand.palmPosX=in.readFloat();
							hand.palmPosY=in.readFloat();
							hand.palmPosZ=in.readFloat();
							hand.palmNormX=in.readFloat();
							hand.palmNormY=in.readFloat();
							hand.palmNormZ=in.readFloat();
							hand.palmWidth=in.readFloat();
							hand.palmDirectionX=in.readFloat();
							hand.palmDirectionY=in.readFloat();
							hand.palmDirectionZ=in.readFloat();
						}
						if(lasthand==null||(lasthand.seqNo<hand.seqNo&&hand.seqNo-lasthand.seqNo<10000)||((lasthand.seqNo>Long.MAX_VALUE/1024*1023)&&(hand.seqNo<Long.MIN_VALUE/1024*1023)))
						{
							boolean forward=false;
							boolean selector=false;
							lasthand=hand;
							if(hand.isPresent)
								loc=new Location(hand.palmPosX,hand.palmPosY,hand.palmPosZ);
							else
								loc=null;
							if(hand.isPresent)
							{
								float dot=hand.palmPosX*hand.palmNormX+hand.palmPosY*hand.palmNormY+hand.palmPosZ*hand.palmNormZ;
								dot=dot*dot/(hand.palmPosX*hand.palmPosX+hand.palmPosY*hand.palmPosY+hand.palmPosZ*hand.palmPosZ);
								if(dot>0.6||(lastForward&&dot>0.3))
								{
									forward=true;
									if(!grabForward&&hand.grabNormalized>0.65)
									{
										grabForward=true;
										if(lastForward)
											onGet.run();
									}
									else if(grabForward&&hand.grabNormalized<0.3)
									{
										grabForward=false;
										if(lastForward)
											onPut.run();
									}
								}
								else if(hand.palmNormZ<-0.35)
									selector=true;
							}
							lastForward=forward;
							if(lastSelector&&!selector)
								onCloseMenu.run();
							else if(selector&&!lastSelector)
								onOpenMenu.run();
							lastSelector=selector;
						}
						else if(lasthand.seqNo==hand.seqNo)
							Log.w(LOG_TAG,String.format("sequence duplicate %d",hand.seqNo));
						else if(lasthand.seqNo>hand.seqNo)
							Log.w(LOG_TAG,String.format("sequence decreased %d %d",lasthand.seqNo,hand.seqNo));
						else
							Log.w(LOG_TAG,String.format("sequence %d %d",lasthand.seqNo,hand.seqNo));
					}catch(IOException e){Log.e(LOG_TAG,"",e);}
				}
			}catch(SocketException e){Log.e(LOG_TAG,"",e);}
		}
	}
}
