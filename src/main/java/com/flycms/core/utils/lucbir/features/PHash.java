package com.flycms.core.utils.lucbir.features;

import com.flycms.core.utils.lucbir.utils.ImageUtil;

import java.awt.image.BufferedImage;



/**
 * pHash<br>
 * 参考链接：http://blog.csdn.net/zouxy09/article/details/17471401<br>
 * http://blog.csdn.net/luoweifu/article/details/8220992
 *
 * @author VenyoWang
 *
 * Created by VenyoWang on 2016/7/8.
 */
public class PHash implements Feature{
    public final static String FEATURE_NAME = "phash";

    private String featureValue = null;

    @Override
    public void extract(BufferedImage srcImg) {
        // 缩小尺寸，简化色彩
        int[][] grayMatrix = ImageUtil.getGrayPixel(srcImg, 32, 32);
        // 计算DCT
        grayMatrix = ImageUtil.DCT(grayMatrix, 32);
        // 缩小DCT，计算平均值
        int[][] newMatrix = new int[8][8];
        double average = 0;
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                newMatrix[i][j] = grayMatrix[i][j];
                average += grayMatrix[i][j];
            }
        }
        average /= 64.0;
        // 计算hash值
        String hash = "";
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if(newMatrix[i][j] < average){
                    hash += '0';
                }
                else{
                    hash += '1';
                }
            }
        }
        this.featureValue = hash;
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureValue == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof PHash){
            PHash pHash = (PHash) feature;
            return ImageUtil.calculateSimilarity(this.featureValue, pHash.featureValue);
        }
        else{
            throw new IllegalArgumentException("对比图片的特征对象与源图片的特征对象不是同一个类，无法进行对比");
        }
    }

    @Override
    public String feature2index() {
        return this.featureValue;
    }

    @Override
    public void index2feature(String index) {
        this.featureValue = index;
    }

    @Override
    public String getFeatureName() {
        return PHash.FEATURE_NAME;
    }
}
