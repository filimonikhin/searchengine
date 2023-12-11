package searchengine.parsers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.Application;
import searchengine.Utility.StrUtl;
import searchengine.config.JsoupConfig;
import searchengine.model.PageEntity;
import searchengine.model.PageWithMessage;
import searchengine.services.AppContext;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {
   private volatile LemmaHelper lemmaHelper;

    public Parser(LemmaHelper lemmaHelper) {
        this.lemmaHelper = lemmaHelper;
    }

    public Map<String, Integer> getLemmas(String htmlText) {
        try {
            //return LemmaHelper.newInstance().getRussianLemmas(Jsoup.parse(htmlText).text());
            return lemmaHelper.getRussianLemmas(Jsoup.parse(htmlText).text());
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static PageWithMessage getHTMLPage(String path, JsoupConfig jsoupConfig) {
        PageWithMessage result = new PageWithMessage();
        PageEntity      page   = new PageEntity();

        path = StrUtl.nvl(path, "");

        try {
            // коннект к странице
            Connection.Response response = Jsoup.connect(path)
                                                .userAgent(jsoupConfig.getUserAgent())
                                                .referrer(jsoupConfig.getReferrer())
                                                .timeout(jsoupConfig.getTimeout())
                                                .execute();

            int statusCode = response.statusCode();

            // проверка, что страница изменена
            if (statusCode == HttpURLConnection.HTTP_OK && !path.equals(response.url().toString()) ) {
                statusCode = HttpURLConnection.HTTP_MOVED_TEMP;
                AppContext.badAddresses.add(path);
            }

            page.setCode(statusCode);
            page.setContent(statusCode == HttpURLConnection.HTTP_OK ? response.parse().toString() : "");
            result.setPage(page);

        } catch (SocketTimeoutException ex) {
            // Application.logger.error(ex.getMessage() + " Path: " + path);
            // Application.logger.error(ex.toString() + " Path: " + path);
            AppContext.badAddresses.add(path);
            page.setCode(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
            page.setContent("");
            result.setPage(page);
        } catch (Exception ex) {
            // Application.logger.error(ex.getMessage() + " Path: " + path);
            Application.logger.error(ex.toString() + " Path: " + path);
            AppContext.badAddresses.add(path);
            page.setCode(HttpURLConnection.HTTP_BAD_REQUEST);
            page.setContent("");
            result.setPage(page);
            result.setMessage(ex.getMessage() + " " + path);
        }
        return result;
    }

    public String createSnippet(String text, Map<String, Integer> lemmas) {
        String snippet     = "";
        int SNIPPET_LENGTH = 255;

        if (StrUtl.nvl(text, "").isBlank()) {
            return snippet;
        }

        HashSet<String> words = new HashSet<>();
        //List<String> rusWords = lemmaHelper.getRussianLowerCaseWords(text);
        List<String> rusWords = lemmaHelper.getRussianWords(text);

        for (String rusWord : rusWords) {

            if (StrUtl.nvl(rusWord, "").isBlank() || words.contains(rusWord)) {
                continue;
            }

            words.add(rusWord);
            String normalWord = lemmaHelper.getNormalWord(rusWord.toLowerCase());

            if (!lemmas.containsKey(normalWord)) {
                continue;
            }

            if (snippet.isEmpty()) {
                snippet  = text.substring(text.toLowerCase().indexOf(rusWord.toLowerCase()));

                // обрезаем до нужной длины
                if (snippet.length() > SNIPPET_LENGTH) {
                    snippet = snippet.substring(0, SNIPPET_LENGTH);
                }
            }

            // оставляем только найденные слова по лемме
//            Set<String> replacedWords = lemmaHelper.getRussianWords(snippet).stream()
//                                                   .filter(s -> s.equals(rusWord))
//                                                   .collect(Collectors.toSet());

            //Set<String> replacedWords = new HashSet<>(lemmaHelper.getRussianWords(snippet));

            //Set<String> replacedWords = new HashSet<>(Arrays.asList(snippet.split("\\s+")));
//            replacedWords.removeAll(replacedWords.stream()
//                                                  .filter(s->!s.equalsIgnoreCase(rusWord))
//                                                  .collect(Collectors.toList()));

            // выделяем слова жирным
//            for (String word : replacedWords) {
//                snippet = snippet.replaceAll("(?<!\\\\S)" + word + "(?!\\\\S)", "<b>" + word + "</b>");
//            }

            snippet = snippet.replaceAll("(?<!\\\\S)" + rusWord + "(?!\\\\S)", "<b>" + rusWord + "</b>");
            //break;
        }

        int openTagPos = snippet.indexOf("<b><b>");

        while (openTagPos != -1) {
            int closeTagPos = snippet.indexOf("</b>", openTagPos);
            snippet = snippet.substring(0, closeTagPos) + snippet.substring(closeTagPos + 4);
            snippet = snippet.substring(0, openTagPos)  + snippet.substring(openTagPos  + 3);
            openTagPos = snippet.indexOf("<b><b>");
        }

        return snippet;
    }

}
