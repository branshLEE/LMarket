package com.lmarket.product.web;

import com.lmarket.product.entity.CategoryEntity;
import com.lmarket.product.service.CategoryService;
import com.lmarket.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){

        //TODO 1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();

        //视图解析器进行拼串
        //classpath:/templates/ +返回值+ .html
        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    //index/catelog.json
    @ResponseBody
    @GetMapping("/index/catelog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){

        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }
}
