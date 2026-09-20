package com.flycms.core.utils.lucbir.features;

import com.flycms.core.utils.lucbir.utils.ImageUtil;

import java.awt.image.BufferedImage;

/**
 * 旋转不变的感知哈希<br>
 *
 * @author VenyoWang
 *
 * Created by VenyoWang on 2016/7/8.
 */
public class RHash implements Feature{
    public final static String FEATURE_NAME = "rhash";

    private String featureValue = null;

    /**
     * 旋转不变性<br>
     * @return
     */
    private String getFeature(int[][] matrix, double average) {
        // 半径
        String featureValue = "";
        int[] r = {2, 4, 6, 8};
        for(int i = 0; i < 4; i++){
            // 正方形左上角的点的下标
            int start = (8 - r[i]) / 2;
            int feature = 0;
            for(int j = start; j < start + r[i]; j++){
                feature = matrix[start][j] < average ? feature<<1 : (feature<<1)+1;
            }
            for(int j = start + 1; j < start + r[i]; j++){
                feature = matrix[j][start + r[i] - 1] < average ? feature<<1 : (feature<<1)+1;
            }
            for(int j = start + r[i] - 2; j >= start; j--){
                feature = matrix[start + r[i] - 1][j] < average ? feature<<1 : (feature<<1)+1;
            }
            for(int j = start + r[i] - 2; j > start; j--){
                feature = matrix[j][start] < average ? feature<<1 : (feature<<1)+1;
            }
            featureValue += getMinFeature(feature, 4 * (r[i] - 1));
        }
        return featureValue;
    }

    private String getMinFeature(int feature, int bitNum) {
        // 位数为bitNum的情况下的最大值
        int max = 1;
        for(int i = 1; i < bitNum; i++){
            max = (max << 1) + 1;
        }

        int min = feature;
        for(int i = 0; i < bitNum - 1; i++){
            // 循环右移一位
            feature = (feature>>1 | feature<<(bitNum - 1)) & max;
            if(feature < min){ min = feature;}
        }

        String result = "";
        for(int i = 0; i < bitNum; i++){
            if(min % 2 == 0){
                result = "0" + result;
            }
            else {
                result = "1" + result;
            }
            min >>= 1;
        }
        return result;
    }

    @Override
    public void extract(BufferedImage srcImg) {
        // 缩小尺寸，简化色彩
        int[][] grayMatrix = ImageUtil.getGrayPixel(srcImg, 8, 8);
        // 缩小DCT，计算平均值
        if(grayMatrix == null || grayMatrix.length == 0 || grayMatrix[0].length == 0){ return;}
        int[][] newMatrix = new int[8][8];
        double average = 0;
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                newMatrix[i][j] = grayMatrix[i][j];
                average += grayMatrix[i][j];
            }
        }
        average /= 64.0;
        this.featureValue = getFeature(newMatrix, average);
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureValue == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof RHash){
            RHash rHash = (RHash) feature;
            return ImageUtil.calculateSimilarity(this.featureValue, rHash.featureValue);
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
        return RHash.FEATURE_NAME;
    }
}
