"""
Stage 2 importer: historical NAV and real metric calculation.

This script intentionally focuses on a small, repeatable stage-2 slice:
- fetch historical open-fund NAV from AkShare for existing funds
- upsert fund_nav rows
- calculate return, max drawdown, volatility and Sharpe ratio
- update fund_performance_metric, fund_score, fund_info.risk_level
- generate a small set of rule-based tag relations
- write data_import_batch and data_import_error records

Run from backend project root:
    $env:FUND_DB_PASSWORD="1234"
    $env:PYTHONIOENCODING="utf-8"
    .\\.venv\\Scripts\\python.exe .\\scripts\\stage2_import_nav_metrics.py --limit 30
"""

from __future__ import annotations

import argparse
import math
import os
import sys
import time
from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import Any

import akshare as ak
import pandas as pd
import psycopg2

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


@dataclass
class DbConfig:
    host: str
    port: int
    database: str
    user: str
    password: str


PERIODS = {
    "1M": 30,
    "3M": 90,
    "6M": 180,
    "1Y": 365,
}

STAGE2_TAGS = [
    ("STAGE2_NAV_IMPORTED", "历史净值已导入", "来源", "阶段二通过 AkShare 导入历史净值"),
    ("STAGE2_LOW_DRAWDOWN", "低回撤", "风险", "真实最大回撤不高于 5%"),
    ("STAGE2_HIGH_VOLATILITY", "波动较大", "风险", "真实年化波动率不低于 20%"),
    ("STAGE2_HIGH_RETURN_1Y", "近一年高收益", "收益", "真实近一年收益不低于 15%"),
    ("STAGE2_BOND_STABLE", "债券稳健", "风格", "债券型基金且真实最大回撤不高于 8%"),
    ("STAGE2_LONG_TERM_GOOD", "长期绩优", "收益", "成立以来或近一年真实收益表现较好"),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import historical NAV and calculate real metrics.")
    parser.add_argument("--host", default=os.getenv("FUND_DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("FUND_DB_PORT", "54321")))
    parser.add_argument("--database", default=os.getenv("FUND_DB_NAME", "fund_research"))
    parser.add_argument("--user", default=os.getenv("FUND_DB_USER", "system"))
    parser.add_argument("--password", default=os.getenv("FUND_DB_PASSWORD", ""))
    parser.add_argument("--limit", type=int, default=30)
    parser.add_argument("--sleep", type=float, default=0.25)
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def connect_db(config: DbConfig):
    return psycopg2.connect(
        host=config.host,
        port=config.port,
        dbname=config.database,
        user=config.user,
        password=config.password,
    )


def q(value: float | int | Decimal | None, scale: int = 4) -> Decimal | None:
    if value is None or pd.isna(value):
        return None
    return Decimal(str(float(value))).quantize(Decimal("1." + "0" * scale), rounding=ROUND_HALF_UP)


def q2(value: float | int | Decimal | None) -> Decimal | None:
    return q(value, 2)


def next_id(cur, table: str, column: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX({column}), 0) + 1 FROM {table}")
    return int(cur.fetchone()[0])


def create_batch(cur, limit: int) -> int:
    batch_id = next_id(cur, "data_import_batch", "batch_id")
    batch_no = "STAGE2_NAV_" + datetime.now().strftime("%Y%m%d%H%M%S")
    cur.execute(
        """
        INSERT INTO data_import_batch (
            batch_id, batch_no, import_type, file_name, file_hash,
            total_count, success_count, error_count, field_mapping,
            validate_summary, status, operator_id, created_time, finished_time
        ) VALUES (
            %s, %s, 'STAGE2_NAV_METRIC', 'stage2_import_nav_metrics.py', NULL,
            %s, 0, 0, %s, %s, 'RUNNING', 1, CURRENT_TIMESTAMP, NULL
        )
        """,
        (
            batch_id,
            batch_no,
            limit,
            '{"source":"AkShare fund_open_fund_info_em","target":"fund_nav,fund_performance_metric"}',
            "阶段二历史净值导入和真实指标计算任务启动。",
        ),
    )
    return batch_id


def finish_batch(cur, batch_id: int, total: int, success: int, errors: int) -> None:
    status = "SUCCESS" if errors == 0 else ("PARTIAL_SUCCESS" if success > 0 else "FAILED")
    cur.execute(
        """
        UPDATE data_import_batch
        SET total_count = %s,
            success_count = %s,
            error_count = %s,
            validate_summary = %s,
            status = %s,
            finished_time = CURRENT_TIMESTAMP
        WHERE batch_id = %s
        """,
        (
            total,
            success,
            errors,
            f"阶段二历史净值导入完成：成功 {success} 只，异常 {errors} 只。",
            status,
            batch_id,
        ),
    )


def write_error(cur, batch_id: int, row_no: int, fund_code: str, reason: str, raw: str = "") -> None:
    cur.execute(
        """
        INSERT INTO data_import_error (
            error_id, batch_id, row_no, fund_code, error_field,
            error_reason, suggestion, raw_data, status, created_time
        ) VALUES (
            %s, %s, %s, %s, 'akshare_nav', %s,
            '稍后重试，或检查该基金是否为 ETF/LOF/QDII 等特殊类型并使用专门接口。',
            %s, 'OPEN', CURRENT_TIMESTAMP
        )
        """,
        (next_id(cur, "data_import_error", "error_id"), batch_id, row_no, fund_code, reason[:240], raw[:1000]),
    )


def fetch_target_funds(cur, limit: int) -> list[dict[str, Any]]:
    cur.execute(
        """
        SELECT fund_id, fund_code, fund_name, fund_type
        FROM fund_info
        WHERE fund_code IS NOT NULL
        ORDER BY fund_id
        LIMIT %s
        """,
        (limit,),
    )
    cols = [c[0] for c in cur.description]
    return [dict(zip(cols, row)) for row in cur.fetchall()]


def normalize_nav_frame(df: pd.DataFrame) -> pd.DataFrame:
    if df is None or df.empty:
        return pd.DataFrame(columns=["nav_date", "unit_nav", "daily_return"])
    rows = []
    for _, row in df.iterrows():
        try:
            nav_date = pd.to_datetime(row.iloc[0]).date()
            unit_nav = float(row.iloc[1])
            daily_return = None
            if len(row) > 2 and not pd.isna(row.iloc[2]):
                daily_return = float(str(row.iloc[2]).replace("%", ""))
            if unit_nav > 0:
                rows.append((nav_date, unit_nav, daily_return))
        except Exception:
            continue
    result = pd.DataFrame(rows, columns=["nav_date", "unit_nav", "daily_return"])
    if result.empty:
        return result
    result = result.drop_duplicates(subset=["nav_date"]).sort_values("nav_date").reset_index(drop=True)
    return result


def fetch_nav_from_akshare(fund_code: str) -> pd.DataFrame:
    raw = ak.fund_open_fund_info_em(symbol=fund_code, indicator="单位净值走势")
    return normalize_nav_frame(raw)


def upsert_nav_rows(cur, fund_id: int, nav_df: pd.DataFrame) -> int:
    inserted = 0
    next_nav_id = next_id(cur, "fund_nav", "nav_id")
    for _, row in nav_df.iterrows():
        cur.execute(
            """
            UPDATE fund_nav
            SET unit_nav = %s,
                acc_nav = COALESCE(acc_nav, %s),
                daily_return = %s
            WHERE fund_id = %s AND nav_date = %s
            """,
            (q(row["unit_nav"]), q(row["unit_nav"]), q(row["daily_return"]), fund_id, row["nav_date"]),
        )
        if cur.rowcount:
            continue
        cur.execute(
            """
            INSERT INTO fund_nav (nav_id, fund_id, nav_date, unit_nav, acc_nav, daily_return, created_time)
            VALUES (%s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
            """,
            (next_nav_id, fund_id, row["nav_date"], q(row["unit_nav"]), q(row["unit_nav"]), q(row["daily_return"])),
        )
        next_nav_id += 1
        inserted += 1
    return inserted


def slice_by_days(nav_df: pd.DataFrame, days: int) -> pd.DataFrame:
    latest = nav_df["nav_date"].max()
    cutoff_date = latest.fromordinal(latest.toordinal() - days)
    sliced = nav_df[nav_df["nav_date"] >= cutoff_date]
    return sliced if len(sliced) >= 2 else nav_df


def calc_metric(nav_df: pd.DataFrame, period_code: str, days: int | None) -> dict[str, Any] | None:
    if nav_df.empty or len(nav_df) < 2:
        return None
    period_df = slice_by_days(nav_df, days) if days is not None else nav_df
    if len(period_df) < 2:
        return None
    start = float(period_df.iloc[0]["unit_nav"])
    end = float(period_df.iloc[-1]["unit_nav"])
    if start <= 0 or end <= 0:
        return None
    date_start = period_df.iloc[0]["nav_date"]
    date_end = period_df.iloc[-1]["nav_date"]
    actual_days = max((date_end - date_start).days, 1)
    returns = period_df["unit_nav"].astype(float).pct_change().dropna()
    return_rate = (end / start - 1.0) * 100.0
    annual_return = ((end / start) ** (365.0 / actual_days) - 1.0) * 100.0 if actual_days > 0 else return_rate
    volatility = float(returns.std(ddof=0) * math.sqrt(252) * 100.0) if len(returns) else 0.0
    running_max = period_df["unit_nav"].astype(float).cummax()
    drawdown = period_df["unit_nav"].astype(float) / running_max - 1.0
    max_drawdown = abs(float(drawdown.min() * 100.0))
    sharpe = ((annual_return - 2.0) / volatility) if volatility and volatility > 0 else None
    return {
        "period_code": period_code,
        "stat_date": date_end,
        "return_rate": return_rate,
        "annual_return": annual_return,
        "volatility": volatility,
        "max_drawdown": max_drawdown,
        "sharpe_ratio": sharpe,
    }


def calc_all_metrics(nav_df: pd.DataFrame) -> dict[str, dict[str, Any]]:
    metrics: dict[str, dict[str, Any]] = {}
    for code, days in PERIODS.items():
        metric = calc_metric(nav_df, code, days)
        if metric:
            metrics[code] = metric
    ytd = nav_df[nav_df["nav_date"] >= date(nav_df["nav_date"].max().year, 1, 1)]
    metric = calc_metric(ytd if len(ytd) >= 2 else nav_df, "YTD", None)
    if metric:
        metrics["YTD"] = metric
    metric = calc_metric(nav_df, "SINCE", None)
    if metric:
        metrics["SINCE"] = metric
    return metrics


def upsert_metric(cur, fund_id: int, metric: dict[str, Any]) -> None:
    cur.execute(
        """
        UPDATE fund_performance_metric
        SET return_rate = %s,
            annual_return = %s,
            volatility = %s,
            max_drawdown = %s,
            sharpe_ratio = %s
        WHERE fund_id = %s AND stat_date = %s AND period_code = %s
        """,
        (
            q(metric["return_rate"]),
            q(metric["annual_return"]),
            q(metric["volatility"]),
            q(metric["max_drawdown"]),
            q(metric["sharpe_ratio"]),
            fund_id,
            metric["stat_date"],
            metric["period_code"],
        ),
    )
    if cur.rowcount:
        return
    cur.execute(
        """
        INSERT INTO fund_performance_metric (
            metric_id, fund_id, stat_date, period_code, return_rate,
            annual_return, volatility, max_drawdown, sharpe_ratio
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
        (
            next_id(cur, "fund_performance_metric", "metric_id"),
            fund_id,
            metric["stat_date"],
            metric["period_code"],
            q(metric["return_rate"]),
            q(metric["annual_return"]),
            q(metric["volatility"]),
            q(metric["max_drawdown"]),
            q(metric["sharpe_ratio"]),
        ),
    )


def infer_risk_level(fund_type: str | None, metric: dict[str, Any] | None) -> str:
    text = fund_type or ""
    max_dd = float(metric.get("max_drawdown", 12.0)) if metric else 12.0
    vol = float(metric.get("volatility", 10.0)) if metric else 10.0
    if "货币" in text or max_dd <= 1.0:
        return "LOW"
    if "债" in text and max_dd <= 8.0:
        return "MEDIUM_LOW"
    if max_dd <= 5.0 and vol <= 8.0:
        return "MEDIUM_LOW"
    if max_dd <= 15.0:
        return "MEDIUM"
    if max_dd <= 25.0:
        return "MEDIUM_HIGH"
    return "HIGH"


def update_risk(cur, fund_id: int, risk_level: str) -> None:
    cur.execute(
        "UPDATE fund_info SET risk_level = %s, updated_time = CURRENT_TIMESTAMP WHERE fund_id = %s",
        (risk_level, fund_id),
    )


def upsert_score(cur, fund_id: int, metric: dict[str, Any], risk_level: str) -> None:
    stat_date = metric["stat_date"]
    ret = float(metric.get("return_rate") or 0)
    max_dd = float(metric.get("max_drawdown") or 0)
    vol = float(metric.get("volatility") or 0)
    yield_score = max(20, min(95, 60 + ret))
    risk_score = max(20, min(95, 95 - max_dd * 2.0))
    stability_score = max(20, min(95, 95 - vol * 3.0))
    manager_score = 70
    scale_score = 65
    total = yield_score * 0.30 + risk_score * 0.30 + stability_score * 0.20 + manager_score * 0.10 + scale_score * 0.10
    if risk_level == "HIGH":
        total -= 3
    recommend = "RECOMMEND" if total >= 75 else ("WATCH" if total >= 65 else "NEUTRAL")
    explain = "阶段二基于历史净值真实计算收益、最大回撤、波动率后生成的评分。"
    cur.execute(
        """
        UPDATE fund_score
        SET yield_score=%s, risk_score=%s, stability_score=%s,
            manager_score=%s, scale_score=%s, total_score=%s,
            recommend_level=%s, explain_text=%s
        WHERE fund_id=%s AND score_date=%s
        """,
        (q2(yield_score), q2(risk_score), q2(stability_score), q2(manager_score), q2(scale_score), q2(total), recommend, explain, fund_id, stat_date),
    )
    if cur.rowcount:
        return
    cur.execute(
        """
        INSERT INTO fund_score (
            score_id, fund_id, score_date, yield_score, risk_score,
            stability_score, manager_score, scale_score, total_score,
            recommend_level, explain_text, created_time
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
        """,
        (next_id(cur, "fund_score", "score_id"), fund_id, stat_date, q2(yield_score), q2(risk_score), q2(stability_score), q2(manager_score), q2(scale_score), q2(total), recommend, explain),
    )


def ensure_stage2_tags(cur) -> dict[str, int]:
    ids: dict[str, int] = {}
    for code, name, category, desc in STAGE2_TAGS:
        cur.execute("SELECT tag_id FROM fund_tag WHERE tag_code=%s", (code,))
        row = cur.fetchone()
        if row:
            tag_id = int(row[0])
            cur.execute(
                """
                UPDATE fund_tag
                SET tag_name=%s, tag_category=%s, description=%s,
                    rule_expression=%s, enabled=1
                WHERE tag_id=%s
                """,
                (name, category, desc, desc, tag_id),
            )
        else:
            tag_id = next_id(cur, "fund_tag", "tag_id")
            cur.execute(
                """
                INSERT INTO fund_tag (tag_id, tag_code, tag_name, tag_category, description, rule_expression, enabled)
                VALUES (%s, %s, %s, %s, %s, %s, 1)
                """,
                (tag_id, code, name, category, desc, desc),
            )
        ids[code] = tag_id
    return ids


def tag_codes_for(fund_type: str | None, metrics: dict[str, dict[str, Any]]) -> list[str]:
    codes = ["STAGE2_NAV_IMPORTED"]
    metric = metrics.get("1Y") or metrics.get("SINCE")
    if not metric:
        return codes
    max_dd = float(metric.get("max_drawdown") or 0)
    vol = float(metric.get("volatility") or 0)
    ret = float(metric.get("return_rate") or 0)
    text = fund_type or ""
    if max_dd <= 5:
        codes.append("STAGE2_LOW_DRAWDOWN")
    if vol >= 20:
        codes.append("STAGE2_HIGH_VOLATILITY")
    if ret >= 15:
        codes.append("STAGE2_HIGH_RETURN_1Y")
    if "债" in text and max_dd <= 8:
        codes.append("STAGE2_BOND_STABLE")
    since_ret = float((metrics.get("SINCE") or {}).get("return_rate") or 0)
    if ret >= 10 or since_ret >= 20:
        codes.append("STAGE2_LONG_TERM_GOOD")
    return sorted(set(codes))


def upsert_tags(cur, fund_id: int, fund_type: str | None, metrics: dict[str, dict[str, Any]], tag_ids: dict[str, int]) -> None:
    tag_date = (metrics.get("1Y") or metrics.get("SINCE"))["stat_date"]
    for code in tag_codes_for(fund_type, metrics):
        tag_id = tag_ids[code]
        cur.execute(
            """
            UPDATE fund_tag_relation
            SET confidence=100.00, source='STAGE2_RULE'
            WHERE fund_id=%s AND tag_id=%s AND tag_date=%s
            """,
            (fund_id, tag_id, tag_date),
        )
        if cur.rowcount:
            continue
        cur.execute(
            """
            INSERT INTO fund_tag_relation (relation_id, fund_id, tag_id, confidence, source, tag_date, created_time)
            VALUES (%s, %s, %s, 100.00, 'STAGE2_RULE', %s, CURRENT_TIMESTAMP)
            """,
            (next_id(cur, "fund_tag_relation", "relation_id"), fund_id, tag_id, tag_date),
        )


def process_fund(cur, fund: dict[str, Any], tag_ids: dict[str, int]) -> int:
    nav_df = fetch_nav_from_akshare(fund["fund_code"])
    if nav_df.empty or len(nav_df) < 2:
        raise RuntimeError("AkShare returned empty or insufficient NAV data")
    inserted = upsert_nav_rows(cur, fund["fund_id"], nav_df)
    metrics = calc_all_metrics(nav_df)
    if not metrics:
        raise RuntimeError("No metrics calculated from NAV data")
    for metric in metrics.values():
        upsert_metric(cur, fund["fund_id"], metric)
    risk_source = metrics.get("1Y") or metrics.get("SINCE")
    risk_level = infer_risk_level(fund.get("fund_type"), risk_source)
    update_risk(cur, fund["fund_id"], risk_level)
    if risk_source:
        upsert_score(cur, fund["fund_id"], risk_source, risk_level)
    upsert_tags(cur, fund["fund_id"], fund.get("fund_type"), metrics, tag_ids)
    return inserted


def main() -> None:
    args = parse_args()
    if not args.password and not args.dry_run:
        raise SystemExit("FUND_DB_PASSWORD is empty. Set it first, e.g. $env:FUND_DB_PASSWORD='1234'")
    config = DbConfig(args.host, args.port, args.database, args.user, args.password)
    total = 0
    success = 0
    errors = 0
    with connect_db(config) as conn:
        with conn.cursor() as cur:
            funds = fetch_target_funds(cur, args.limit)
            total = len(funds)
            batch_id = create_batch(cur, total)
            tag_ids = ensure_stage2_tags(cur)
            if args.dry_run:
                print(f"[DRY-RUN] Prepared {total} funds, batch_id would be {batch_id}")
                conn.rollback()
                return
            for index, fund in enumerate(funds, start=1):
                try:
                    inserted = process_fund(cur, fund, tag_ids)
                    success += 1
                    print(f"[{index}/{total}] {fund['fund_code']} {fund['fund_name']} OK, new NAV rows: {inserted}")
                    conn.commit()
                except Exception as exc:
                    errors += 1
                    conn.rollback()
                    with conn.cursor() as err_cur:
                        write_error(err_cur, batch_id, index, fund["fund_code"], str(exc), repr(fund))
                        conn.commit()
                    print(f"[{index}/{total}] {fund['fund_code']} FAILED: {exc}")
                time.sleep(args.sleep)
            with conn.cursor() as cur2:
                finish_batch(cur2, batch_id, total, success, errors)
            conn.commit()
    print(f"Stage 2 import completed: total={total}, success={success}, errors={errors}")


if __name__ == "__main__":
    main()
