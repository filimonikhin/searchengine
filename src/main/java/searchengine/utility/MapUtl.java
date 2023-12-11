package searchengine.utility;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtl {
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueDesc(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<K, V>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueAsc(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<K, V>comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /* сортировка мапа (удалаяет элементы у которых value одинаковые ХЗ)
    public static <K,V extends Comparable<V>> Map<K,V> sortMapByValue(final Map<K,V> map, OrderType orderType)
    {
        Comparator<K> valueComparator = (k1, k2) -> {
            int comp = map.get(k1).compareTo(map.get(k2));
            return comp * (orderType == OrderType.ASC ? 1 : -1);
        };

        Map<K, V> sorted = new TreeMap<>(valueComparator);
        sorted.putAll(map);

        return sorted;
    }
    */

}
