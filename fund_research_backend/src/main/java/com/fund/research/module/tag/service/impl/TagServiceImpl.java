package com.fund.research.module.tag.service.impl;

import com.fund.research.module.tag.dto.TagQueryDTO;
import com.fund.research.module.tag.mapper.TagMapper;
import com.fund.research.module.tag.service.TagService;
import com.fund.research.module.tag.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagVO> listTags(TagQueryDTO query) {
        return tagMapper.selectTags(query);
    }

    @Override
    public List<String> listCategories() {
        return tagMapper.selectCategories();
    }
}
