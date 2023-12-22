package searchengine.repository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.Application;
import searchengine.utility.StrUtl;
import searchengine.model.*;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class DBRepository {
    // По условию задачи, исключать из полученного списка леммы, которые встречаются на слишком
    // большом количестве страниц. Определить этот процент самостоятельно
    private static final int MAX_PERCENT_PAGE_BY_LEMMA = 80;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public DBRepository(SiteRepository  siteRepository,
                        PageRepository  pageRepository,
                        LemmaRepository lemmaRepository,
                        IndexRepository indexRepository)
    {
        this.siteRepository  = siteRepository;
        this.pageRepository  = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    // поиск сайта по URL
    public SiteEntity getSite(String siteUrl) {

        String mask = "^(http|https)://.*";
        siteUrl = StrUtl.nvl(siteUrl.toLowerCase(), "");

        if (!siteUrl.matches(mask)) {
            return null;
        }

        try {
            URL url = new URI(siteUrl).toURL();

            // оставляем url без path
            String siteURL = siteUrl.replace(url.getPath(), "");

            return siteRepository.findByUrl(siteURL);
        } catch(Exception ex) {
            Application.LOGGER.error("Сайт " + siteUrl + " не определен", ex);
        }

      return null;

    }

    public PageEntity getPage(String path, String root, Integer siteId) {
        path = StrUtl.nvl(path, "");
        root = StrUtl.nvl(root, "");
        String path2Find = "";

        try {
            path2Find = path.equals(root) ? "/" : path.substring(root.length());
            return pageRepository.findBySiteIdAndPath(siteId, path2Find);
        } catch (NullPointerException ex) {
            Application.LOGGER.error("Страница " + path2Find + " не определена", ex);
        }

        return null;
    }

    public void updateStatusTimeById(Integer siteId) {
        siteRepository.updateStatusTimeById(siteId, LocalDateTime.now());
    }

    public SiteEntity findSiteById(Integer id) {
        return siteRepository.findSiteById(id);
    }

    public PageEntity findPageById(Integer id) {
        return pageRepository.findPageById(id);
    }

    public void setStatusToIndexFailed(Integer siteId, String lastError) {
        siteRepository.setStatusToIndexFailed(siteId, lastError, LocalDateTime.now());
        Application.LOGGER.warn(lastError);
    }

    public void setLastError(Integer siteId, String lastError) {
        siteRepository.setLastError(siteId, lastError, LocalDateTime.now());
    }

    public void setStatusToIndexed(Integer siteId) {
        siteRepository.setStatusToIndexed(siteId, LocalDateTime.now());
    }

    public void savePage(PageEntity page) {
        pageRepository.savePage(page.getCode(), page.getSiteId(), page.getPath(), page.getContent());
    }

    public void saveLemmas(Integer siteId, Integer pageId, Map<String, Integer> lemmas) {
        lemmas.forEach((lemma, lemmaFreq) -> {
            lemmaRepository.saveLemma(siteId, lemma);
            Integer lemmaId = lemmaRepository.getLemma(siteId, lemma).getId();
            indexRepository.saveIndex(pageId, lemmaId, lemmaFreq);
        });
    }

    @Transactional
    public void deletePage(Integer pageId) {
        // декрементируем леммы
        lemmaRepository.updateByPageId(pageId);
        // удаляем леммы у которых frequency = 0
        lemmaRepository.deleteByPageId(pageId);
        indexRepository.deleteByPage(pageId);
        pageRepository.deleteById(pageId);
    }

    public HashMap<String, Integer> getFilteredLemmas(Set<String> lemmas, Integer siteId, Integer maxPercentageOfPage) {

        HashMap<String, Integer> result = new HashMap<>();

        // общее кол-во страниц по сайту / по всем сайтам (siteId = null)
        int totalPages = getPageCountBySiteId(siteId);

        if (totalPages == 0) {
            return result;
        }

        for (String lemma : lemmas) {
            int frequency = lemmaRepository.getFrequency(siteId, lemma);

            if (frequency > 0) {
                float pageRatio = ((float)frequency / (float)totalPages) * 100;

                if (pageRatio <= MAX_PERCENT_PAGE_BY_LEMMA) {
                    result.put(lemma, frequency);
                }
            }
        }

        return result;
    }

    public HashMap<Integer, Float> getPages(Integer siteId, String lemma) {
        Set<IndexEntity> indexSet = indexRepository.getIndexListByLemma(siteId, lemma);

        HashMap<Integer, Float> foundPages = new HashMap<>();
        indexSet.forEach(index -> foundPages.put(index.getPageId(), index.getRank()));

        return foundPages;
    }

    public SiteEntity findSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    public void deleteDataBySiteId(Integer siteId) {
        lemmaRepository.deleteBySite(siteId);
        indexRepository.deleteBySite(siteId);
        pageRepository.deleteBySite(siteId);
        siteRepository.deleteById(siteId);
    }

    public void saveSite(SiteEntity siteEntity) {
        siteRepository.save(siteEntity);
    }

    public int getPageCountBySiteId(Integer siteId) {
        return pageRepository.getPageCountBySiteId(siteId);
    }
}
