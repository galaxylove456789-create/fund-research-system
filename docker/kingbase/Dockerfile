FROM kingbase_v009r001c010b0004_single_x86:v1

COPY fundresearch-entrypoint.sh /home/kingbase/fundresearch-entrypoint.sh

USER root
RUN chmod +x /home/kingbase/fundresearch-entrypoint.sh \
    && chown kingbase:kingbase /home/kingbase/fundresearch-entrypoint.sh

USER kingbase
ENTRYPOINT ["/bin/bash", "/home/kingbase/fundresearch-entrypoint.sh"]
