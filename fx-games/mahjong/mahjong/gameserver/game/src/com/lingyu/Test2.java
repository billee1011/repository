package com.lingyu;

public class Test2
{
    private boolean jiang=false;//是否找到将
    
    public Test2(){}

    //花色，判断这个花色是否被减完了（如果没有减完自然没有和）

    private int huase(int[] hua)
    {
        if (hua.length==7)
        {
            //判断字，自比较特殊，没有顺
            for (int i=0;i<hua.length ;i++ )
            {
                if (hua[i]==3||hua[i]==4)
                {
                    hua[i]=0;
                    huase(hua);
                }
                //如果字有两个，肯定是将
                if (hua[i]==2&&!jiang)
                {
                    hua[i]=0;
                    jiang=true;
                    huase(hua);
                }
            }
        }
        else
        {
            for (int i=0;i<hua.length ;i++ )
            {
                //如果没有将，先把将减出去
                if (!jiang&&hua[i]>=2)
                {
                    hua[i]=hua[i]-2;
                    jiang=true;
                    int fanhui=huase(hua);
                    //如果递归回来依旧没有减完，则把将加回去
                    if (fanhui!=0)
                    {
                        hua[i]=hua[i]+2;
                        jiang=false;
                    }
                } 
                if (hua[i]!=0&&i<7&&hua[i+1]!=0&&hua[i+2]!=0)
                {
                    hua[i]--;
                    hua[i+1]--;
                    hua[i+2]--;
                    huase(hua);
                    int fanhui=huase(hua);
                    //如果递归回来依旧没有减完，减去的加回去
                    if (fanhui!=0)
                    {
                        hua[i]++;
                        hua[i+1]++;
                        hua[i+2]++;
                    }
                }
                if (hua[i]==3||hua[i]==4)
                {
                    int temp=hua[i];
                    hua[i]=0;
                    huase(hua);
                    int fanhui=huase(hua);
                    //如果递归回来依旧没有减完，减去的加回去
                    if (fanhui!=0)
                    {
                        hua[i]++;
                        hua[i]=temp;
                    }
                }
            }
        }
        int re=0;
        //最后判断减没减完
        for (int i=0;i<hua.length ;i++ )
        {
            re=re+hua[i];
        }
        return re;
    }


    public boolean Hu(int[] aWan,int[] aTong,int[] aTiao,int[] aZi)//aTiao
    {
        
        if(huase(aZi) == 0 && huase(aWan) == 0 
            && huase(aTong) == 0 && huase(aTiao) == 0 && jiang)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    public static void main(String[] args) 
   {
    	Test2 mj=new Test2();
         int[] w={0,0,0,0,0,0,1,1,1};
        int[] to={0,0,0,0,1,1,1,0,0};
        int[] ti={0,0,0,2,0,0,0,0,0};
        int[] z={0,0,0,0,0,0,0};
        long beginTime = System.currentTimeMillis();
             mj.Hu(w,to,ti,z);
        long endTime = System.currentTimeMillis();
        System.out.println("耗时：" + (endTime - beginTime));
    }
}