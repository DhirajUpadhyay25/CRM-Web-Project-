package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Instructor;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {
}
