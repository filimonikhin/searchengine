package searchengine.services;

import searchengine.Application;
import searchengine.config.JsoupConfig;
import searchengine.parsers.LemmaHelper;
import searchengine.parsers.PageParser;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.parsers.PageParserParams;
import searchengine.parsers.Parser;
import searchengine.repository.DBRepository;

import java.util.concurrent.ForkJoinPool;

public class StartIndexing extends Thread {
    private final String url;
    private final Integer siteId;
    private final DBRepository dbRepository;
    private final JsoupConfig jsoupConfig;

    public StartIndexing(String url, String name, DBRepository dbRepository, JsoupConfig jsoupConfig) {
        this.url = url;
        this.dbRepository = dbRepository;
        this.jsoupConfig  = jsoupConfig;

        SiteEntity siteEntity = dbRepository.findSiteByUrl(url);

        if (siteEntity != null) {
            // удаление индексов, лемм, страниц и сайт из базы
            int siteId = siteEntity.getId();
            dbRepository.deleteDataBySiteId(siteId);
        }

        // сохраняем сайт со статусом INDEXING
        dbRepository.saveSite(new SiteEntity(StatusType.INDEXING, url, name));
        this.siteId = dbRepository.findSiteByUrl(url).getId();
    }

    @Override
    public void run() {
        try {
            ForkJoinPool pool = new ForkJoinPool();
            Application.LOGGER.info("==== Start indexing site: " + url + " ====");

            PageParserParams params = PageParserParams.builder()
                                        .pageAddress(url)
                                        .root(url)
                                        .siteId(this.siteId)
                                        .dbRepository(dbRepository)
                                        .jsoupConfig(jsoupConfig)
                                        .isRoot(true)
                                        .fromPage("/")
                                        .parser(new Parser(LemmaHelper.newInstance()))
                                        .build();

            Integer res = pool.invoke(new PageParser(params));
            dbRepository.setStatusToIndexed(siteId);
            Application.LOGGER.info("==== Stop indexing site: " + url + " ====");

        } catch (Exception ex) {
            Application.LOGGER.error("==== Error indexing site: " + ex.getMessage() + " ====");
        }
    }

}
