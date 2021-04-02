package com.lmarket.search.controller;

import com.lmarket.search.service.LMSearchService;
import com.lmarket.search.vo.SearchParam;
import com.lmarket.search.vo.SearchResult;
import org.elasticsearch.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SearchController {


    @Autowired
    LMSearchService lmSearchService;

    /**
     * 自动将页面提交过来的所有请求查询参数封装成指定的对象
     * @param param
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){

        String queryString = request.getQueryString();
        param.set_queryString((queryString));

        //1、根据传递来的页面的查询参数，去es中检索商品
        SearchResult result = lmSearchService.search(param);
        model.addAttribute("result", result);

        return "list";
    }
}
