package com.lingyu;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;

public class Test {
	
	    /*
	    先拿掉一个，然后再检测缺少什么，补上，这里不行，就补别的地方
	    */
	    public ArrayList entryList = new ArrayList();   //替换对子
	    
	    private int fmaj = -1;        //可以打掉的牌
	    private int tmaj = -1;        //可以补上的牌
	    
	    int[] w={3,1,1,1,2,1,1,1,3};  //1-9万各个牌的个数
	    int[] to={0,0,0,0,0,0,0,0,0};
	    int[] ti={0,0,0,0,0,0,0,0,0};
	    int[] z={0,0,0,0,0,0,0};
	    
	    public Test(int[] w, int[] to, int[] ti, int[] z)
	    {
	        this.w = w;
	        this.to = to;
	        this.ti = ti;
	        this.z = z;
	    }
	    private void ting()
	    {        
	        for(int i = 0; i < 34; i++)
	        {
	            boolean exeIT = false;
	            if((i < 9 && w[i] >0) )
	            {
	                w[i]--;
	                fmaj = i;
	                if(!check())
	                { w[i]++;continue;}
	                w[i]++;
	            }
	            else if(i >=9 && i < 18 && to[i-9] >0)
	            {
	                to[i-9]--;
	                fmaj = i;
	                if(!check())
	                { to[i-9]++;continue;}
	                to[i-9]++;
	            } 
	            else if(i >=18 && i <27 && ti[i-18] >0)
	            {
	                ti[i-18]--;
	                fmaj = i;
	                if(!check())
	                { ti[i-18]++;continue;}
	                ti[i-18]++;
	            }
	            else if((i >= 27 && z[i-27] >0))
	            {
	                z[i-27] = z[i-27] -1;
	                fmaj = i;
	                if(!check())
	                { z[i-27]++;continue;}
	                z[i-27]++;
	            }
	        }
	    }

