package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Page", uniqueConstraints = {
        @UniqueConstraint(name = "site_path", columnNames = {"site_id", "path"})})
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "site_id", nullable = false)
    private Integer siteId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}
