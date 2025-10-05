package ai.vision.vishnu.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Environment extends Auditable<Long>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private boolean active;
}
