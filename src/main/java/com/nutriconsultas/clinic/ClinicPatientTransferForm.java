package com.nutriconsultas.clinic;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicPatientTransferForm {

	private Long sourceMemberId;

	private Long targetMemberId;

	private List<Long> patientIds = new ArrayList<>();

	private boolean transferAll;

}
