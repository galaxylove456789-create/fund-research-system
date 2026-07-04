package com.fund.research.module.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.fund.dto.FundListQueryDTO;
import com.fund.research.module.fund.entity.FundInfo;
import com.fund.research.module.fund.vo.FundAnnouncementVO;
import com.fund.research.module.fund.vo.FundAttributionVO;
import com.fund.research.module.fund.vo.FundDetailVO;
import com.fund.research.module.fund.vo.FundHoldingDetailVO;
import com.fund.research.module.fund.vo.FundHoldingReportVO;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.fund.vo.FundMetricVO;
import com.fund.research.module.fund.vo.FundNavPointVO;
import com.fund.research.module.fund.vo.FundScoreVO;
import com.fund.research.module.fund.vo.FundTagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 基金 Mapper。
 */
@Mapper
public interface FundMapper extends BaseMapper<FundInfo> {

    /**
     * 分页查询基金列表，关联 fund_company 与 fund_score 最新一条记录。
     */
    IPage<FundListVO> selectFundPage(IPage<FundListVO> page, @Param("query") FundListQueryDTO query);

    /**
     * 查询基金详情（关联公司与最新评分）。
     */
    FundDetailVO selectFundDetail(@Param("fundId") Long fundId);

    /**
     * 查询基金最新评分。
     */
    FundScoreVO selectLatestScore(@Param("fundId") Long fundId);

    /**
     * 查询基金标签。
     */
    List<FundTagVO> selectFundTags(@Param("fundId") Long fundId);

    /**
     * 查询基金净值曲线。
     */
    List<FundNavPointVO> selectFundNav(@Param("fundId") Long fundId, @Param("limit") Integer limit);

    /**
     * 查询基金最新统计日的阶段指标。
     */
    List<FundMetricVO> selectLatestMetrics(@Param("fundId") Long fundId);

    /**
     * 查询基金公告。
     */
    List<FundAnnouncementVO> selectAnnouncements(@Param("fundId") Long fundId, @Param("limit") Integer limit);
    List<FundHoldingReportVO> selectHoldingReports(@Param("fundId") Long fundId, @Param("limit") Integer limit);

    List<FundHoldingDetailVO> selectHoldingDetails(@Param("reportId") Long reportId);

    List<FundAttributionVO> selectAttributions(@Param("fundId") Long fundId, @Param("limit") Integer limit);
}
