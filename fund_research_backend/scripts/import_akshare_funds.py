"""
Import necessary fund research data from AkShare into KingbaseES.

This development importer writes the data needed by the current backend and the
next frontend pages:
- fund_info: fund code/name/type/risk level/basic detail
- fund_company, fund_manager, fund_manager_relation: detail enrichment for the first N funds
- fund_nav: latest NAV
- fund_performance_metric: rank-based period returns
- fund_score: simple rule-based seed score
- fund_tag, fund_tag_relation: basic tag system and generated fund tags

Run from the project root:
    $env:FUND_DB_PASSWORD = "1234"
    $env:PYTHONIOENCODING = "utf-8"
    .\\.venv\\Scripts\\python.exe .\\scripts\\import_akshare_funds.py --limit 100 --detail-limit 20
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, InvalidOperation
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


BASIC_TAGS = [
    ("FUND_TYPE_STOCK", "股票型", "基金类型", "股票型或股票相关基金"),
    ("FUND_TYPE_MIXED", "混合型", "基金类型", "混合型基金"),
    ("FUND_TYPE_BOND", "债券型", "基金类型", "债券型基金"),
    ("FUND_TYPE_MONEY", "货币型", "基金类型", "货币型基金"),
    ("FUND_TYPE_INDEX", "指数型", "基金类型", "指数基金"),
    ("FUND_TYPE_QDII", "QDII", "基金类型", "QDII 或海外资产相关基金"),
    ("RISK_HIGH", "高风险", "风险", "权益类或波动较高基金"),
    ("RISK_MEDIUM", "中风险", "风险", "混合型或中等风险基金"),
    ("RISK_LOW", "低风险", "风险", "货币、短债等低风险基金"),
    ("RETURN_1Y_HIGH", "近一年高收益", "收益", "近一年收益率较高"),
    ("RETURN_6M_HIGH", "近六月高收益", "收益", "近六月收益率较高"),
    ("SHORT_TERM_STRONG", "短期强势", "收益", "近一月或近一周表现强"),
    ("TOTAL_SCORE_RECOMMEND", "综合评分推荐", "评分", "综合评分达到推荐区间"),
    ("AKSHARE_IMPORTED", "AkShare导入", "来源", "由 AkShare 脚本导入"),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import AkShare fund data into KingbaseES.")
    parser.add_argument("--host", default=os.getenv("FUND_DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("FUND_DB_PORT", "54321")))
    parser.add_argument("--database", default=os.getenv("FUND_DB_NAME", "fund_research"))
    parser.add_argument("--user", default=os.getenv("FUND_DB_USER", "system"))
    parser.add_argument("--password", default=os.getenv("FUND_DB_PASSWORD", ""))
    parser.add_argument("--limit", type=int, default=100, help="Maximum number of funds to import.")
    parser.add_argument("--detail-limit", type=int, default=20, help="Enrich the first N funds with company/manager detail.")
    parser.add_argument("--dry-run", action="store_true", help="Fetch and transform data without writing DB.")
    parser.add_argument("--no-score", action="store_true", help="Do not write fund_score rows.")
    parser.add_argument("--no-tags", action="store_true", help="Do not write tags.")
    parser.add_argument("--no-details", action="store_true", help="Skip per-fund detail enrichment.")
    return parser.parse_args()


def clean_str(value: Any) -> str | None:
    if value is None or pd.isna(value):
        return None
    text = str(value).strip()
    return text if text and text.lower() != "nan" else None


def to_decimal(value: Any, scale: int = 4) -> Decimal | None:
    text = clean_str(value)
    if not text or text in {"-", "--"}:
        return None
    text = text.replace("%", "").replace(",", "")
    try:
        return Decimal(text).quantize(Decimal("1." + "0" * scale))
    except (InvalidOperation, ValueError):
        return None


def to_score(value: Any) -> Decimal | None:
    number = to_decimal(value, scale=4)
    if number is None:
        return None
    score = Decimal("60") + number / Decimal("2")
    score = min(Decimal("95"), max(Decimal("20"), score))
    return score.quantize(Decimal("1.00"))


def parse_date(value: Any) -> date | None:
    text = clean_str(value)
    if not text:
        return None
    match = re.search(r"\d{4}-\d{2}-\d{2}", text)
    if not match:
        return None
    try:
        return date.fromisoformat(match.group(0))
    except ValueError:
        return None


def parse_yi_amount(value: Any) -> Decimal | None:
    text = clean_str(value)
    if not text:
        return None
    match = re.search(r"[-+]?\d+(?:\.\d+)?", text.replace(",", ""))
    if not match:
        return None
    amount = Decimal(match.group(0))
    if "万" in text:
        amount = amount / Decimal("10000")
    return amount.quantize(Decimal("1.00"))


def infer_risk_level(fund_type: str | None) -> str | None:
    text = fund_type or ""
    if any(token in text for token in ["货币", "短债"]):
        return "LOW"
    if "债券" in text:
        return "MEDIUM_LOW"
    if any(token in text for token in ["混合", "FOF"]):
        return "MEDIUM"
    if any(token in text for token in ["股票", "指数", "QDII", "商品"]):
        return "HIGH"
    return "MEDIUM"


def find_nav_date(columns: list[Any]) -> date:
    for col in columns:
        match = re.search(r"(\d{4}-\d{2}-\d{2})-", str(col))
        if match:
            return date.fromisoformat(match.group(1))
    return date.today()


def load_akshare_frames() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    print("[1/4] Fetching fund names from AkShare...")
    fund_names = ak.fund_name_em()
    print(f"      fund_name_em rows: {len(fund_names)}")

    print("[2/4] Fetching latest daily NAV from AkShare...")
    daily_nav = ak.fund_open_fund_daily_em()
    print(f"      fund_open_fund_daily_em rows: {len(daily_nav)}")

    print("[3/4] Fetching fund ranking data from AkShare...")
    try:
        rank = ak.fund_open_fund_rank_em()
        print(f"      fund_open_fund_rank_em rows: {len(rank)}")
    except Exception as exc:
        print(f"      rank fetch failed, score/metric import will use defaults: {exc}")
        rank = pd.DataFrame()

    return fund_names, daily_nav, rank


def build_records(fund_names: pd.DataFrame, daily_nav: pd.DataFrame, rank: pd.DataFrame, limit: int) -> list[dict[str, Any]]:
    name_map: dict[str, dict[str, Any]] = {}
    for _, row in fund_names.iterrows():
        code = clean_str(row.iloc[0])
        if not code:
            continue
        name_map[code] = {
            "fund_name": clean_str(row.iloc[2]) if len(row) > 2 else None,
            "fund_type": clean_str(row.iloc[3]) if len(row) > 3 else None,
        }

    rank_map: dict[str, dict[str, Any]] = {}
    if not rank.empty:
        for _, row in rank.iterrows():
            code = clean_str(row.iloc[1]) if len(row) > 1 else None
            if not code:
                continue
            rank_map[code] = {
                "rank_date": clean_str(row.iloc[3]) if len(row) > 3 else None,
                "unit_nav": row.iloc[4] if len(row) > 4 else None,
                "acc_nav": row.iloc[5] if len(row) > 5 else None,
                "daily_return": row.iloc[6] if len(row) > 6 else None,
                "return_1w": row.iloc[7] if len(row) > 7 else None,
                "return_1m": row.iloc[8] if len(row) > 8 else None,
                "return_3m": row.iloc[9] if len(row) > 9 else None,
                "return_6m": row.iloc[10] if len(row) > 10 else None,
                "return_1y": row.iloc[11] if len(row) > 11 else None,
                "return_2y": row.iloc[12] if len(row) > 12 else None,
                "return_3y": row.iloc[13] if len(row) > 13 else None,
                "return_ytd": row.iloc[14] if len(row) > 14 else None,
                "return_since": row.iloc[15] if len(row) > 15 else None,
            }

    nav_date = find_nav_date(list(daily_nav.columns))
    records: list[dict[str, Any]] = []
    for _, row in daily_nav.iterrows():
        if len(records) >= limit:
            break
        code = clean_str(row.iloc[0])
        if not code:
            continue
        fund_name = clean_str(row.iloc[1]) or name_map.get(code, {}).get("fund_name")
        if not fund_name:
            continue
        fund_type = name_map.get(code, {}).get("fund_type")
        records.append(
            {
                "fund_code": code,
                "fund_name": fund_name,
                "fund_type": fund_type,
                "risk_level": infer_risk_level(fund_type),
                "nav_date": nav_date,
                "unit_nav": to_decimal(row.iloc[2], scale=4) if len(row) > 2 else None,
                "acc_nav": to_decimal(row.iloc[3], scale=4) if len(row) > 3 else None,
                "daily_return": to_decimal(row.iloc[7], scale=4) if len(row) > 7 else None,
                "rank_info": rank_map.get(code, {}),
            }
        )
    return records


def connect_db(config: DbConfig):
    return psycopg2.connect(
        host=config.host,
        port=config.port,
        dbname=config.database,
        user=config.user,
        password=config.password,
    )


def get_fund_id(cur, fund_code: str) -> int | None:
    cur.execute("SELECT fund_id FROM fund_info WHERE fund_code = %s", (fund_code,))
    row = cur.fetchone()
    return int(row[0]) if row else None


def upsert_fund_info(cur, record: dict[str, Any]) -> int:
    fund_id = get_fund_id(cur, record["fund_code"])
    if fund_id:
        cur.execute(
            """
            UPDATE fund_info
            SET fund_name = %s,
                fund_type = %s,
                risk_level = %s,
                source = 'AkShare',
                status = 1,
                updated_time = CURRENT_TIMESTAMP
            WHERE fund_id = %s
            """,
            (record["fund_name"], record["fund_type"], record.get("risk_level"), fund_id),
        )
        return fund_id

    cur.execute(
        """
        INSERT INTO fund_info (
            fund_code, fund_name, fund_type, risk_level, status, source, updated_time
        ) VALUES (%s, %s, %s, %s, 1, 'AkShare', CURRENT_TIMESTAMP)
        """,
        (record["fund_code"], record["fund_name"], record["fund_type"], record.get("risk_level")),
    )
    fund_id = get_fund_id(cur, record["fund_code"])
    if not fund_id:
        raise RuntimeError(f"Failed to insert fund_info for {record['fund_code']}")
    return fund_id


def upsert_nav(cur, fund_id: int, record: dict[str, Any]) -> None:
    cur.execute(
        """
        UPDATE fund_nav
        SET unit_nav = %s,
            acc_nav = %s,
            daily_return = %s
        WHERE fund_id = %s AND nav_date = %s
        """,
        (record["unit_nav"], record["acc_nav"], record["daily_return"], fund_id, record["nav_date"]),
    )
    if cur.rowcount:
        return
    cur.execute(
        """
        INSERT INTO fund_nav (fund_id, nav_date, unit_nav, acc_nav, daily_return)
        VALUES (%s, %s, %s, %s, %s)
        """,
        (fund_id, record["nav_date"], record["unit_nav"], record["acc_nav"], record["daily_return"]),
    )


def upsert_score(cur, fund_id: int, record: dict[str, Any]) -> None:
    rank_info = record.get("rank_info") or {}
    score_date = record["nav_date"]
    yield_score = to_score(rank_info.get("return_1y") or rank_info.get("return_6m") or record.get("daily_return"))
    if yield_score is None:
        yield_score = Decimal("60.00")
    risk_score = Decimal("70.00")
    if record.get("risk_level") == "HIGH":
        risk_score = Decimal("60.00")
    elif record.get("risk_level") in {"LOW", "MEDIUM_LOW"}:
        risk_score = Decimal("82.00")
    stability_score = Decimal("70.00")
    manager_score = Decimal("60.00")
    scale_score = Decimal("60.00")
    total_score = (
        yield_score * Decimal("0.30")
        + risk_score * Decimal("0.30")
        + stability_score * Decimal("0.20")
        + manager_score * Decimal("0.10")
        + scale_score * Decimal("0.10")
    ).quantize(Decimal("1.00"))
    recommend_level = "WATCH" if total_score < Decimal("75") else "RECOMMEND"
    explain_text = "AkShare seed score for development; replace with formal scoring model later."

    cur.execute(
        """
        UPDATE fund_score
        SET yield_score = %s,
            risk_score = %s,
            stability_score = %s,
            manager_score = %s,
            scale_score = %s,
            total_score = %s,
            recommend_level = %s,
            explain_text = %s
        WHERE fund_id = %s AND score_date = %s
        """,
        (
            yield_score,
            risk_score,
            stability_score,
            manager_score,
            scale_score,
            total_score,
            recommend_level,
            explain_text,
            fund_id,
            score_date,
        ),
    )
    if cur.rowcount:
        return
    cur.execute(
        """
        INSERT INTO fund_score (
            fund_id, score_date, yield_score, risk_score, stability_score,
            manager_score, scale_score, total_score, recommend_level, explain_text
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
        (
            fund_id,
            score_date,
            yield_score,
            risk_score,
            stability_score,
            manager_score,
            scale_score,
            total_score,
            recommend_level,
            explain_text,
        ),
    )


