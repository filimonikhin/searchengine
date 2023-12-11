package searchengine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchItem {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
