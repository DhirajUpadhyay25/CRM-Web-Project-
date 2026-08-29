package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.User;
import in.project.main.repositories.UserRepository;

@Service
public class UserService
{
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Transactional
	public void registerUserService(User user)
	{
		if (userRepository.findByEmail(user.getEmail()) != null) {
			throw new RuntimeException("An account with this email already exists.");
		}
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		userRepository.save(user);
	}
	
	public boolean loginUserService(String email, String password)
	{
		User user = userRepository.findByEmail(email);
		if(user != null)
		{
			return passwordEncoder.matches(password, user.getPassword());
		}
		return false;
	}
	
	@Transactional
	public void migratePlaintextPasswords() {
		Iterable<User> users = userRepository.findAll();
		for (User user : users) {
			if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
				user.setPassword(passwordEncoder.encode(user.getPassword()));
				userRepository.save(user);
			}
		}
	}
}