def upsert_metrics(cur, fund_id: int, record: dict[str, Any]) -> None:
    rank_info = record.get("rank_info") or {}
    period_map = {
        "1W": rank_info.get("return_1w"),
        "1M": rank_info.get("return_1m"),
        "3M": rank_info.get("return_3m"),
        "6M": rank_info.get("return_6m"),
        "1Y": rank_info.get("return_1y"),
        "YTD": rank_info.get("return_ytd"),
        "SINCE": rank_info.get("return_since"),
    }
    stat_date = parse_date(rank_info.get("rank_date")) or record["nav_date"]
    for period_code, value in period_map.items():
        return_rate = to_decimal(value, scale=4)
        if return_rate is None:
            continue
        cur.execute(
            """
            UPDATE fund_performance_metric
            SET return_rate = %s
            WHERE fund_id = %s AND stat_date = %s AND period_code = %s
            """,
            (return_rate, fund_id, stat_date, period_code),
        )
        if cur.rowcount:
            continue
        cur.execute(
            """
            INSERT INTO fund_performance_metric (fund_id, stat_date, period_code, return_rate)
            VALUES (%s, %s, %s, %s)
            """,
            (fund_id, stat_date, period_code, return_rate),
        )


def ensure_tags(cur) -> dict[str, int]:
    tag_ids: dict[str, int] = {}
    for code, name, category, desc in BASIC_TAGS:
        cur.execute("SELECT tag_id FROM fund_tag WHERE tag_code = %s", (code,))
        row = cur.fetchone()
        if row:
            tag_id = int(row[0])
            cur.execute(
                """
                UPDATE fund_tag
                SET tag_name = %s,
                    tag_category = %s,
                    description = %s,
                    enabled = 1
                WHERE tag_id = %s
                """,
                (name, category, desc, tag_id),
            )
        else:
            cur.execute(
                """
                INSERT INTO fund_tag (tag_code, tag_name, tag_category, description, enabled)
                VALUES (%s, %s, %s, %s, 1)
                """,
                (code, name, category, desc),
            )
            cur.execute("SELECT tag_id FROM fund_tag WHERE tag_code = %s", (code,))
            tag_id = int(cur.fetchone()[0])
        tag_ids[code] = tag_id
    return tag_ids


