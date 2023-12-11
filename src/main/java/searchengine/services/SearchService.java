package searchengine.services;

import searchengine.dto.SearchResponse;
import searchengine.dto.search.SearchQuery;
import java.io.IOException;

public interface SearchService {
    SearchResponse search(SearchQuery searchQuery);
}
