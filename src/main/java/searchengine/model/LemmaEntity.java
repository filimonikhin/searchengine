package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Lemma", uniqueConstraints = {
        @UniqueConstraint(name = "site_lemma", columnNames = {"site_id", "lemma"})})
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "site_id", nullable = false)
    private Integer siteId;
    @Column(nullable = false)
    private String lemma;
    @Column(nullable = false)
    private Integer frequency;
}
