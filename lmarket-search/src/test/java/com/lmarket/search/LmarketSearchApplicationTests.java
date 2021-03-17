package com.lmarket.search;

import com.alibaba.fastjson.JSON;
import com.lmarket.search.config.LmarketElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class LmarketSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 测试存储数据到es
     * @throws IOException
     */
    @Test
    public void indexData() throws IOException {

        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1"); //数据的id
//        indexRequest.source("userName", "zhangsan", "age", 18, "gender", "男");
        User user = new User();
        user.setUserName("LEE");
        user.setAge(18);
        user.setGender("女");

        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON); //要保存的内容

        //执行保存操作
        IndexResponse index = client.index(indexRequest, LmarketElasticSearchConfig.COMMON_OPTIONS);

        //提取有用的相应数据
        System.out.println(index);
    }

    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

}
