package com.flycms.core.utils.lucbir.features;

import com.flycms.core.utils.lucbir.utils.ImageUtil;
import com.flycms.core.utils.lucbir.utils.Pixel;
import com.flycms.core.utils.lucbir.utils.Util;

import java.awt.image.BufferedImage;


/**
 * Created by VenyoWang on 2016/7/8.
 */
public class RGBColorHistogram implements Feature{
    public final static String FEATURE_NAME = "rgbch";

    private int[][] featureMatrix = null;

    @Override
    public void extract(BufferedImage srcImg) {
        Pixel[][] matrix = ImageUtil.getImagePixel(srcImg, 200, 200);
        if(matrix == null || matrix.length == 0 || matrix[0].length == 0){ return;}
        int[][] histogram = new int[3][256];
        for(int i = 0; i < matrix.length; i++){
            for(int j = 0; j < matrix[0].length; j++){
                histogram[0][matrix[i][j].red]++;
                histogram[1][matrix[i][j].green]++;
                histogram[2][matrix[i][j].blue]++;
            }
        }
        this.featureMatrix = histogram;
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureMatrix == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof RGBColorHistogram){
            RGBColorHistogram histogram = (RGBColorHistogram) feature;
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
        this.featureMatrix = Util.string2matrix(index, 3, 256);
    }

    @Override
    public String getFeatureName() {
        return RGBColorHistogram.FEATURE_NAME;
    }
}
