"""
Seed sample data into KingbaseES for Fund Research demo.

Inserts records into:
  - community_author_profile, community_post, community_comment,
    community_like, community_author_follow
  - ai_recommend_record, ai_recommend_item
  - user_favorite, user_saved_filter, user_recent_fund_view
  - fund_portfolio, portfolio_fund_relation
  - fund_compare_record, fund_compare_item
  - data_import_batch, data_import_error

Uses existing fund_info.fund_id values for FK references.
Does NOT touch fund_info, fund_score, fund_nav, fund_performance_metric, etc.
"""

from __future__ import annotations

import json
import os
from datetime import datetime, timedelta, timezone
from decimal import Decimal

import psycopg2

DB_CONFIG = dict(
    host=os.getenv("FUND_DB_HOST", "localhost"),
    port=int(os.getenv("FUND_DB_PORT", "54321")),
    database=os.getenv("FUND_DB_NAME", "fund_research"),
    user=os.getenv("FUND_DB_USER", "system"),
    password=os.getenv("FUND_DB_PASSWORD", "1234"),
)

USER_ID = 1  # default demo user (matches fund_user row)

DEMO_USERS = [
    ("integration_user", "INTEGRATION", "BALANCED", "关注低回撤和长期稳健收益，偏好用数据做基金研究。"),
    ("Iris0504", "USER", "BALANCED", "理财小白视角，记录低回撤债券和成长混合研究心得。"),
    ("quant_zhou", "USER", "AGGRESSIVE", "量化研究员，关注基金风格漂移和夏普比率。"),
    ("amay_investor", "USER", "BALANCED", "理财小白视角，分享选基和组合心得。"),
    ("stable_li", "USER", "STABLE", "偏好债基和货基，控回撤是信仰。"),
    ("tech_kx", "USER", "AGGRESSIVE", "聚焦科技、新能源、半导体主题基金。"),
    ("fof_daren", "USER", "BALANCED", "FOF 组合构建，研究大类资产配置。"),
    ("index_enhancer", "USER", "BALANCED", "指数基金和增强策略爱好者。"),
    ("admin", "ADMIN", "BALANCED", "系统管理员账号"),
]


def ensure_users(cur) -> dict[int, int]:
    """Insert demo users if absent and return {original_user_id: real_user_id}."""
    mapping: dict[int, int] = {}
    for idx, (username, role, risk, signature) in enumerate(DEMO_USERS, start=1):
        cur.execute("SELECT user_id FROM fund_user WHERE username = %s", (username,))
        row = cur.fetchone()
        if row:
            mapping[idx] = row[0]
            cur.execute(
                "UPDATE fund_user SET role_code=%s, risk_preference=%s, signature=%s WHERE user_id=%s",
                (role, risk, signature, row[0]),
            )
            continue
        cur.execute(
            """
            INSERT INTO fund_user (username, password_hash, role_code, risk_preference, status, signature)
            VALUES (%s, 'demo_hash', %s, %s, 1, %s)
            RETURNING user_id
            """,
            (username, role, risk, signature),
        )
        mapping[idx] = cur.fetchone()[0]
    return mapping

CATEGORIES = [
    "基金分析",
    "筛选策略",
    "组合讨论",
    "风险提示",
    "基金经理",
    "市场观察",
]

TAG_POOL = [
    "低回撤", "债券稳健", "新能源主题", "红利策略", "科技成长",
    "长期绩优", "均衡配置", "基金经理变更", "AI推荐", "智能投顾",
    "回撤控制", "高夏普", "FOF", "QDII", "指数增强",
]


def fetch_funds(cur, limit: int = 40) -> list[dict]:
    cur.execute(
        """
        SELECT f.fund_id, f.fund_code, f.fund_name, f.fund_type, f.risk_level,
               (SELECT total_score FROM fund_score WHERE fund_id = f.fund_id ORDER BY score_date DESC LIMIT 1) AS total_score,
               (SELECT return_rate FROM fund_performance_metric WHERE fund_id = f.fund_id AND period_code='1Y' ORDER BY stat_date DESC LIMIT 1) AS return_1y,
               (SELECT max_drawdown FROM fund_performance_metric WHERE fund_id = f.fund_id AND period_code='1Y' ORDER BY stat_date DESC LIMIT 1) AS max_drawdown
        FROM fund_info f
        ORDER BY f.fund_id
        LIMIT %s
        """,
        (limit,),
    )
    cols = [c[0] for c in cur.description]
    return [dict(zip(cols, r)) for r in cur.fetchall()]


