package com.flycms.core.utils.lucbir.index;

import com.flycms.core.utils.lucbir.features.Feature;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by Administrator on 2016/7/11.
 */
public class LucbirIndexer {
    /**
     * 在此仅作为特征提取工具<br>
     * 不作为一个特征对象看待
     * */
    private Feature feature = null;
    private Analyzer analyzer = null;
    private IndexWriterConfig config = null;

    public final static String FEATURE_NAME_FIELD_NAME = "feature_name";
    public final static String FEATURE_VALUE_FIELD_NAME = "feature_value";
    public final static String IMAGE_PATH_FIELD_NAME = "image_path";

    public LucbirIndexer(Feature feature){
        this.feature = feature;
        this.analyzer = new StandardAnalyzer();
        this.config = new IndexWriterConfig(this.analyzer);
    }

    /**
     *
     * @param indexDirectory 用于存放索引文件的目录
     * @param pictureDirectory 要进行图像索引的图片对象所在的目录
     */
    public void generateIndex(String indexDirectory, String pictureDirectory){
        if(indexDirectory == null || indexDirectory.length() == 0 || pictureDirectory == null || pictureDirectory.length() == 0){
            return;
        }
        else {
            try {
                IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(indexDirectory)), this.config);
                iterateFile(writer, new File(pictureDirectory));
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * 遍历所有文件，将根据文件生成的图像特征写入 writer 中
     * @param writer
     * @param file
     */
    private void iterateFile(IndexWriter writer, File file){
        if(file.isFile()){
            try {
                BufferedImage bi = ImageIO.read(file);
                this.feature.extract(bi);
                String featureValue = this.feature.feature2index();
                Document document = new Document();
                document.add(new StringField(LucbirIndexer.FEATURE_NAME_FIELD_NAME, this.feature.getFeatureName(), Field.Store.YES));
                document.add(new StringField(LucbirIndexer.FEATURE_VALUE_FIELD_NAME, featureValue, Field.Store.YES));
                System.out.println(featureValue);
                document.add(new StringField(LucbirIndexer.IMAGE_PATH_FIELD_NAME, file.getAbsolutePath(), Field.Store.YES));
                writer.addDocument(document);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            for(File f : file.listFiles()){
                iterateFile(writer, f);
            }
        }
    }
    
	public String iterateFile(File file){
		String featureValue = null;
		if(file.isFile()){
            try {
                BufferedImage bi = ImageIO.read(file);
                this.feature.extract(bi);
                featureValue = this.feature.feature2index();
                //System.out.println(featureValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		return featureValue;
    }
}
