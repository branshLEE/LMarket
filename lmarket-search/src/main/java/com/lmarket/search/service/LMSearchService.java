package com.lmarket.search.service;

import com.lmarket.search.vo.SearchParam;
import com.lmarket.search.vo.SearchResult;

public interface LMSearchService {

    /**
     *
     * @param param 检索的所有参数
     * @return
     */
    SearchResult search(SearchParam param);
}