def upsert_authors(cur, user_mapping: dict[int, int]) -> list[dict]:
    authors = [
        {"nickname": "基金研究Iris", "avatar": "I", "intro": "专注低回撤基金与长期持有策略，3年实盘经验。", "article_count": 38, "follower_count": 1280},
        {"nickname": "量化老周", "avatar": "Z", "intro": "量化研究员，关注基金风格漂移和夏普比率。", "article_count": 52, "follower_count": 2150},
        {"nickname": "基民阿May", "avatar": "M", "intro": "理财小白视角，分享选基和组合心得。", "article_count": 21, "follower_count": 480},
        {"nickname": "稳健派老李", "avatar": "L", "intro": "偏好债基和货基，控回撤是信仰。", "article_count": 33, "follower_count": 980},
        {"nickname": "科技成长派", "avatar": "K", "intro": "聚焦科技、新能源、半导体主题基金。", "article_count": 44, "follower_count": 1760},
        {"nickname": "FOF达人", "avatar": "F", "intro": "FOF组合构建，研究大类资产配置。", "article_count": 29, "follower_count": 720},
        {"nickname": "指数增强控", "avatar": "E", "intro": "指数基金和增强策略爱好者。", "article_count": 18, "follower_count": 540},
    ]
    # clear existing authors and posts (idempotent)
    cur.execute("DELETE FROM community_author_follow")
    cur.execute("DELETE FROM community_like")
    cur.execute("DELETE FROM community_comment")
    cur.execute("DELETE FROM community_post")
    cur.execute("DELETE FROM community_author_profile")

    author_ids = []
    for idx, a in enumerate(authors, start=1):
        real_user_id = user_mapping.get(idx + 1) or user_mapping.get(idx) or USER_ID
        cur.execute(
            """
            INSERT INTO community_author_profile (user_id, nickname, avatar, intro, article_count, follower_count, created_time, updated_time)
            VALUES (%s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING author_id
            """,
            (real_user_id, a["nickname"], a["avatar"], a["intro"], a["article_count"], a["follower_count"]),
        )
        author_ids.append(cur.fetchone()[0])
    return [{"author_id": aid, **a} for aid, a in zip(author_ids, authors)]


