package ru.platonov.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platonov.user_service.model.Tutor;

import java.util.List;
import java.util.UUID;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, UUID> {

    @Query("SELECT t FROM Tutor t WHERE t.subject.id = :subjectId")
    List<Tutor> findTutorsBySubjectId(@Param("subjectId") UUID subjectId);
}