def infer_tag_codes(record: dict[str, Any], total_score: Decimal | None) -> list[str]:
    tags = ["AKSHARE_IMPORTED"]
    fund_type = record.get("fund_type") or ""
    risk_level = record.get("risk_level")
    rank_info = record.get("rank_info") or {}

    if "股票" in fund_type:
        tags.append("FUND_TYPE_STOCK")
    if "混合" in fund_type:
        tags.append("FUND_TYPE_MIXED")
    if "债券" in fund_type:
        tags.append("FUND_TYPE_BOND")
    if "货币" in fund_type:
        tags.append("FUND_TYPE_MONEY")
    if "指数" in fund_type:
        tags.append("FUND_TYPE_INDEX")
    if "QDII" in fund_type:
        tags.append("FUND_TYPE_QDII")

    if risk_level == "HIGH":
        tags.append("RISK_HIGH")
    elif risk_level in {"LOW", "MEDIUM_LOW"}:
        tags.append("RISK_LOW")
    else:
        tags.append("RISK_MEDIUM")

    ret_1y = to_decimal(rank_info.get("return_1y"), scale=4)
    ret_6m = to_decimal(rank_info.get("return_6m"), scale=4)
    ret_1m = to_decimal(rank_info.get("return_1m"), scale=4)
    ret_1w = to_decimal(rank_info.get("return_1w"), scale=4)
    if ret_1y is not None and ret_1y >= Decimal("20"):
        tags.append("RETURN_1Y_HIGH")
    if ret_6m is not None and ret_6m >= Decimal("10"):
        tags.append("RETURN_6M_HIGH")
    if (ret_1m is not None and ret_1m >= Decimal("5")) or (ret_1w is not None and ret_1w >= Decimal("2")):
        tags.append("SHORT_TERM_STRONG")
    if total_score is not None and total_score >= Decimal("75"):
        tags.append("TOTAL_SCORE_RECOMMEND")

    return sorted(set(tags))


