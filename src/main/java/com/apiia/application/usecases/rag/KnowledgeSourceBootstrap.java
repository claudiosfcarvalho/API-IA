package com.apiia.application.usecases.rag;

import com.apiia.config.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSourceBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSourceBootstrap.class);

    private final KnowledgeSourceIngestionService ingestionService;
    private final AppProperties appProperties;

    public KnowledgeSourceBootstrap(KnowledgeSourceIngestionService ingestionService,
                                    AppProperties appProperties) {
        this.ingestionService = ingestionService;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!appProperties.getRag().isBootstrapOnStartup()) {
            log.info("bootstrap de knowledge-source desabilitado por configuracao");
            return;
        }

        KnowledgeBootstrapSummary summary = ingestionService.bootstrapAll();
        log.info("knowledge-source carregada: scanned={} indexed={} updated={} skipped={} failed={}",
                summary.scanned(), summary.indexed(), summary.updated(), summary.skipped(), summary.failed());
    }
}
