package searchengine.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    PageEntity findPageById(Integer id);

    @Query(value = "SELECT * FROM Page WHERE site_id = :p_site_id AND path = :p_path", nativeQuery = true)
    PageEntity findBySiteIdAndPath(
            @Param("p_site_id") Integer siteId,
            @Param("p_path")    String path
    );

    // если siteId = NULL то ищем по всем сайтам
    @Query(value = "SELECT COUNT(*) FROM Page WHERE site_id = IFNULL(:p_site_id, site_id)", nativeQuery = true)
    int getPageCountBySiteId(
            @Param("p_site_id") Integer siteId
    );

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM Page WHERE site_id = :p_site_id", nativeQuery = true)
    void deleteBySite(
            @Param("p_site_id") Integer siteId
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO page (code, site_id, path, content) " +
                   "VALUES (:p_code, :p_site_id, :p_path, :p_content)",
           nativeQuery = true
    )
    void savePage(
            @Param("p_code")    Integer code,
            @Param("p_site_id") Integer siteId,
            @Param("p_path")    String path,
            @Param("p_content") String content
    );

}