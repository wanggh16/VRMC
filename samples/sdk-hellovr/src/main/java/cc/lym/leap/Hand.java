package cc.lym.leap;

public class Hand
{
	Hand(){}
	Hand(Hand other)
	{
		seqNo=other.seqNo;
		isPresent=other.isPresent;
		id	=	other.	id	;
		isRight	=	other.	isRight	;
		visibleTime	=	other.	visibleTime	;
		pinchDistance	=	other.	pinchDistance	;
		grabAngle	=	other.	grabAngle	;
		pinchNormalized	=	other.	pinchNormalized	;
		grabNormalized	=	other.	grabNormalized	;
		palmPosX	=	other.	palmPosX	;
		palmPosY	=	other.	palmPosY	;
		palmPosZ	=	other.	palmPosZ	;
		palmNormX	=	other.	palmNormX	;
		palmNormY	=	other.	palmNormY	;
		palmNormZ	=	other.	palmNormZ	;
		palmWidth	=	other.	palmWidth	;
		palmDirectionX	=	other.	palmDirectionX	;
		palmDirectionY	=	other.	palmDirectionY	;
		palmDirectionZ	=	other.	palmDirectionZ	;
	}
	public long seqNo;
	public boolean isPresent;
	
	public int id;
	public boolean isRight;
	public long visibleTime;
	public float pinchDistance;
	public float grabAngle;
	public float pinchNormalized;
	public float grabNormalized;
	
	public float palmPosX;
	public float palmPosY;
	public float palmPosZ;
	public float palmNormX;
	public float palmNormY;
	public float palmNormZ;
	public float palmWidth;
	public float palmDirectionX;
	public float palmDirectionY;
	public float palmDirectionZ;
}
