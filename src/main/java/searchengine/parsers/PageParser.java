package searchengine.parsers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.Application;
import searchengine.utility.StrUtl;
import searchengine.config.JsoupConfig;
import searchengine.model.*;
import searchengine.repository.DBRepository;
import searchengine.services.AppContext;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class PageParser extends RecursiveTask<Integer> {
    private final String pageAddress;
    private final String root;
    private final Integer siteId;
    private final int rootLength;
    final DBRepository dbRepository;
    final JsoupConfig jsoupConfig;
    private int statusCode;
    private String pageHTML;
    private final boolean isRoot;
    private final String fromPage;
    private final Parser parser;
    private final HashSet<String> links;
    private final List<PageParser> taskList;

    public PageParser(PageParserParams params) {
        this.pageAddress  = params.getPageAddress();
        this.root         = params.getRoot();
        this.siteId       = params.getSiteId();
        this.rootLength   = params.getRoot().length();
        this.dbRepository = params.getDbRepository();
        this.jsoupConfig  = params.getJsoupConfig();
        this.isRoot       = params.getIsRoot();
        this.fromPage     = params.getFromPage();
        this.parser       = params.parser;
        this.links        = new HashSet<>();
        this.taskList     = new ArrayList<>();
    }

    private boolean isBadLink (String path) {
        if ((path = StrUtl.nvl(path, "")).isBlank()) {
            return true;
        }

        if (StrUtl.isContainsBadChar(path)) {
            return true;
        }

        if (AppContext.badAddresses.contains(path)) {
            return true;
        }

        if (StrUtl.isHrefFile(path)) {
            return true;
        }

        if (!isRoot && path.equals(root)) {
            return true;
        }

        if (dbRepository.getPage(path, root, siteId) != null) {
            AppContext.badAddresses.add(path);
            return true;
        }

        return false;
    }

    private void stopIndexing() {
        AppContext.decrementThreads();
        String msg = "Индексирование остановлено пользователем";
        dbRepository.setStatusToIndexFailed(siteId, msg);
        Application.LOGGER.info(msg);
    }

    void savePage() {
        if (dbRepository.getPage(pageAddress, root, siteId) == null) {
            PageEntity pageEntity = new PageEntity();
            pageEntity.setSiteId(siteId);
            pageEntity.setCode(statusCode);

            // удаляем root адрес
            String path = StrUtl.nvl(pageAddress, "").substring(rootLength).trim();

            if (path.isBlank()) {
                path = "/";
            }

            pageEntity.setPath(path);
            pageEntity.setContent((pageHTML == null ? "" : pageHTML));
            dbRepository.savePage(pageEntity);
        }
    }

    void parsePage() {
        Map<String, Integer> lemmas = parser.getLemmas(pageHTML);

        if (lemmas.size() == 0) {
            String errMsg = "Ошибка при распознавании страницы " + pageAddress;
            Application.LOGGER.error(errMsg);
            dbRepository.setLastError(siteId, errMsg);
            AppContext.badAddresses.add(pageAddress);
            return;
        }

        PageEntity pageEntity = dbRepository.getPage(pageAddress, root, siteId);

        if (pageEntity == null) {
            return;
        }

        dbRepository.saveLemmas(siteId, pageEntity.getId(), lemmas);
    }

    @Override
    protected Integer compute() {
        int res = 0;

        if (isBadLink(pageAddress)) {return 1;}

        if (!AppContext.isIndexing()) {
            stopIndexing();
            return 1;
        }

        AppContext.incrementThreads();
        delayThread();
        PageWithMessage parsedPage = Parser.getHTMLPage(pageAddress, jsoupConfig);

        if (parsedPage.getMessage() != null) {
            dbRepository.setLastError(siteId, parsedPage.getMessage());
        }

        pageHTML   = parsedPage.getPage().getContent();
        statusCode = parsedPage.getPage().getCode();
        savePage();

        if (statusCode == 200) {parsePage();}

        if (isRoot && (statusCode != 200 || pageHTML.isEmpty())) {
            dbRepository.setStatusToIndexFailed(siteId, "Невозможно получить главную страницу сайта");
        } else {
            dbRepository.updateStatusTimeById(siteId);
        }

        if (pageHTML.isEmpty()) {
            AppContext.decrementThreads();
            return 1;
        }

        if (!AppContext.isIndexing()) {
            stopIndexing();
            return 1;
        }

        // получаем все ссылки на странице
        Elements elements = Jsoup.parse(pageHTML).select("a");
        elements.forEach(this::processElement);
        for (PageParser task : taskList) {res += task.join();}

        AppContext.decrementThreads();

        if (isRoot && AppContext.isIndexing()) {
            dbRepository.setStatusToIndexed(siteId);
        }

        return res;
    }

    private void delayThread() {
        try {
            Thread.sleep(500L + (long) (Math.random() * 1000));
        } catch (InterruptedException ex) {
            AppContext.decrementThreads();
            throw new RuntimeException(ex);
        }
    }

    private void processElement(Element element) {
        String link = element.attr("href");

        if (!link.equals("/") && link.startsWith("/")) {
            link = root + link;
        }

        if (!links.contains(link) && isCorrectLink(link)) {
            links.add(link);
            AppContext.checkedAddresses.add(link);
            runTask(link);
        }
    }

    private void runTask(String link) {
        PageParserParams params = PageParserParams.builder()
                                                  .pageAddress(link).root(root).siteId(siteId)
                                                  .dbRepository(dbRepository).jsoupConfig(jsoupConfig)
                                                  .isRoot(false).fromPage(pageAddress).parser(parser)
                                                  .build();
        PageParser task = new PageParser(params);
        task.fork();
        taskList.add(task);
    }

    private boolean isCorrectLink(String link) {
        return (!AppContext.checkedAddresses.contains(link)
                && AppContext.isIndexing()
                && link.startsWith(root)
                && !isBadLink(link)
                && !link.equals(pageAddress)
               );
    }

}
