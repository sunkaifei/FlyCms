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
    private com.flycms.module.config.service.ConfigService configService;

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

	/**
	 * S6 编辑器通道统一登记：前台编辑器（/ucenter/upload 等）传图后调本方法——
	 * 登记 fly_images（原 user_id 归属）+ R 水印 + Q1 多尺寸，与 /api 直传同口径；
	 * 失败静默（编辑器上传主流程不受影响，仅缺附件库登记）。
	 *
	 * @param webUrl 编辑器使用的 Web 地址（http://host/upload/usertmp/…），用于还原磁盘路径与相对 URL
	 * @return 登记后的 fly_images 行 id（失败返回 null）
	 */
	public Long registerEditorUpload(String webUrl, Long userId) {
		try {
			if (webUrl == null || webUrl.isBlank()) {
				return null;
			}
			// webUrl → 相对 URL（/upload/usertmp/{uid}/x.png）→ 磁盘路径（uploadfiles/upload/usertmp/…）
			String rel = webUrl.substring(webUrl.indexOf("/upload/"));
			java.io.File disk = new java.io.File("uploadfiles" + rel);
			if (!disk.exists()) {
				return null;
			}
			java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(disk);
			Images images = new Images();
			images.setId(SnowFlake.getInstance().nextId());
			images.setImgUrl(rel);
			images.setFileSize(String.format("%.1f", disk.length() / 1024.0));
			if (src != null) {
				images.setImgWidth(Integer.toString(src.getWidth()));
				images.setImgHeight(Integer.toString(src.getHeight()));
			}
			images.setSort(0);
			images.setCreateTime(new java.util.Date());
			images.setInfoCount(0);
			if (userId != null) {
				images.setUserId(userId);
			}
			imagesDao.addImages(images);
			// R 水印 + Q1 多尺寸（与管理端直传同口径）
			generateMultiSizes(images);
			return images.getId();
		} catch (Exception e) {
			logger.warn("编辑器上传登记失败（{}）：{}", webUrl, e.getMessage());
			return null;
		}
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

	/**
	 * Q3 内容图片本地化：抓取 content 里所有外站 <img> 到本地 /upload/content/，
	 * 登记 fly_images（进附件库/引用计数），并把 img src 替换为本地地址；
	 * targetDomain 非空时本地地址前缀该域名（如 https://img.example.com），空=相对路径。
	 * 站内/相对路径图片不动（避免重复搬移）。
	 *
	 * @return 本地化后的 content（无外站图片时原样返回）
	 */
	public String localizeContent(Long userId, String content, String targetDomain) throws Exception {
		if (content == null || content.isBlank() || !content.contains("<img")) {
			return content;
		}
		// 本站域名图不重复抓取（fly_url 前缀的完整 URL 已在本地，跳过防重复搬移）
		String selfBase = null;
		try {
			String siteUrl = configService.getStringByKey("fly_url");
			selfBase = siteUrl == null || siteUrl.isBlank() ? null : siteUrl.replaceAll("/+$", "");
		} catch (Exception ignored) {
		}
		String localized = localizeTo(content, selfBase, userId);
		if (targetDomain != null && !targetDomain.isBlank()) {
			String dom = targetDomain.trim();
			if (!dom.startsWith("http")) {
				dom = "https://" + dom;
			}
			dom = dom.endsWith("/") ? dom.substring(0, dom.length() - 1) : dom;
			localized = localized.replace("src=\"/upload/content/", "src=\"" + dom + "/upload/content/");
		}
		return localized;
	}

	/**
	 * 抓取 content 里所有外站 img 到 /upload/content/ 并登记 fly_images（本站图跳过），
	 * 返回本地化后的 content（相对地址）。replaceContent 内部排除正则已含
	 * img.baidu.com/127.0.0.1 白名单与站内路径判断。
	 */
	private String localizeTo(String content, String selfBase, Long userId) {
		try {
			return replaceContent(0, 0L, userId == null ? 0L : userId, content);
		} catch (Exception e) {
			logger.warn("图片本地化失败：{}", e.getMessage());
			return content;
		}
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
			String reg = "(?!.*((img.baidu.com)|(127.0.0.1)|(^/upload/content/))).*$";
			String pathac ="";
			// 落盘扩展名以文件头魔数为准（不由 URL 后缀决定），故先声明再在分支内赋值
			String filename = null;
			if (remoteFileurl.matches(reg)) {
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
				String baseName = Md5Utils.code(df.format(new Date()) + nFileNum, 16) + "_" + nFileNum;
				// 安全抓取：协议白名单 + 内网地址拒绝 + 超时 + 大小上限 + 魔数校验
				String ext = fetchRemoteImage(remoteFileurl, Const.UPLOAD_PATH + imgpath + baseName);
				if (ext == null) {
					// 目标不安全或抓取失败：保留原始外链，不重写 src、不登记
					continue;
				}
				filename = baseName + "." + ext;
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
					String extension = StringHelperUtils.getImageUrlSuffix(remoteFileurl);
					if (!REMOTE_IMAGE_EXT.contains(extension.toLowerCase(java.util.Locale.ROOT))) {
						// 站内历史文件扩展名不在图片白名单内（如 .html/.svg）：不搬移、不重写
						continue;
					}
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
					filename = Md5Utils.code(df.format(new Date()) + nFileNum, 16) + "_"+ nFileNum + "." + extension;
					if(FileUtils.isFile(Const.UPLOAD_PATH + "/" + StringHelperUtils.getImageRootUrl(remoteFileurl))){  //判断文件是否存在，不存在则不执行
						FileUtils.moveFile(Const.UPLOAD_PATH + "/" + StringHelperUtils.getImageRootUrl(remoteFileurl),Const.UPLOAD_PATH + imgpath + filename);//开始移动文件
					}
					pathac = imgpath + filename;
					mRemoteFileurl.appendReplacement(sb, "<img src=\"" + pathac+"\" ");
				}

				nFileNum = nFileNum + 1;
			}
			if (filename == null) {
				// 既非外站图也非站内 content 图：没有可落盘的文件名，跳过登记
				// （原先会在下面 new FileInputStream 抛 FileNotFoundException 并中断整篇本地化）
				continue;
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
			String reg = "(?!.*((127.0.0.1)|(^/upload/content/))).*$";
			if (remoteFileurl.matches(reg)) {
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
				String baseName = Md5Utils.code(df.format(new Date()) + nFileNum, 16) + "_" + nFileNum;
				// 安全抓取：协议白名单 + 内网地址拒绝 + 超时 + 大小上限 + 魔数校验
				String ext = fetchRemoteImage(remoteFileurl, sitedirect + imgpath + baseName);
				if (ext == null) {
					// 目标不安全或抓取失败：保留原始外链，不重写 src
					continue;
				}
				String filename = baseName + "." + ext;
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
					String extension = StringHelperUtils.getImageUrlSuffix(remoteFileurl);
					if (!REMOTE_IMAGE_EXT.contains(extension.toLowerCase(java.util.Locale.ROOT))) {
						// 站内历史文件扩展名不在图片白名单内：不搬移、不重写
						continue;
					}
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
					String filename = Md5Utils.code(df.format(new Date()) + nFileNum, 16) + "_"+ nFileNum + "." + extension;
					if (FileUtils.isFile(sitedirect + StringHelperUtils.getImageRootUrl(remoteFileurl))) {
						FileUtils.moveFile(sitedirect + StringHelperUtils.getImageRootUrl(remoteFileurl),sitedirect + imgpath + filename);
					}
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

	// /////////////////// 远程图片安全抓取（SSRF / 本地文件读取 / 慢速 URL 防护） ///////////////////

	/** 允许落盘的图片扩展名白名单（用于站内历史文件初筛；外站图最终以文件头魔数判定） */
	private static final java.util.Set<String> REMOTE_IMAGE_EXT =
			java.util.Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

	/** 单张远程图片大小上限 5MB，防止超大文件打爆内存与磁盘 */
	private static final long MAX_REMOTE_IMAGE_BYTES = 5L * 1024 * 1024;

	/** 连接/读取超时，防止慢速 URL 长期占用 Tomcat 工作线程与数据库连接 */
	private static final int REMOTE_CONNECT_TIMEOUT_MS = 3000;
	private static final int REMOTE_READ_TIMEOUT_MS = 5000;

	/** 手动跟随的最大重定向跳数（每一跳都重新做协议与内网校验） */
	private static final int MAX_REDIRECTS = 3;

	/**
	 * 安全抓取远程图片并落盘为 {@code basePath + "." + 实际扩展名}。
	 *
	 * <p><b>为什么必须这么写</b>：本方法处理的是「内容里出现的外站 img src」，
	 * 该值完全由编辑内容的人控制。此前的实现是
	 * {@code new URL(fileUrl).openConnection()} 直接下载，存在四类问题：
	 * <ol>
	 *   <li><b>协议不限</b>：{@code file:///etc/passwd} 会被下载并落到对外公开的
	 *       uploadfiles 目录 → 本地任意文件读取；</li>
	 *   <li><b>主机不限</b>：可访问内网、云元数据（169.254.169.254）等只对服务器
	 *       可见的地址 → SSRF；</li>
	 *   <li><b>无超时</b>：慢速/不响应的 URL 会一直占住请求线程；</li>
	 *   <li><b>落盘扩展名取自 URL 后缀</b>：{@code x.html} / {@code x.svg} 会被当作
	 *       静态页提供 → 存储型 XSS。</li>
	 * </ol>
	 * 现按「协议白名单 → DNS 解析后拒绝内网/保留地址（含逐跳重定向复检）→ 超时 →
	 * 大小上限 → 魔数判定扩展名 → 落盘路径必须在上传根目录内」依次校验，
	 * 任一环节不通过即返回 null，调用方保留原始外链（而非中断整篇本地化）。
	 *
	 * @param fileUrl  内容里出现的外站图片地址
	 * @param basePath 落盘基名（不含扩展名），扩展名由魔数决定
	 * @return 实际扩展名（不含点）；校验不通过或抓取失败返回 null
	 */
	private String fetchRemoteImage(String fileUrl, String basePath) {
		try {
			String current = fileUrl;
			for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
				URL url = new URL(current);
				if (!isHttpProtocol(url) || isInternalHost(url.getHost())) {
					logger.warn("图片本地化已拦截（协议或内网地址）：{}", current);
					return null;
				}
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(REMOTE_CONNECT_TIMEOUT_MS);
				connection.setReadTimeout(REMOTE_READ_TIMEOUT_MS);
				// 关闭自动跳转：重定向目标同样可能是内网地址，必须逐跳复检
				connection.setInstanceFollowRedirects(false);
				connection.setRequestProperty("User-Agent", "FlyCms-ImageLocalizer");
				int code = connection.getResponseCode();
				if (code >= 300 && code < 400) {
					String location = connection.getHeaderField("Location");
					connection.disconnect();
					if (location == null || location.isEmpty()) {
						return null;
					}
					current = new URL(url, location).toString();
					continue;
				}
				if (code != 200) {
					connection.disconnect();
					logger.warn("图片本地化放弃：HTTP {} {}", code, current);
					return null;
				}
				byte[] data = readCapped(connection.getInputStream(), MAX_REMOTE_IMAGE_BYTES);
				connection.disconnect();
				if (data == null) {
					logger.warn("图片本地化放弃：读取为空或超过 {} 字节 {}", MAX_REMOTE_IMAGE_BYTES, current);
					return null;
				}
				String ext = detectImageExt(data);
				if (ext == null) {
					logger.warn("图片本地化已拦截：响应内容不是白名单图片格式 {}", current);
					return null;
				}
				File target = new File(basePath + "." + ext);
				// 纵深防御：落盘路径必须仍在上传根目录内（basePath 由服务端拼装，此处兜底）
				java.nio.file.Path uploadRoot = new File(Const.UPLOAD_PATH).getCanonicalFile().toPath();
				if (!target.getCanonicalFile().toPath().startsWith(uploadRoot)) {
					logger.warn("图片本地化已拦截：落盘路径越出上传目录 {}", target.getPath());
					return null;
				}
				File parent = target.getParentFile();
				if (parent != null && !parent.exists()) {
					parent.mkdirs();
				}
				try (FileOutputStream out = new FileOutputStream(target)) {
					out.write(data);
				}
				return ext;
			}
			logger.warn("图片本地化已拦截：重定向超过 {} 跳", MAX_REDIRECTS);
			return null;
		} catch (Exception e) {
			logger.warn("图片本地化抓取失败：{}（{}）", fileUrl, e.getMessage());
			return null;
		}
	}

	/** 仅放行 http/https，阻断 file:// 等本地文件读取协议 */
	private static boolean isHttpProtocol(URL url) {
		String protocol = url.getProtocol();
		return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
	}

	/**
	 * 主机是否解析到内网/保留地址。解析失败一律按不安全处理。
	 * 注意：本方法与连接之间仍存在 DNS rebinding 的理论窗口，属于本场景可接受的残余风险。
	 */
	private static boolean isInternalHost(String host) {
		if (host == null || host.isEmpty()) {
			return true;
		}
		String h = host;
		if (h.startsWith("[") && h.endsWith("]")) {
			h = h.substring(1, h.length() - 1);
		}
		String lower = h.toLowerCase(java.util.Locale.ROOT);
		if ("localhost".equals(lower) || lower.endsWith(".localhost")
				|| lower.endsWith(".local") || lower.endsWith(".internal")) {
			return true;
		}
		try {
			for (java.net.InetAddress addr : java.net.InetAddress.getAllByName(h)) {
				if (isUnsafeAddress(addr)) {
					return true;
				}
			}
		} catch (Exception e) {
			return true;
		}
		return false;
	}

	/** 回环 / 任意本地 / 站点本地(10、172.16-31、192.168) / 链路本地(169.254、fe80) / 组播 / CGNAT / 保留段 */
	private static boolean isUnsafeAddress(java.net.InetAddress addr) {
		if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isSiteLocalAddress()
				|| addr.isLinkLocalAddress() || addr.isMulticastAddress()) {
			return true;
		}
		byte[] b = addr.getAddress();
		if (b.length == 4) {
			int first = b[0] & 0xFF;
			int second = b[1] & 0xFF;
			if (first == 0 || first == 127) {
				return true;                                  // 0.0.0.0/8、127.0.0.0/8
			}
			if (first == 100 && second >= 64 && second <= 127) {
				return true;                                  // 100.64.0.0/10 CGNAT
			}
			if (first == 192 && second == 0) {
				return true;                                  // 192.0.0.0/24
			}
			if (first == 198 && (second == 18 || second == 19)) {
				return true;                                  // 198.18.0.0/15 基准测试段
			}
			return false;
		}
		if (b.length == 16) {
			if ((b[0] & 0xFE) == 0xFC) {
				return true;                                  // fc00::/7 唯一本地地址
			}
			boolean mapped = true;
			for (int i = 0; i < 10; i++) {
				if (b[i] != 0) {
					mapped = false;
					break;
				}
			}
			if (mapped && (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF) {
				try {                                         // ::ffff:a.b.c.d 按 IPv4 规则复检
					return isUnsafeAddress(java.net.InetAddress.getByAddress(
							new byte[]{b[12], b[13], b[14], b[15]}));
				} catch (Exception e) {
					return true;
				}
			}
		}
		return false;
	}

	/** 带总量上限的读取：超限返回 null，不留下半截文件 */
	private static byte[] readCapped(java.io.InputStream in, long maxBytes) throws IOException {
		if (in == null) {
			return null;
		}
		try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
			byte[] buffer = new byte[4096];
			long total = 0;
			int count;
			while ((count = in.read(buffer)) > 0) {
				total += count;
				if (total > maxBytes) {
					return null;
				}
				bos.write(buffer, 0, count);
			}
			return bos.size() == 0 ? null : bos.toByteArray();
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {
				// 关闭失败不影响主流程
			}
		}
	}

	/** 按文件头魔数判定图片类型（复用上传侧 UploadSafeUtil 的同一份白名单） */
	private static String detectImageExt(byte[] data) {
		if (data == null) {
			return null;
		}
		for (com.flycms.core.utils.UploadSafeUtil.ImageType t
				: com.flycms.core.utils.UploadSafeUtil.ImageType.values()) {
			if (t.magicMatches(data)) {
				return t.getExt();
			}
		}
		return null;
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
