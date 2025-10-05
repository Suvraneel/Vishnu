package ai.vision.vishnu.entity;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

public abstract class Auditable<T> {
    @CreatedDate
    protected Date createdAt;

    @CreatedBy
    protected T createdBy;

    @LastModifiedDate
    protected Date lastModifiedAt;

    @LastModifiedBy
    protected T lastModifiedBy;
}