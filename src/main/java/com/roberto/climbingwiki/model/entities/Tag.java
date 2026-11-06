package com.roberto.climbingwiki.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
