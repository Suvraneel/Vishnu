package ai.vision.vishnu.entity;

import ai.vision.vishnu.enums.FlagType;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class FeatureFlag extends Auditable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String flagKey;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FlagType flagType = FlagType.BOOLEAN;

    @Column
    private String description;

    @Column
    private String defaultValue;

    @Column
    private boolean enabled;
}
