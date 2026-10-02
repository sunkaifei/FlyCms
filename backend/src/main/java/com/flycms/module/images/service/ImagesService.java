package com.flycms.module.images.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.flycms.core.utils.*;
import com.flycms.constant.Const;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.images.dao.ImagesDao;
import com.flycms.module.images.model.Images;
import com.flycms.module.images.model.ImagesInfoMerge;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 图片管理服务
 *
 * @author sunkaifei
 *
 */
@Service
public class ImagesService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private UserService userService;

	@Autowired
	private ImagesDao imagesDao;

	// ///////////////////////////////
	// ///// 增加 ////////
	// ///////////////////////////////
	/**
	 * 保存内容中的图片本地化路径处理
	 *
	 * @param typeId
	 *         信息类型，0问题，1答案，2文章，3分享
	 * @param infoId
	 *         信息id
	 * @param userId
	 *         用户id
	 * @param content
	 *         需要分析处理并下载的内容
	 * @return
	 * @throws Exception
	 */
	// ///////////////////////////////
	// /////  附件库（阶段 B2）  /////
	// ///////////////////////////////

	/**
	 * 附件库分页列表
	 *
	 * @param keyword    文件名/路径模糊匹配
	 * @param onlyOrphan true=只看孤儿（引用计数为 0 或已标记删除）
	 */
	public PageVo<Images> getImagesLibraryPage(String keyword, Boolean onlyOrphan, int pageNum, int rows) {
		PageVo<Images> pageVo = new PageVo<Images>(pageNum);
		pageVo.setRows(rows);
		pageVo.setList(imagesDao.getImagesLibraryList(keyword, onlyOrphan, pageVo.getOffset(), pageVo.getRows()));
		pageVo.setCount(imagesDao.getImagesLibraryCount(keyword, onlyOrphan));
		return pageVo;
	}

	/**
	 * 管理端直传入库（W 批次字段控件层）：只插 fly_images 行，info_count=0 孤儿态，
	 * 引用计数由内容保存链路（incrImagesRefCount）接管。
	 */
	public void addAdminUpload(Images images) {
		imagesDao.addImages(images);
	}

	// /////////////////// Q1 媒体库多尺寸 ///////////////////

	/** 多尺寸规格（从小到大；只缩不放——原图小于目标宽则跳过该档） */
	private static final int[][] SIZE_SPECS = {{150, 150}, {320, 320}, {768, 768}};
	private static final String[] SIZE_NAMES = {"thumb", "mid", "large"};

	/**
	 * 为已落盘的原图生成多尺寸副本并回填 fly_images.sizes（JSON 数组，从小到大）。
	 * 保持原格式（png 保透明/gif 首帧/jpg），副本命名 原名_{宽}.{ext}，与原图同目录。
	 *
	 * @return sizes JSON 字符串（无可生成档位时返回 null）
	 */
	public String generateMultiSizes(Images images) {
		try {
			String imgUrl = images.getImgUrl();
			if (imgUrl == null || imgUrl.isBlank()) {
				return null;
			}
			// imgUrl 形如 /upload/admin/20261002/x.png → 磁盘 ./uploadfiles/upload/admin/20261002/x.png
			String diskPath = "uploadfiles" + imgUrl.substring(imgUrl.indexOf('/'));
			java.io.File original = new java.io.File(diskPath);
			if (!original.exists()) {
				return null;
			}
			String ext = imgUrl.substring(imgUrl.lastIndexOf('.') + 1).toLowerCase();
			String format = ext.equals("jpg") ? "jpg" : ext; // ImageIO 规范名
			java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(original);
			if (src == null) {
				return null;
			}
			int srcW = src.getWidth(), srcH = src.getHeight();
			if (srcW <= 0 || srcH <= 0) {
				return null;
			}
			com.alibaba.fastjson2.JSONArray arr = new com.alibaba.fastjson2.JSONArray();
			for (int i = 0; i < SIZE_SPECS.length; i++) {
				int targetW = SIZE_SPECS[i][0];
				int targetH = Math.max(1, srcH * targetW / srcW);
				if (targetW >= srcW) {
					continue; // 只缩不放
				}
				String suffix = "_" + targetW;
				String diskName = original.getName().replaceFirst("\\.[A-Za-z0-9]+$", suffix + "." + ext);
				java.io.File out = new java.io.File(original.getParent(), diskName);
				java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
						targetW, targetH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
				java.awt.Graphics2D g = scaled.createGraphics();
				g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
						java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(src, 0, 0, targetW, targetH, null);
				g.dispose();
				javax.imageio.ImageIO.write(scaled, format, out);
				JSONObject item = new com.alibaba.fastjson2.JSONObject(new LinkedHashMap<String, Object>());
				item.put("n", SIZE_NAMES[i]);
				item.put("u", imgUrl.replaceFirst("\\.[A-Za-z0-9]+$", suffix + "." + ext));
				item.put("w", targetW);
				item.put("h", targetH);
				arr.add(item);
			}
			if (arr.isEmpty()) {
				return null;
			}
			String json = arr.toJSONString();
			images.setSizes(json);
			imagesDao.updateSizesById(images.getId(), json);
			return json;
		} catch (Exception e) {
			logger.warn("多尺寸生成失败（{}）：{}", images.getImgUrl(), e.getMessage());
			return null;
		}
	}

	/** 批量 id → {id,imgUrl,imgName} 映射（控件回显，修「刷新后缩略图丢失」） */
	public List<Map<String, Object>> findByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return new ArrayList<>();
		}
		return imagesDao.findByIds(ids);
	}

	/**
	 * 清理孤儿附件（无任何内容引用）。
	 * ids 为空时清理全部孤儿；否则只清理传入 id 中确认为孤儿的那些。
	 *
	 * @return 删除条数
	 */
	public int deleteOrphanImages(List<Long> ids) {
		List<Long> targets;
		if (ids == null || ids.isEmpty()) {
			// 清理全部孤儿：先查出孤儿 id 再删，避免误删有引用的记录
			PageVo<Images> all = getImagesLibraryPage(null, true, 1, Integer.MAX_VALUE / 4);
			targets = new ArrayList<Long>();
			if (all.getList() != null) {
				for (Images img : all.getList()) {
					targets.add(img.getId());
				}
			}
		} else {
			// 只删传入集合中确认为孤儿的
			List<Images> exists = imagesDao.getImagesByIds(ids);
			targets = new ArrayList<Long>();
			for (Images img : exists) {
				Integer count = img.getInfoCount();
				if (count == null || count <= 0) {
					targets.add(img.getId());
				}
			}
		}
		if (targets.isEmpty()) {
			return 0;
		}
		return imagesDao.deleteImagesByIds(targets);
	}

	/** 孤儿附件数量 */
	public int countOrphanImages() {
		return imagesDao.countOrphanImages();
	}

	public String replaceContent(Integer typeId,Long infoId,Long userId,String content) throws Exception {
		SnowFlake snowFlake = SnowFlake.getInstance();
		Pattern pRemoteFileurl = Pattern.compile("<img.*?src=\"?(.*?)(\"|>|\\s+)");
		Matcher mRemoteFileurl = pRemoteFileurl.matcher(content);
		StringBuffer sb = new StringBuffer();
		String remoteFileurl = null;
		int nFileNum = 0;
		String imgpath = getImgPath();
		StringBuffer imgBuffer = new StringBuffer();
		while (mRemoteFileurl.find()) {
			remoteFileurl = mRemoteFileurl.group(1);
			String extension = StringHelperUtils.getImageUrlSuffix(remoteFileurl);
			extension = "." + extension;
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
			String filename = Md5Utils.code(df.format(new Date()) + nFileNum, 16) + "_"+ nFileNum + extension;
			String reg = "(?!.*((img.baidu.com)|(127.0.0.1)|(^/upload/content/))).*$";
			String pathac ="";
			if (remoteFileurl.matches(reg)) {
				saveUrlAs(remoteFileurl, Const.UPLOAD_PATH+imgpath + filename);
				pathac = imgpath + filename;
				mRemoteFileurl.appendReplacement(sb, "<img src=\"" + pathac+"\" ");
				if (imgBuffer.toString().length() < 1) {
					imgBuffer.append(imgpath + filename);
				} else {
					imgBuffer.append(";").append(imgpath + filename);
				}
				nFileNum = nFileNum + 1;
			} else {
				if (getContentUrl(remoteFileurl)) {
					if(FileUtils.isFile(Const.UPLOAD_PATH + "/" + StringHelperUtils.getImageRootUrl(remoteFileurl))){  //判断文件是否存在，不存在则不执行
						FileUtils.moveFile(Const.UPLOAD_PATH + "/" + StringHelperUtils.getImageRootUrl(remoteFileurl),Const.UPLOAD_PATH + imgpath + filename);//开始移动文件
					}
					pathac = imgpath + filename;
					mRemoteFileurl.appendReplacement(sb, "<img src=\"" + pathac+"\" ");
				}

				nFileNum = nFileNum + 1;
			}
			String pictureUrl= null;
			if (remoteFileurl.matches(reg)) {
				pictureUrl = imgpath + filename;
			} else {
				pictureUrl =  remoteFileurl;
			}
			Images imaData=this.findImagesByImgurl(pictureUrl);
			if(imaData==null){
				File picture = new File(Const.UPLOAD_PATH +imgpath + filename);
				FileInputStream fis = new FileInputStream(picture);
				BufferedImage sourceImg = ImageIO.read(fis);
				Images images = new Images();
				images.setId(snowFlake.nextId());
				images.setImgUrl(imgpath + filename);
				images.setFileSize(String.format("%.1f", picture.length() / 1024.0));
				images.setImgWidth(Integer.toString(sourceImg.getWidth()));
				images.setImgHeight(Integer.toString(sourceImg.getHeight()));
				images.setSort(nFileNum);
				images.setCreateTime(new Date());
				images.setInfoCount(1);
				int totalCount=imagesDao.addImages(images);
				if(totalCount > 0){
					ImagesInfoMerge merge = new ImagesInfoMerge();
					merge.setId(snowFlake.nextId());
					merge.setInfoType(typeId);
					merge.setInfoId(infoId);
					merge.setImgId(images.getId());
					merge.setUserId(userId);
					imagesDao.addImagesInfoMerge(merge);
				}
				fis.close();
			}else{
				ImagesInfoMerge merge = new ImagesInfoMerge();
				merge.setId(snowFlake.nextId());
				merge.setInfoType(typeId);
				merge.setInfoId(infoId);
				merge.setImgId(imaData.getId());
				merge.setUserId(userId);
				imagesDao.addImagesInfoMerge(merge);
				imagesDao.updateImagesCount(imaData.getId());
			}

		}
		mRemoteFileurl.appendTail(sb);
		return sb.toString();
	}

	public DataVo saveAvatarDataFile(User user, BufferedImage image) throws ParseException {
		DataVo data = DataVo.failure("操作失败");
		try {
			String savePath = Const.UPLOAD_PATH +"/upload/";
			//文件保存目录URL
			String saveUrl  = "/upload/";
			//创建文件夹
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String ymds=sdf.format(user.getCreateTime());
			savePath += "avatar/"+ymds + "/"+user.getUserId() + "/";
			saveUrl += "avatar/"+ymds + "/"+user.getUserId() + "/";
			File dirFile = new File(savePath);
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
			String fileName = "avatar.jpg";
			File file = new File(savePath + fileName);
			if (!file.exists())
				file.createNewFile();
			ImageIO.write(image, "PNG", file);
			userService.updateAvatar(user.getUserId(),saveUrl + fileName);
			return DataVo.jump("上传成功", saveUrl + fileName);
		} catch (IOException e) {
			logger.warn("用户头像文件写入失败（userId={}）：{}", user.getUserId(), e.getMessage());
		}
		return data;
	}

	// ///////////////////////////////
	// ///// 刪除 ////////
	// ///////////////////////////////
	/**
	 * 按图片索引ID删除图片信息
	 *
	 * @param id
	 * @return
	 */
	public boolean deleteImagesById(Long id) {
		int totalCount = imagesDao.deleteImagesById(id);
        return totalCount > 0 ? true : false;
	}

	/**
	 * 按图片信息id和图片指纹删除图片信息
     * @param tid
     *         信息id
	 * @param imgurl
     *         图片地址
	 * @return
	 */
	public boolean deleteImagesByTidAndImgurl(Long tid, String imgurl) {
		int totalCount = imagesDao.deleteImagesByTidAndImgurl(tid,imgurl);
        return totalCount > 0 ? true : false;
	}

	/**
	 * 按信息分类和内容id删除图片信息
	 *
	 * @param channelid
	 *        信息分类id
	 * @param article_id
	 *        内容id
	 * @return
	 */
	public boolean deleteImagesByTid(Integer channelid,Long article_id) {
		List<Images> imglist=imagesDao.getImagesListByTid(article_id);
		if(imglist.size()>0){//未做排除有其他内容内有本文中的图片
			for (Images list : imglist) {
				FileUtils.delFileA(Const.UPLOAD_PATH + list.getImgUrl()); //删除内容中图片
			}
		}
		int totalCount = imagesDao.deleteImagesByTid(channelid,article_id);
        return totalCount > 0 ? true : false;
	}

	/**
	 * 删除图片文件和图片数据
	 *
	 * @param tid
     *         信息id
	 * @param imgurl
     *         图片地址
	 * @return
	 */
	public boolean delImagesByDateAndFile(Long tid, String imgurl) {
		FileUtils.delFileA(Const.UPLOAD_PATH + imgurl); //删除内容中图片
		int totalCount = imagesDao.deleteImagesByTidAndImgurl(tid,imgurl);
        return totalCount > 0 ? true : false;
	}
	// ///////////////////////////////
	// ///// 修改 ////////
	// ///////////////////////////////



	// ///////////////////////////////
	// ///// 查询 ////////
	// ///////////////////////////////

	/**
	 * 按信息类型id和信息id查询第一个文章图片
	 *
	 * @param imgUrl
	 * @return
	 */
	public Images findImagesByImgurl(String imgUrl){
		return imagesDao.findImagesByImgurl(imgUrl);
	}

    /**
     * 用信息id和图片地址查询该图片是否存在
     *
     * @param imgurl
     *         图片指纹
     * @return
     */
    public boolean checkImagesByTidAndImgurl(Long tid, String imgurl) {
        int totalCount = imagesDao.checkImagesByTidAndImgurl(tid,imgurl);
        return totalCount > 0 ? true : false;
    }

	/**
	 * 查询图片路径是否存在
	 *
	 * @param imgUrl
	 *        图片地址
	 * @return
	 */
	public boolean checkImagesByImgurl(String imgUrl) {
		int totalCount = imagesDao.checkImagesByImgurl(imgUrl);
        return totalCount > 0 ? true : false;
	}

	/**
	 * 缩放图片服务处理
	 *
	 * @param width
	 * @param height
	 * @param savePath
	 * @param targetURL
	 * @return
	 * @throws IOException
	 */
	public String thumbImages(Integer width, Integer height,String savePath, String targetURL) throws IOException {
		if(width>0 && height>0){
			ScaleImageUtils.forcedResize(width,height,savePath, new File(targetURL));
		}else{
			if(width>0){
				ScaleImageUtils.resize(width,savePath, new File(targetURL));
			}else if(height>0){
				ScaleImageUtils.resizeByHeight(height,savePath, new File(targetURL));
			}
		}
		return savePath;
	}


	/**
	 * 更新内容时对已有的图片数据分析本地化路径处理
	 *
	 * @param content
	 * @param savepath
	 * @param accesspath
	 * @param channelid
	 * @param tid
	 * @return
	 * @throws Exception
	 */
	public String replaceContent(String content, String savepath, String accesspath,Integer channelid, Integer tid) throws Exception {
		String sitedirect = Const.UPLOAD_PATH;
		Pattern pRemoteFileurl = Pattern.compile("<img.*?src=\"?(.*?)(\"|>|\\s+)");
		Matcher mRemoteFileurl = pRemoteFileurl.matcher(content);
		StringBuffer sb = new StringBuffer();
		String remoteFileurl = null;
		int nFileNum = 0;
		//创建文件夹并返回根目录，如：“/upload/content/2018/8/20/4CAF5F732B9E4523_0.jpg”
		String imgpath = getImgPath();
		StringBuffer imgPath = new StringBuffer();
		String pathac ="";
		while (mRemoteFileurl.find()) {
			remoteFileurl = mRemoteFileurl.group(1);
			String extension = StringHelperUtils.getImageUrlSuffix(remoteFileurl);
			extension = "." + extension;
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
			String filename = Md5Utils.code(df.format(new Date()) + nFileNum, 16) + "_"+ nFileNum + extension;
			String reg = "(?!.*((127.0.0.1)|(^/upload/content/))).*$";
			if (remoteFileurl.matches(reg)) {
				saveUrlAs(remoteFileurl, sitedirect + imgpath + filename);
				pathac = imgpath + filename;
				mRemoteFileurl.appendReplacement(sb, "<img src=\"" + pathac+"\" ");
				if (imgPath.toString().length() < 1) {
					imgPath.append(imgpath + filename);
				} else {
					imgPath.append(";").append(imgpath + filename);
				}
				nFileNum = nFileNum + 1;
			} else {
				if (getContentUrl(remoteFileurl)) {
					FileUtils.moveFile(sitedirect + StringHelperUtils.getImageRootUrl(remoteFileurl),sitedirect + imgpath + filename);
					pathac = imgpath + filename;
					mRemoteFileurl.appendReplacement(sb, "<img src=\"" + pathac+"\" ");
				}
			}
		}
		mRemoteFileurl.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 查询图片list内是否包含该图片路径
	 *
	 * @param name
	 * @param list
	 * @return
	 */
	public boolean listSearch(String name,List<Images> list){
		   for(int i=0; i < list.size(); i++){
		      if(name.equals(list.get(i).getImgUrl())){
		    	  return true;
		      }
		   }
		   return false;
	}

    /*
     *
     * 内容下载图片保存路径设置
     */
    public static String getImgPath() {
        String path=Const.UPLOAD_PATH,
                filepath= "/upload/content/" + Calendar.getInstance().get(Calendar.YEAR)
                        + "/" + (1 + Calendar.getInstance().get(Calendar.MONTH)) + "/"
                        + (Calendar.getInstance().get(Calendar.DATE)) + "/";

        File file = new File(path+filepath);
        // 如果文件夹不存在则创建
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
        return filepath;
    }

    public static void uploadFile(byte[] file, String filePath, String fileName) throws Exception {
        File targetFile = new File(filePath);
        if(!targetFile.exists()){
            targetFile.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(filePath+fileName);
        out.write(file);
        out.flush();
        out.close();
    }

	/**
	 * @param fileUrl
	 *            文件来源地址
	 * @param savePath
	 *            文件保存地址
	 * @return
	 */
	public static boolean saveUrlAs(String fileUrl, String savePath) {
		try {
			URL url = new URL(fileUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			DataInputStream in = new DataInputStream(connection.getInputStream());
			DataOutputStream out = new DataOutputStream(new FileOutputStream(savePath));
			byte[] buffer = new byte[4096];
			int count = 0;
			while ((count = in.read(buffer)) > 0) {
				out.write(buffer, 0, count);
			}
			out.close();
			in.close();
			connection.disconnect();
			return true;

		} catch (Exception e) {
			return false;
		}
	}


	/**
	 * 判断url是否是用户临时文件
	 *
	 * @param url
	 *            需要判断的url
	 * @return
	 */
	public static boolean getContentUrl(String url) {
		Pattern p = Pattern.compile("/upload/usertmp/+[a-zA-Z0-9]+/",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(url);
		return matcher.find();
	}

}
