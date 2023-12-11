package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "Site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum", nullable = false)
    private StatusType status;
    @Column(name="status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(name="last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(nullable = false)
    private String url;
    @Column(nullable = false)
    private String name;

    public SiteEntity(StatusType status, String url, String name) {
        this.status = status;
        this.url = url;
        this.name = name;
        this.statusTime = LocalDateTime.now();
    }
}
