from __future__ import annotations

import os
from datetime import datetime, timedelta

import psycopg2


DB_CONFIG = {
    "host": os.getenv("FUND_DB_HOST", "localhost"),
    "port": int(os.getenv("FUND_DB_PORT", "54321")),
    "database": os.getenv("FUND_DB_NAME", "fund_research"),
    "user": os.getenv("FUND_DB_USER", "system"),
    "password": os.getenv("FUND_DB_PASSWORD", "1234"),
}

USERS = [
    ("fund_research_assistant", "USER", "BALANCED", "基金研究助手，擅长把收益、回撤和标签组合成可读结论。", "基金研究助手", "助"),
    ("stable_allocator", "USER", "STABLE", "关注低回撤、债券基金和稳健配置。", "稳健配置研究员", "稳"),
    ("index_observer", "USER", "BALANCED", "长期跟踪指数基金、增强策略和宽基配置。", "指数基金观察", "指"),
    ("dividend_notes", "USER", "BALANCED", "记录红利策略、低波策略和长期现金流资产。", "红利策略笔记", "红"),
    ("new_energy_alpha", "USER", "AGGRESSIVE", "关注新能源、科技成长和主题基金风险提示。", "新能源主题分析", "新"),
    ("quant_fund_lab", "USER", "AGGRESSIVE", "用量化指标复盘基金筛选和对比结果。", "量化选基研究员", "量"),
    ("bond_watch", "USER", "STABLE", "跟踪纯债、短债和固收+基金。", "债券基金观察", "债"),
    ("manager_tracker", "USER", "BALANCED", "关注基金经理变更、任职年限和风格漂移。", "基金经理跟踪员", "经"),
    ("ai_advisor_bot", "USER", "BALANCED", "解释智能评分、AI 推荐和组合风险提示。", "智能投顾助手", "AI"),
    ("beginner_growth", "USER", "STABLE", "新手视角记录选基问题和答疑。", "新手选基笔记", "新"),
]

POSTS = [
    ("低回撤债券基金筛选思路", "筛选策略", "用基金类型、近一年收益稳定性和最大回撤过滤稳健型基金。", ["低回撤", "债券稳健", "筛选策略"]),
    ("新能源主题基金近期表现复盘", "基金分析", "新能源主题基金弹性较大，需要结合持仓集中度和回撤控制观察。", ["新能源主题", "高波动", "风险提示"]),
    ("如何用最大回撤筛选稳健基金", "新手提问", "最大回撤能帮助新手理解基金下跌幅度，但不能单独作为买入依据。", ["最大回撤", "新手选基", "稳健配置"]),
    ("红利策略基金的长期配置价值", "基金分析", "红利策略更适合追求长期现金流和防御属性的用户持续跟踪。", ["红利策略", "低波动", "长期配置"]),
    ("基金经理变更后是否应该继续持有", "风险提示", "基金经理变更后要观察投资框架、持仓变化和历史风格延续性。", ["基金经理变更", "风险提示", "持仓观察"]),
    ("中高风险混合基金对比记录分享", "组合讨论", "通过收益、波动率、最大回撤和综合评分对比三只混合基金。", ["基金对比", "混合型", "综合评分"]),
    ("AI 推荐基金可以直接参考吗", "风险提示", "AI 推荐适合作为研究入口，仍需结合基金画像和个人风险偏好判断。", ["AI推荐", "智能投顾", "风险提示"]),
    ("指数增强基金长期跟踪方法", "筛选策略", "指数增强基金要重点跟踪超额收益稳定性、跟踪误差和管理费率。", ["指数增强", "长期绩优", "量化指标"]),
    ("固收+组合如何控制波动", "组合讨论", "固收+组合要控制权益仓位，并关注可转债和信用债暴露。", ["固收+", "组合讨论", "回撤控制"]),
    ("基金公司规模大一定更好吗", "基金分析", "基金公司规模代表资源和平台，但不能替代产品业绩和风格匹配。", ["基金公司", "研究库", "产品分析"]),
    ("新手如何读懂基金画像", "新手提问", "先看基金类型、风险等级、收益回撤，再看持仓和基金经理。", ["基金画像", "新手选基", "研究路径"]),
    ("季度调仓后如何更新自选池", "筛选策略", "自选池不是收藏夹，应定期根据评分、公告和回撤变化做清理。", ["我的研究池", "自选基金", "动态跟踪"]),
]


def ensure_user(cur, username, role, risk, signature):
    cur.execute("SELECT user_id FROM fund_user WHERE username=%s", (username,))
    row = cur.fetchone()
    if row:
        user_id = row[0]
        cur.execute(
            "UPDATE fund_user SET role_code=%s, risk_preference=%s, signature=%s WHERE user_id=%s",
            (role, risk, signature, user_id),
        )
        return user_id
    cur.execute(
        """
        INSERT INTO fund_user(username, password_hash, role_code, risk_preference, status, signature)
        VALUES (%s, 'demo_hash', %s, %s, 1, %s)
        RETURNING user_id
        """,
        (username, role, risk, signature),
    )
    return cur.fetchone()[0]


