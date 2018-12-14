package com.flycms.core.utils;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 22:12 2018/8/16
 */
public class TokenUtils {
    private static final String TOKEN_LIST_NAME = "tokenList";
    public static final String TOKEN_STRING_NAME = "token";

    private static ArrayList getTokenList(HttpSession session) {
        Object obj = session.getAttribute(TOKEN_LIST_NAME);
        if (obj != null) {
            return (ArrayList) obj;
        } else {
            ArrayList tokenList = new ArrayList();
            session.setAttribute(TOKEN_LIST_NAME, tokenList);
            return tokenList;
        }
    }

    private static void saveTokenString(String tokenStr, HttpSession session) {
        ArrayList tokenList = getTokenList(session);
        tokenList.add(tokenStr);
        session.setAttribute(TOKEN_LIST_NAME, tokenList);
    }

    private static String generateTokenString(){
        return new Long(System.currentTimeMillis()).toString();
    }


    /**
     * Generate a token string, and save the string in session, then return the token string.
     * @param session
     * @return a token string used for enforcing a single request for a particular transaction.
     */

    public static String getTokenString(HttpSession session) {
        String tokenStr = generateTokenString();
        saveTokenString(tokenStr, session);
        return tokenStr;
    }

    /**

     * check whether token string is valid. if session contains the token string, return true.
     * otherwise, return false.
     * @param tokenStr
     * @param session
     * @return true: session contains tokenStr; false: session is null or tokenStr is id not in session
     */

    public static boolean isTokenStringValid(String tokenStr, HttpSession session) {
        boolean valid = false;
        if(session != null){
            ArrayList tokenList = getTokenList(session);
            if (tokenList.contains(tokenStr)) {
                valid = true;
                tokenList.remove(tokenStr);
            }
        }
        return valid;
    }
}
