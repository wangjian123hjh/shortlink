package com.nageoffer.shortlink.project.util;

import jakarta.servlet.http.HttpServletRequest;
import ua_parser.Parser;

public class UserAgentUtil {
    private static Parser parser = new Parser();
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        // 简单的 User-Agent 解析示例，可以使用更复杂的解析库如 ua-parser
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            return "Internet Explorer";
        } else if (userAgent.contains("Edge")) {
            return "Microsoft Edge";
        } else if (userAgent.contains("Chrome")) {
            return "Google Chrome";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Apple Safari";
        } else if (userAgent.contains("Firefox")) {
            return "Mozilla Firefox";
        } else if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
            return "Opera";
        } else {
            return "Unknown";
        }

    }
    public static String getOperatingSystem(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }
        // 简单的 User-Agent 解析示例，可以使用更复杂的解析库如 ua-parser
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone")) {
            return "iOS";
        } else {
            return "Unknown";
        }
    }

    public static String getDevice(HttpServletRequest request) {

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }
        String s = userAgent.toLowerCase();
        if (s.contains("mobile")){
            return "Mobile";
        }
        return "PC";
    }
}
