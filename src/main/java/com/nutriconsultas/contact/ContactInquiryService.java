package com.nutriconsultas.contact;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.controller.ContactForm;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ContactInquiryService {

	private final ContactInquiryRepository contactInquiryRepository;

	public ContactInquiryService(final ContactInquiryRepository contactInquiryRepository) {
		this.contactInquiryRepository = contactInquiryRepository;
	}

	@Transactional
	public ContactInquiry saveFromForm(final ContactForm contactForm) {
		final ContactInquiry inquiry = new ContactInquiry();
		inquiry.setName(contactForm.getName());
		inquiry.setEmail(contactForm.getEmail());
		inquiry.setSubject(contactForm.getSubject());
		inquiry.setMessage(contactForm.getMessage());
		inquiry.setReadByAdmin(false);
		final ContactInquiry saved = contactInquiryRepository.save(inquiry);
		log.info("Contact inquiry saved: {}", LogRedaction.redactContactInquiry(saved.getId()));
		return saved;
	}

	@Transactional(readOnly = true)
	public java.util.List<ContactInquiry> findAllNewestFirst() {
		return contactInquiryRepository.findAllByOrderByCreatedAtDesc();
	}

}
