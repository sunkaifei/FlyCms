package com.flycms.core.utils.lucbir.utils;

/**
 * Created by Administrator on 2016/7/11.
 */
public class LucbirResult {
    private String featureName = null;
    private String imagePath = null;
    private double similarity = 0.0;

    public LucbirResult(String featureName, String imagePath, double similarity){
        this.featureName = featureName;
        this.imagePath = imagePath;
        this.similarity = similarity;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getSimilarity() {
        return similarity;
    }

    public String getFeatureName() {
        return featureName;
    }
}
