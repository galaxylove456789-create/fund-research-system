package com.fund.research.module.admin.service;

import java.util.List;
import java.util.Map;

/**
 * Admin / system level service interface.
 */
public interface SystemService {

    /**
     * Predefined business table list of the fund_research database.
     */
    List<String> businessTables();

    /**
     * Inspect database and return summary information:
     * - expected business table count
     * - actually existing table count and names
     * - JDBC connection metadata
     */
    Map<String, Object> dbCheck();
}