def insert_posts(cur, authors: list[dict], funds: list[dict]) -> list[dict]:
    seed_posts = [
        ("低回撤债券基金筛选思路：四个硬指标", "筛选策略",
         "用近一年最大回撤、波动率、夏普比率和债性纯度四个维度筛选稳健债基。",
         "低回撤", "债券稳健", "高夏普"),
        ("新能源主题基金还能上车吗？三类风险提示", "风险提示",
         "主题基金估值不便宜，警惕高位接盘、风格漂移和基金经理变更三类风险。",
         "新能源主题", "回撤控制", "科技成长"),
        ("FOF组合搭建：股债 6:4 还是 5:5？", "组合讨论",
         "用近三年回撤数据回测不同股债比例，给进取型和平衡型用户两种参考组合。",
         "FOF", "均衡配置", "长期绩优"),
        ("我的核心观察池：长期持有的三只基金", "基金分析",
         "分别从科技成长、红利策略和均衡混合三个方向，长期跟踪的三只核心基金。",
         "长期绩优", "红利策略", "AI推荐"),
        ("基金经理变更后，怎么办？三类应对方案", "基金经理",
         "老将离任后，可以选择继续持有、止盈观察或换成同风格新基金。",
         "基金经理变更", "风险提示"),
        ("高夏普基金筛选：分子选债基", "筛选策略",
         "夏普比率 > 1.5 是优质债基的入门线，结合卡玛比率排除尾部风险。",
         "高夏普", "债券稳健"),
        ("指数增强基金真的能跑赢沪深300吗？", "市场观察",
         "从2018-2025年的增强基金样本看，超额收益逐年收窄，需关注增强策略失效风险。",
         "指数增强", "长期绩优"),
        ("QDII 美股基金配置建议：5% 还是 15%？", "组合讨论",
         "QDII 美股作为卫星仓位，建议占比不超过 15%，注意汇率风险与溢价风险。",
         "QDII", "均衡配置"),
        ("红利策略基金：防御行情下的优选", "基金分析",
         "红利低波类基金近两年规模快速扩张，防御属性强，但成长性有限。",
         "红利策略", "低回撤"),
        ("智能投顾生成的组合，可信吗？", "风险提示",
         "AI 推荐应作为研究起点，不应直接照搬，注意推荐逻辑的样本偏差。",
         "AI推荐", "智能投顾"),
    ]

    inserted: list[dict] = []
    base_time = datetime(2026, 6, 1, 9, 30, 0)
    for idx, (title, category, summary, *tags) in enumerate(seed_posts):
        author = authors[idx % len(authors)]
        fund = funds[idx % len(funds)] if funds else None
        related_fund_id = fund["fund_id"] if fund else None
        related_fund_name = fund["fund_name"] if fund else None
        related_fund_code = fund["fund_code"] if fund else None
        content = (
            f"{summary}\n\n一、背景：最近市场环境与基金表现概述。\n"
            f"二、数据：基于近一年/三年的收益、回撤、夏普对比。\n"
            f"三、操作建议：分稳健、平衡、进取三档给出参考配置。\n"
            f"四、风险提示：本帖内容仅作为基金研究参考，不构成投资建议。"
        )
        created_time = base_time + timedelta(hours=idx * 6)
        view_count = 120 + idx * 47
        comment_count = 4 + (idx % 6)
        like_count = 18 + (idx % 9) * 7

        cur.execute(
            """
            INSERT INTO community_post (
                author_id, title, category, related_fund_id,
                summary, content, tags,
                view_count, comment_count, like_count,
                status, created_time, updated_time
            ) VALUES (
                %s, %s, %s, %s,
                %s, %s, %s,
                %s, %s, %s,
                'PUBLISHED', %s, %s
            ) RETURNING post_id
            """,
            (
                author["author_id"], title, category, related_fund_id,
                summary, content, ",".join(tags),
                view_count, comment_count, like_count,
                created_time, created_time,
            ),
        )
        post_id = cur.fetchone()[0]
        inserted.append({
            "post_id": post_id,
            "title": title,
            "category": category,
            "tags": list(tags),
            "author": author["nickname"],
            "author_id": author["author_id"],
            "related_fund_id": related_fund_id,
            "related_fund_name": related_fund_name,
            "related_fund_code": related_fund_code,
            "summary": summary,
            "view_count": view_count,
            "comment_count": comment_count,
            "like_count": like_count,
            "created_time": created_time,
        })
    return inserted


def insert_comments(cur, posts: list[dict], user_ids: list[int]) -> None:
    seed_comments = [
        "很有参考价值，已加入筛选清单。",
        "我之前也用类似的思路，但是加入卡玛比率后效果更好。",
        "想问下回撤阈值一般设多少合适？",
        "赞同观点，AI 推荐只能作为辅助判断。",
        "可以分享下具体基金代码吗？想加入对比。",
        "我是稳健型，这篇帖子帮大忙了。",
        "感觉第二段的数据分析比较关键，希望再补充图表。",
        "主题基金这块我之前吃过大亏，现在只做卫星仓位。",
        "基金经理变更确实是风险点，最近遇到两只都在换人。",
        "请问 FOF 平台费率怎么算的，会拉低收益吗？",
    ]
    for post in posts:
        for n in range(min(post["comment_count"], 4)):
            cur.execute(
                """
                INSERT INTO community_comment (post_id, user_id, content, status, created_time)
                VALUES (%s, %s, %s, 'PUBLISHED', %s)
                """,
                (
                    post["post_id"],
                    user_ids[n % len(user_ids)],
                    seed_comments[(post["post_id"] + n) % len(seed_comments)],
                    post["created_time"] + timedelta(hours=n + 1),
                ),
            )


def insert_likes(cur, posts: list[dict], user_ids: list[int]) -> None:
    for post in posts:
        liked_users = user_ids[: min(len(user_ids), (post["post_id"] % 5) + 2)]
        for uid in liked_users:
            cur.execute(
                """
                INSERT INTO community_like (user_id, target_type, target_id, created_time)
                VALUES (%s, 'POST', %s, %s)
                """,
                (uid, post["post_id"], post["created_time"] + timedelta(minutes=30)),
            )


