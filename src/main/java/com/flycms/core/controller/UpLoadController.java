package com.flycms.core.controller;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.flycms.core.utils.ImageUtils;
import com.flycms.constant.Const;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.CkeditorUp;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.UpImgMsg;
import com.flycms.module.question.service.ImagesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * 图片上传Controller
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年5月25日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email admin@97560.com
 * @version 1.0,2017年7月25日 <br/>
 * 
 */
@Controller
public class UpLoadController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(CaptchaController.class);
    @Autowired
    private ImagesService imagesService;

	/*
	 * 图片命名格式
	 */
	private static final String DEFAULT_SUB_FOLDER_FORMAT_AUTO = "yyyyMMddHHmmss";

	/*
	 * 上传图片文件夹
	 */
	private static final String UPLOAD_PATH = "/upload/usertmp/";

	/*
	 * wangEditor上传图片
	 */
    @ResponseBody
    @RequestMapping("/ucenter/upload")
    public Map<String, Object> singleFileUpload(@RequestParam("file") MultipartFile file)throws Exception, IOException{
        Map<String, Object> map = new HashMap<>();
        if (!file.isEmpty()) {
            String proName = Const.UPLOAD_PATH;
            String path = proName + "/upload/usertmp/"+getUser().getUserId()+"/";
            String fileName = file.getOriginalFilename();
            String uploadContentType = file.getContentType();
            String expandedName = "";
            if ("image/jpeg".equals(uploadContentType)
                    || uploadContentType.equals("image/jpeg")) {
                // IE6上传jpg图片的headimageContentType是image/pjpeg，而IE9以及火狐上传的jpg图片是image/jpeg
                expandedName = ".jpg";
            } else if ("image/png".equals(uploadContentType) || "image/x-png".equals(uploadContentType)) {
                // IE6上传的png图片的headimageContentType是"image/x-png"
                expandedName = ".png";
            } else if ("image/gif".equals(uploadContentType)) {
                expandedName = ".gif";
            } else if ("image/bmp".equals(uploadContentType)) {
                expandedName = ".bmp";
            } else {
                map.put("errno", 1);
                map.put("desc", "文件格式不正确（必须为.jpg/.gif/.bmp/.png文件）");
                return map;
            }
            if (file.getSize() > 1024 * 1024 * 2) {
                map.put("errno", 1);
                map.put("desc", "文件大小不得大于2M");
                return map;
            }

            DateFormat df = new SimpleDateFormat(DEFAULT_SUB_FOLDER_FORMAT_AUTO);
            fileName = df.format(new Date()) + expandedName;
            File dirFile = new File(path + fileName);
            //判断文件父目录是否存在
            if(!dirFile.getParentFile().exists()){
                dirFile.getParentFile().mkdir();
            }
            imagesService.uploadFile(file.getBytes(), path, fileName);
            int port=request.getServerPort();
            String portstr="";
            if(port>0){
                portstr+=":"+port;
            }
            map.put("errno", 0);
            map.put("data", Arrays.asList("http://"+ request.getServerName()+portstr+"/upload/usertmp/"+getUser().getUserId() + "/" + fileName));
        } else {
            map.put("errno", 1);
            map.put("desc", "请选择图片");
        }
        return map;
	}

    /*
     * KindEditor编辑器上传图片接口
     */
    @ResponseBody
    @RequestMapping("/ucenter/kindEditorUpload")
    public Map<String, Object> kindEditorFileUpload(@RequestParam("imgFile") MultipartFile file)throws Exception, IOException{
        Map<String, Object> map = new HashMap<>();
        if (!file.isEmpty()) {
            String proName = Const.UPLOAD_PATH;
            String path = proName + "/upload/usertmp/"+getUser().getUserId()+"/";
            String fileName = file.getOriginalFilename();
            String uploadContentType = file.getContentType();
            String expandedName = "";
            if ("image/jpeg".equals(uploadContentType)
                    || uploadContentType.equals("image/jpeg")) {
                // IE6上传jpg图片的headimageContentType是image/pjpeg，而IE9以及火狐上传的jpg图片是image/jpeg
                expandedName = ".jpg";
            } else if ("image/png".equals(uploadContentType) || "image/x-png".equals(uploadContentType)) {
                // IE6上传的png图片的headimageContentType是"image/x-png"
                expandedName = ".png";
            } else if ("image/gif".equals(uploadContentType)) {
                expandedName = ".gif";
            } else if ("image/bmp".equals(uploadContentType)) {
                expandedName = ".bmp";
            } else {
                map.put("errno", 1);
                map.put("message", "文件格式不正确（必须为.jpg/.gif/.bmp/.png文件）");
                return map;
            }
            if (file.getSize() > 1024 * 1024 * 2) {
                map.put("errno", 1);
                map.put("message", "文件大小不得大于2M");
                return map;
            }

            DateFormat df = new SimpleDateFormat(DEFAULT_SUB_FOLDER_FORMAT_AUTO);
            fileName = df.format(new Date()) + expandedName;
            File dirFile = new File(path + fileName);
            //判断文件父目录是否存在
            if(!dirFile.getParentFile().exists()){
                dirFile.getParentFile().mkdir();
            }
            imagesService.uploadFile(file.getBytes(), path, fileName);
            int port=request.getServerPort();
            String portstr="";
            if(port>0){
                portstr+=":"+port;
            }
            map.put("error", 0);
            map.put("url", "http://"+ request.getServerName()+portstr+"/upload/usertmp/"+getUser().getUserId() + "/" + fileName);
        } else {
            map.put("error", 1);
            map.put("message", "请选择图片");
        }
        return map;
    }
	
	@ResponseBody
	@PutMapping(value = "/system/upload")
    public DataVo uploadFile(HttpServletRequest request, ModelMap modelMap){
		DataVo data = DataVo.failure("操作失败");
		UpImgMsg msg=new UpImgMsg();
		String filePath = UPLOAD_PATH;
        String filePathUrl="./uploadfiles"+filePath;
        try {
        	UpImgMsg file = ImageUtils.uploadFile(request, filePath,filePathUrl);
            if(file.getImgurl()==null){
            	msg.setCode(-1);
            	msg.setImgurl(null);
            	msg.setFilesize(file.getFilesize());
            	msg.setMsg("上传失败");
            	return DataVo.success("上传失败", msg);
            }else{
            	msg.setCode(0);
            	msg.setImgurl("/"+file.getImgurl());
            	msg.setFilesize(file.getFilesize());
            	msg.setMsg("上传成功");
            	return DataVo.success("上传成功", msg);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(Exception ex){
            ex.printStackTrace();
        }
        return data;
    }

    /**
     * 上传图片
     * @param file
     */
    @RequestMapping(value = "/ucenter/uploadImage", method = RequestMethod.POST)
    @ResponseBody
    public CkeditorUp uploadImage(@RequestParam("upload") MultipartFile file)throws Exception {
        if (!file.isEmpty()) {
            String proName = Const.UPLOAD_PATH;
            String path = proName + "/upload/usertmp/"+getUser().getUserId()+"/";
            String fileName = file.getOriginalFilename();
            String uploadContentType = file.getContentType();
            String expandedName = "";
            if ("image/jpeg".equals(uploadContentType)
                    || uploadContentType.equals("image/jpeg")) {
                // IE6上传jpg图片的headimageContentType是image/pjpeg，而IE9以及火狐上传的jpg图片是image/jpeg
                expandedName = ".jpg";
            } else if ("image/png".equals(uploadContentType) || "image/x-png".equals(uploadContentType)) {
                // IE6上传的png图片的headimageContentType是"image/x-png"
                expandedName = ".png";
            } else if ("image/gif".equals(uploadContentType)) {
                expandedName = ".gif";
            } else if ("image/bmp".equals(uploadContentType)) {
                expandedName = ".bmp";
            } else {
                return CkeditorUp.failure("文件格式不正确（必须为.jpg/.gif/.bmp/.png文件）");
            }
            if (file.getSize() > 1024 * 1024 * 2) {
                return CkeditorUp.failure("文件大小不得大于2M");
            }

            DateFormat df = new SimpleDateFormat(DEFAULT_SUB_FOLDER_FORMAT_AUTO);
            fileName = df.format(new Date()) + expandedName;
            File dirFile = new File(path + fileName);
            //判断文件父目录是否存在
            if(!dirFile.getParentFile().exists()){
                dirFile.getParentFile().mkdir();
            }
            imagesService.uploadFile(file.getBytes(), path, fileName);
            int port=request.getServerPort();
            String portstr="";
            if(port>0){
                portstr+=":"+port;
            }
            return CkeditorUp.success(1,fileName,"http://"+ request.getServerName()+portstr+"/upload/usertmp/"+getUser().getUserId() + "/" + fileName);
        } else {
            return CkeditorUp.failure("上传失败");
        }
    }
}