	    private boolean check()
	    {
	        boolean res = true;
	        res = checkZi(z, 27);  //如果字牌不符合条件，就不用检查序数牌了
	        checkNum(w, 0);
	        checkNum(to, 9);
	        checkNum(ti, 18);
	        return res;
	    }
	    //检查字：因为字和序数牌不同，单独拿出来
	    private boolean checkZi(int[] hua, int beginLoc)
	    {
	        //判断字
	        boolean checkjiang = false;
	        for(int i = 0; i < hua.length; i++)
	        {
	            if(beginLoc + i == fmaj) continue;
	            if(hua[i] == 2)
	            {
	                hua[i]++;
	                if(hu(w,to,ti,z))
	                {
	                    tmaj = beginLoc+i;
	                    
	                        print();
	                    entryList.add(new Entry(fmaj, tmaj));
	                }
	                else
	                {
	                    if(!checkjiang)
	                    {
	                        checkjiang = true;
	                    }
	                    else
	                    {
	                        hua[i]--;
	                        return false;//不用往下匹对了,
	                    }
	                }
	                hua[i]--;
	            }
	            else if(hua[i] == 1)
	            {
	                hua[i]++;
	                if(hu(w,to,ti,z))
	                {
	                    tmaj = beginLoc+i;
	                    print();
	                    entryList.add(new Entry(fmaj, tmaj));
	                }
	                else 
	                {
	                    hua[i]--;
	                    return false;//不用往下匹对了
	                }
	                hua[i]--;
	            }
	        }
	        return true;
	    }
	    private void checkNum(int[] hua, int beginLoc)
	    {
	        boolean[] hasreplace = new boolean[9];  //检查是否已经替换过
	        for (int i=0;i<hua.length ;i++ )
	        {
	               if(beginLoc + i == fmaj) continue;
	              if((i == 0 || hua[i] >0) && !hasreplace[i])
	               {
	                   hua[i]++;
	                   hasreplace[i] = true;
	                   if(hu(w,to,ti,z))
	                   {
	                       tmaj = beginLoc+i;
	                    entryList.add(new Entry(fmaj, tmaj));
	                   }
	                   hua[i]--;
	                   //替换成i前面的那张牌
	                   if(i > 0 && i < 9 && !hasreplace[i-1])
	                   {
	                       hua[i-1]++;
	                       hasreplace[i-1] = true;
	                       if(hu(w,to,ti,z))
	                       {
	                           tmaj = beginLoc + i - 1;
	                        entryList.add(new Entry(fmaj, tmaj));
	                       }
	                       hua[i-1]--;
	                   }
	                   //替换成i前面的那张牌
	                   if(i > 0 && i < 8 && !hasreplace[i+1])
	                   {
	                       hua[i+1]++;
	                       hasreplace[i+1] = true;
	                       if(hu(w,to,ti,z))
	                       {
	                           tmaj = beginLoc + i + 1;
	                        entryList.add(new Entry(fmaj, tmaj));
	                       }
	                       hua[i+1]--;
	                   }
	               }
	        }
	    }
	    public boolean hu(int[] aWan,int[] aTong,int[] aTiao,int[] aZi)
	    {
	        int[] tempWang = new int[aWan.length];
	        int[] tempTong = new int[aTong.length];
	        int[] tempTiao = new int[aTiao.length];
	        int[] tempZi = new int[aZi.length];
	        System.arraycopy(aWan, 0, tempWang, 0, aWan.length);
	        System.arraycopy(aTong, 0, tempTong, 0, aTong.length);
	        System.arraycopy(aTiao, 0, tempTiao, 0, aTiao.length);
	        System.arraycopy(aZi, 0, tempZi, 0, aZi.length);
	        boolean res = new Test2().Hu(tempWang, tempTong, tempTiao, tempZi);
	        return res;
	        
	    }
	    private void print()
	    {
	        System.out.print("w = ");
	        for(int i = 0; i < w.length; i++)
	        {
	            System.out.print(w[i] + ", ");
	        }
	        System.out.println();
	        System.out.print("to = ");
	        for(int i = 0; i < to.length; i++)
	        {
	            System.out.print(to[i] + ", ");
	        }
	        System.out.println();
	        System.out.print("ti = ");
	        for(int i = 0; i < ti.length; i++)
	        {
	            System.out.print(ti[i] + ", ");
	        }
	        System.out.println();
	        System.out.print("z = ");
	        for(int i = 0; i < z.length; i++)
	        {
	            System.out.print(z[i] + ", ");
	        }
	        System.out.println();                        
	    }
	    
	    class Entry
	    {
	        
	        public int fmaj;
	        public int tmaj;
	        
	        public Entry(int fmaj, int tmaj)
	        {
	            this.fmaj = fmaj;
	            this.tmaj = tmaj;
	        }
	        public void setFmaj(int fmaj)
	        {
	            this.fmaj = fmaj;
	        }
	        public void setTmaj(int tmaj)
	        {
	            this.tmaj = tmaj;
	        }
	        public String toString()
	        {
	            return "(" + fmaj + ", " + tmaj + ")";
	        }
	    } 
	public static void main(String[] args) {
		// 99 555 24
		int[] w={0,0,0,0,0,0,0,1,1};
        int[] to={0,0,0,0,1,1,1,0,0};
        int[] ti={0,1,0,2,0,0,0,0,0};
        int[] z={0,0,0,0,0,0,0};
        Test mj=new Test(w,to,ti,z);
        mj.ting();
        System.out.println(mj.entryList);
        
		/*// c-s
		int msgType = 10001;
		Object msg[] = new Object[]{1,2,3};
		TestVo vo = new TestVo();
		vo.setMsgType(msgType);
		vo.setMsg(msg);
		String json = JSON.toJSONString(vo);
		System.out.println(json);
		byte bytes[] = null;
		try {
			bytes = json.getBytes("utf-8");
			// 对bytes进行加密 传给服务器
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// s-c
		// 对bytes解密，拿到bytes
		String str = new String(bytes);
		TestVo vo2 = JSON.parseObject(str, TestVo.class);
		System.out.println(vo2.getMsgType()+","+vo2.getMsg());
		
		
		String result = JSON.toJSONString(msg);
		System.out.println("数组转成json："+result);
		Object s  = JSON.parseArray(result, Object.class);
		System.out.println("json转成数组："+s);*/
	}

}
