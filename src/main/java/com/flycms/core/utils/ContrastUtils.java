package com.flycms.core.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:17 2018/9/28
 */
public class ContrastUtils<T> {
    public String contrastObj(Object oldBean, Object newBean) {
        String str="";
        //if (oldBean instanceof SysConfServer && newBean instanceof SysConfServer) {
        T pojo1 = (T) oldBean;
        T pojo2 = (T) newBean;
        try {
            Class clazz = pojo1.getClass();
            Field[] fields = pojo1.getClass().getDeclaredFields();
            int i=1;
            for (Field field : fields) {
                if("serialVersionUID".equals(field.getName())){
                    continue;
                }
                PropertyDescriptor pd = new PropertyDescriptor(field.getName(), clazz);
                Method getMethod = pd.getReadMethod();
                Object o1 = getMethod.invoke(pojo1);
                Object o2 = getMethod.invoke(pojo2);
                if(o1==null || o2 == null){
                    continue;
                }
                if (!o1.toString().equals(o2.toString())) {
                    if(i!=1){
                        str+=";";
                    }
                    str+=i+"、字段名称"+field.getName()+",旧值:"+o1+",新值:"+o2;
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // }
        return str;
    }
}
