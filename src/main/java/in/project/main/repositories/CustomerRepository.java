package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.User;

public interface CustomerRepository extends JpaRepository<User, Long> 
{
	User findByEmail(String email);
}
