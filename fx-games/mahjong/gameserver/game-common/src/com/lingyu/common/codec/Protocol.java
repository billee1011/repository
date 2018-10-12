package com.lingyu.common.codec;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/***
 * 协议的实体类，传递数据
 *
 * @author zqgame
 *
 */

public class Protocol implements Serializable {
    private static final long serialVersionUID = 1L;
    public int cmd;
    public JSONObject body = null;

    public Protocol() {
        this.body = new JSONObject();
    }

    public Protocol(int cmd, JSONObject body) {
        this.cmd = cmd;
        this.body = body;
    }

    @Override
    public String toString() {
        return "{body=" + body + ", id=" + cmd + "}";
    }
}