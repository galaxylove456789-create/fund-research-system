"""
Stage 2 importer: fund holding reports and holding details.

This script imports a small, repeatable slice of holding data from AkShare:
- fetch stock holding details by fund code and year
- upsert fund_holding_report by fund_id/report_date/report_type
- replace holding details for the imported reports
- write data_import_batch and data_import_error records

Run from backend project root:
    $env:FUND_DB_PASSWORD="1234"
    $env:PYTHONIOENCODING="utf-8"
    .\\.venv\\Scripts\\python.exe .\\scripts\\stage2_import_holdings.py --limit 10 --year 2024
"""

from __future__ import annotations

import argparse
import os
import re
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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import fund holding reports from AkShare.")
    parser.add_argument("--host", default=os.getenv("FUND_DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("FUND_DB_PORT", "54321")))
    parser.add_argument("--database", default=os.getenv("FUND_DB_NAME", "fund_research"))
    parser.add_argument("--user", default=os.getenv("FUND_DB_USER", "system"))
    parser.add_argument("--password", default=os.getenv("FUND_DB_PASSWORD", ""))
    parser.add_argument("--limit", type=int, default=10)
    parser.add_argument("--year", default="2024")
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


def next_id(cur, table: str, column: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX({column}), 0) + 1 FROM {table}")
    return int(cur.fetchone()[0])


def create_batch(cur, total: int, year: str) -> int:
    batch_id = next_id(cur, "data_import_batch", "batch_id")
    batch_no = "STAGE2_HOLDING_" + datetime.now().strftime("%Y%m%d%H%M%S")
    cur.execute(
        """
        INSERT INTO data_import_batch (
            batch_id, batch_no, import_type, file_name, file_hash,
            total_count, success_count, error_count, field_mapping,
            validate_summary, status, operator_id, created_time, finished_time
        ) VALUES (
            %s,
            %s,
            'STAGE2_HOLDING',
            'stage2_import_holdings.py',
            NULL,
            %s,
            0,
            0,
            %s,
            %s,
            'RUNNING',
            1,
            CURRENT_TIMESTAMP,
            NULL
        )
        """,
        (
            batch_id,
            batch_no,
            total,
            f'{{"source":"AkShare fund_portfolio_hold_em","year":"{year}","target":"fund_holding_report,fund_holding_detail"}}',
            f"阶段二基金持仓导入任务启动，年份 {year}。",
        ),
    )
    return batch_id


def finish_batch(cur, batch_id: int, total: int, success: int, errors: int) -> None:
    status = "SUCCESS" if errors == 0 else ("PARTIAL_SUCCESS" if success > 0 else "FAILED")
    cur.execute(
        """
        UPDATE data_import_batch
        SET total_count=%s,
            success_count=%s,
            error_count=%s,
            validate_summary=%s,
            status=%s,
            finished_time=CURRENT_TIMESTAMP
        WHERE batch_id=%s
        """,
        (total, success, errors, f"阶段二持仓导入完成：成功 {success} 只，异常 {errors} 只。", status, batch_id),
    )


def write_error(cur, batch_id: int, row_no: int, fund_code: str, reason: str, raw: str = "") -> None:
    cur.execute(
        """
        INSERT INTO data_import_error (
            error_id, batch_id, row_no, fund_code, error_field,
            error_reason, suggestion, raw_data, status, created_time
        ) VALUES (
            %s, %s, %s, %s, 'akshare_holding', %s,
            '稍后重试，或检查该基金是否披露了对应年份持仓数据。',
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


def report_date_from_quarter(text: str, fallback_year: str) -> date:
    match = re.search(r"(\d{4})年(\d)季度", text or "")
    year = int(match.group(1)) if match else int(fallback_year)
    quarter = int(match.group(2)) if match else 4
    month_day = {1: (3, 31), 2: (6, 30), 3: (9, 30), 4: (12, 31)}.get(quarter, (12, 31))
    return date(year, month_day[0], month_day[1])


def normalize_holding_frame(df: pd.DataFrame, fallback_year: str) -> pd.DataFrame:
    if df is None or df.empty:
        return pd.DataFrame(columns=["report_date", "security_code", "security_name", "holding_ratio", "shares", "market_value"])
    rows = []
    for _, row in df.iterrows():
        try:
            quarter = str(row.get("季度", ""))
            rows.append(
                {
                    "report_date": report_date_from_quarter(quarter, fallback_year),
                    "security_code": str(row.get("股票代码", "")).strip(),
                    "security_name": str(row.get("股票名称", "")).strip(),
                    "holding_ratio": float(row.get("占净值比例")),
                    "shares": row.get("持股数"),
                    "market_value": float(row.get("持仓市值")) if not pd.isna(row.get("持仓市值")) else None,
                }
            )
        except Exception:
            continue
    result = pd.DataFrame(rows)
    if result.empty:
        return result
    return result.dropna(subset=["report_date", "security_code", "security_name"]).sort_values(
        ["report_date", "holding_ratio"], ascending=[False, False]
    )


def fetch_holdings_from_akshare(fund_code: str, year: str) -> pd.DataFrame:
    raw = ak.fund_portfolio_hold_em(symbol=fund_code, date=year)
    return normalize_holding_frame(raw, year)


def upsert_report(cur, fund_id: int, report_date: date, report_df: pd.DataFrame) -> int:
    top10 = report_df.sort_values("holding_ratio", ascending=False).head(10)
    stock_ratio = float(report_df["holding_ratio"].sum()) if not report_df.empty else None
    top10_concentration = float(top10["holding_ratio"].sum()) if not top10.empty else None
    cur.execute(
        """
        SELECT report_id
        FROM fund_holding_report
        WHERE fund_id=%s AND report_date=%s AND COALESCE(report_type, '')='STOCK'
        """,
        (fund_id, report_date),
    )
    row = cur.fetchone()
    if row:
        report_id = int(row[0])
        cur.execute(
            """
            UPDATE fund_holding_report
            SET stock_ratio=%s,
                bond_ratio=NULL,
                cash_ratio=NULL,
                top10_concentration=%s
            WHERE report_id=%s
            """,
            (q(stock_ratio, 2), q(top10_concentration, 2), report_id),
        )
    else:
        report_id = next_id(cur, "fund_holding_report", "report_id")
        cur.execute(
            """
            INSERT INTO fund_holding_report (
                report_id, fund_id, report_date, report_type,
                stock_ratio, bond_ratio, cash_ratio, top10_concentration, created_time
            ) VALUES (%s, %s, %s, 'STOCK', %s, NULL, NULL, %s, CURRENT_TIMESTAMP)
            """,
            (report_id, fund_id, report_date, q(stock_ratio, 2), q(top10_concentration, 2)),
        )
    cur.execute("DELETE FROM fund_holding_detail WHERE report_id=%s", (report_id,))
    return report_id


def insert_details(cur, report_id: int, report_df: pd.DataFrame) -> int:
    next_holding_id = next_id(cur, "fund_holding_detail", "holding_id")
    inserted = 0
    for _, row in report_df.sort_values("holding_ratio", ascending=False).head(20).iterrows():
        cur.execute(
            """
            INSERT INTO fund_holding_detail (
                holding_id, report_id, security_code, security_name,
                security_type, industry_name, market_value, holding_ratio
            ) VALUES (%s, %s, %s, %s, 'STOCK', NULL, %s, %s)
            """,
            (
                next_holding_id,
                report_id,
                row["security_code"],
                row["security_name"],
                q(row["market_value"], 4),
                q(row["holding_ratio"], 4),
            ),
        )
        next_holding_id += 1
        inserted += 1
    return inserted


def process_fund(cur, fund: dict[str, Any], year: str) -> tuple[int, int]:
    holding_df = fetch_holdings_from_akshare(fund["fund_code"], year)
    if holding_df.empty:
        raise RuntimeError("AkShare returned empty holding data")
    reports = 0
    details = 0
    for report_date, report_df in holding_df.groupby("report_date"):
        report_id = upsert_report(cur, fund["fund_id"], report_date, report_df)
        details += insert_details(cur, report_id, report_df)
        reports += 1
    return reports, details


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
            batch_id = create_batch(cur, total, args.year)
            if args.dry_run:
                print(f"[DRY-RUN] Prepared {total} funds, batch_id would be {batch_id}")
                conn.rollback()
                return
            for index, fund in enumerate(funds, start=1):
                try:
                    reports, details = process_fund(cur, fund, args.year)
                    success += 1
                    print(f"[{index}/{total}] {fund['fund_code']} {fund['fund_name']} OK, reports={reports}, details={details}")
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
    print(f"Stage 2 holding import completed: total={total}, success={success}, errors={errors}")


if __name__ == "__main__":
    main()
