package searchengine.parsers;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LemmaHelper {
    private LuceneMorphology luceneMorphology;

    // прячем пустой конструкор
    private LemmaHelper() {
        // ошибка, если вызвали внутри класса
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    private LemmaHelper(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    // фабричный метод
    public static LemmaHelper newInstance() throws IOException {
        LuceneMorphology rusMorphology = new RussianLuceneMorphology();
        return new LemmaHelper(rusMorphology);
    }

    public LuceneMorphology getLuceneMorphology() {
        return luceneMorphology;
    }

    // список русских лемм
    public Map<String, Integer> getRussianLemmas (String text) {

        List<String> rusWords = getRussianLowerCaseWords(text);
        Map<String, Integer> rusLemmas = new HashMap<>();

        for (String rusWord : rusWords) {
            String normalWord = getNormalWord(rusWord);

            if (normalWord == null) {
                continue;
            }

            // суммируем кол-во найденных лемм, если лемма есть в мапе, иначе инициализируем в 1
            rusLemmas.merge(normalWord, 1, Integer::sum); // можно Math::AddExact
        }

        return rusLemmas;
    }

    // разбиение текста на русские слова

    public List<String> getRussianLowerCaseWords(String text) {
        return getRussianWords(text, true);
    }

    public List<String> getRussianWords(String text) {
        return getRussianWords(text, false);
    }

    private List<String> getRussianWords(String text, boolean isLowerCase) {

        String s = text;
        String pattern;

        if (isLowerCase) {
            s = s.toLowerCase(Locale.ROOT);
            pattern = "([^а-я\\s])";
        } else {
            pattern = "([^а-яА-Я\\s])";
        }

        return Arrays.asList(s.replaceAll(pattern, " ").trim().split("\\s+"))
               .stream()
               .filter(word -> word.length() > 1) // оставляем слова длиной > 1
               .collect(Collectors.toList());
    }

    // нормальная форма слова
    public String getNormalWord(String word) {

        if (word == null || word.isBlank()) {
            return null;
        }

        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);

        if (anyWordBaseIsPartOfServicesWords(wordBaseForms)) {
            return null;
        }

        List<String> normalForms = luceneMorphology.getNormalForms(word);

        if (normalForms.isEmpty()) {
            return null;
        }

        return normalForms.get(0);
    }

    // проверка на служебные слова (после вызова luceneMorphology.getMorphInfo)
    private boolean anyWordBaseIsPartOfServicesWords(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::isPartOfServicesWords);
    }
    private boolean isPartOfServicesWords(String word) {
        List<String> words = List.of("МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ");
        return words.stream().anyMatch(s -> word.toUpperCase().contains(s));
    }

}
