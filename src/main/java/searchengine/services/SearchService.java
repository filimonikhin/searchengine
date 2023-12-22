package searchengine.services;

import searchengine.dto.SearchResponse;
import searchengine.dto.search.SearchQuery;

public interface SearchService {
    SearchResponse search(SearchQuery searchQuery);
}
