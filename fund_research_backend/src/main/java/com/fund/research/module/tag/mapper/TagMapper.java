package com.fund.research.module.tag.mapper;

import com.fund.research.module.tag.dto.TagQueryDTO;
import com.fund.research.module.tag.vo.TagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    List<TagVO> selectTags(@Param("query") TagQueryDTO query);

    List<String> selectCategories();
}
