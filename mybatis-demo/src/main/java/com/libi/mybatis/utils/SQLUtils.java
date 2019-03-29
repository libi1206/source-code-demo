package com.libi.mybatis.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL拼接<br>
 * 作者: 每特教育-余胜军<br>
 * 联系方式:QQ644064779|WWW.itmayiedu.com<br>
 */
public class SQLUtils {
    /**
     * 获取Insert语句后面values 参数信息<br>
     * 作者: 每特教育-余胜军<br>
     * 联系方式:QQ644064779|WWW.itmayiedu.com<br>
     *
     * @param sql
     * @return
     */
    public static String[] sqlInsertParameter(String sql) {
        int startIndex = sql.indexOf("values");
        int endIndex = sql.length();
        String substring = sql.substring(startIndex + 6, endIndex).replace("(", "").replace(")", "").replace("#{", "")
                .replace("}", "");
        String[] split = substring.split(",");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
        }
        return split;
    }

    /**
     * 获取select 后面where语句 作者: 每特教育-余胜军<br>
     * 联系方式:QQ644064779|WWW.itmayiedu.com<br>
     *
     * @param sql
     * @return
     */
    public static List<String> sqlSelectParameter(String sql) {
        int startIndex = sql.indexOf("where");
        int endIndex = sql.length();
        String substring = sql.substring(startIndex + 5, endIndex);
        String[] split = substring.split("and");
        List<String> listArr = new ArrayList<>();
        for (String string : split) {
            String[] sp2 = string.split("=");
            String param = sp2[0].trim();
            if ("username".equals(param)) {
                param = "userName";
            }
            listArr.add(param);
        }
        return listArr;
    }

    /**
     * 将SQL语句的参数替换变为?<br>
     * 作者: 每特教育-余胜军<br>
     * 联系方式:QQ644064779|WWW.itmayiedu.com<br>
     *
     * @param sql
     * @param parameterName
     * @return
     */
    public static String parameQuestion(String sql, String[] parameterName) {
        for (String string : parameterName) {
            sql = sql.replace("#{" + string + "}", "?");
        }
        return sql;
    }

    public static String parameQuestion(String sql, List<String> parameterName) {
        for (String string : parameterName) {
            sql = sql.replace("#{" + string + "}", "?");
        }
        return sql;
    }
}
