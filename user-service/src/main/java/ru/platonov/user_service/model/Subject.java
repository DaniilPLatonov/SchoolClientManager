package ru.platonov.user_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;


import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;


    @ManyToMany(mappedBy = "subjects")
    private List<User> users;

    @OneToMany(mappedBy = "subject")
    private List<Tutor> tutors;
}

