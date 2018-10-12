import org.apache.log4j.PropertyConfigurator;

import com.game.Player;
import com.game.common.Cfg;
import com.game.common.util.PressGlobalExecutor;
import com.game.manager.TimmerManager;
import com.game.network.tasks.LoginTask;
import com.game.service.HandlerService;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午4:10:39 <br/>
 */
public class App {

	public static void main(String[] args) {

		PropertyConfigurator.configureAndWatch("log4j.properties", 5000);

		App app = new App();
		app.init();
		app.mainUI();
		for (int i = 0; i < Cfg.ONLINE_NUM; i++) {
			PressGlobalExecutor.execute(new LoginTask(new Player(Cfg.NAME_PRE + i)));
		}
	}

	private void init() {
		HandlerService.getInstance().start();
		TimmerManager.init();
	}

	/**
	 * 
	 */
	private void mainUI() {
		System.out.println("=================================");
	}
}
