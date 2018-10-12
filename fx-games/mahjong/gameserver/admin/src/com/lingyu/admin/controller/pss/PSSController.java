package com.lingyu.admin.controller.pss;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.lingyu.admin.vo.PlayerVo;
import com.lingyu.admin.vo.PlayerVos;

/**
 * 客服系统Controller.
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Controller
@RequestMapping(value = "/pss")
public class PSSController {
	private static final Logger logger = LogManager.getLogger(PSSController.class);

	/** 客服系统主页面UI */
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void indexUI() {
	}
	
	/** 客服系统批量上传玩家 */
	@ResponseBody
	@RequestMapping(value = "/batchPlayersUpload.do", method = RequestMethod.POST)
	public PlayerVos batchUploadPlayers(@RequestParam("players") MultipartFile multipartFile){
		PlayerVos ret = new PlayerVos();
		try(
			BufferedReader br = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));
		){
			String line = null;
			while((line = br.readLine()) != null){
				String[] ss = line.split("\\s+");
				if(ss.length > 1){
					PlayerVo playerVo = new PlayerVo();
					playerVo.setId(ss[0]);
					playerVo.setName(ss[1]);
					ret.addPlayerVo(playerVo);
				}else{
					ret.setErrorCode("文件格式有错误");
				}
			}
		}catch(Exception e){
			ret.setErrorCode("文件格式有错误");
        }
		return ret;
	}
}
