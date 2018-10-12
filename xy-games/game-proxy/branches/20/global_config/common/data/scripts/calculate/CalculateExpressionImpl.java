package calculate;  

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.expression.CEService;
import com.cai.expression.ICalculateExpression;

/**
 * 动态运行计算类实现
 * @author run
 *
 */
public class CalculateExpressionImpl implements ICalculateExpression{
	
	private static Logger logger = LoggerFactory.getLogger(CalculateExpressionImpl.class);
	
	private static CalculateExpressionImpl instance = null;

	private CalculateExpressionImpl() {

	}

	public static CalculateExpressionImpl getInstance() {
		if (instance == null) {
			instance = new CalculateExpressionImpl();
		}
		return instance;
	}
	
	/**
	 * 入口,初始化
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("进入动态脚本.....");
		CEService.regiesterICEservice(CalculateExpressionImpl.getInstance());
	}

	@Override
	public boolean doSystemWork1() {
		System.out.println("in doSystemWork1...1123."); 
		return false;
	}

	@Override
	public boolean doSystemWork2() {
		System.out.println("in doSystemWork2.....");
		return false;
	}
	
	
	
	
	
	
	
	

}
