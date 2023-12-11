package searchengine.controllers;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.Response;
import searchengine.dto.SearchResponse;
import searchengine.dto.search.SearchQuery;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService,
                         IndexingService   indexingService,
                         SearchService     searchService)
    {
        this.statisticsService = statisticsService;
        this.indexingService   = indexingService;
        this.searchService     = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<JSONObject> startIndexing() {
        Response startIndexingResponse = indexingService.startIndexing();
        return new ResponseEntity<>(startIndexingResponse.get(), startIndexingResponse.getHttpStatus());

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<JSONObject> stopIndexing() {
        Response stopIndexingResponse = indexingService.stopIndexing();
        return new ResponseEntity<>(stopIndexingResponse.get(), stopIndexingResponse.getHttpStatus());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<JSONObject> indexPage(String url) {
        Response indexPageResponse = indexingService.indexPage(url);
        return new ResponseEntity<>(indexPageResponse.get(), indexPageResponse.getHttpStatus());
    }

    @GetMapping("/search")
    public ResponseEntity search(SearchQuery searchQuery) {
            SearchResponse response =  searchService.search(searchQuery);

            if (!response.isResult()) {
                return new ResponseEntity<>(responseError(response.getMessage()), HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(response);
    }

    private static JSONObject responseError(String message) {
        JSONObject response = new JSONObject();
        response.put("result", false);
        response.put("error", message);
        return response;
    }

}
