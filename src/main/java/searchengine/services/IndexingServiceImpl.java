package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.config.JsoupConfig;
import searchengine.dto.OKResponse;
import searchengine.dto.Response;
import searchengine.dto.MessageResponse;
import searchengine.model.PageEntity;
import searchengine.model.PageWithMessage;
import searchengine.model.SiteEntity;
import searchengine.parsers.LemmaHelper;
import searchengine.parsers.Parser;
import searchengine.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteList sites;
    private final JsoupConfig jsoupConfig;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @Override
    public Response startIndexing() {

        if (AppContext.isIndexing()) {
            return new MessageResponse("Индексация уже запущена", HttpStatus.BAD_REQUEST);
        }

        AppContext.resetCounter();
        AppContext.setIsIndexing(true);

        List<Site> sitesList = sites.getSites();
        List<Thread> threads = new ArrayList<>();

        sitesList.forEach(site -> {
            DBRepository dbRepository = new DBRepository(siteRepository, pageRepository,
                                                          lemmaRepository, indexRepository);

            threads.add(new StartIndexing(site.getUrl(), site.getName(), dbRepository, jsoupConfig));
        });

        threads.forEach(Thread::start);
        return new OKResponse();
    }

    @Override
    public Response stopIndexing() {

        if (!AppContext.isIndexing()) {
            return new MessageResponse("Индексация не запущена", HttpStatus.BAD_REQUEST);
        }

        AppContext.setIsIndexing(false);
        return new OKResponse();
    }

    @Override
    public Response indexPage(String pageUrl) {
        Application.LOGGER.info("==== Start indexing page: " + pageUrl + " ====");

        if (AppContext.isIndexing()) {return new MessageResponse("Индексация уже запущена", HttpStatus.BAD_REQUEST);}
        if (pageUrl.isEmpty()) {return new MessageResponse("Введена пустая строка", HttpStatus.BAD_REQUEST);}

        // проверяем, что url должен начинаться как один из списка сайтов
        if (!isPartOfSite(pageUrl)) {
            return new MessageResponse("Данная страница находится за пределами сайтов,\n" +
                    "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST);
        }

        DBRepository dbRepository = new DBRepository(siteRepository, pageRepository, lemmaRepository, indexRepository);
        SiteEntity siteEntity = dbRepository.getSite(pageUrl);

        if (siteEntity == null) {return new MessageResponse("Сайт не индексирован", HttpStatus.BAD_REQUEST);}

        Integer siteId = siteEntity.getId();
        PageWithMessage parsedPage = Parser.getHTMLPage(pageUrl, jsoupConfig);

        if (parsedPage.getMessage() != null) {
            dbRepository.setLastError(siteId, parsedPage.getMessage());
        }

        PageEntity page2save = parsedPage.getPage();

        if (page2save.getContent().isEmpty()) {
            return new MessageResponse("Страницу не удалось прочитать", HttpStatus.BAD_REQUEST);
        }

        String url = pageUrl + (pageUrl.equals(siteEntity.getUrl()) ? "/" : "");
        PageEntity pageInDB = dbRepository.getPage(url, siteEntity.getUrl(), siteId);

        if (pageInDB != null) {dbRepository.deletePage(pageInDB.getId());}

        String path2Save = url.substring(siteEntity.getUrl().length());
        page2save.setSiteId(siteId);
        page2save.setPath(path2Save.isEmpty() ? "/" : path2Save);

        dbRepository.savePage(page2save);

        try {
            Map<String, Integer> lemmas = new Parser(LemmaHelper.newInstance()).getLemmas(page2save.getContent());
            dbRepository.saveLemmas(siteId, dbRepository.getPage(url, siteEntity.getUrl(), siteId).getId(), lemmas);
            Application.LOGGER.info("==== Stop indexing page: " + pageUrl + " ====");
        } catch(IOException e) {
            throw new RuntimeException();
        }

        return new OKResponse();
    }

    // проверка введенной страницы
    private boolean isPartOfSite(String url) {
        List<Site> siteList = sites.getSites();
        return siteList.stream().anyMatch(site -> url.toLowerCase().startsWith(site.getUrl().toLowerCase()));
    }

}
