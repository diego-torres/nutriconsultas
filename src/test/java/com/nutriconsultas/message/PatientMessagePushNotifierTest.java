package com.nutriconsultas.message;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.device.PatientPushSender;
import com.nutriconsultas.device.PushEvent;

@ExtendWith(MockitoExtension.class)
class PatientMessagePushNotifierTest {

	@InjectMocks
	private PatientMessagePushNotifier notifier;

	@Mock
	private PatientPushSender patientPushSender;

	@Test
	void notifyNewNutritionistMessage_delegatesToPushSender() {
		notifier.notifyNewNutritionistMessage(5L, 99L);

		verify(patientPushSender).send(5L, PushEvent.newMessage(99L));
	}

	@Test
	void notifyNewNutritionistMessage_whenIdsNull_isNoOp() {
		notifier.notifyNewNutritionistMessage(null, 99L);
		notifier.notifyNewNutritionistMessage(5L, null);

		verifyNoInteractions(patientPushSender);
	}

}
