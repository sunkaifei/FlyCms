package com.flycms.module.question.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.flycms.core.utils.*;
import com.flycms.constant.Const;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.question.dao.ImagesDao;
import com.flycms.module.question.model.Images;
import com.flycms.module.question.model.ImagesInfoMerge;
import com.flycms.module.question.model.Question;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.apache.log4j.Logger;
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
	private Logger logger = Logger.getLogger(this.getClass());
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
	public String replaceContent(Integer typeId,Long infoId,Long userId,String content) throws Exception {
		SnowFlake snowFlake = new SnowFlake(2, 3);
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
			e.printStackTrace();
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
	 * 按信息id查询所有关联图片的数量
	 *
	 * @param channelid
	 *        信息类别id
	 * @param img_width
	 *        图片宽度
	 * @param img_height
	 *        图片高度
	 * @return
	 */
	public int getImagesByTidListCount(Integer channelid, Integer img_width, Integer img_height){
		return imagesDao.getImagesByTidListCount(channelid,img_width,img_height);
	}




	/**
		 *按信息id查询所有关联的图片
		 *
		 * @param channelid
		 *        信息类别id
		 * @param img_width
		 *        图片宽度
		 * @param img_height
		 *        图片高度
		 * @param pageNum
		 *        页码数
		 * @param rows
		 *        查询记录条数
	 * @return
	 */
	public PageVo<Question> getImagesByTidList(Integer channelid, Integer img_width, Integer img_height, Integer pageNum, Integer rows){
		PageVo<Question> pageVo = new PageVo<Question>(pageNum);
		pageVo.setRows(rows);
		pageVo.setCount(getImagesByTidListCount(channelid,img_width,img_height));
		List<Question> imageslist = imagesDao.getImagesByTidList(channelid,img_width,img_height, pageVo.getOffset(), pageVo.getRows());
		pageVo.setList(imageslist);
		return pageVo;
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
	public static void main(String[] args) {
		System.out.println(getContentUrl("/upload/content/2019/1/24/30EC1B7906CBFC8B_0.png"));


	}

}
