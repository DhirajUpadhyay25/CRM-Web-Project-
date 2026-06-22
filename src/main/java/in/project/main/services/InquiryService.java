package in.project.main.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.entities.Inquiry;
import in.project.main.repositories.InquiryRepository;

@Service
public class InquiryService
{
	@Autowired
	private InquiryRepository inquiryRepository;
	
	public void addNewInquiry(Inquiry inquiry)
	{
		inquiryRepository.save(inquiry);
	}
	
	public List<Inquiry> findInquiriesUsingPhno(String phoneno)
	{
		return inquiryRepository.findByPhoneno(phoneno);
	}
}
