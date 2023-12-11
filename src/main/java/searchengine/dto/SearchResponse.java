package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor

public class SearchResponse {
    private boolean result;
    private int count;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
    private List<SearchItem> data;

    public SearchResponse(boolean result, String message) {
        this.result  = result;
        this.message = message;
    }

    public SearchResponse(boolean result, String message, int count) {
        this(result, message);
        this.count = count;
    }

    public SearchResponse(boolean result, String message, int count, List<SearchItem> data) {
        this(result, message, count);
        this.data = data;
    }


}
