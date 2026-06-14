package com.nutriconsultas.contact;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

	@Transactional
	public void markAsRead(final Long id) {
		final ContactInquiry inquiry = contactInquiryRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!inquiry.isReadByAdmin()) {
			inquiry.setReadByAdmin(true);
			contactInquiryRepository.save(inquiry);
			log.info("Contact inquiry marked read: {}", LogRedaction.redactContactInquiry(id));
		}
	}

	@Transactional
	public void deleteById(final Long id) {
		if (!contactInquiryRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		contactInquiryRepository.deleteById(id);
		log.info("Contact inquiry deleted: {}", LogRedaction.redactContactInquiry(id));
	}

}
