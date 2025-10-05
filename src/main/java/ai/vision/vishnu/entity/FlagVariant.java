package ai.vision.vishnu.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class FlagVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    private String variantKey;

    @Column
    private String variantValue;

    @ManyToOne
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    @ManyToOne
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column
    private float percentageRollout = 100f;

    @Column
    private boolean enabled;

    @Column
    private String description;
}
