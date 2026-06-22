package com.nutriconsultas.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
		form.setPlanRoleSlug("director-consultorio");

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
		assertThat(captor.getValue().getPlanRoleSlug()).isEqualTo("director-consultorio");
		assertThat(captor.getValue().isReadByAdmin()).isFalse();
	}

	@Test
	void saveFromForm_ignoresInvalidPlanSlug() {
		final ContactForm form = new ContactForm();
		form.setName("Ana López");
		form.setEmail("ana@example.com");
		form.setSubject("Acceso");
		form.setMessage("Quiero solicitar acceso");
		form.setPlanRoleSlug("invalid-plan");

		when(contactInquiryRepository.save(any(ContactInquiry.class))).thenAnswer(invocation -> {
			final ContactInquiry inquiry = invocation.getArgument(0);
			inquiry.setId(11L);
			return inquiry;
		});

		contactInquiryService.saveFromForm(form);

		final ArgumentCaptor<ContactInquiry> captor = ArgumentCaptor.forClass(ContactInquiry.class);
		verify(contactInquiryRepository).save(captor.capture());
		assertThat(captor.getValue().getPlanRoleSlug()).isNull();
	}

	@Test
	void markAsRead_setsReadByAdminTrue() {
		final ContactInquiry inquiry = new ContactInquiry();
		inquiry.setId(5L);
		inquiry.setReadByAdmin(false);
		when(contactInquiryRepository.findById(5L)).thenReturn(Optional.of(inquiry));
		when(contactInquiryRepository.save(any(ContactInquiry.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		contactInquiryService.markAsRead(5L);

		assertThat(inquiry.isReadByAdmin()).isTrue();
		verify(contactInquiryRepository).save(inquiry);
	}

	@Test
	void markAsRead_whenAlreadyRead_doesNotSave() {
		final ContactInquiry inquiry = new ContactInquiry();
		inquiry.setId(5L);
		inquiry.setReadByAdmin(true);
		when(contactInquiryRepository.findById(5L)).thenReturn(Optional.of(inquiry));

		contactInquiryService.markAsRead(5L);

		verify(contactInquiryRepository, never()).save(any(ContactInquiry.class));
	}

	@Test
	void markAsRead_whenNotFound_throwsNotFound() {
		when(contactInquiryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> contactInquiryService.markAsRead(99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteById_removesInquiry() {
		when(contactInquiryRepository.existsById(7L)).thenReturn(true);

		contactInquiryService.deleteById(7L);

		verify(contactInquiryRepository).deleteById(7L);
	}

	@Test
	void deleteById_whenNotFound_throwsNotFound() {
		when(contactInquiryRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> contactInquiryService.deleteById(99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

}
