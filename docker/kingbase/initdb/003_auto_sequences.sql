-- Standard sequence defaults for Kingbase/PostgreSQL-compatible mode.
-- Run after importing previous schema/data so AUTO primary keys continue to work.

CREATE SEQUENCE IF NOT EXISTS public.ai_analysis_record_analysis_id_seq;
SELECT setval('public.ai_analysis_record_analysis_id_seq', COALESCE((SELECT MAX(analysis_id) FROM public.ai_analysis_record), 0) + 1, false);
ALTER TABLE public.ai_analysis_record ALTER COLUMN analysis_id SET DEFAULT nextval('public.ai_analysis_record_analysis_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.ai_recommend_item_item_id_seq;
SELECT setval('public.ai_recommend_item_item_id_seq', COALESCE((SELECT MAX(item_id) FROM public.ai_recommend_item), 0) + 1, false);
ALTER TABLE public.ai_recommend_item ALTER COLUMN item_id SET DEFAULT nextval('public.ai_recommend_item_item_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.ai_recommend_record_recommend_id_seq;
SELECT setval('public.ai_recommend_record_recommend_id_seq', COALESCE((SELECT MAX(recommend_id) FROM public.ai_recommend_record), 0) + 1, false);
ALTER TABLE public.ai_recommend_record ALTER COLUMN recommend_id SET DEFAULT nextval('public.ai_recommend_record_recommend_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.community_author_follow_follow_id_seq;
SELECT setval('public.community_author_follow_follow_id_seq', COALESCE((SELECT MAX(follow_id) FROM public.community_author_follow), 0) + 1, false);
ALTER TABLE public.community_author_follow ALTER COLUMN follow_id SET DEFAULT nextval('public.community_author_follow_follow_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.community_author_profile_author_id_seq;
SELECT setval('public.community_author_profile_author_id_seq', COALESCE((SELECT MAX(author_id) FROM public.community_author_profile), 0) + 1, false);
ALTER TABLE public.community_author_profile ALTER COLUMN author_id SET DEFAULT nextval('public.community_author_profile_author_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.community_comment_comment_id_seq;
SELECT setval('public.community_comment_comment_id_seq', COALESCE((SELECT MAX(comment_id) FROM public.community_comment), 0) + 1, false);
ALTER TABLE public.community_comment ALTER COLUMN comment_id SET DEFAULT nextval('public.community_comment_comment_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.community_like_like_id_seq;
SELECT setval('public.community_like_like_id_seq', COALESCE((SELECT MAX(like_id) FROM public.community_like), 0) + 1, false);
ALTER TABLE public.community_like ALTER COLUMN like_id SET DEFAULT nextval('public.community_like_like_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.community_post_post_id_seq;
SELECT setval('public.community_post_post_id_seq', COALESCE((SELECT MAX(post_id) FROM public.community_post), 0) + 1, false);
ALTER TABLE public.community_post ALTER COLUMN post_id SET DEFAULT nextval('public.community_post_post_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.community_post_view_view_id_seq;
SELECT setval('public.community_post_view_view_id_seq', COALESCE((SELECT MAX(view_id) FROM public.community_post_view), 0) + 1, false);
ALTER TABLE public.community_post_view ALTER COLUMN view_id SET DEFAULT nextval('public.community_post_view_view_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.company_tag_relation_relation_id_seq;
SELECT setval('public.company_tag_relation_relation_id_seq', COALESCE((SELECT MAX(relation_id) FROM public.company_tag_relation), 0) + 1, false);
ALTER TABLE public.company_tag_relation ALTER COLUMN relation_id SET DEFAULT nextval('public.company_tag_relation_relation_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.data_import_batch_batch_id_seq;
SELECT setval('public.data_import_batch_batch_id_seq', COALESCE((SELECT MAX(batch_id) FROM public.data_import_batch), 0) + 1, false);
ALTER TABLE public.data_import_batch ALTER COLUMN batch_id SET DEFAULT nextval('public.data_import_batch_batch_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.data_import_error_error_id_seq;
SELECT setval('public.data_import_error_error_id_seq', COALESCE((SELECT MAX(error_id) FROM public.data_import_error), 0) + 1, false);
ALTER TABLE public.data_import_error ALTER COLUMN error_id SET DEFAULT nextval('public.data_import_error_error_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_announcement_announcement_id_seq;
SELECT setval('public.fund_announcement_announcement_id_seq', COALESCE((SELECT MAX(announcement_id) FROM public.fund_announcement), 0) + 1, false);
ALTER TABLE public.fund_announcement ALTER COLUMN announcement_id SET DEFAULT nextval('public.fund_announcement_announcement_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_attribution_attribution_id_seq;
SELECT setval('public.fund_attribution_attribution_id_seq', COALESCE((SELECT MAX(attribution_id) FROM public.fund_attribution), 0) + 1, false);
ALTER TABLE public.fund_attribution ALTER COLUMN attribution_id SET DEFAULT nextval('public.fund_attribution_attribution_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_company_company_id_seq;
SELECT setval('public.fund_company_company_id_seq', COALESCE((SELECT MAX(company_id) FROM public.fund_company), 0) + 1, false);
ALTER TABLE public.fund_company ALTER COLUMN company_id SET DEFAULT nextval('public.fund_company_company_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_compare_item_item_id_seq;
SELECT setval('public.fund_compare_item_item_id_seq', COALESCE((SELECT MAX(item_id) FROM public.fund_compare_item), 0) + 1, false);
ALTER TABLE public.fund_compare_item ALTER COLUMN item_id SET DEFAULT nextval('public.fund_compare_item_item_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_compare_record_compare_id_seq;
SELECT setval('public.fund_compare_record_compare_id_seq', COALESCE((SELECT MAX(compare_id) FROM public.fund_compare_record), 0) + 1, false);
ALTER TABLE public.fund_compare_record ALTER COLUMN compare_id SET DEFAULT nextval('public.fund_compare_record_compare_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_holding_detail_holding_id_seq;
SELECT setval('public.fund_holding_detail_holding_id_seq', COALESCE((SELECT MAX(holding_id) FROM public.fund_holding_detail), 0) + 1, false);
ALTER TABLE public.fund_holding_detail ALTER COLUMN holding_id SET DEFAULT nextval('public.fund_holding_detail_holding_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_holding_report_report_id_seq;
SELECT setval('public.fund_holding_report_report_id_seq', COALESCE((SELECT MAX(report_id) FROM public.fund_holding_report), 0) + 1, false);
ALTER TABLE public.fund_holding_report ALTER COLUMN report_id SET DEFAULT nextval('public.fund_holding_report_report_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_info_fund_id_seq;
SELECT setval('public.fund_info_fund_id_seq', COALESCE((SELECT MAX(fund_id) FROM public.fund_info), 0) + 1, false);
ALTER TABLE public.fund_info ALTER COLUMN fund_id SET DEFAULT nextval('public.fund_info_fund_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_manager_manager_id_seq;
SELECT setval('public.fund_manager_manager_id_seq', COALESCE((SELECT MAX(manager_id) FROM public.fund_manager), 0) + 1, false);
ALTER TABLE public.fund_manager ALTER COLUMN manager_id SET DEFAULT nextval('public.fund_manager_manager_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_manager_relation_relation_id_seq;
SELECT setval('public.fund_manager_relation_relation_id_seq', COALESCE((SELECT MAX(relation_id) FROM public.fund_manager_relation), 0) + 1, false);
ALTER TABLE public.fund_manager_relation ALTER COLUMN relation_id SET DEFAULT nextval('public.fund_manager_relation_relation_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_nav_nav_id_seq;
SELECT setval('public.fund_nav_nav_id_seq', COALESCE((SELECT MAX(nav_id) FROM public.fund_nav), 0) + 1, false);
ALTER TABLE public.fund_nav ALTER COLUMN nav_id SET DEFAULT nextval('public.fund_nav_nav_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_performance_metric_metric_id_seq;
SELECT setval('public.fund_performance_metric_metric_id_seq', COALESCE((SELECT MAX(metric_id) FROM public.fund_performance_metric), 0) + 1, false);
ALTER TABLE public.fund_performance_metric ALTER COLUMN metric_id SET DEFAULT nextval('public.fund_performance_metric_metric_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_portfolio_portfolio_id_seq;
SELECT setval('public.fund_portfolio_portfolio_id_seq', COALESCE((SELECT MAX(portfolio_id) FROM public.fund_portfolio), 0) + 1, false);
ALTER TABLE public.fund_portfolio ALTER COLUMN portfolio_id SET DEFAULT nextval('public.fund_portfolio_portfolio_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_score_score_id_seq;
SELECT setval('public.fund_score_score_id_seq', COALESCE((SELECT MAX(score_id) FROM public.fund_score), 0) + 1, false);
ALTER TABLE public.fund_score ALTER COLUMN score_id SET DEFAULT nextval('public.fund_score_score_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_tag_tag_id_seq;
SELECT setval('public.fund_tag_tag_id_seq', COALESCE((SELECT MAX(tag_id) FROM public.fund_tag), 0) + 1, false);
ALTER TABLE public.fund_tag ALTER COLUMN tag_id SET DEFAULT nextval('public.fund_tag_tag_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_tag_relation_relation_id_seq;
SELECT setval('public.fund_tag_relation_relation_id_seq', COALESCE((SELECT MAX(relation_id) FROM public.fund_tag_relation), 0) + 1, false);
ALTER TABLE public.fund_tag_relation ALTER COLUMN relation_id SET DEFAULT nextval('public.fund_tag_relation_relation_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.fund_user_user_id_seq;
SELECT setval('public.fund_user_user_id_seq', COALESCE((SELECT MAX(user_id) FROM public.fund_user), 0) + 1, false);
ALTER TABLE public.fund_user ALTER COLUMN user_id SET DEFAULT nextval('public.fund_user_user_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.manager_tag_relation_relation_id_seq;
SELECT setval('public.manager_tag_relation_relation_id_seq', COALESCE((SELECT MAX(relation_id) FROM public.manager_tag_relation), 0) + 1, false);
ALTER TABLE public.manager_tag_relation ALTER COLUMN relation_id SET DEFAULT nextval('public.manager_tag_relation_relation_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.portfolio_fund_relation_relation_id_seq;
SELECT setval('public.portfolio_fund_relation_relation_id_seq', COALESCE((SELECT MAX(relation_id) FROM public.portfolio_fund_relation), 0) + 1, false);
ALTER TABLE public.portfolio_fund_relation ALTER COLUMN relation_id SET DEFAULT nextval('public.portfolio_fund_relation_relation_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.user_favorite_favorite_id_seq;
SELECT setval('public.user_favorite_favorite_id_seq', COALESCE((SELECT MAX(favorite_id) FROM public.user_favorite), 0) + 1, false);
ALTER TABLE public.user_favorite ALTER COLUMN favorite_id SET DEFAULT nextval('public.user_favorite_favorite_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.user_recent_fund_view_view_id_seq;
SELECT setval('public.user_recent_fund_view_view_id_seq', COALESCE((SELECT MAX(view_id) FROM public.user_recent_fund_view), 0) + 1, false);
ALTER TABLE public.user_recent_fund_view ALTER COLUMN view_id SET DEFAULT nextval('public.user_recent_fund_view_view_id_seq');

CREATE SEQUENCE IF NOT EXISTS public.user_saved_filter_filter_id_seq;
SELECT setval('public.user_saved_filter_filter_id_seq', COALESCE((SELECT MAX(filter_id) FROM public.user_saved_filter), 0) + 1, false);
ALTER TABLE public.user_saved_filter ALTER COLUMN filter_id SET DEFAULT nextval('public.user_saved_filter_filter_id_seq');
