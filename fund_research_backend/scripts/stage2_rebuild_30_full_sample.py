"""
Rebuild the database into a 30-fund complete demo sample.

The script keeps the first 30 funds by fund_id, physically removes other funds
and fund-related rows, then fills missing demo data for the remaining funds:
- manager relations
- metrics, score, tags
- holding reports and details
- attributions
- announcements

Run from backend project root:
    $env:FUND_DB_PASSWORD="1234"
    $env:PYTHONIOENCODING="utf-8"
    .\\.venv\\Scripts\\python.exe .\\scripts\\stage2_rebuild_30_full_sample.py
"""

from __future__ import annotations

import os
import sys
from datetime import date, datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import Any

import psycopg2

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


DB = {
    "host": os.getenv("FUND_DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("FUND_DB_PORT", "54321")),
    "dbname": os.getenv("FUND_DB_NAME", "fund_research"),
    "user": os.getenv("FUND_DB_USER", "system"),
    "password": os.getenv("FUND_DB_PASSWORD", ""),
}

HOLDING_TEMPLATES = [
    ("600519", "贵州茅台", "食品饮料"),
    ("300750", "宁德时代", "电力设备"),
    ("000858", "五粮液", "食品饮料"),
    ("601318", "中国平安", "非银金融"),
    ("600036", "招商银行", "银行"),
    ("002594", "比亚迪", "汽车"),
    ("688981", "中芯国际", "电子"),
    ("600276", "恒瑞医药", "医药生物"),
    ("000333", "美的集团", "家用电器"),
    ("002415", "海康威视", "计算机"),
    ("601888", "中国中免", "商贸零售"),
    ("600900", "长江电力", "公用事业"),
]

TAG_DEFS = [
    ("DEMO_FULL_SAMPLE", "完整样本基金", "来源", "阶段二重构后的 30 只完整演示基金"),
    ("DEMO_HOLDING_READY", "持仓已补齐", "持仓", "已具备持仓报告和明细"),
    ("DEMO_ATTRIBUTION_READY", "归因已补齐", "归因", "已具备业绩归因摘要"),
    ("DEMO_ANNOUNCEMENT_READY", "公告已补齐", "公告", "已具备公告数据"),
]


def q(value: float | int | Decimal | None, scale: int = 4) -> Decimal | None:
    if value is None:
        return None
    return Decimal(str(float(value))).quantize(Decimal("1." + "0" * scale), rounding=ROUND_HALF_UP)


def next_id(cur, table: str, column: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX({column}), 0) + 1 FROM {table}")
    return int(cur.fetchone()[0])


def rows(cur, sql: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
    cur.execute(sql, params)
    cols = [c[0] for c in cur.description]
    return [dict(zip(cols, row)) for row in cur.fetchall()]


def fetch_keep_funds(cur) -> list[dict[str, Any]]:
    return rows(
        cur,
        """
        SELECT fund_id, fund_code, fund_name, fund_type, risk_level, company_id, fund_scale, establish_date
        FROM fund_info
        ORDER BY fund_id
        LIMIT 30
        """,
    )


def cleanup_non_sample_funds(cur, keep_ids: list[int]) -> None:
    keep_tuple = tuple(keep_ids)
    cur.execute("UPDATE community_post SET related_fund_id = NULL WHERE related_fund_id IS NOT NULL AND related_fund_id NOT IN %s", (keep_tuple,))
    cur.execute("DELETE FROM user_recent_fund_view WHERE fund_id NOT IN %s", (keep_tuple,))
    cur.execute("DELETE FROM ai_recommend_item WHERE fund_id NOT IN %s", (keep_tuple,))
    cur.execute("DELETE FROM ai_recommend_record WHERE recommend_id NOT IN (SELECT DISTINCT recommend_id FROM ai_recommend_item)", ())
    cur.execute("DELETE FROM fund_compare_item WHERE fund_id NOT IN %s", (keep_tuple,))
    cur.execute("DELETE FROM fund_compare_record WHERE compare_id NOT IN (SELECT DISTINCT compare_id FROM fund_compare_item)", ())
    cur.execute("DELETE FROM portfolio_fund_relation WHERE fund_id NOT IN %s", (keep_tuple,))
    cur.execute("DELETE FROM fund_portfolio WHERE portfolio_id NOT IN (SELECT DISTINCT portfolio_id FROM portfolio_fund_relation)", ())
    cur.execute("DELETE FROM user_favorite WHERE fund_id NOT IN %s", (keep_tuple,))
    cur.execute("DELETE FROM fund_info WHERE fund_id NOT IN %s", (keep_tuple,))


def normalize_fund_base(cur, funds: list[dict[str, Any]]) -> None:
    company_ids = [r["company_id"] for r in rows(cur, "SELECT company_id FROM fund_company ORDER BY company_id") if r["company_id"] is not None]
    if not company_ids:
        raise RuntimeError("fund_company is empty")
    for idx, fund in enumerate(funds):
        company_id = fund["company_id"] or company_ids[idx % len(company_ids)]
        fund_type = fund["fund_type"] or ["混合型-偏股", "债券型-中长期", "股票型", "指数型"][idx % 4]
        risk_level = fund["risk_level"] or ("MEDIUM_LOW" if "债" in fund_type else "MEDIUM")
        scale = fund["fund_scale"] or q(8 + idx * 1.37, 2)
        establish = fund["establish_date"] or date(2020 - idx % 5, (idx % 12) + 1, 15)
        cur.execute(
            """
            UPDATE fund_info
            SET company_id=%s,
                fund_type=%s,
                risk_level=%s,
                fund_scale=%s,
                establish_date=%s,
                full_name=COALESCE(NULLIF(full_name, ''), %s),
                benchmark=COALESCE(benchmark, %s),
                custodian=COALESCE(custodian, '中国建设银行股份有限公司'),
                status=1,
                source='30_FULL_SAMPLE',
                updated_time=CURRENT_TIMESTAMP
            WHERE fund_id=%s
            """,
            (
                company_id,
                fund_type,
                risk_level,
                scale,
                establish,
                f"{fund['fund_name']}证券投资基金",
                "沪深300指数收益率*60%+中债综合指数收益率*40%",
                fund["fund_id"],
            ),
        )


def ensure_manager_relations(cur, funds: list[dict[str, Any]]) -> None:
    manager_ids = [r["manager_id"] for r in rows(cur, "SELECT manager_id FROM fund_manager ORDER BY manager_id")]
    if not manager_ids:
        raise RuntimeError("fund_manager is empty")
    next_relation_id = next_id(cur, "fund_manager_relation", "relation_id")
    for idx, fund in enumerate(funds):
        cur.execute("SELECT COUNT(*) FROM fund_manager_relation WHERE fund_id=%s", (fund["fund_id"],))
        if cur.fetchone()[0]:
            continue
        cur.execute(
            """
            INSERT INTO fund_manager_relation (
                relation_id, fund_id, manager_id, start_date, end_date, is_current
            ) VALUES (%s, %s, %s, %s, NULL, 1)
            """,
            (next_relation_id, fund["fund_id"], manager_ids[idx % len(manager_ids)], date(2021 + idx % 3, 1 + idx % 12, 1)),
        )
        next_relation_id += 1


def ensure_tags(cur) -> dict[str, int]:
    tag_ids: dict[str, int] = {}
    for code, name, category, desc in TAG_DEFS:
        cur.execute("SELECT tag_id FROM fund_tag WHERE tag_code=%s", (code,))
        row = cur.fetchone()
        if row:
            tag_id = int(row[0])
        else:
            tag_id = next_id(cur, "fund_tag", "tag_id")
            cur.execute(
                """
                INSERT INTO fund_tag (tag_id, tag_code, tag_name, tag_category, description, rule_expression, enabled)
                VALUES (%s, %s, %s, %s, %s, %s, 1)
                """,
                (tag_id, code, name, category, desc, desc),
            )
        tag_ids[code] = tag_id
    return tag_ids


def latest_metric(cur, fund_id: int) -> dict[str, Any] | None:
    metric = rows(
        cur,
        """
        SELECT stat_date, return_rate, annual_return, volatility, max_drawdown, sharpe_ratio
        FROM fund_performance_metric
        WHERE fund_id=%s AND period_code='1Y'
        ORDER BY stat_date DESC
        LIMIT 1
        """,
        (fund_id,),
    )
    return metric[0] if metric else None


def ensure_metrics_score_tags(cur, funds: list[dict[str, Any]], tag_ids: dict[str, int]) -> None:
    next_metric_id = next_id(cur, "fund_performance_metric", "metric_id")
    next_score_id = next_id(cur, "fund_score", "score_id")
    next_relation_id = next_id(cur, "fund_tag_relation", "relation_id")
    periods = [("1M", 30), ("3M", 90), ("6M", 180), ("1Y", 365), ("YTD", 180), ("SINCE", 1000)]
    stat_date = date(2026, 6, 30)
    for idx, fund in enumerate(funds):
        base_return = 4 + (idx % 10) * 2.1
        base_drawdown = 4 + (idx % 8) * 2.8
        base_volatility = 5 + (idx % 9) * 2.3
        for period, _days in periods:
            cur.execute(
                "SELECT COUNT(*) FROM fund_performance_metric WHERE fund_id=%s AND stat_date=%s AND period_code=%s",
                (fund["fund_id"], stat_date, period),
            )
            if cur.fetchone()[0]:
                continue
            factor = {"1M": 0.16, "3M": 0.42, "6M": 0.72, "1Y": 1.0, "YTD": 0.68, "SINCE": 1.35}[period]
            ret = base_return * factor
            ann = ret if period == "1Y" else ret * (1.0 / max(factor, 0.16))
            dd = min(base_drawdown * (0.55 + factor / 2), 35)
            vol = min(base_volatility * (0.65 + factor / 2), 45)
            sharpe = (ann - 2) / vol if vol else 0
            cur.execute(
                """
                INSERT INTO fund_performance_metric (
                    metric_id, fund_id, stat_date, period_code, return_rate,
                    annual_return, volatility, max_drawdown, sharpe_ratio
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (next_metric_id, fund["fund_id"], stat_date, period, q(ret), q(ann), q(vol), q(dd), q(sharpe)),
            )
            next_metric_id += 1
        metric = latest_metric(cur, fund["fund_id"]) or {
            "return_rate": base_return,
            "max_drawdown": base_drawdown,
            "volatility": base_volatility,
            "sharpe_ratio": 1.0,
        }
        ret = float(metric["return_rate"] or 0)
        dd = float(metric["max_drawdown"] or 0)
        vol = float(metric["volatility"] or 0)
        yield_score = max(35, min(95, 62 + ret * 0.8))
        risk_score = max(30, min(95, 96 - dd * 1.8))
        stability_score = max(30, min(95, 95 - vol * 1.4))
        total = yield_score * 0.3 + risk_score * 0.3 + stability_score * 0.2 + 70 * 0.1 + 68 * 0.1
        recommend = "RECOMMEND" if total >= 75 else ("WATCH" if total >= 65 else "NEUTRAL")
        cur.execute("DELETE FROM fund_score WHERE fund_id=%s AND score_date=%s", (fund["fund_id"], stat_date))
        cur.execute(
            """
            INSERT INTO fund_score (
                score_id, fund_id, score_date, yield_score, risk_score,
                stability_score, manager_score, scale_score, total_score,
                recommend_level, explain_text, created_time
            ) VALUES (%s, %s, %s, %s, %s, %s, 70.00, 68.00, %s, %s, %s, CURRENT_TIMESTAMP)
            """,
            (
                next_score_id,
                fund["fund_id"],
                stat_date,
                q(yield_score, 2),
                q(risk_score, 2),
                q(stability_score, 2),
                q(total, 2),
                recommend,
                "30只完整样本基金：基于历史净值指标和演示规则生成的综合评分。",
            ),
        )
        next_score_id += 1
        for tag_id in tag_ids.values():
            cur.execute(
                """
                SELECT COUNT(*) FROM fund_tag_relation
                WHERE fund_id=%s AND tag_id=%s AND tag_date=%s
                """,
                (fund["fund_id"], tag_id, stat_date),
            )
            if cur.fetchone()[0]:
                continue
            cur.execute(
                """
                INSERT INTO fund_tag_relation (relation_id, fund_id, tag_id, confidence, source, tag_date, created_time)
                VALUES (%s, %s, %s, 100.00, '30_FULL_SAMPLE', %s, CURRENT_TIMESTAMP)
                """,
                (next_relation_id, fund["fund_id"], tag_id, stat_date),
            )
            next_relation_id += 1


def ensure_holdings(cur, funds: list[dict[str, Any]]) -> None:
    next_report_id = next_id(cur, "fund_holding_report", "report_id")
    next_holding_id = next_id(cur, "fund_holding_detail", "holding_id")
    report_dates = [date(2024, 12, 31), date(2024, 9, 30), date(2024, 6, 30), date(2024, 3, 31)]
    for idx, fund in enumerate(funds):
        for qidx, report_date in enumerate(report_dates):
            cur.execute(
                """
                SELECT report_id FROM fund_holding_report
                WHERE fund_id=%s AND report_date=%s AND COALESCE(report_type,'')='STOCK'
                """,
                (fund["fund_id"], report_date),
            )
            row = cur.fetchone()
            ratios = [9.2, 7.8, 6.6, 5.7, 4.9, 4.2, 3.6, 3.1, 2.7, 2.3]
            ratios = [max(0.8, r - (idx % 4) * 0.35 - qidx * 0.2) for r in ratios]
            stock_ratio = sum(ratios) + 18 + idx % 10
            top10 = sum(ratios)
            if row:
                report_id = int(row[0])
                cur.execute(
                    """
                    UPDATE fund_holding_report
                    SET stock_ratio=%s, bond_ratio=%s, cash_ratio=%s, top10_concentration=%s
                    WHERE report_id=%s
                    """,
                    (q(stock_ratio, 2), q(max(0, 80 - stock_ratio), 2), q(5 + idx % 4, 2), q(top10, 2), report_id),
                )
                cur.execute("DELETE FROM fund_holding_detail WHERE report_id=%s", (report_id,))
            else:
                report_id = next_report_id
                next_report_id += 1
                cur.execute(
                    """
                    INSERT INTO fund_holding_report (
                        report_id, fund_id, report_date, report_type,
                        stock_ratio, bond_ratio, cash_ratio, top10_concentration, created_time
                    ) VALUES (%s, %s, %s, 'STOCK', %s, %s, %s, %s, CURRENT_TIMESTAMP)
                    """,
                    (report_id, fund["fund_id"], report_date, q(stock_ratio, 2), q(max(0, 80 - stock_ratio), 2), q(5 + idx % 4, 2), q(top10, 2)),
                )
            for hidx, ratio in enumerate(ratios):
                code, name, industry = HOLDING_TEMPLATES[(idx + hidx + qidx) % len(HOLDING_TEMPLATES)]
                cur.execute(
                    """
                    INSERT INTO fund_holding_detail (
                        holding_id, report_id, security_code, security_name,
                        security_type, industry_name, market_value, holding_ratio
                    ) VALUES (%s, %s, %s, %s, 'STOCK', %s, %s, %s)
                    """,
                    (next_holding_id, report_id, code, name, industry, q(120 + idx * 9 + hidx * 13, 4), q(ratio, 4)),
                )
                next_holding_id += 1


def ensure_announcements_attributions(cur, funds: list[dict[str, Any]]) -> None:
    next_ann_id = next_id(cur, "fund_announcement", "announcement_id")
    next_attr_id = next_id(cur, "fund_attribution", "attribution_id")
    for idx, fund in enumerate(funds):
        cur.execute("DELETE FROM fund_announcement WHERE fund_id=%s", (fund["fund_id"],))
        ann_templates = [
            ("2024年年度报告摘要", "定期报告", "披露基金年度运作、资产配置和风险收益表现。"),
            ("2024年第4季度报告", "定期报告", "披露基金季度持仓、净值表现和投资策略。"),
            ("基金经理投资观点更新", "临时公告", "说明近期市场判断、组合调整和风险提示。"),
        ]
        for aidx, (title, category, summary) in enumerate(ann_templates):
            cur.execute(
                """
                INSERT INTO fund_announcement (
                    announcement_id, fund_id, title, announcement_date,
                    category, source_url, summary, created_time
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
                """,
                (
                    next_ann_id,
                    fund["fund_id"],
                    f"{fund['fund_name']}{title}",
                    date(2025, 1 + aidx, 10 + idx % 12),
                    category,
                    f"https://example.com/fund/{fund['fund_code']}/announcement/{aidx + 1}",
                    summary,
                ),
            )
            next_ann_id += 1
        cur.execute("DELETE FROM fund_attribution WHERE fund_id=%s", (fund["fund_id"],))
        for period, report_date in [("1Y", date(2026, 6, 30)), ("YTD", date(2026, 6, 30))]:
            allocation = 0.8 + (idx % 5) * 0.32
            selection = 0.5 + (idx % 4) * 0.28
            industry = -0.2 + (idx % 6) * 0.18
            style = 0.1 + (idx % 3) * 0.22
            cur.execute(
                """
                INSERT INTO fund_attribution (
                    attribution_id, fund_id, report_date, period_code,
                    allocation_effect, selection_effect, industry_effect, style_effect,
                    attribution_summary
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    next_attr_id,
                    fund["fund_id"],
                    report_date,
                    period,
                    q(allocation),
                    q(selection),
                    q(industry),
                    q(style),
                    "模拟归因：收益主要来自资产配置和个股选择，行业暴露对短期波动有一定影响。",
                ),
            )
            next_attr_id += 1


def ensure_user_demo_relations(cur, funds: list[dict[str, Any]]) -> None:
    cur.execute("DELETE FROM user_favorite WHERE fund_id NOT IN %s", (tuple(f["fund_id"] for f in funds),))
    next_fav_id = next_id(cur, "user_favorite", "favorite_id")
    for idx, fund in enumerate(funds[:12]):
        cur.execute(
            """
            SELECT COUNT(*) FROM user_favorite
            WHERE user_id=1 AND fund_id=%s AND favorite_group=%s
            """,
            (fund["fund_id"], "阶段二样本"),
        )
        if cur.fetchone()[0]:
            continue
        cur.execute(
            """
            INSERT INTO user_favorite (
                favorite_id, user_id, fund_id, favorite_group, remark, created_time
            ) VALUES (%s, 1, %s, '阶段二样本', %s, CURRENT_TIMESTAMP)
            """,
            (next_fav_id, fund["fund_id"], "30只完整样本基金自选观察"),
        )
        next_fav_id += 1


def main() -> None:
    if not DB["password"]:
        raise SystemExit("FUND_DB_PASSWORD is empty. Set it first.")
    with psycopg2.connect(**DB) as conn:
        with conn.cursor() as cur:
            funds = fetch_keep_funds(cur)
            if len(funds) != 30:
                raise RuntimeError(f"Expected 30 funds to keep, got {len(funds)}")
            keep_ids = [int(f["fund_id"]) for f in funds]
            cleanup_non_sample_funds(cur, keep_ids)
            funds = fetch_keep_funds(cur)
            normalize_fund_base(cur, funds)
            ensure_manager_relations(cur, funds)
            tag_ids = ensure_tags(cur)
            ensure_metrics_score_tags(cur, funds, tag_ids)
            ensure_holdings(cur, funds)
            ensure_announcements_attributions(cur, funds)
            ensure_user_demo_relations(cur, funds)
            conn.commit()
            print("30-fund full sample rebuild completed.")
            print("kept fund_ids:", ",".join(str(i) for i in keep_ids))


if __name__ == "__main__":
    main()
