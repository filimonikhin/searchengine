package searchengine.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.time.LocalDateTime;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    SiteEntity findByUrl(String url);

    SiteEntity findSiteById(Integer id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site SET Status_Time = :p_status_time WHERE id = :p_site_id", nativeQuery = true)
    void updateStatusTimeById(
            @Param("p_site_id")     Integer siteId,
            @Param("p_status_time") LocalDateTime statusTime
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site" +
                   "   SET Status = 'INDEXED', Status_Time = :p_status_time" +
                   " WHERE id = :p_site_id",
           nativeQuery = true
    )
    void setStatusToIndexed(
            @Param("p_site_id")     Integer siteId,
            @Param("p_status_time") LocalDateTime statusTime
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site" +
                   "   SET last_error = :p_last_error, Status_Time = :p_status_time" +
                   " WHERE id = :p_site_id",
           nativeQuery = true
    )
    void setLastError(
            @Param("p_site_id")     Integer siteId,
            @Param("p_last_error")  String lastError,
            @Param("p_status_time") LocalDateTime statusTime
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site" +
                   "   SET Status = 'FAILED', last_error = :p_last_error, Status_Time = :p_status_time" +
                   " WHERE id = :p_site_id",
           nativeQuery = true
    )
    void setStatusToIndexFailed(
            @Param("p_site_id")     Integer siteId,
            @Param("p_last_error")  String lastError,
            @Param("p_status_time") LocalDateTime statusTime
    );
}
