package com.lmarket.thirdparty;

import com.aliyun.oss.OSS;
import com.lmarket.thirdparty.component.SmsComponent;
import com.lmarket.thirdparty.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class LmarketThirdPartyApplicationTests {

    @Autowired
    OSS ossClient;

    @Autowired
    SmsComponent smsComponent;

    @Test
    public void testSendSms(){
        smsComponent.sendSmsCode("19173311311", "1311");
    }

    @Test
    public void testUpload() throws FileNotFoundException {
        // Endpoint以杭州为例，其它Region请按实际情况填写。
//		String endpoint = "oss-cn-hangzhou.aliyuncs.com";
//		// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//		String accessKeyId = "LTAI4GFmAuvbsrEwYpBHNdHb";
//		String accessKeySecret = "SAMZVZhAAuv7gqYSgEWKccDe6CAXGh";
//
//		// 创建OSSClient实例。
//		OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传文件流。
        InputStream inputStream = new FileInputStream("D:\\IdeaProjects\\pics\\5b5e74d0978360a1.jpg");

        ossClient.putObject("lmarket", "5b5e74d0978360a1.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功!");
    }

    @Test
    void contextLoads() {
    }

}
