package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AiAssistantWidgetAdviceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private DietaService dietaService;

	@Mock
	private PlatilloService platilloService;

	private AiAssistantWidgetAdvice advice;

	@BeforeEach
	void setUp() {
		final AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		advice = new AiAssistantWidgetAdvice(properties, pacienteRepository, dietaService, platilloService);
	}

	@Test
	void patientDetailPageEnablesWidget() {
		final Paciente paciente = new Paciente();
		paciente.setId(12L);
		when(pacienteRepository.findByIdAndUserId(12L, NUTRITIONIST_ID)).thenReturn(Optional.of(paciente));

		final AiAssistantWidgetContext context = advice.aiAssistantWidgetContext(request("/admin/pacientes/12/perfil"),
				principal());

		assertThat(context).isNotNull();
		assertThat(context.patientId()).isEqualTo(12L);
		assertThat(context.storageScopeKey()).isEqualTo("patient-12");
	}

	@Test
	void dietaDetailPageIncludesDietaContext() {
		final Dieta dieta = new Dieta();
		dieta.setId(4L);
		dieta.setNombre("Plan semanal");
		dieta.setUserId(NUTRITIONIST_ID);
		when(dietaService.getDieta(4L)).thenReturn(dieta);

		final AiAssistantWidgetContext context = advice.aiAssistantWidgetContext(request("/admin/dietas/4"),
				principal());

		assertThat(context).isNotNull();
		assertThat(context.dietaId()).isEqualTo(4L);
		assertThat(context.scopeLabel()).contains("Plan semanal");
	}

	@Test
	void platilloDetailPageIncludesPlatilloContext() {
		final Platillo platillo = new Platillo();
		platillo.setId(6L);
		platillo.setName("Sopa");
		platillo.setUserId(NUTRITIONIST_ID);
		when(platilloService.findByIdAndUserId(6L, NUTRITIONIST_ID)).thenReturn(platillo);

		final AiAssistantWidgetContext context = advice.aiAssistantWidgetContext(request("/admin/platillos/6"),
				principal());

		assertThat(context).isNotNull();
		assertThat(context.platilloId()).isEqualTo(6L);
	}

	@Test
	void systemPlatilloIsAccessibleToNutritionist() {
		final Platillo platillo = new Platillo();
		platillo.setId(9L);
		platillo.setName("Catalogo");
		platillo.setUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		when(platilloService.findByIdAndUserId(9L, NUTRITIONIST_ID)).thenReturn(null);
		when(platilloService.findById(9L)).thenReturn(platillo);

		final AiAssistantWidgetContext context = advice.aiAssistantWidgetContext(request("/admin/platillos/9"),
				principal());

		assertThat(context).isNotNull();
		assertThat(context.platilloId()).isEqualTo(9L);
	}

	@Test
	void unrelatedAdminPageReturnsNull() {
		assertThat(advice.aiAssistantWidgetContext(request("/admin"), principal())).isNull();
	}

	private static MockHttpServletRequest request(final String path) {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(path);
		return request;
	}

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
