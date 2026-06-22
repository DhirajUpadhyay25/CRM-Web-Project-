package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.FollowUps;

import java.util.Optional;
import java.util.List;



@Repository
public interface FollowUpsRepository extends JpaRepository<FollowUps, Long>
{
	Optional<FollowUps> findByPhoneno(String phoneno);
	
	List<FollowUps> findByEmpEmailAndFollowUpDate(String empEmail, String followUpDate);
}