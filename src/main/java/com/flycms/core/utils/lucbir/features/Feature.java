package com.flycms.core.utils.lucbir.features;

import java.awt.image.BufferedImage;

/**
 * Created by VenyoWang on 2016/7/8.
 */
public interface Feature {
    public void extract(BufferedImage srcImg);
    /** 返回值的取值范围为0-1 */
    public double calculateSimilarity(Feature feature);
    /** 将特征值转换为用于索引的字符串 */
    public String feature2index();
    /** 将索引字符串转换为特征值 */
    public void index2feature(String index);
    public String getFeatureName();
}
