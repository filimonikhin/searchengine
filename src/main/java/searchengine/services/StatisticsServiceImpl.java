package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.config.JsoupConfig;
import searchengine.dto.statistics.*;
import searchengine.model.*;
import searchengine.repository.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
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
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());

            Integer siteId = (siteEntity == null ? 0 : siteEntity.getId());
            Integer pages  = (siteEntity == null ? 0 : pageRepository.getPageCountBySiteId(siteId));
            Integer lemmas = (siteEntity == null ? 0 : lemmaRepository.getCountBySiteId(siteId));

            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity == null ? "NOT INDEXED" : siteEntity.getStatus().toString());
            item.setError(siteEntity == null ? "Индексирование не запускалось" : siteEntity.getLastError());

            if (siteEntity == null) {
                item.setStatusTime(System.currentTimeMillis());
            } else {
                item.setStatusTime(ZonedDateTime.of(siteEntity.getStatusTime(),
                                                    ZoneId.systemDefault()).toInstant().toEpochMilli());
            }

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

}
