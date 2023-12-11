package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;

import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer>{

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index`" +
                   " WHERE page_id IN (SELECT id FROM page WHERE site_id = :p_site_id)",
           nativeQuery = true
    )
    void deleteBySite(@Param("p_site_id") Integer siteId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index` WHERE page_id = :p_page_id", nativeQuery = true)
    void deleteByPage(@Param("p_page_id") Integer pageId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO `index` (page_id, lemma_id, `rank`) " +
                   "VALUES (:p_page_id, :p_lemma_id, :p_rank)",
           nativeQuery = true
    )
    void saveIndex(
            @Param("p_page_id")  Integer pageId,
            @Param("p_lemma_id") Integer lemmaId,
            @Param("p_rank")     Integer rank
    );

    @Query(value = "SELECT * FROM `index`" +
                   " WHERE lemma_id IN (SELECT id FROM lemma" +
                   "                     WHERE site_id = IFNULL(:p_site_id, site_id)" +
                   "                       AND lemma   = :p_lemma)",
           nativeQuery = true
    )
    Set<IndexEntity> getIndexListByLemma(
            @Param("p_site_id")  Integer siteId,
            @Param("p_lemma")    String  lemma
    );
}
