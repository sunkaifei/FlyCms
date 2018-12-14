package com.flycms.core.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;

import com.flycms.core.entity.UpImgMsg;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * ImageUtil 图片下载处理
 * 
 * @author sunkaifei
 * 
 */
@Service
public class ImageUtils {

	private Logger logger = Logger.getLogger(this.getClass());

    private static String DEFAULT_CUT_PREVFIX = "cut_";

	public static String getRegexUrl(String url) {
		String regular="(http://|ftp://|https://|www){0,1}(([a-zA-z0-9]|-){1,}\\.){1,}[a-zA-z0-9]{1,}-*";
		Pattern p = Pattern.compile(regular,Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(url);
		String regex = "";
		if (matcher.find()) {
			regex = matcher.group();
			// System.out.println(regex);
		}
		return regex;
	}

	public static String getImgName(String url) {
		String regular="[^\\/]*\\.(jpg|jpeg|gif|png|bmp)\\??[^?]*$";
		Pattern p = Pattern.compile(regular,Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(url);
		String regex = "";
		if (matcher.find()) {
			regex = matcher.group();
		}
		return regex;
	}


	/**
	 * byte数组转换成16进制字符串
	 * 
	 * @param src
	 * @return
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder();
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * 根据文件流读取图片文件真实类型
	 * 
	 * @param is
	 * @return
	 */
	public String getTypeByStream(FileInputStream is) {
		byte[] b = new byte[4];
		try {
			is.read(b, 0, b.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String type = bytesToHexString(b).toUpperCase();
		if (type.contains("FFD8FF")) {
			return "jpg";
		} else if (type.contains("89504E47")) {
			return "png";
		} else if (type.contains("47494638")) {
			return "gif";
		} else if (type.contains("49492A00")) {
			return "tif";
		} else if (type.contains("424D")) {
			return "bmp";
		}
		return type;
	}  
    
    /**
     * <p>Title: cutImage</p>
     * <p>Description:  根据原图与裁切size截取局部图片</p>
     * @param srcImg    源图片
     * @param output    图片输出流
     * @param rect        需要截取部分的坐标和大小
     */
    public void cutImage(File srcImg, OutputStream output, java.awt.Rectangle rect){
        if(srcImg.exists()){
            FileInputStream fis = null;
            ImageInputStream iis = null;
            try {
                fis = new FileInputStream(srcImg);
                // ImageIO 支持的图片类型 : [BMP, bmp, jpg, JPG, wbmp, jpeg, png, PNG, JPEG, WBMP, GIF, gif]
                String types = Arrays.toString(ImageIO.getReaderFormatNames()).replace("]", ",");
                String suffix = null;
                // 获取图片后缀
                if(srcImg.getName().indexOf(".") > -1) {
                    suffix = srcImg.getName().substring(srcImg.getName().lastIndexOf(".") + 1);
                }// 类型和图片后缀全部小写，然后判断后缀是否合法
                if(suffix == null || types.toLowerCase().indexOf(suffix.toLowerCase()+",") < 0){
                	logger.info("Sorry, the image suffix is illegal. the standard image suffix is {}." + types);
                    return ;
                }
                // 将FileInputStream 转换为ImageInputStream
                iis = ImageIO.createImageInputStream(fis);
                // 根据图片类型获取该种类型的ImageReader
                ImageReader reader = ImageIO.getImageReadersBySuffix(suffix).next();
                reader.setInput(iis,true);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceRegion(rect);
                BufferedImage bi = reader.read(0, param);
                ImageIO.write(bi, suffix, output);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(fis != null){ fis.close();}
                    if(iis != null){ iis.close();}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else {
        	logger.info("the src image is not exist.");
        }
    }
    
    public void cutImage(File srcImg, OutputStream output, int x, int y, int width, int height){
        cutImage(srcImg, output, new java.awt.Rectangle(x, y, width, height));
    }
    
    public void cutImage(File srcImg, String destImgPath, java.awt.Rectangle rect){
        File destImg = new File(destImgPath);
        if(destImg.exists()){
            String p = destImg.getPath();
            try {
                if(!destImg.isDirectory()){ p = destImg.getParent();}
                if(!p.endsWith(File.separator)){ p = p + File.separator;}
                cutImage(srcImg, new java.io.FileOutputStream(p + DEFAULT_CUT_PREVFIX + "_" + new java.util.Date().getTime() + "_" + srcImg.getName()), rect);
            } catch (FileNotFoundException e) {
            	logger.info("the dest image is not exist.");
            }
        }else{ logger.info("the dest image folder is not exist.");}
    }
    
    public void cutImage(File srcImg, String destImg, int x, int y, int width, int height){
        cutImage(srcImg, destImg, new java.awt.Rectangle(x, y, width, height));
    }
    
    public void cutImage(String srcImg, String destImg, int x, int y, int width, int height){
        cutImage(new File(srcImg), destImg, new java.awt.Rectangle(x, y, width, height));
    }
    
    /** 
     * 通过读取文件并获取其width及height的方式，来判断判断当前文件是否图片，这是一种非常简单的方式。 
     *  
     * @param imageFile 
     * @return 
     */  
    public static boolean isImage(File imageFile) {  
        if (!imageFile.exists()) {  
            return false;  
        }  
        Image img = null;  
        try {  
            img = ImageIO.read(imageFile);  
            if (img == null || img.getWidth(null) <= 0 || img.getHeight(null) <= 0) {  
                return false;  
            }  
            return true;  
        } catch (Exception e) {  
            return false;  
        } finally {  
            img = null;  
        }  
    }  
    
	/**
	 * 获取图片地址的图片后缀
	 * 
	 * @param url
	 *        网络图片地址
	 * @return
	 */
	public static String getFileExt(String url){
		String regular="\\.(jpg|jpeg|gif|png|bmp)\\??[^?]*$";
		Pattern p =Pattern.compile(regular, Pattern.CASE_INSENSITIVE);  
        Matcher matcher = p.matcher(url);
        String regex ="";
        if(matcher.find()){  
        	regex = matcher.group(0);  
        }
		return regex;  
	}

    /**
     * 图集上传
     * 
     * @param request
     * @param filePath
     * @param filePathUrl
     * @return
     * @throws FileNotFoundException
     */
    public static UpImgMsg uploadFile(HttpServletRequest request, String filePath, String filePathUrl) throws FileNotFoundException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        UpImgMsg msg=new UpImgMsg();
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        String fileName = null;
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile mf = entity.getValue();
            fileName = UUID.randomUUID().toString().replaceAll("-", "")+getFileExt(mf.getOriginalFilename());
            String newfilepath = filePathUrl + File.separatorChar + fileName;

            System.out.println("文件大小=" + mf.getSize()/1024);
            File dest = new File(newfilepath);
            if (!dest.exists()) {
                dest.mkdirs();
            }
            File uploadFile = new File(newfilepath);
            if (uploadFile.exists()) {
                uploadFile.delete();
            }
            try {
                FileCopyUtils.copy(mf.getBytes(), uploadFile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                return null;
            }
            msg.setImgurl(filePath+"/"+fileName);
            msg.setFilesize(mf.getSize()/1024);
            
        }
        return msg;
    }
}