def upsert_tag_relations(cur, fund_id: int, record: dict[str, Any], tag_ids: dict[str, int]) -> None:
    cur.execute("SELECT total_score FROM fund_score WHERE fund_id = %s ORDER BY score_date DESC LIMIT 1", (fund_id,))
    row = cur.fetchone()
    total_score = Decimal(row[0]) if row and row[0] is not None else None
    for tag_code in infer_tag_codes(record, total_score):
        tag_id = tag_ids.get(tag_code)
        if not tag_id:
            continue
        cur.execute(
            """
            UPDATE fund_tag_relation
            SET confidence = 100.00,
                source = 'RULE'
            WHERE fund_id = %s AND tag_id = %s AND tag_date = %s
            """,
            (fund_id, tag_id, record["nav_date"]),
        )
        if cur.rowcount:
            continue
        cur.execute(
            """
            INSERT INTO fund_tag_relation (fund_id, tag_id, confidence, source, tag_date)
            VALUES (%s, %s, 100.00, 'RULE', %s)
            """,
            (fund_id, tag_id, record["nav_date"]),
        )


def detail_to_map(df: pd.DataFrame) -> dict[str, str]:
    result: dict[str, str] = {}
    if df is None or df.empty:
        return result
    for _, row in df.iterrows():
        key = clean_str(row.iloc[0]) if len(row) > 0 else None
        value = clean_str(row.iloc[1]) if len(row) > 1 else None
        if key and value:
            result[key] = value
    return result


