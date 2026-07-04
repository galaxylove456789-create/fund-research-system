package com.fund.research.module.tag.service;

import com.fund.research.module.tag.dto.TagQueryDTO;
import com.fund.research.module.tag.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> listTags(TagQueryDTO query);

    List<String> listCategories();
}
