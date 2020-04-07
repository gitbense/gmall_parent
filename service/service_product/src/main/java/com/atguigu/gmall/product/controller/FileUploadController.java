package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zr
 * @create 2020-03-14 下午 18:05
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception {

        String path = null;

        //如果上传对象不为空
        if (file != null) {
            //获取resources目录下的tracker.conf
            String configPath = this.getClass().getResource("/tracker.conf").getFile();
            //初始化配置
            ClientGlobal.init(configPath);
            //创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();
            //创建storageClient1对象
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            //上传获取到path
            //第一个参数：文件的字节数组
            //第二个参数：文件的后缀名
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
            System.out.println("文件路径：" + fileUrl + path);
        }
        //返回的是一个路径，服务器的IP地址
        return Result.ok(fileUrl + path);
    }
}
