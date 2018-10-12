package com.lingyu.game.service.versionnotice;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.VersionNotice;

@Service
public class VersionNoticeManager {
    // private static final Logger logger =
    // LogManager.getLogger(VersionNoticeManager.class);
    @Autowired
    private VersionNoticeRepository versionNoticeRepository;

    /**
     * 添加版本公告
     *
     * @param type
     * @param content
     * @param addTime
     */
    public void addVersionNotice(int type, String content, Date addTime, String version) {
        VersionNotice vn = versionNoticeRepository.cacheLoad(type);
        if (vn == null) {
            vn = new VersionNotice();
            vn.setId(type);
            vn.setContent(content);
            vn.setVersion(version);
            vn.setAddTime(addTime);
            vn.setModifyTime(addTime);
            versionNoticeRepository.cacheInsert(vn);
        } else {
            vn.setContent(content);
            vn.setVersion(version);
            vn.setAddTime(addTime);
            versionNoticeRepository.cacheUpdate(vn);
        }
        // 不给玩家推送了吧，要不然是给全服在线的玩家推送。没必要。在开服前，把版本更新公告添加进来
    }

    /**
     * 获取所有版本公告信息
     *
     * @param roleId
     * @return
     */
    public JSONObject getAllVersionNotice(long roleId) {
        JSONObject result = new JSONObject();
        JSONArray array = new JSONArray();
        result.put(ErrorCode.RESULT, ErrorCode.EC_OK);

        List<VersionNotice> list = versionNoticeRepository.cacheLoadAll();
        for (VersionNotice v : list) {
            array.add(versionNoticeVo(v.getId(), v.getContent(), v.getVersion()));
        }
        result.put("data", array);
        return result;
    }

    private JSONObject versionNoticeVo(int type, String content, String version) {
        JSONObject res = new JSONObject();
        res.put("type", type);
        res.put("content", content);
        res.put("version", version);
        return res;
    }
}
