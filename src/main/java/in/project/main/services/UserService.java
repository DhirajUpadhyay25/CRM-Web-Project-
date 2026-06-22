package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.entities.User;
import in.project.main.repositories.UserRepository;

@Service
public class UserService
{
	@Autowired
	private UserRepository userRepository;
	
	public void registerUserService(User user)
	{
		userRepository.save(user);
	}
	
	public boolean loginUserService(String email, String password)
	{
		User user = userRepository.findByEmail(email);
		if(user != null)
		{
			return password.equals(user.getPassword());
		}
		return false;
	}
}
