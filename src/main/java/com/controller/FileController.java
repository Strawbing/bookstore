package com.controller;

import com.annotation.IgnoreAuth;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.entity.ConfigEntity;
import com.entity.EIException;
import com.service.ConfigService;
import com.utils.R;
import org.apache.commons.io.FileUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

/**
 * 上传文件映射表
 */
@RestController
@RequestMapping("file")
@SuppressWarnings({"unchecked","rawtypes"})
public class FileController{
	@Autowired
    private ConfigService configService;
	@Autowired
	private ResourceLoader resourceLoader;

	/**
	 * 上传文件
	 */
	@RequestMapping("/upload")
	public R upload(@RequestParam("file") MultipartFile file, String type) throws Exception {
		if (file.isEmpty()) {
			throw new EIException("上传文件不能为空");
		}
		String originalFilename = file.getOriginalFilename();
		// 获取文件扩展名
		String fileExt = StringUtils.getFilenameExtension(originalFilename);
		// 获取上传目录
		String uploadDirPath = "src/main/resources/static/upload/";
		Path uploadDir = Paths.get(uploadDirPath);
		// 在 upload 目录下创建文件
		String fileName = new Date().getTime() + "." + fileExt;
		Path destPath = uploadDir.resolve(fileName);
		Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
		// 在 target 目录下创建文件
		String targetDirPath = resourceLoader.getResource("classpath:/static/upload/").getFile().getPath();
		InputStream inputStream = file.getInputStream();
		Path targetDir = Paths.get(targetDirPath);
		Path targetPath = targetDir.resolve(fileName);
		Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
		// 如果 type 不为空且等于 1
		if (org.apache.commons.lang3.StringUtils.isNotBlank(type) && type.equals("1")) {
			// 查询名为 faceFile 的配置实体
			ConfigEntity configEntity = configService.selectOne(new EntityWrapper<ConfigEntity>().eq("name", "faceFile"));
			if (configEntity == null) {
				// 如果不存在，则创建该配置实体并设置值为 fileName
				configEntity = new ConfigEntity();
				configEntity.setName("faceFile");
				configEntity.setValue(fileName);
			} else {
				// 如果存在，则更新值为 fileName
				configEntity.setValue(fileName);
			}
			// 插入或更新配置实体
			configService.insertOrUpdate(configEntity);
		}
		// 返回成功响应，并携带上传的文件名
		return R.ok().put("file", fileName);
	}
	
	/**
	 * 下载文件
	 */
	@IgnoreAuth
	@RequestMapping("/download")
	public ResponseEntity<byte[]> download(@RequestParam String fileName) {
		try {
			File path = new File(ResourceUtils.getURL("classpath:static").getPath());
			if(!path.exists()) {
			    path = new File("");
			}
			File upload = new File(path.getAbsolutePath(),"/upload/");
			if(!upload.exists()) {
			    upload.mkdirs();
			}
			File file = new File(upload.getAbsolutePath()+"/"+fileName);
			if(file.exists()){
				/*if(!fileService.canRead(file, SessionManager.getSessionUser())){
					getResponse().sendError(403);
				}*/
				HttpHeaders headers = new HttpHeaders();
			    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);    
			    headers.setContentDispositionFormData("attachment", fileName);    
			    return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file),headers, HttpStatus.CREATED);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
}
