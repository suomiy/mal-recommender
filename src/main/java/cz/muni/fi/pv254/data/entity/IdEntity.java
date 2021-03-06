package cz.muni.fi.pv254.data.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Represents base for all reasonable entities with Long Id.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class IdEntity implements Serializable {
    @GenericGenerator(
            name = "seq",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                    @Parameter(name = "sequence_name", value = "hibernate_sequence"),
                    @Parameter(name = "initial_value", value = "1"),
                    @Parameter(name = "increment_size", value = "500"),
                    @Parameter(name = "optimizer", value = "hilo")
            })

    @GeneratedValue(generator = "seq", strategy=GenerationType.SEQUENCE) // @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Id
    private Long id;
    private boolean deleted = false;

    protected IdEntity() {
    }

    protected IdEntity(IdEntity entity) {
        this.id = entity.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdEntity)) return false;

        IdEntity entity = (IdEntity) o;

        return id != null && entity.id != null && id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.intValue() : 0;
    }

    @Override
    public String toString() {
        return "IdEntity{id=" + id + '}';
    }
}
