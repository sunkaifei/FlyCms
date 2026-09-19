package com.flycms.core.utils.lucbir.utils;

/**
 * Created by VenyoWang on 2016/7/8.
 */
public class Util {
    public static int[] matrix2vector(int[][] matrix){
        if(matrix == null || matrix.length <= 0 || matrix[0].length <= 0){
            return null;
        }
        int[] vector = new int[matrix.length * matrix[0].length];
        int index = 0;
        for(int i = 0; i < matrix.length; i++){
            for(int j = 0; j < matrix[0].length; j++, index++){
                vector[index] = matrix[i][j];
            }
        }
        return vector;
    }

    public static boolean isStringEmpty(String str){
        return str == null || str.length() == 0;
    }

    public static String matrix2string(int[][] matrix){
        if(matrix == null || matrix.length == 0 || matrix[0].length == 0){ return null;}
        String str = String.valueOf(matrix[0][0]);
        for(int i = 0; i < matrix.length; i++){
            for(int j = 0; j < matrix[0].length; j++){
                if(i == 0 && j == 0){ continue;}
                str += " " + matrix[i][j];
            }
        }
        return str;
    }

    public static int[][] string2matrix(String str, int rows, int columns){
        if(isStringEmpty(str)){ return null;}
        String[] strs = str.split(" ");
        int[][] matrix = new int[rows][columns];
        int index = 0;
        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++, index++){
                matrix[i][j] = Integer.parseInt(strs[index]);
            }
        }
        return matrix;
    }

    public static String vector2string(int[] vector){
        if(vector == null || vector.length == 0){ return null;}
        String str = String.valueOf(vector[0]);
        for(int i = 1; i < vector.length; i++){
            str += " " + vector[i];
        }
        return str;
    }

    public static int[] string2vector(String str){
        if(isStringEmpty(str)){ return null;}
        String[] strs = str.split(" ");
        int[] vector = new int[strs.length];
        for(int i = 0; i < strs.length; i++){
            vector[i] = Integer.parseInt(strs[i]);
        }
        return vector;
    }
}
