package com.flycms.core.utils.lucbir.features;

import com.flycms.core.utils.lucbir.utils.ImageUtil;
import com.flycms.core.utils.lucbir.utils.Util;

import java.awt.image.BufferedImage;

/**
 * 颜色聚合向量<br>
 * 参考链接：http://www.docin.com/p-396527256.html
 *
 * @author VenyoWang
 *
 * Created by VenyoWang on 2016/7/8.
 */
public class ColorCoherenceVector implements Feature{
    public final static String FEATURE_NAME = "ccv";

    private static int BIN_WIDTH = 4;
    /**
     * 该特征提取算法处理的图像的固定宽高<br>
     * 即在特征提取前，需要先把图像重置为固定尺寸
     **/
    private final static int WIDTH = 200, HEIGHT = 200;

    private int[][] featureMatrix = null;

    /**
     * 对矩阵进行分组
     * @param matrix
     * @param groupNums
     * @return 返回总共的组别数量
     */
    private int groupMatrix(int[][] matrix, int[][] groupNums) {
        if(matrix == null || matrix.length == 0 || matrix[0].length == 0){ return 0;}
        if(groupNums == null || groupNums.length == 0 || groupNums[0].length == 0){ return 0;}
        // 初始化，标为-1表示未进行分组的像素
        for(int i = 0; i < groupNums.length; i++){
            for(int j = 0; j < groupNums[0].length; j++){
                groupNums[i][j] = -1;
            }
        }

        int groupNum = 0;
        for(int i = 0; i < groupNums.length; i++){
            for(int j = 0; j < groupNums[0].length; j++){
                if(groupNums[i][j] < 0){
                    // 该像素点未进行分组，对其进行分组
                    groupNums[i][j] = groupNum;
                    recursive(matrix, i, j, groupNum, groupNums);
                    groupNum++;
                }
            }
        }
        return groupNum + 1;
    }

    /**
     * 递归查找与当前像素点拥有相同像素值的点，并对其进行分组
     * @param matrix
     * @param i
     * @param j
     * @param groupNum
     * @param groupNums
     */
    private void recursive(int[][] matrix, int i, int j, int groupNum, int[][] groupNums){
        if(matrix == null || matrix.length == 0 || matrix[0].length == 0){ return;}
        if(groupNums == null || groupNums.length == 0 || groupNums[0].length == 0){ return;}
        int num = matrix[i][j];
        int x = i - 1, y = j - 1;
        int maxX = matrix.length, maxY = matrix[0].length;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        y = j;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        y = j + 1;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        x = i;y = j - 1;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        y = j + 1;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        x = i + 1;y = j - 1;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        y = j;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
        y = j + 1;
        if(x >= 0 && y >= 0 && x < maxX && y < maxY && groupNums[x][y] < 0 && matrix[x][y] == num){
            groupNums[x][y] = groupNum;
            recursive(matrix, x, y, groupNum, groupNums);
        }
    }

    @Override
    public void extract(BufferedImage srcImg) {
        // 均匀量化
        int[][] grayMatrix = ImageUtil.getGrayPixel(srcImg, ColorCoherenceVector.WIDTH, ColorCoherenceVector.HEIGHT);
        if(grayMatrix == null || grayMatrix.length == 0 || grayMatrix[0].length == 0){ return;}
        int width = grayMatrix[0].length;
        int height = grayMatrix.length;
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                grayMatrix[i][j] /= ColorCoherenceVector.BIN_WIDTH;
            }
        }

        // 划分连通区域
        int[][] groupNums = new int[grayMatrix.length][grayMatrix[0].length];
        int groupNum = groupMatrix(grayMatrix, groupNums);

        // 判断聚合性
        // 统计每个分组下的像素数
        int[] groupCount = new int[groupNum];
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                groupCount[groupNums[i][j]]++;
            }
        }

        // 阈值
        int threshold = width * height / 100;
        for(int i = 0; i < groupNum; i++){
            if(groupCount[i] < threshold){
                // 0表示非聚合
                groupCount[i] = 0;
            }
            else{
                // 1表示聚合
                groupCount[i] = 1;
            }
        }

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                groupNums[i][j] = groupCount[groupNums[i][j]];
            }
        }

        // 计算图像特征
        int[][] feature = new int[256 / ColorCoherenceVector.BIN_WIDTH][2];
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                if(groupNums[i][j] == 0){
                    feature[grayMatrix[i][j] / ColorCoherenceVector.BIN_WIDTH][0]++;
                }
                else {
                    feature[grayMatrix[i][j] / ColorCoherenceVector.BIN_WIDTH][1]++;
                }
            }
        }

        this.featureMatrix = feature;
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureMatrix == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof ColorCoherenceVector){
            ColorCoherenceVector histogram = (ColorCoherenceVector) feature;
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
        this.featureMatrix = Util.string2matrix(index, 256 / ColorCoherenceVector.BIN_WIDTH, 2);
    }

    @Override
    public String getFeatureName() {
        return ColorCoherenceVector.FEATURE_NAME;
    }
}
