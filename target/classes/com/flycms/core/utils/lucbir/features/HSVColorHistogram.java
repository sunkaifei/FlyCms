package com.flycms.core.utils.lucbir.features;


import com.flycms.core.utils.lucbir.utils.HSV;
import com.flycms.core.utils.lucbir.utils.ImageUtil;
import com.flycms.core.utils.lucbir.utils.Pixel;
import com.flycms.core.utils.lucbir.utils.Util;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * 该颜色直方图更接近于人类对图片的识别程度
 * @author VenyoWang
 *
 * Created by VenyoWang on 2016/7/8.
 */
public class HSVColorHistogram implements Feature{
    public final static String FEATURE_NAME = "hsvch";

    /**
     * 该特征提取算法处理的图像的固定宽高<br>
     * 即在特征提取前，需要先把图像重置为固定尺寸
     **/
    private final static int WIDTH = 200, HEIGHT = 200;

    private int[][] featureMatrix = null;

    @Override
    public void extract(BufferedImage srcImg) {
        // 获取RGB矩阵
        Pixel[][] matrix = ImageUtil.getImagePixel(srcImg, HSVColorHistogram.WIDTH, HSVColorHistogram.HEIGHT);

        // 转化为HSV矩阵
        if(matrix == null || matrix.length == 0 || matrix[0].length == 0){ return;}
        HSV[][] hsvMatrix = new HSV[matrix.length][];
        for(int i = 0; i < matrix.length; i++){
            hsvMatrix[i] = new HSV[matrix[i].length];
            for(int j = 0; j < matrix[i].length; j++){
                float[] fs = Color.RGBtoHSB(matrix[i][j].red, matrix[i][j].green, matrix[i][j].blue, null);
                HSV hsv = new HSV();
                hsv.h = (int)(fs[0] * 255);
                hsv.s = (int)(fs[1] * 255);
                hsv.v = (int)(fs[2] * 255);
                hsvMatrix[i][j] = hsv;
            }
        }

        // 统计
        int[][] histogram = new int[3][256];
        for(int i = 0; i < hsvMatrix.length; i++){
            for(int j = 0; j < hsvMatrix[0].length; j++){
                histogram[0][hsvMatrix[i][j].h]++;
                histogram[1][hsvMatrix[i][j].s]++;
                histogram[2][hsvMatrix[i][j].v]++;
            }
        }
        this.featureMatrix = histogram;
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureMatrix == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof HSVColorHistogram){
            HSVColorHistogram histogram = (HSVColorHistogram) feature;
            return ImageUtil.calculateSimilarity(this.featureMatrix, histogram.featureMatrix);
        }
        else{
            throw new IllegalArgumentException("对比图片的特征对象与源图片的特征对象不是同一个类，无法进行对比");
        }
    }

    @Override
    public String feature2index() {
        if(this.featureMatrix == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        return Util.matrix2string(this.featureMatrix);
    }

    @Override
    public void index2feature(String index) {

    }

    @Override
    public String getFeatureName() {
        return HSVColorHistogram.FEATURE_NAME;
    }
}