def insert_follows(cur, authors: list[dict], user_ids: list[int]) -> None:
    for idx, author in enumerate(authors):
        followers = user_ids[: min(len(user_ids), (idx % 4) + 2)]
        for uid in followers:
            cur.execute(
                """
                INSERT INTO community_author_follow (user_id, author_id, created_time)
                VALUES (%s, %s, CURRENT_TIMESTAMP)
                """,
                (uid, author["author_id"]),
            )


def upsert_recommend_records(cur, funds: list[dict]) -> None:
    cur.execute("DELETE FROM ai_recommend_item")
    cur.execute("DELETE FROM ai_recommend_record")

    scenes = [
        ("DASHBOARD", "平衡型", "fund-recommend-dashboard", "balance"),
        ("WEEKLY", "稳健型", "fund-recommend-weekly", "stable"),
        ("THEME", "进取型", "fund-recommend-theme", "growth"),
    ]
    # sort by total_score desc, take top 6 for each scene
    sorted_funds = sorted(funds, key=lambda f: f.get("total_score") or 0, reverse=True)

    for scene_idx, (scene, risk, workflow, key) in enumerate(scenes):
        request_payload = json.dumps({
            "userId": USER_ID,
            "scene": scene,
            "riskPreference": risk,
            "limit": 6,
        }, ensure_ascii=False)
        cur.execute(
            """
            INSERT INTO ai_recommend_record (
                user_id, recommend_scene, risk_preference_snapshot,
                dify_workflow_id, request_payload, response_summary,
                status, created_time
            ) VALUES (%s, %s, %s, %s, %s, %s, 'SUCCESS', %s)
            RETURNING recommend_id
            """,
            (
                USER_ID, scene, risk, workflow, request_payload,
                f"基于 {risk} 风险偏好和 Dify {workflow} 工作流生成的 {scene} 推荐结果，包含 6 只精选基金。",
                datetime(2026, 6, 29, 10, 0, 0) - timedelta(days=scene_idx),
            ),
        )
        recommend_id = cur.fetchone()[0]
        picks = sorted_funds[scene_idx:scene_idx + 6] if len(sorted_funds) > scene_idx + 6 else sorted_funds[:6]
        for order, fund in enumerate(picks, start=1):
            total_score = float(fund.get("total_score") or 75)
            recommend_level = "RECOMMEND" if total_score >= 75 else "WATCH"
            reason = (
                f"近一年收益 {float(fund.get('return_1y') or 0):.2f}%，综合评分 {total_score:.2f}，"
                f"策略匹配度较高，适合{risk}风险偏好的用户长期跟踪。"
            )
            risk_warning = (
                "市场波动较大时仍可能出现短期回撤，请结合自身风险承受能力配置仓位。"
            )
            suitable_user = "稳健型/平衡型" if risk == "稳健型" else "平衡型/进取型" if risk == "平衡型" else "进取型"
            tag_text = "综合评分推荐,长期绩优"
            cur.execute(
                """
                INSERT INTO ai_recommend_item (
                    recommend_id, fund_id, display_order, recommend_level,
                    total_score_snapshot, reason, risk_warning, suitable_user, tags,
                    created_time
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    recommend_id, fund["fund_id"], order, recommend_level,
                    Decimal(str(total_score)), reason, risk_warning, suitable_user, tag_text,
                    datetime(2026, 6, 29, 10, 0, 0) - timedelta(days=scene_idx),
                ),
            )


def upsert_user_favorites(cur, funds: list[dict]) -> None:
    cur.execute("DELETE FROM user_favorite WHERE user_id = %s", (USER_ID,))
    groups = ["核心观察", "稳健底仓", "主题机会"]
    remarks = ["重点跟踪回撤", "适合组合底仓", "等待估值回落", "近一年表现稳健", "可关注夏普"]
    picks = funds[:12]
    for idx, fund in enumerate(picks):
        cur.execute(
            """
            INSERT INTO user_favorite (user_id, fund_id, favorite_group, remark, created_time)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (
                USER_ID,
                fund["fund_id"],
                groups[idx % len(groups)],
                remarks[idx % len(remarks)],
                datetime(2026, 6, 1, 9, 0, 0) + timedelta(days=idx),
            ),
        )


