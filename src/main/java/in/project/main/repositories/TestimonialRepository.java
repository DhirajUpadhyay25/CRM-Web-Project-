package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Testimonial;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
}
