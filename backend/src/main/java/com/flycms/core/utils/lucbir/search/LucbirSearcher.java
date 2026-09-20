package com.flycms.core.utils.lucbir.search;

import com.flycms.core.utils.lucbir.features.Feature;
import com.flycms.core.utils.lucbir.index.LucbirIndexer;
import com.flycms.core.utils.lucbir.utils.LucbirResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/7/11.
 */
public class LucbirSearcher {
    /**
     * 存放输入图片的图像特征
     */
    private Feature feature = null;
    /**
     * 图像搜索的索引文件目录<br>
     * 若该目录未设置就需要每次搜索的时候都传入目录参数
     **/
    private String directory = null;

    public LucbirSearcher(Feature feature){
        this.feature = feature;
    }

    public LucbirSearcher(Feature feature, String directory){
        this.feature = feature;
        this.directory = directory;
    }

    public LucbirResult[] searchWithArrayResult(File inputFile, int resultNum){
        LucbirResult[] results = new LucbirResult[0];
        try {
            results = this.search(inputFile, FSDirectory.open(Paths.get(this.directory)));
            LucbirResult[] result = new LucbirResult[resultNum];
            return this.getTopNResult(results, resultNum).toArray(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<LucbirResult> searchWithListResult(File inputFile, int resultNum){
        LucbirResult[] results = new LucbirResult[0];
        try {
            results = this.search(inputFile, FSDirectory.open(Paths.get(this.directory)));
            return this.getTopNResult(results, resultNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LucbirResult[] searchWithArrayResult(File inputFile, int resultNum, String directory){
        LucbirResult[] results = new LucbirResult[0];
        try {
            results = this.search(inputFile, FSDirectory.open(Paths.get(directory)));
            LucbirResult[] result = new LucbirResult[resultNum];
            return this.getTopNResult(results, resultNum).toArray(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<LucbirResult> searchWithListResult(File inputFile, int resultNum, String directory){
        LucbirResult[] results = new LucbirResult[0];
        try {
            results = this.search(inputFile, FSDirectory.open(Paths.get(directory)));
            return this.getTopNResult(results, resultNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private LucbirResult[] search(File inputFile, Directory directory){
        if(inputFile == null){ return null;}
        try {
            // 获取输入图片的图像特征
            BufferedImage bi = ImageIO.read(inputFile);
            this.feature.extract(bi);
            
            // 遍历索引文件中的图像特征，逐一进行对比
            Feature feature2 = this.feature.getClass().newInstance();
            System.out.println(feature2);
            IndexReader reader = DirectoryReader.open(directory);
            //System.out.println("================"+reader);
            LucbirResult[] results = new LucbirResult[reader.numDocs()];
            for(int i = 0; i < reader.numDocs(); i++){
                Document document = reader.document(i);
                if(!document.getField(LucbirIndexer.FEATURE_NAME_FIELD_NAME).stringValue().equalsIgnoreCase(this.feature.getFeatureName())){
                    throw new RuntimeException("提供给搜索器的索引文件与初始化搜索器的 Feature 为不同类型，无法进行比较");
                }
                feature2.index2feature(document.getField(LucbirIndexer.FEATURE_VALUE_FIELD_NAME).stringValue());
                double similarity = this.feature.calculateSimilarity(feature2);
                System.out.println(document.getField(LucbirIndexer.FEATURE_VALUE_FIELD_NAME).stringValue());
                LucbirResult result = new LucbirResult(this.feature.getFeatureName(), document.getField(LucbirIndexer.IMAGE_PATH_FIELD_NAME).stringValue(), similarity);
                results[i] = result;
            }
            return results;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<LucbirResult> getTopNResult(LucbirResult[] results, int resultNum){
        if(results == null || resultNum <= 0){ return null;}
        if(resultNum > results.length){
            resultNum = results.length;
        }
        List<LucbirResult> resultList = new ArrayList<LucbirResult>(resultNum);
        // 执行 resultNum 次堆排序的子过程便可获得 top resultNum
        for(int i = 0; i < resultNum; i++){
            int size = results.length - i;
            // 获取最后一棵子树的父节点下标
            int index = size / 2 - 1;
            for(int j = index; j >= 0; j--){
                int left = 2 * j + 1, right = 2 * j + 2;
                // 比较左右子树的大小
                // 初始左子树较大
                int larger = left;
                // 若存在右子树
                if(right < size){
                    if(results[right].getSimilarity() > results[left].getSimilarity()){
                        larger = right;
                    }
                }

                // 较大子节点与父节点比较
                if(results[larger].getSimilarity() > results[j].getSimilarity()){
                    this.swap(results, larger, j);
                }
            }

            // 执行一次堆排子过程后，将第一个元素进行记录，并将其与最后一个元素置换
            resultList.add(results[0]);
            this.swap(results, 0, size - 1);
        }
        return resultList;
    }

    private void swap(LucbirResult[] results, int i, int j){
        LucbirResult r = results[i];
        results[i] = results[j];
        results[j] = r;
    }
}
