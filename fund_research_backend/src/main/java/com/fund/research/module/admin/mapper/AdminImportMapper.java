package com.fund.research.module.admin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.admin.vo.ImportBatchVO;
import com.fund.research.module.admin.vo.ImportErrorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminImportMapper {

    IPage<ImportBatchVO> selectImportBatches(IPage<ImportBatchVO> page);

    IPage<ImportErrorVO> selectImportErrors(IPage<ImportErrorVO> page,
                                            @Param("batchId") Long batchId);

    int insertImportBatch(@Param("batch") Map<String, Object> batch);

    List<Map<String, Object>> selectTagRules();

    int insertTagRule(@Param("rule") Map<String, Object> rule);

    int updateTagRule(@Param("tagId") Long tagId,
                      @Param("rule") Map<String, Object> rule);
}
