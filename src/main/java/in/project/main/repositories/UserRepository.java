package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
	User findByEmail(String email);
}