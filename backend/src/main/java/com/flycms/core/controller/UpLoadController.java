package com.flycms.core.controller;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import com.flycms.core.utils.ImageUtils;
import com.flycms.core.utils.UploadSafeUtil;
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
    private static Logger logger = LoggerFactory.getLogger(UpLoadController.class);
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
            // 阶段 A5：扩展名 + MIME + 文件头魔数三重白名单，通过后强制重命名（丢弃原始文件名）
            UploadSafeUtil.ImageType type = UploadSafeUtil.safeImage(file, UploadSafeUtil.MAX_IMAGE_BYTES);
            if (type == null) {
                map.put("errno", 1);
                map.put("desc", "文件不是合法图片（仅支持 .jpg/.png/.gif/.bmp/.webp，且不得超过 2MB）");
                return map;
            }
            String fileName = UploadSafeUtil.rename(type);
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
            // 阶段 A5：三重白名单校验 + 强制重命名
            UploadSafeUtil.ImageType type = UploadSafeUtil.safeImage(file, UploadSafeUtil.MAX_IMAGE_BYTES);
            if (type == null) {
                map.put("error", 1);
                map.put("message", "文件不是合法图片（仅支持 .jpg/.png/.gif/.bmp/.webp，且不得超过 2MB）");
                return map;
            }
            String fileName = UploadSafeUtil.rename(type);
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
            if(file == null || file.getImgurl()==null){
                msg.setCode(-1);
                msg.setImgurl(null);
                msg.setFilesize(file == null ? 0 : file.getFilesize());
                String failMsg = (file == null || file.getMsg() == null) ? "上传失败" : file.getMsg();
                msg.setMsg(failMsg);
                // 历史 Bug：此处曾返回 DataVo.success("上传失败")，状态码与文案矛盾，
                // 前端无法据此判断成败，现统一按失败返回。
                return DataVo.failure(failMsg, msg);
            }else{
                msg.setCode(0);
                msg.setImgurl("/"+file.getImgurl());
                msg.setFilesize(file.getFilesize());
                msg.setMsg("上传成功");
                return DataVo.success("上传成功", msg);
            }
        } catch (FileNotFoundException e) {
            logger.error("上传文件未找到", e);
        } catch(Exception ex){
            logger.error("上传文件失败", ex);
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
            // 阶段 A5：三重白名单校验 + 强制重命名
            UploadSafeUtil.ImageType type = UploadSafeUtil.safeImage(file, UploadSafeUtil.MAX_IMAGE_BYTES);
            if (type == null) {
                return CkeditorUp.failure("文件不是合法图片（仅支持 .jpg/.png/.gif/.bmp/.webp，且不得超过 2MB）");
            }
            String fileName = UploadSafeUtil.rename(type);
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