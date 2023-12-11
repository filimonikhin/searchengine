package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchQuery {
    private String query;
    private Integer offset;
    private Integer limit;
    private String site;
}
