package cn.yicha.cartoon.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.yicha.cartoon.index.CoffSearchIndex;
import cn.yicha.cartoon.util.Config;
import cn.yicha.cartoon.util.ZipUtil;

/**
 * 漫咖啡索引上传业务层
 * @author yicha
 *
 */
public class UploadIndexCoffService {

	private static final Log log = LogFactory.getLog(UploadIndexCoffService.class);
	
	CoffSearchIndex coffSearchIndex = CoffSearchIndex.getInstance();
	
	/**
	 * 上传索引
	 * @param request
	 * @param version
	 * @param indexBack
	 * @param indexPath
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String uploadIndex(HttpServletRequest request, String version, String indexBack, String indexPath) {
		String result = "success";
		String filename = "";
		//获取上传文件
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		try {
			List<FileItem> items = upload.parseRequest(request);
			for(FileItem item: items){
				if (!item.isFormField()) {		
					File f = new File(indexBack + "/" + item.getName());
					item.write(f);
					filename = item.getName();
				}
			}
			//解压文件
			ZipUtil.unzip(indexBack + "/" + filename, indexPath + "/" + filename.substring(0, filename.indexOf('.')) + "/");
			//更新索引
			result = changeIndex(filename.substring(0, filename.indexOf('.')));
		}
		catch (Exception e) {
			//使用指定的旧版本索引
			result = "error:change index. result:" + changeIndex(version);
			log.error(e.toString());
		}
		if(result.startsWith("error:")) {
			result = "error:change index. result:" + changeIndex(version);
			log.error(result);
		}
		return result;
	}
	
	/**
	 * 切换索引
	 * @param version
	 * @return
	 */
	public String changeIndex(String version) {
		String result = "success";
		//如果version版本索引可用，则替换
		if(coffSearchIndex.changeSearcher(version)) {
			try {
				//更改配置文件
				Config.changeCoffIndexName(version);
				
				//更新缓存
				Config.cateCache.clear();
				
				coffSearchIndex.init();
			} catch (IOException e) {
				result = "error: change index name error";
				log.error(e.toString());
				log.error(result);
			}
		}
		else {
			result = "error: index unavailable";
		}
		if(result.startsWith("error:")) {
			log.error(result);
		}
		return result;
	}
}
