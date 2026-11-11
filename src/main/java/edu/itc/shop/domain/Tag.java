package edu.itc.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity 
@Table (name = "tag") 
public class Tag {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "tag_seq_gen"
    )

    @SequenceGenerator (
        name = "tag_seq_gen",
        sequenceName = "tag_seq",
        allocationSize = 50
    )

    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    protected Tag() {

    }
}
