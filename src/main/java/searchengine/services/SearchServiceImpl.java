package searchengine.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.utility.MapUtl;
import searchengine.utility.StrUtl;
import searchengine.config.SearchConfig;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.SearchItem;
import searchengine.dto.SearchResponse;
import searchengine.dto.search.SearchQuery;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.parsers.LemmaHelper;
import searchengine.parsers.Parser;
import searchengine.repository.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SearchServiceImpl implements SearchService {
    private final DBRepository dbRepository;
    private final SiteList siteList;
    private final SearchConfig searchConfig;

    public SearchServiceImpl(SiteRepository  siteRepository,
                             PageRepository  pageRepository,
                             LemmaRepository lemmaRepository,
                             IndexRepository indexRepository,
                             SiteList        siteList,
                             SearchConfig    searchConfig)
    {
        this.dbRepository = new DBRepository(siteRepository, pageRepository, lemmaRepository, indexRepository);
        this.siteList = siteList;
        this.searchConfig = searchConfig;
    }

    @Override
    public SearchResponse search(SearchQuery searchQuery) {
        String msg = "Задан поисковый запрос \"" + searchQuery.getQuery()
                   + "\" для сайта \"" + searchQuery.getSite() + "\"";

        Application.LOGGER.info(msg);

        try {
            String query = StrUtl.nvl(searchQuery.getQuery(), "");
            if (query.isBlank()) {
                msg = "Задан пустой поисковый запрос";
                Application.LOGGER.warn(msg);
                return new SearchResponse(false, msg);
            }

            // результирующий список
            List<SearchItem> resultSearhList = new ArrayList<>();

            // разбиваем запрос на леммы
            Parser parser = new Parser(LemmaHelper.newInstance());

            Map<String, Integer> lemmas = parser.getLemmas(query);

            if (lemmas.size() == 0) {
                return new SearchResponse(false, "", 0, resultSearhList);
            }

            Integer    siteId     = null;
            SiteEntity siteEntity = null;

            if (searchQuery.getSite() != null) {
                siteEntity = dbRepository.getSite(searchQuery.getSite());

                if (siteEntity == null) {
                    msg = "Сайт " + searchQuery.getSite() + " не найден в БД";
                    Application.LOGGER.warn(msg);
                    return new SearchResponse(false, msg);
                }

                siteId = siteEntity.getId();
            }

            if (!siteIsIndexed(siteEntity)) {
                return new SearchResponse(false, (siteEntity == null ? "Один из сайтов не проиндексирован" : "Сайт не проиндексирован"));
            }

            // по ТЗ отсекаем леммы, которые встречались на большом кол-ве страниц
            // (значение этого процента dbRepository.MAX_PERCENT_PAGE_BY_LEMMA определяем самостоятельно)
            // получаем леммы по сайту (сайтам) и сортировка в порядке возрастания значения frequency
            Map<String, Integer> ascLemmas = MapUtl.sortMapByValueAsc(dbRepository.getFilteredLemmas(lemmas.keySet(), siteId, searchConfig.getMaxPercentageOfPage()));

            if (ascLemmas.size() == 0) {
                return new SearchResponse(false, "", 0, resultSearhList);
            }

            lemmas.keySet().removeIf(k -> !ascLemmas.containsKey(k));

            Map<Integer, Float> descPages = getOrderedPagesByDesc(siteId, ascLemmas);

            float maxRank = descPages.isEmpty() ? 1 : descPages.values().iterator().next(); // т.к. отсортирована по убыванию

            int offSetIndex = searchQuery.getOffset() == null ?  0 : searchQuery.getOffset();
            int limitCount  = searchQuery.getLimit()  == null ? 20 : searchQuery.getLimit();

            for (Map.Entry<Integer, Float> pageEntry : descPages.entrySet()) {

                if (offSetIndex-- > 0) {
                    continue;
                }

                PageEntity page = dbRepository.findPageById(pageEntry.getKey());
                SiteEntity site = dbRepository.findSiteById(page.getSiteId());

                SearchItem dataItem = SearchItem.builder()
                        .site(site.getUrl())
                        .siteName(site.getName())
                        .uri(page.getPath())
                        .title(Jsoup.parse(page.getContent()).title())
                        .snippet(parser.createSnippet(Jsoup.parse(page.getContent()).text(), lemmas))
                        .relevance(pageEntry.getValue() / maxRank)
                        .build();
                resultSearhList.add(dataItem);

                if (--limitCount == 0) {
                    break;
                }
            }

            return new SearchResponse(true, "", descPages.size(), resultSearhList);

        } catch(IOException e) {
            throw new RuntimeException();
        }
    }

    // проверка, что сайт проиндексирован
    private boolean siteIsIndexed(SiteEntity siteEntity) {

        // выбран поиск по одному сайту
        if (siteEntity != null) {
            return siteEntity.getStatus() == StatusType.INDEXED;
        }

        // выбран поиск по всем сайтам
        for (Site site : siteList.getSites()) {
            SiteEntity siteEntity_ = dbRepository.getSite(site.getUrl());

            if (siteEntity_ != null && siteEntity_.getStatus() != StatusType.INDEXED) {
                return false;
            }
        }

        return true;
    }


    private Map<Integer, Float> getOrderedPagesByDesc(Integer siteId, Map<String, Integer> lemmas) {
        ConcurrentHashMap<Integer, Float> allFoundPages = new ConcurrentHashMap<>();
        boolean isFirst = true;

        for (String lemma : lemmas.keySet()) {
            Map<Integer, Float> foundPages = dbRepository.getPages(siteId, lemma);

            if (isFirst) {
                allFoundPages.putAll(foundPages);
                isFirst = false;
            } else {
                for (Integer k : allFoundPages.keySet()) {
                    // удаляем станицу из общего списка найденных страниц
                    if (!foundPages.containsKey(k)) {
                        allFoundPages.remove(k);
                        continue;
                    }
                    allFoundPages.replace(k, allFoundPages.get(k) + foundPages.get(k));
                }
            }

            if (allFoundPages.size() == 0) {
                break;
            }
        }

        Map<Integer, Float> descPages;

        if (allFoundPages.isEmpty()) {
            descPages = new HashMap<>();
        } else {

            descPages = MapUtl.sortMapByValueDesc(allFoundPages);
        }

        return descPages;
    }

}