def ensure_author(cur, user_id, nickname, avatar, intro, article_count, follower_count):
    cur.execute("SELECT author_id FROM community_author_profile WHERE user_id=%s", (user_id,))
    row = cur.fetchone()
    if row:
        author_id = row[0]
        cur.execute(
            """
            UPDATE community_author_profile
            SET nickname=%s, avatar=%s, intro=%s, article_count=%s, follower_count=%s, updated_time=CURRENT_TIMESTAMP
            WHERE author_id=%s
            """,
            (nickname, avatar, intro, article_count, follower_count, author_id),
        )
        return author_id
    cur.execute(
        """
        INSERT INTO community_author_profile(user_id, nickname, avatar, intro, article_count, follower_count, created_time, updated_time)
        VALUES (%s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING author_id
        """,
        (user_id, nickname, avatar, intro, article_count, follower_count),
    )
    return cur.fetchone()[0]


def fetch_funds(cur):
    cur.execute(
        """
        SELECT fund_id, fund_code, fund_name
        FROM fund_info
        ORDER BY fund_id
        LIMIT 30
        """
    )
    return cur.fetchall()


def main():
    conn = psycopg2.connect(**DB_CONFIG)
    try:
        with conn:
            with conn.cursor() as cur:
                funds = fetch_funds(cur)
                if not funds:
                    raise RuntimeError("fund_info is empty; import fund data first.")

                author_ids = []
                user_ids = []
                for idx, (username, role, risk, intro, nickname, avatar) in enumerate(USERS):
                    user_id = ensure_user(cur, username, role, risk, intro)
                    user_ids.append(user_id)
                    author_id = ensure_author(
                        cur,
                        user_id,
                        nickname,
                        avatar,
                        intro,
                        article_count=18 + idx * 5,
                        follower_count=520 + idx * 210 + (idx % 3) * 160,
                    )
                    author_ids.append(author_id)

                # Remove only this script's showcase posts to keep the operation repeatable.
                cur.execute(
                    """
                    DELETE FROM community_like
                    WHERE target_type='POST' AND target_id IN (
                        SELECT post_id FROM community_post WHERE title = ANY(%s)
                    )
                    """,
                    ([p[0] for p in POSTS],),
                )
                cur.execute(
                    "DELETE FROM community_comment WHERE post_id IN (SELECT post_id FROM community_post WHERE title = ANY(%s))",
                    ([p[0] for p in POSTS],),
                )
                cur.execute("DELETE FROM community_post WHERE title = ANY(%s)", ([p[0] for p in POSTS],))

                base_time = datetime(2026, 6, 20, 9, 0, 0)
                post_ids = []
                for idx, (title, category, summary, tags) in enumerate(POSTS):
                    fund_id, _, _ = funds[idx % len(funds)]
                    author_id = author_ids[idx % len(author_ids)]
                    created = base_time + timedelta(hours=idx * 5)
                    content = (
                        f"{summary}\n\n"
                        "研究过程：先从基金类型和风险等级入手，再结合近一年收益、最大回撤、综合评分和标签进行二次筛选。\n"
                        "观察结论：该主题适合作为基金研究的线索，不建议脱离个人风险偏好直接配置。\n"
                        "风险提示：历史表现不代表未来收益，基金研究结果仅作为课程系统展示和辅助分析参考。"
                    )
                    cur.execute(
                        """
                        INSERT INTO community_post(
                            author_id, title, category, related_fund_id, summary, content, tags,
                            view_count, comment_count, like_count, status, created_time, updated_time
                        )
                        VALUES (%s, %s, %s, %s, %s, %s, %s,
                                %s, %s, %s, 'PUBLISHED', %s, %s)
                        RETURNING post_id
                        """,
                        (
                            author_id, title, category, fund_id, summary, content, ",".join(tags),
                            220 + idx * 73, 6 + idx % 7, 24 + idx * 9, created, created,
                        ),
                    )
                    post_ids.append(cur.fetchone()[0])

                comments = [
                    "这个筛选思路很适合放进我的研究池继续跟踪。",
                    "建议再结合基金经理任职年限一起观察。",
                    "我更关心最大回撤和波动率，收益反而放第二位。",
                    "这类主题基金适合小仓位观察，不适合重仓。",
                    "如果能加入对比记录会更直观。",
                ]
                for idx, post_id in enumerate(post_ids):
                    for j in range(3):
                        cur.execute(
                            """
                            INSERT INTO community_comment(post_id, user_id, content, status, created_time)
                            VALUES (%s, %s, %s, 'PUBLISHED', %s)
                            """,
                            (post_id, user_ids[(idx + j) % len(user_ids)], comments[(idx + j) % len(comments)], base_time + timedelta(hours=idx * 5 + j + 1)),
                        )

                # Follow relationships: make highly followed authors look lively without exposing scoring rules.
                cur.execute("DELETE FROM community_author_follow WHERE author_id = ANY(%s)", (author_ids,))
                for i, follower in enumerate(user_ids):
                    for author_id in author_ids:
                        if (author_id + follower + i) % 3 != 0:
                            cur.execute(
                                "INSERT INTO community_author_follow(user_id, author_id, created_time) VALUES (%s, %s, CURRENT_TIMESTAMP)",
                                (follower, author_id),
                            )

                cur.execute("DELETE FROM community_like WHERE target_type='POST' AND target_id = ANY(%s)", (post_ids,))
                for idx, post_id in enumerate(post_ids):
                    for user_id in user_ids[: 4 + idx % 5]:
                        cur.execute(
                            "INSERT INTO community_like(user_id, target_type, target_id, created_time) VALUES (%s, 'POST', %s, CURRENT_TIMESTAMP)",
                            (user_id, post_id),
                        )

        print(f"Community showcase seeded: authors={len(author_ids)}, posts={len(post_ids)}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
