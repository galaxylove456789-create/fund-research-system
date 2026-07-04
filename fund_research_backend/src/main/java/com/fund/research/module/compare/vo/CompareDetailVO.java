package com.fund.research.module.compare.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CompareDetailVO extends CompareRecordVO {

    private static final long serialVersionUID = 1L;

    private List<CompareFundVO> funds = new ArrayList<>();
}