def fetch_detail(symbol: str) -> dict[str, Any]:
    detail: dict[str, Any] = {}
    try:
        detail.update(detail_to_map(ak.fund_individual_basic_info_xq(symbol=symbol)))
    except Exception as exc:
        print(f"      detail xq failed for {symbol}: {exc}")
    try:
        ths = detail_to_map(ak.fund_info_ths(symbol=symbol))
        for key, value in ths.items():
            detail.setdefault(key, value)
    except Exception as exc:
        print(f"      detail ths failed for {symbol}: {exc}")
    return detail


def upsert_company(cur, company_name: str | None) -> int | None:
    if not company_name:
        return None
    cur.execute("SELECT company_id FROM fund_company WHERE company_name = %s", (company_name,))
    row = cur.fetchone()
    if row:
        return int(row[0])
    short_name = company_name.replace("基金管理有限公司", "").replace("有限公司", "")[:64]
    cur.execute(
        """
        INSERT INTO fund_company (company_name, short_name, source, updated_time)
        VALUES (%s, %s, 'AkShare', CURRENT_TIMESTAMP)
        """,
        (company_name, short_name),
    )
    cur.execute("SELECT company_id FROM fund_company WHERE company_name = %s", (company_name,))
    return int(cur.fetchone()[0])


def upsert_manager(cur, manager_name: str, company_id: int | None) -> int:
    cur.execute(
        """
        SELECT manager_id
        FROM fund_manager
        WHERE manager_name = %s AND (company_id = %s OR company_id IS NULL)
        ORDER BY manager_id
        LIMIT 1
        """,
        (manager_name, company_id),
    )
    row = cur.fetchone()
    if row:
        manager_id = int(row[0])
        if company_id:
            cur.execute("UPDATE fund_manager SET company_id = %s, updated_time = CURRENT_TIMESTAMP WHERE manager_id = %s", (company_id, manager_id))
        return manager_id
    cur.execute(
        "INSERT INTO fund_manager (manager_name, company_id, updated_time) VALUES (%s, %s, CURRENT_TIMESTAMP)",
        (manager_name, company_id),
    )
    cur.execute("SELECT manager_id FROM fund_manager WHERE manager_name = %s ORDER BY manager_id DESC LIMIT 1", (manager_name,))
    return int(cur.fetchone()[0])


def split_manager_names(text: str | None) -> list[str]:
    if not text:
        return []
    parts = re.split(r"[,，、/；;\s]+", text)
    return [p.strip() for p in parts if p.strip()]


