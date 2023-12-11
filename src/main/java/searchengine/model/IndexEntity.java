package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "`Index`")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "page_id", nullable = false)
    private Integer pageId;
    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;
    @Column(name = "`rank`", nullable = false)
    private float rank;
}
