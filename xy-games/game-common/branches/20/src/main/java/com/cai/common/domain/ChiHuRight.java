package com.cai.common.domain;

import com.cai.common.constant.GameConstants;

public class ChiHuRight {

	private static boolean m_bInit = false;
	private static long	m_dwRightMask[] = new long[]{0};
	
	public long m_dwRight[] = new long[]{0};
	
	public long type_list[] = new long[GameConstants.MAX_CHI_HU_TYPE];
	
	public long type_mul_list[] = new long[GameConstants.MAX_CHI_HU_TYPE];
	public int type_mul_count;
	
	public int type_count;
	
	public int da_hu_count;
	
	public boolean _show_all;
	public int _index_da_si_xi;
	public int _index_liul_liu_shun_1;
	public int _index_liul_liu_shun_2;
	
	public boolean _valid;
	
	
	public int bao_ting_index;
	public int bao_ting_card;
	public int hua_count;
	
	
	public void reset_card(){
		_show_all = false;
		_index_da_si_xi = _index_liul_liu_shun_1 = _index_liul_liu_shun_2 = GameConstants.MAX_INDEX;
	}
	
	public ChiHuRight(){
		if( !m_bInit )
		{
			m_bInit = true;
			for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ )
			{
				if( 0 == i )
					m_dwRightMask[i] = 0;
				else
					m_dwRightMask[i] = ((long)(Math.pow((float)2,(float)(i-1))))<<28;
			}
			type_count=0;
			for(int i=0; i < GameConstants.MAX_CHI_HU_TYPE;i++){
				type_list[i] = 0;
				type_mul_list[i]=0;
			}
		}
		_show_all = false;
		_index_da_si_xi = _index_liul_liu_shun_1 = _index_liul_liu_shun_2 = GameConstants.MAX_INDEX;
		da_hu_count=0;
		_valid=false;
		hua_count=0;
		bao_ting_card=0;
		bao_ting_index=-1;
		type_mul_count=0;
	}
	
	public String get_chi_hu_des(){
		
		
		return "";
	}
		
		
	
	//检查仅位是否正确
	public boolean is_valid_right( long dwRight )
	{
		long dwRightHead = dwRight & 0xF0000000;
		for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ )
			if( m_dwRightMask[i] == dwRightHead ) return true;
		return false;
	}
	
	//赋值符重载 = 
	public void  opr_equal( long dwRight )
	{
		long dwOtherRight = 0;
		//验证权位
		if( !is_valid_right( dwRight ) )
		{
			//验证取反权位
			if( !is_valid_right( ~dwRight ) ) return ;
			dwRight = ~dwRight;
			dwOtherRight = GameConstants.MASK_CHI_HU_RIGHT;
		}

		
		for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ )
		{
			if( ((dwRight&m_dwRightMask[i])!=0) || (i==0&&dwRight<0x10000000) ){
				m_dwRight[i] = dwRight&GameConstants.MASK_CHI_HU_RIGHT;
			}
			else {
				m_dwRight[i] = dwOtherRight;
			}
		}
	}
	
	//赋值符重载 &= 
	public void  opr_and_equal( long dwRight )
	{
		boolean bNavigate = false;
		//验证权位
		if( !is_valid_right( dwRight ) )
		{
			//验证取反权位
			if( !is_valid_right( ~dwRight ) ) return ;
			//调整权位
			long dwHeadRight = (~dwRight)&0xF0000000;
			long dwTailRight = dwRight&GameConstants.MASK_CHI_HU_RIGHT;
			dwRight = dwHeadRight|dwTailRight;
			bNavigate = true;
		}

		for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ )
		{
			if( ((dwRight&m_dwRightMask[i])!=0) || (i==0&&dwRight<0x10000000) )
			{
				long ddd = dwRight&GameConstants.MASK_CHI_HU_RIGHT;
				m_dwRight[i] &=ddd;
			}
			else if( !bNavigate )
				m_dwRight[i] = 0;
		}
	}
	
	//赋值符重载 |= 
	public void  opr_or_equal( long dwRight )
	{
		//验证权位
		if( !is_valid_right( dwRight ) ) return ;

		for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ )
		{
			if( ((dwRight&m_dwRightMask[i])!=0) || (i==0&&dwRight<0x10000000) )
				m_dwRight[i] |= (dwRight&GameConstants.MASK_CHI_HU_RIGHT);
		}
	}
	
	//赋值符重载 &
	public ChiHuRight  opr_and( long dwRight )
	{
		ChiHuRight tem = new ChiHuRight();
		tem.set_right_data(m_dwRight, GameConstants.MAX_RIGHT_COUNT);
		tem.opr_and_equal(dwRight);
		
		return tem;
	}
	
	//赋值符重载 |
	public void  opr_or( long dwRight )
	{
		
		if(opr_and(dwRight).is_empty()){
			if(type_count<GameConstants.MAX_CHI_HU_TYPE){
				type_list[type_count] = dwRight;
				type_count++;
				
				switch((int)dwRight){
					case GameConstants.CHR_HUNAN_PENGPENG_HU://碰碰胡
					case GameConstants.CHR_HUNAN_JIANGJIANG_HU://将将胡
					case GameConstants.CHR_HUNAN_QING_YI_SE://清一色
					case GameConstants.CHR_HUNAN_HAI_DI_LAO://海底捞
					case GameConstants.CHR_HUNAN_HAI_DI_PAO://海底炮
					case GameConstants.CHR_HUNAN_QI_XIAO_DUI://七小对
					case GameConstants.CHR_HUNAN_GANG_KAI:
					case GameConstants.CHR_HUNAN_QIANG_GANG_HU://抢杠胡
					case GameConstants.CHR_HUNAN_GANG_SHANG_PAO://杠上跑
					case GameConstants.CHR_HUNAN_QUAN_QIU_REN://全求人
					case GameConstants.CHR_HUNAN_TIAN_HU://天胡
					case GameConstants.CHR_HUNAN_DI_HU://地胡
					case GameConstants.CHR_HUNAN_MEN_QING://门清
					{
						this.da_hu_count++;
					}
					break;
					case GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI://豪华七小对
					{
						this.da_hu_count+=2;
					}
					break;
					case GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI://双豪华七小对
					{
						this.da_hu_count+=3;
					}
					break;
					case GameConstants.CHR_HUNAN_SHUANG_GANG_KAI://双杠开
					{
						this.da_hu_count+=1;
					}
					break;
					case GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO://双杠上炮
					{
						this.da_hu_count+=1;
					}
					break;
					default:
						break;

				}
			}
		}else{
			type_mul_list[type_mul_count++] = dwRight;
			switch((int)dwRight){
				case GameConstants.CHR_HUNAN_PENGPENG_HU://碰碰胡
				case GameConstants.CHR_HUNAN_JIANGJIANG_HU://将将胡
				case GameConstants.CHR_HUNAN_QING_YI_SE://清一色
				case GameConstants.CHR_HUNAN_HAI_DI_LAO://海底捞
				case GameConstants.CHR_HUNAN_HAI_DI_PAO://海底炮
				case GameConstants.CHR_HUNAN_QI_XIAO_DUI://七小对
				case GameConstants.CHR_HUNAN_GANG_KAI:
				case GameConstants.CHR_HUNAN_QIANG_GANG_HU://抢杠胡
				case GameConstants.CHR_HUNAN_GANG_SHANG_PAO://杠上跑
				case GameConstants.CHR_HUNAN_QUAN_QIU_REN://全求人
				case GameConstants.CHR_HUNAN_TIAN_HU://天胡
				case GameConstants.CHR_HUNAN_DI_HU://地胡
				{
					this.da_hu_count++;
				}
				break;
				case GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI://豪华七小对
				{
					this.da_hu_count+=2;
				}
				break;
				case GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI://双豪华七小对
				{
					this.da_hu_count+=3;
				}
				break;
				case GameConstants.CHR_HUNAN_SHUANG_GANG_KAI://双杠开
				{
					this.da_hu_count+=1;
				}
				break;
				case GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO://双杠上炮
				{
					this.da_hu_count+=1;
				}
				break;
				default:
					break;
	
			}
		}
		opr_or_equal(dwRight);
	}
	
	//是否权位为空
	public boolean is_empty(){
		for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ ){
			if( m_dwRight[i]!=0 ) return false;
		}
		_show_all = false;
		_index_da_si_xi = _index_liul_liu_shun_1 = _index_liul_liu_shun_2 = GameConstants.MAX_INDEX;
		
		return true;
	}
	//设置权位为空
	public void set_empty()
	{
		for( int i = 0; i < GameConstants.MAX_RIGHT_COUNT; i++ )
		{
			m_dwRight[i]=0;
		}
		type_count=0;
		for(int i=0; i < GameConstants.MAX_CHI_HU_TYPE;i++){
			type_list[i] = 0;
			type_mul_list[i]=0;
		}
		_valid=false;
		da_hu_count=0;
		type_mul_count=0;
		reset_card();
	}
	
	//获取权位数值
	public int get_right_data( long dwRight[] )
	{
		//if( cbMaxCount < m_dwRight.length ) return 0;
		
		for(int i =0; i < GameConstants.MAX_RIGHT_COUNT;i++){
			dwRight[i] = (long)m_dwRight[i];
		}
		
		return m_dwRight.length;
	}
	
	//设置权位数值
	public boolean set_right_data( long dwRight[], int cbRightCount )
	{
		if( cbRightCount > m_dwRight.length)  return false;

		for(int i =0; i < cbRightCount;i++){
			m_dwRight[i] = dwRight[i];
		}
		
		return true;
	}
	
	public void copy(ChiHuRight ch){
		set_right_data(ch.m_dwRight,1);
		this.type_count = ch.type_count;
		for(int i=0; i < this.type_count;i++){
			type_list[i] = ch.type_list[i];
		}
	}
	
	public boolean is_mul(int cbRight){
		for(int i=0; i < type_mul_count;i++){
			if(type_mul_list[i] ==cbRight){
				return true;
			}
		}
		return false;
	}

	public boolean is_show_all() {
		return _show_all;
	}

	public void set_show_all(boolean _show_all) {
		this._show_all = _show_all;
	}

	public int get_index_da_si_xi() {
		return _index_da_si_xi;
	}

	public void set_index_da_si_xi(int _index_da_si_xi) {
		this._index_da_si_xi = _index_da_si_xi;
	}

	public int get_index_liul_liu_shun_1() {
		return _index_liul_liu_shun_1;
	}

	public void set_index_liul_liu_shun_1(int _index_liul_liu_shun_1) {
		this._index_liul_liu_shun_1 = _index_liul_liu_shun_1;
	}

	public int get_index_liul_liu_shun_2() {
		return _index_liul_liu_shun_2;
	}

	public void set_index_liul_liu_shun_2(int _index_liul_liu_shun_2) {
		this._index_liul_liu_shun_2 = _index_liul_liu_shun_2;
	}

	public long[] getType_list() {
		return type_list;
	}

	public void setType_list(long[] type_list) {
		this.type_list = type_list;
	}

	public int getType_count() {
		return type_count;
	}

	public void setType_count(int type_count) {
		this.type_count = type_count;
	}

	public boolean is_valid() {
		return _valid;
	}

	public void set_valid(boolean _valid) {
		this._valid = _valid;
	}

	public int getDa_hu_count() {
		return da_hu_count;
	}

	public void setDa_hu_count(int da_hu_count) {
		this.da_hu_count = da_hu_count;
	}

	public long[] getType_mul_list() {
		return type_mul_list;
	}

	public void setType_mul_list(long[] type_mul_list) {
		this.type_mul_list = type_mul_list;
	}
}
