package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer>{

    @Query(value = "SELECT * FROM lemma WHERE site_id = :p_site_id", nativeQuery = true)
    LemmaEntity findBySiteId(@Param("p_site_id") Integer siteId);

    @Query(value = "SELECT COUNT(*) FROM lemma WHERE site_id = :p_site_id", nativeQuery = true)
    int getCountBySiteId(@Param("p_site_id") Integer siteId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) " +
                   "VALUES (:p_site_id, :p_lemma, 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1",
           nativeQuery = true
    )
    void saveLemma(
            @Param("p_site_id") Integer siteId,
            @Param("p_lemma")   String lemma
    );

    @Query(value = "SELECT * FROM lemma " +
                   " WHERE site_id = :p_site_id AND lemma = :p_lemma",
           nativeQuery = true
    )
    LemmaEntity getLemma(
            @Param("p_site_id") Integer siteId,
            @Param("p_lemma")   String lemma
    );

    @Query(value = "SELECT IFNULL(SUM(frequency),0) FROM lemma " +
                   " WHERE site_id = IFNULL(:p_site_id, site_id)" +
                   "   AND lemma   = :p_lemma",
           nativeQuery = true
    )
    int getFrequency(
            @Param("p_site_id") Integer siteId,
            @Param("p_lemma")   String lemma
    );

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma WHERE site_id = :p_site_id", nativeQuery = true)
    void deleteBySite(@Param("p_site_id") Integer siteId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE lemma" +
                   "   SET frequency = frequency - 1" +
                   " WHERE id IN (SELECT lemma_id FROM `index` WHERE page_id = :p_page_id)",
            nativeQuery = true
    )
    void updateByPageId(@Param("p_page_id") Integer pageId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma" +
                   " WHERE id IN (SELECT lemma_id FROM `index` WHERE page_id = :p_page_id) " +
                   "   AND frequency = 0",
            nativeQuery = true
    )
    void deleteByPageId(@Param("p_page_id") Integer pageId);

}