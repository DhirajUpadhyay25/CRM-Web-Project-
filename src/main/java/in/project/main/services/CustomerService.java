package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import in.project.main.entities.User;
import in.project.main.repositories.UserRepository;

@Service
public class CustomerService
{
	@Autowired
	private UserRepository userRepository;
	
	public Page<User> getAllUserDetailsByPagination(Pageable pageable)
	{
		return userRepository.findAll(pageable);
	}
	
	public User getCustomerDetails(String userEmail)
	{
		return userRepository.findByEmail(userEmail);
	}
	
	public void updateUserBanStatus(User user)
	{
		userRepository.save(user);
	}
}
