package com.nutriconsultas.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.controller.ContactForm;

@ExtendWith(MockitoExtension.class)
class ContactInquiryServiceTest {

	@InjectMocks
	private ContactInquiryService contactInquiryService;

	@Mock
	private ContactInquiryRepository contactInquiryRepository;

	@Test
	void saveFromForm_persistsInquiry() {
		final ContactForm form = new ContactForm();
		form.setName("Ana López");
		form.setEmail("ana@example.com");
		form.setSubject("Acceso");
		form.setMessage("Quiero solicitar acceso");

		when(contactInquiryRepository.save(any(ContactInquiry.class))).thenAnswer(invocation -> {
			final ContactInquiry inquiry = invocation.getArgument(0);
			inquiry.setId(10L);
			return inquiry;
		});

		final ContactInquiry saved = contactInquiryService.saveFromForm(form);

		final ArgumentCaptor<ContactInquiry> captor = ArgumentCaptor.forClass(ContactInquiry.class);
		verify(contactInquiryRepository).save(captor.capture());
		assertThat(saved.getId()).isEqualTo(10L);
		assertThat(captor.getValue().getName()).isEqualTo("Ana López");
		assertThat(captor.getValue().getEmail()).isEqualTo("ana@example.com");
		assertThat(captor.getValue().getSubject()).isEqualTo("Acceso");
		assertThat(captor.getValue().getMessage()).isEqualTo("Quiero solicitar acceso");
		assertThat(captor.getValue().isReadByAdmin()).isFalse();
	}

}
