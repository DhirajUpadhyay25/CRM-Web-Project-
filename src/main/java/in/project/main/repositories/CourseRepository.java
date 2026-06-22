package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>
{
	Course findByName(String courseName);
}
