package com.cai.core;

import java.net.InetAddress;

import com.cai.common.util.PropertiesUtil;
import org.apache.commons.lang.StringUtils;


public class SystemConfig {

    public static int game_socket_port;

    public static int logic_index;


    public static String localip = "";

    /**
     * 1=开启调试
     */
    public static int gameDebug = 0;

    //

    static {
        try {
            InetAddress ia = InetAddress.getLocalHost();
            localip = ia.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void init(PropertiesUtil prop) {

        game_socket_port = Integer.parseInt(prop.getProperty("game.socket.port"));

        logic_index = Integer.parseInt(prop.getProperty("game.logic_index"));
        gameDebug = Integer.parseInt(prop.getProperty("game.debug"));
    }


}