def enrich_fund_detail(cur, fund_id: int, record: dict[str, Any], detail: dict[str, Any]) -> None:
    company_name = detail.get("基金公司") or detail.get("基金管理人")
    manager_text = detail.get("基金经理")
    company_id = upsert_company(cur, company_name)
    full_name = detail.get("基金全称")
    establish_date = parse_date(detail.get("成立时间") or detail.get("成立日期"))
    fund_scale = parse_yi_amount(detail.get("最新规模"))
    benchmark = detail.get("业绩比较基准")
    custodian = detail.get("托管银行") or detail.get("基金托管人")
    fund_type = detail.get("基金类型") or record.get("fund_type")
    risk_level = infer_risk_level(fund_type)
    cur.execute(
        """
        UPDATE fund_info
        SET full_name = COALESCE(%s, full_name),
            fund_type = COALESCE(%s, fund_type),
            risk_level = COALESCE(%s, risk_level),
            company_id = COALESCE(%s, company_id),
            fund_scale = COALESCE(%s, fund_scale),
            establish_date = COALESCE(%s, establish_date),
            benchmark = COALESCE(%s, benchmark),
            custodian = COALESCE(%s, custodian),
            updated_time = CURRENT_TIMESTAMP
        WHERE fund_id = %s
        """,
        (full_name, fund_type, risk_level, company_id, fund_scale, establish_date, benchmark, custodian, fund_id),
    )
    for name in split_manager_names(manager_text):
        manager_id = upsert_manager(cur, name, company_id)
        cur.execute(
            """
            SELECT relation_id
            FROM fund_manager_relation
            WHERE fund_id = %s AND manager_id = %s AND is_current = 1
            LIMIT 1
            """,
            (fund_id, manager_id),
        )
        if cur.fetchone():
            continue
        cur.execute(
            """
            INSERT INTO fund_manager_relation (fund_id, manager_id, is_current, role_name)
            VALUES (%s, %s, 1, '基金经理')
            """,
            (fund_id, manager_id),
        )


def write_records(
    config: DbConfig,
    records: list[dict[str, Any]],
    write_score: bool,
    write_tags: bool,
    dry_run: bool,
    detail_limit: int,
    with_details: bool,
) -> None:
    if dry_run:
        print("[DRY-RUN] Sample records:")
        for record in records[:5]:
            print(record)
        return

    print("[4/4] Writing records into KingbaseES...")
    with connect_db(config) as conn:
        with conn.cursor() as cur:
            tag_ids = ensure_tags(cur) if write_tags else {}
            for index, record in enumerate(records, start=1):
                fund_id = upsert_fund_info(cur, record)
                upsert_nav(cur, fund_id, record)
                if write_score:
                    upsert_score(cur, fund_id, record)
                upsert_metrics(cur, fund_id, record)
                if write_tags:
                    upsert_tag_relations(cur, fund_id, record, tag_ids)
                if with_details and index <= detail_limit:
                    detail = fetch_detail(record["fund_code"])
                    if detail:
                        enrich_fund_detail(cur, fund_id, record, detail)
                    time.sleep(0.2)
                print(f"      [{index}/{len(records)}] {record['fund_code']} {record['fund_name']}")
        conn.commit()


def print_db_summary(config: DbConfig) -> None:
    with connect_db(config) as conn:
        with conn.cursor() as cur:
            for table in [
                "fund_info",
                "fund_company",
                "fund_manager",
                "fund_manager_relation",
                "fund_nav",
                "fund_performance_metric",
                "fund_score",
                "fund_tag",
                "fund_tag_relation",
            ]:
                cur.execute(f"SELECT COUNT(*) FROM {table}")
                print(f"      {table}: {cur.fetchone()[0]}")


def main() -> None:
    args = parse_args()
    if not args.password and not args.dry_run:
        raise SystemExit("FUND_DB_PASSWORD is empty. Set it first, e.g. $env:FUND_DB_PASSWORD='1234'")

    config = DbConfig(
        host=args.host,
        port=args.port,
        database=args.database,
        user=args.user,
        password=args.password,
    )

    fund_names, daily_nav, rank = load_akshare_frames()
    records = build_records(fund_names, daily_nav, rank, limit=args.limit)
    if not records:
        raise SystemExit("No fund records were built from AkShare data.")

    print(f"      records prepared: {len(records)}")
    write_records(
        config,
        records,
        write_score=not args.no_score,
        write_tags=not args.no_tags,
        dry_run=args.dry_run,
        detail_limit=args.detail_limit,
        with_details=not args.no_details,
    )
    if not args.dry_run:
        print("Import completed. Current table counts:")
        print_db_summary(config)


if __name__ == "__main__":
    main()