def upsert_saved_filters(cur) -> None:
    cur.execute("DELETE FROM user_saved_filter WHERE user_id = %s", (USER_ID,))
    filters = [
        ("低回撤稳健债券池", "低回撤 + 债券型 + 近一年收益稳定", json.dumps({"riskLevel": "LOW", "fundType": "债券型", "maxDrawdownMax": 5}, ensure_ascii=False), 32),
        ("新能源成长观察", "新能源主题 + 中高风险 + 近一年收益大于 10%", json.dumps({"tags": ["新能源主题"], "minReturn": 10}, ensure_ascii=False), 18),
        ("长期绩优平衡组合候选", "长期绩优 + 混合型 + 综合评分大于 80", json.dumps({"fundType": "混合型", "minScore": 80}, ensure_ascii=False), 24),
        ("AI 推荐高分基金", "AI 推荐 + 综合评分 >= 85", json.dumps({"minScore": 85}, ensure_ascii=False), 12),
        ("红利策略防御组合", "红利策略 + 低回撤 + 混合型", json.dumps({"tag": "红利策略", "fundType": "混合型"}, ensure_ascii=False), 9),
    ]
    for idx, (name, summary, condition, hit) in enumerate(filters):
        cur.execute(
            """
            INSERT INTO user_saved_filter (
                user_id, filter_name, filter_summary, filter_condition,
                hit_count, created_time, updated_time
            ) VALUES (%s, %s, %s, %s, %s, %s, %s)
            """,
            (
                USER_ID, name, summary, condition, hit,
                datetime(2026, 6, 20, 10, 0, 0) + timedelta(days=idx),
                datetime(2026, 6, 28, 9, 0, 0) + timedelta(days=idx),
            ),
        )


def upsert_recent_views(cur, funds: list[dict]) -> None:
    cur.execute("DELETE FROM user_recent_fund_view WHERE user_id = %s", (USER_ID,))
    pages = ["fund_detail", "fund_list", "compare", "ai_recommend"]
    picks = funds[:8]
    for idx, fund in enumerate(picks):
        cur.execute(
            """
            INSERT INTO user_recent_fund_view (user_id, fund_id, view_time, source_page)
            VALUES (%s, %s, %s, %s)
            """,
            (
                USER_ID,
                fund["fund_id"],
                datetime(2026, 6, 29, 16, 0, 0) - timedelta(hours=idx * 3),
                pages[idx % len(pages)],
            ),
        )


def upsert_portfolios(cur, funds: list[dict]) -> None:
    cur.execute("DELETE FROM portfolio_fund_relation")
    cur.execute("DELETE FROM fund_portfolio")
    portfolios = [
        {
            "name": "平衡成长组合",
            "portfolio_type": "RESEARCH_POOL",
            "source_dimension": "长期绩优 + 均衡配置",
            "description": "权益和债券按 6:4 配置，追求长期年化 8%-10%。",
            "portfolio_style": "平衡型",
            "risk_level": "MEDIUM",
            "funds": [(0, 30), (1, 20), (3, 15), (8, 15), (12, 20)],
        },
        {
            "name": "稳健底仓组合",
            "portfolio_type": "RESEARCH_POOL",
            "source_dimension": "债券稳健 + 低回撤",
            "description": "债基为主，搭配少量混合基金作为收益增强。",
            "portfolio_style": "稳健型",
            "risk_level": "LOW",
            "funds": [(15, 40), (17, 30), (24, 20), (30, 10)],
        },
        {
            "name": "主题机会观察",
            "portfolio_type": "RESEARCH_POOL",
            "source_dimension": "新能源主题 + 科技成长",
            "description": "卫星仓位，跟踪科技/新能源主题机会。",
            "portfolio_style": "进取型",
            "risk_level": "HIGH",
            "funds": [(4, 35), (10, 25), (20, 20), (33, 20)],
        },
    ]
    for pidx, p in enumerate(portfolios):
        cur.execute(
            """
            INSERT INTO fund_portfolio (
                user_id, portfolio_name, portfolio_type,
                source_dimension, source_condition, description,
                avg_risk_score, avg_total_score, tracking_enabled,
                created_time, updated_time,
                portfolio_style, risk_level
            ) VALUES (
                %s, %s, %s,
                %s, %s, %s,
                %s, %s, 1,
                %s, %s,
                %s, %s
            ) RETURNING portfolio_id
            """,
            (
                USER_ID, p["name"], p["portfolio_type"],
                p["source_dimension"], json.dumps({"dim": p["source_dimension"]}, ensure_ascii=False),
                p["description"],
                72.5 + pidx, 78.5 + pidx,
                datetime(2026, 6, 12, 9, 0, 0) + timedelta(days=pidx * 6),
                datetime(2026, 6, 26, 9, 0, 0) + timedelta(days=pidx),
                p["portfolio_style"], p["risk_level"],
            ),
        )
        portfolio_id = cur.fetchone()[0]
        for order, (idx, weight) in enumerate(p["funds"], start=1):
            fund = funds[idx]
            cur.execute(
                """
                INSERT INTO portfolio_fund_relation (
                    portfolio_id, fund_id, weight, add_source,
                    snapshot_score, remark, snapshot_return_1y,
                    snapshot_max_drawdown, snapshot_risk_level
                ) VALUES (%s, %s, %s, 'MANUAL',
                          %s, %s, %s, %s, %s)
                """,
                (
                    portfolio_id, fund["fund_id"], Decimal(str(weight)),
                    fund.get("total_score") or 75,
                    "组合核心持仓" if order == 1 else "卫星仓位",
                    fund.get("return_1y"),
                    fund.get("max_drawdown"),
                    fund.get("risk_level"),
                ),
            )


