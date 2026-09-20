package com.flycms.core.utils.lucbir.features;

import com.flycms.core.utils.lucbir.utils.ImageUtil;
import com.flycms.core.utils.lucbir.utils.Util;

import java.awt.image.BufferedImage;

/**
 * 均匀LBP纹理特征<br>
 * 参考链接：http://blog.csdn.net/lu597203933/article/details/17184503
 *
 * @author VenyoWang
 *
 * Created by VewnyoWang on 2016/7/8.
 */
public class UniformLBP implements Feature{
    public final static String FEATURE_NAME = "ulbp";

    private int[] featureVector = null;

    /**
     * 对256个特征值进行分组<br>
     * groupNums的下标为特征值，value为组号
     * @return
     */
    private int[] groupFeatureValue(){
        int[] groupNums = new int[256];
        int num = 1;
        for(int i = 0; i <= 255; i++){
            if(getHopCount(i) <= 2){
                groupNums[i] = num;
                num++;
            }
        }
        return groupNums;
    }

    /**
     * 计算跳变次数
     * @param i
     * @return
     */
    private int getHopCount(int i)
    {
        int[] a = new int[8];
        int cnt = 0;
        int k = 7;
        while(i > 0)
        {
            a[k] = i & 1;
            i = i >> 1;
            --k;
        }
        for(k = 0; k < 7; k++)
        {
            if(a[k] != a[k+1])
            {
                ++cnt;
            }
        }
        if(a[0] != a[7])
        {
            ++cnt;
        }
        return cnt;
    }

    /**
     * 旋转不变性<br>
     * 此处的feature的二进制位数固定为8
     * @param feature
     * @return
     */
    private int getMinFeature(int feature){
        int minFeature = feature;
        for(int i = 0; i < 7; i++){
            // 循环右移一位
            feature = (feature>>1 | feature<<7) & 0xff;
            if(feature < minFeature){ minFeature = feature;}
        }

        return minFeature;
    }

    @Override
    public void extract(BufferedImage srcImg) {
        // 获取灰度矩阵
        int[][] grayMatrix = ImageUtil.getGrayPixel(srcImg, 200, 200);

        // 为特征值(0-255)分组(降维)
        int[] groupNums = groupFeatureValue();

        // 遍历像素点，计算其特征值，并确定其分组，并进行分组统计
        if(grayMatrix == null || grayMatrix.length == 0 || grayMatrix[0].length == 0){ return;}
        int[] vector = new int[59];
        for(int i = 1; i < grayMatrix.length - 1; i++){
            for(int j = 1; j < grayMatrix[0].length - 1; j++){
                int center = grayMatrix[i][j];
                int feature = 0;
                feature = grayMatrix[i - 1][j - 1]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i][j - 1]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i + 1][j - 1]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i + 1][j]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i + 1][j + 1]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i][j + 1]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i - 1][j + 1]>=center ? (feature<<1)+1 : (feature<<1);
                feature = grayMatrix[i - 1][j]>=center ? (feature<<1)+1 : (feature<<1);
                feature = getMinFeature(feature);
                vector[groupNums[feature]]++;
            }
        }

        this.featureVector = vector;
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureVector == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof UniformLBP){
            UniformLBP lbp = (UniformLBP) feature;
            return ImageUtil.calculateSimilarity(this.featureVector, lbp.featureVector);
        }
        else{
            throw new IllegalArgumentException("对比图片的特征对象与源图片的特征对象不是同一个类，无法进行对比");
        }
    }

    @Override
    public String feature2index() {
        if(this.featureVector == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        return Util.vector2string(this.featureVector);
    }

    @Override
    public void index2feature(String index) {
        this.featureVector = Util.string2vector(index);
    }

    @Override
    public String getFeatureName() {
        return UniformLBP.FEATURE_NAME;
    }
}
