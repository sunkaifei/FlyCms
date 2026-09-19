package com.flycms.core.utils.lucbir.features;

import com.flycms.core.utils.lucbir.utils.Coordinate;
import com.flycms.core.utils.lucbir.utils.ImageUtil;
import com.flycms.core.utils.lucbir.utils.Util;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 环形颜色分布直方图<br>
 * 参考文献：<br>
 * http://wenku.baidu.com/link?url=bWfVkM-oyUn7cJuCnhICReeByt2XR-MUx07J-
 * 1pXvBz7UKzoe4iGmH4S-8j4MiuAXyzetBV7NEDwJC7BBjT8ecCpHvo7oSBNChO0gLJMhI7<br>
 * http://wenku.baidu.com/link?url=s13-4JCwPWfHsnv1EKXcScLNs06-NEN2gBG-
 * oFpKL4VvFOy1r5lznMJg9rQ2dWvbzXiSoEOO_Oge0THJZF6nEedJG5hJtAGZm-cyNqSoEZW<br>
 *
 * @author VenyoWang
 *
 * Created by VenyoWang on 2016/7/8.
 */
public class AnnularColorLayoutHistogram implements Feature{
    public final static String FEATURE_NAME = "aclh";

    /** 同心圆个数 */
    private final static int N = 10;
    /**
     * 该特征提取算法处理的图像的固定宽高<br>
     * 即在特征提取前，需要先把图像重置为固定尺寸
     **/
    private final static int WIDTH = 200, HEIGHT = 200;

    private int[][] featureMatrix = null;

    @Override
    public void extract(BufferedImage srcImg) {
        // 获取灰度矩阵
        int[][] matrix = ImageUtil.getGrayPixel(srcImg, AnnularColorLayoutHistogram.WIDTH, AnnularColorLayoutHistogram.HEIGHT);

        // 根据灰度值对像素点进行分组
        if(matrix == null || matrix.length == 0 || matrix[0].length == 0){ return;}
        Map<Integer, List<Coordinate>> groupedPixels = new HashMap<Integer, List<Coordinate>>();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                Coordinate coordinate = new Coordinate();
                coordinate.x = i;
                coordinate.y = j;
                List<Coordinate> list = null;
                if (groupedPixels.containsKey(matrix[i][j])) {
                    list = groupedPixels.get(matrix[i][j]);
                    list.add(coordinate);
                } else {
                    list = new ArrayList<Coordinate>();
                    list.add(coordinate);
                    groupedPixels.put(matrix[i][j], list);
                }
            }
        }

        // 为不同的灰度值计算质心
        Coordinate[] centroid = new Coordinate[256];
        for (int i = 0; i <= 255; i++) {
            if (groupedPixels.containsKey(i)) {
                List<Coordinate> list = groupedPixels.get(i);
                double x = 0, y = 0;
                for (int j = 0; j < list.size(); j++) {
                    Coordinate coordinate = list.get(j);
                    x += coordinate.x;
                    y += coordinate.y;
                }
                x = x / list.size();
                y = y / list.size();
                Coordinate coordinate = new Coordinate();
                coordinate.x = x;
                coordinate.y = y;
                centroid[i] = coordinate;
            }
        }

        // 为每一个像素计算其到质心的距离
        double[][] distances = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                // 获取与(i,j)像素点拥有相同像素的质心
                Coordinate coordinate = centroid[matrix[i][j]];
                distances[i][j] = Math.sqrt(Math.pow(i - coordinate.x, 2) + Math.pow(j - coordinate.y, 2));
            }
        }

        // 比较出最大距离
        double[] maxDistances = new double[256];
        for (int i = 0; i <= 255; i++) {
            if (groupedPixels.containsKey(i)) {
                List<Coordinate> list = groupedPixels.get(i);
                double max = 0;
                for (int j = 0; j < list.size(); j++) {
                    Coordinate coordinate = list.get(j);
                    double distance = distances[(int) coordinate.x][(int) coordinate.y];
                    if (distance > max) {
                        max = distance;
                    }
                }
                maxDistances[i] = max;
            }
        }

        // 统计以不同距离为半径的同心圆内包含的像素数量
        int[][] nums = new int[256][AnnularColorLayoutHistogram.N];
        for (int i = 0; i <= 255; i++) {
            for (int j = 1; j <= AnnularColorLayoutHistogram.N; j++) {
                double minDis = maxDistances[i] * (j - 1) / AnnularColorLayoutHistogram.N;
                double maxDis = maxDistances[i] * j / AnnularColorLayoutHistogram.N;
                // 第一个同心圆的取值范围必须为[0, maxDis * j / n]
                // 必须包含0，因为有可能存在像素点和质心重叠的情况
                if (j == 1) {
                    minDis = -1;
                }
                if(groupedPixels.containsKey(i)){
                    List<Coordinate> list = groupedPixels.get(i);
                    int num = 0;
                    for (int k = 0; k < list.size(); k++) {
                        Coordinate coordinate = list.get(k);
                        double dis = distances[(int) coordinate.x][(int) coordinate.y];
                        if (dis > minDis && dis <= maxDis) {
                            num++;
                        }
                    }
                    nums[i][j - 1] = num;
                }
            }
        }

        this.featureMatrix = nums;
    }

    @Override
    public double calculateSimilarity(Feature feature) {
        if(this.featureMatrix == null){
            throw new RuntimeException("该对象还未提取图像的特征值，请先调用extract方法提取图像的特征值");
        }
        if(feature instanceof AnnularColorLayoutHistogram){
            AnnularColorLayoutHistogram histogram = (AnnularColorLayoutHistogram) feature;
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
        this.featureMatrix = Util.string2matrix(index, 256, AnnularColorLayoutHistogram.N);
    }

    @Override
    public String getFeatureName() {
        return AnnularColorLayoutHistogram.FEATURE_NAME;
    }
}