def upsert_compare_records(cur, funds: list[dict]) -> None:
    cur.execute("DELETE FROM fund_compare_item")
    cur.execute("DELETE FROM fund_compare_record")
    records = [
        {
            "name": "稳健债基横向对比",
            "dim": "风险维度",
            "summary": "对比 3 只稳健债基的近一年回撤、夏普与卡玛比率。",
            "risk": "债基组合请关注利率风险和信用风险。",
            "fund_ids": [funds[15]["fund_id"], funds[17]["fund_id"], funds[24]["fund_id"]],
        },
        {
            "name": "科技成长主动 vs 指数",
            "dim": "策略维度",
            "summary": "对比主动管理基金和指数增强基金的超额收益稳定性。",
            "risk": "主题基金波动较大，需控制卫星仓位。",
            "fund_ids": [funds[4]["fund_id"], funds[10]["fund_id"], funds[33]["fund_id"]],
        },
        {
            "name": "AI 推荐 Top3 复盘",
            "dim": "AI 推荐",
            "summary": "复盘上期 AI 推荐基金的实际表现，对比推荐分数与实际回报。",
            "risk": "AI 推荐结果基于历史数据，未来表现不可线性外推。",
            "fund_ids": [funds[0]["fund_id"], funds[1]["fund_id"], funds[3]["fund_id"]],
        },
    ]
    for rec in records:
        cur.execute(
            """
            INSERT INTO fund_compare_record (
                user_id, compare_dimension, result_summary,
                created_time, compare_name, risk_warning, status
            ) VALUES (%s, %s, %s, %s, %s, %s, 'SAVED')
            RETURNING compare_id
            """,
            (
                USER_ID, rec["dim"], rec["summary"],
                datetime(2026, 6, 25, 14, 0, 0),
                rec["name"], rec["risk"],
            ),
        )
        compare_id = cur.fetchone()[0]
        for order, fid in enumerate(rec["fund_ids"], start=1):
            fund = next((f for f in funds if f["fund_id"] == fid), None)
            if not fund:
                continue
            cur.execute(
                """
                INSERT INTO fund_compare_item (
                    compare_id, fund_id, display_order,
                    snapshot_return_1y, snapshot_max_drawdown,
                    snapshot_volatility, snapshot_total_score
                ) VALUES (%s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    compare_id, fid, order,
                    fund.get("return_1y"),
                    fund.get("max_drawdown"),
                    Decimal("12.50"),
                    fund.get("total_score"),
                ),
            )


def upsert_import_batches(cur) -> None:
    cur.execute("DELETE FROM data_import_error")
    cur.execute("DELETE FROM data_import_batch")
    batches = [
        ("B20260629001", "fund_basic", "fund_basic_2026Q2.csv", "fund_basic_2026Q2", 100, 98, 2,
         json.dumps({"fund_code": "fund_code", "fund_name": "fund_name", "risk_level": "risk_level"}, ensure_ascii=False),
         "AkShare 全量基金基础信息导入，含 100 只基金。",
         "SUCCESS", datetime(2026, 6, 29, 9, 30, 0), datetime(2026, 6, 29, 9, 32, 0)),
        ("B20260629002", "fund_nav", "fund_nav_history.csv", "fund_nav_history", 100, 100, 0,
         json.dumps({"fund_code": "fund_code", "nav_date": "nav_date", "unit_nav": "unit_nav"}, ensure_ascii=False),
         "导入 100 只基金最新一日净值。",
         "SUCCESS", datetime(2026, 6, 29, 9, 35, 0), datetime(2026, 6, 29, 9, 36, 0)),
        ("B20260628003", "fund_performance", "fund_perf_history.csv", "fund_perf_history", 666, 660, 6,
         json.dumps({"fund_id": "fund_id", "period_code": "period_code", "return_rate": "return_rate"}, ensure_ascii=False),
         "导入近 1 周/1 月/3 月/6 月/1 年/YTD/SINCE 七档业绩。",
         "SUCCESS", datetime(2026, 6, 28, 18, 20, 0), datetime(2026, 6, 28, 18, 25, 0)),
        ("B20260627004", "fund_score", "fund_score_seed.csv", "fund_score_seed", 100, 100, 0,
         json.dumps({"fund_id": "fund_id", "total_score": "total_score"}, ensure_ascii=False),
         "种子评分导入，作为推荐基础分数。",
         "SUCCESS", datetime(2026, 6, 27, 20, 10, 0), datetime(2026, 6, 27, 20, 12, 0)),
        ("B20260626005", "fund_tag_rule", "fund_tag_rules.csv", "fund_tag_rules", 14, 14, 0,
         json.dumps({"tag_code": "tag_code", "tag_name": "tag_name"}, ensure_ascii=False),
         "导入基础标签规则。",
         "SUCCESS", datetime(2026, 6, 26, 19, 30, 0), datetime(2026, 6, 26, 19, 32, 0)),
    ]
    batch_ids = []
    for b in batches:
        cur.execute(
            """
            INSERT INTO data_import_batch (
                batch_no, import_type, file_name, file_hash,
                total_count, success_count, error_count,
                field_mapping, validate_summary, status,
                operator_id, created_time, finished_time
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING batch_id
            """,
            (b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9], 1, b[10], b[11]),
        )
        batch_ids.append(cur.fetchone()[0])

    errors = [
        (batch_ids[0], 12, "000000", "fund_code", "基金代码不存在", "确认代码是否已下线"),
        (batch_ids[0], 47, "009999", "risk_level", "枚举值不在允许范围", "映射为中风险后重试"),
        (batch_ids[0], 88, None, "establish_date", "日期格式无法识别", "使用 yyyy-MM-dd 重新上传"),
        (batch_ids[2], 102, None, "return_rate", "数值超出合理范围", "剔除极端值后重跑"),
        (batch_ids[2], 305, None, "max_drawdown", "字段为空", "进入待补充队列"),
        (batch_ids[2], 488, None, "annual_return", "与 return_rate 不一致", "以 return_rate 为准重新计算"),
    ]
    for err in errors:
        cur.execute(
            """
            INSERT INTO data_import_error (
                batch_id, row_no, fund_code, error_field,
                error_reason, suggestion, raw_data, status
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, 'PENDING')
            """,
            (err[0], err[1], err[2], err[3], err[4], err[5], json.dumps({"raw": "示例数据"}, ensure_ascii=False)),
        )


def main() -> None:
    conn = psycopg2.connect(**DB_CONFIG)
    try:
        with conn:
            with conn.cursor() as cur:
                funds = fetch_funds(cur, limit=40)
                print(f"Loaded {len(funds)} funds")
                user_mapping = ensure_users(cur)
                print(f"User mapping: {user_mapping}")
                extra_user_ids = [uid for uid in user_mapping.values() if uid != USER_ID][:7] or [USER_ID]
                authors = upsert_authors(cur, user_mapping)
                print(f"Inserted {len(authors)} authors")
                posts = insert_posts(cur, authors, funds)
                print(f"Inserted {len(posts)} posts")
                insert_comments(cur, posts, extra_user_ids)
                insert_likes(cur, posts, extra_user_ids)
                insert_follows(cur, authors, extra_user_ids)
                upsert_recommend_records(cur, funds)
                upsert_user_favorites(cur, funds)
                upsert_saved_filters(cur)
                upsert_recent_views(cur, funds)
                upsert_portfolios(cur, funds)
                upsert_compare_records(cur, funds)
                upsert_import_batches(cur)
        print("Seed completed.")
    finally:
        conn.close()


if __name__ == "__main__":
    main()