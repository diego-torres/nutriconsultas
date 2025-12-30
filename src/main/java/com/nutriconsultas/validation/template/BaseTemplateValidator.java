package com.nutriconsultas.validation.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation of TemplateValidator that provides common mock variables used
 * across many templates. Individual validators can extend this and add template-specific
 * mocks.
 */
public abstract class BaseTemplateValidator implements TemplateValidator {

	/**
	 * Creates base mock model variables that are common across templates. Subclasses can
	 * override this and call super to add template-specific variables.
	 * @return a map of common mock variables
	 */
	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = new HashMap<>();

		// Common attributes from AbstractAuthorizedController
		variables.put("username", "");
		variables.put("user_picture", "");

		// Common menu attribute
		variables.put("activeMenu", "");

		// Common list attributes
		variables.put("ingestas", new ArrayList<>());
		variables.put("platillos", new ArrayList<>());
		variables.put("alimentosList", new ArrayList<>());
		variables.put("alimentos", new ArrayList<>());

		// Common numeric attributes
		variables.put("minId", 0L);

		// Mock #fields object for form validation (used in templates with
		// th:if="${#fields.hasErrors(...)}")
		variables.put("fields", new MockFields());

		return variables;
	}

	/**
	 * Creates a MockBean instance with the given properties. This is a helper method for
	 * creating mock objects that Thymeleaf can use for field binding.
	 * @param properties key-value pairs of properties (e.g., "id", 1L, "name", "Test")
	 * @return a MockBean instance
	 */
	protected MockBean createMockBean(final Object... properties) {
		return new MockBean(properties);
	}

	/**
	 * Simple mock bean class that allows property access via getters. This is used to
	 * provide mock objects for Thymeleaf field binding expressions like
	 * `th:field="*{id}"`.
	 */
	public static class MockBean {

		private final Map<String, Object> properties = new HashMap<>();

		public MockBean(final Object... keyValuePairs) {
			for (int i = 0; i < keyValuePairs.length; i += 2) {
				if (i + 1 < keyValuePairs.length) {
					properties.put(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
				}
			}
		}

		// Generic getter that OGNL can use to access properties
		public Object get(final String propertyName) {
			return properties.get(propertyName);
		}

		// Specific getters for common properties that Thymeleaf field binding expects
		public Long getId() {
			final Object id = properties.get("id");
			return id instanceof Long ? (Long) id : id != null ? Long.valueOf(id.toString()) : 0L;
		}

		public String getNombre() {
			final Object nombre = properties.get("nombre");
			return nombre != null ? nombre.toString() : "";
		}

		public String getName() {
			final Object name = properties.get("name");
			return name != null ? name.toString() : "";
		}

		public String getEmail() {
			final Object email = properties.get("email");
			return email != null ? email.toString() : "";
		}

		public String getPhone() {
			final Object phone = properties.get("phone");
			return phone != null ? phone.toString() : "";
		}

		public String getGender() {
			final Object gender = properties.get("gender");
			return gender != null ? gender.toString() : "";
		}

		public Double getEnergia() {
			final Object energia = properties.get("energia");
			return energia instanceof Double ? (Double) energia
					: energia != null ? Double.valueOf(energia.toString()) : 0.0;
		}

		public Double getProteina() {
			final Object proteina = properties.get("proteina");
			return proteina instanceof Double ? (Double) proteina
					: proteina != null ? Double.valueOf(proteina.toString()) : 0.0;
		}

		public Double getLipidos() {
			final Object lipidos = properties.get("lipidos");
			return lipidos instanceof Double ? (Double) lipidos
					: lipidos != null ? Double.valueOf(lipidos.toString()) : 0.0;
		}

		public Double getHidratosDeCarbono() {
			final Object hidratosDeCarbono = properties.get("hidratosDeCarbono");
			return hidratosDeCarbono instanceof Double ? (Double) hidratosDeCarbono
					: hidratosDeCarbono != null ? Double.valueOf(hidratosDeCarbono.toString()) : 0.0;
		}

		public Double getAzucarPorEquivalente() {
			final Object azucarPorEquivalente = properties.get("azucarPorEquivalente");
			return azucarPorEquivalente instanceof Double ? (Double) azucarPorEquivalente
					: azucarPorEquivalente != null ? Double.valueOf(azucarPorEquivalente.toString()) : 0.0;
		}

		public String getDescription() {
			final Object description = properties.get("description");
			return description != null ? description.toString() : "";
		}

		public String getImageUrl() {
			final Object imageUrl = properties.get("imageUrl");
			return imageUrl != null ? imageUrl.toString() : "";
		}

		public String getPdfUrl() {
			final Object pdfUrl = properties.get("pdfUrl");
			return pdfUrl != null ? pdfUrl.toString() : "";
		}

		public String getVideoUrl() {
			final Object videoUrl = properties.get("videoUrl");
			return videoUrl != null ? videoUrl.toString() : "";
		}

		// Collection getters
		public java.util.List<?> getIngestas() {
			final Object ingestas = properties.get("ingestas");
			java.util.List<?> result;
			if (ingestas instanceof java.util.List) {
				result = (java.util.List<?>) ingestas;
			} else {
				result = new ArrayList<>();
			}
			return result;
		}

		public java.util.List<?> getPlatillos() {
			final Object platillos = properties.get("platillos");
			java.util.List<?> result;
			if (platillos instanceof java.util.List) {
				result = (java.util.List<?>) platillos;
			} else {
				result = new ArrayList<>();
			}
			return result;
		}

		public java.util.List<?> getAlimentos() {
			final Object alimentos = properties.get("alimentos");
			java.util.List<?> result;
			if (alimentos instanceof java.util.List) {
				result = (java.util.List<?>) alimentos;
			} else {
				result = new ArrayList<>();
			}
			return result;
		}

		// Additional getters for Paciente properties
		public Double getPeso() {
			final Object peso = properties.get("peso");
			return peso instanceof Double ? (Double) peso : peso != null ? Double.valueOf(peso.toString()) : null;
		}

		public Double getEstatura() {
			final Object estatura = properties.get("estatura");
			return estatura instanceof Double ? (Double) estatura
					: estatura != null ? Double.valueOf(estatura.toString()) : null;
		}

		public Double getImc() {
			final Object imc = properties.get("imc");
			return imc instanceof Double ? (Double) imc : imc != null ? Double.valueOf(imc.toString()) : null;
		}

		public String getResponsibleName() {
			final Object responsibleName = properties.get("responsibleName");
			return responsibleName != null ? responsibleName.toString() : "";
		}

		public String getParentesco() {
			final Object parentesco = properties.get("parentesco");
			return parentesco != null ? parentesco.toString() : "";
		}

		// Getters for Paciente antecedentes fields
		public String getTipoSanguineo() {
			final Object tipoSanguineo = properties.get("tipoSanguineo");
			return tipoSanguineo != null ? tipoSanguineo.toString() : "";
		}

		public String getAntecedentesPrenatales() {
			final Object antecedentesPrenatales = properties.get("antecedentesPrenatales");
			return antecedentesPrenatales != null ? antecedentesPrenatales.toString() : "";
		}

		public String getAntecedentesNatales() {
			final Object antecedentesNatales = properties.get("antecedentesNatales");
			return antecedentesNatales != null ? antecedentesNatales.toString() : "";
		}

		public String getAntecedentesPatologicosPersonales() {
			final Object antecedentesPatologicosPersonales = properties.get("antecedentesPatologicosPersonales");
			return antecedentesPatologicosPersonales != null ? antecedentesPatologicosPersonales.toString() : "";
		}

		public String getAntecedentesPatologicosFamiliares() {
			final Object antecedentesPatologicosFamiliares = properties.get("antecedentesPatologicosFamiliares");
			return antecedentesPatologicosFamiliares != null ? antecedentesPatologicosFamiliares.toString() : "";
		}

		public String getComplicaciones() {
			final Object complicaciones = properties.get("complicaciones");
			return complicaciones != null ? complicaciones.toString() : "";
		}

		// Getters for Paciente desarrollo e inmunización fields
		public String getHistorialAlimenticio() {
			final Object historialAlimenticio = properties.get("historialAlimenticio");
			return historialAlimenticio != null ? historialAlimenticio.toString() : "";
		}

		public String getDesarrolloPsicomotor() {
			final Object desarrolloPsicomotor = properties.get("desarrolloPsicomotor");
			return desarrolloPsicomotor != null ? desarrolloPsicomotor.toString() : "";
		}

		public String getAlergias() {
			final Object alergias = properties.get("alergias");
			return alergias != null ? alergias.toString() : "";
		}

		// Getters for Paciente patologías flags
		public Boolean getEnfermedadesHepaticas() {
			final Object enfermedadesHepaticas = properties.get("enfermedadesHepaticas");
			return enfermedadesHepaticas instanceof Boolean ? (Boolean) enfermedadesHepaticas
					: enfermedadesHepaticas != null ? Boolean.valueOf(enfermedadesHepaticas.toString()) : false;
		}

		public Boolean getHipertension() {
			final Object hipertension = properties.get("hipertension");
			return hipertension instanceof Boolean ? (Boolean) hipertension
					: hipertension != null ? Boolean.valueOf(hipertension.toString()) : false;
		}

		public Boolean getDiabetes() {
			final Object diabetes = properties.get("diabetes");
			return diabetes instanceof Boolean ? (Boolean) diabetes
					: diabetes != null ? Boolean.valueOf(diabetes.toString()) : false;
		}

		public Boolean getHipotiroidismo() {
			final Object hipotiroidismo = properties.get("hipotiroidismo");
			return hipotiroidismo instanceof Boolean ? (Boolean) hipotiroidismo
					: hipotiroidismo != null ? Boolean.valueOf(hipotiroidismo.toString()) : false;
		}

		public Boolean getObesidad() {
			final Object obesidad = properties.get("obesidad");
			return obesidad instanceof Boolean ? (Boolean) obesidad
					: obesidad != null ? Boolean.valueOf(obesidad.toString()) : false;
		}

		public Boolean getAnemia() {
			final Object anemia = properties.get("anemia");
			return anemia instanceof Boolean ? (Boolean) anemia
					: anemia != null ? Boolean.valueOf(anemia.toString()) : false;
		}

		public Boolean getBulimia() {
			final Object bulimia = properties.get("bulimia");
			return bulimia instanceof Boolean ? (Boolean) bulimia
					: bulimia != null ? Boolean.valueOf(bulimia.toString()) : false;
		}

		public Boolean getAnorexia() {
			final Object anorexia = properties.get("anorexia");
			return anorexia instanceof Boolean ? (Boolean) anorexia
					: anorexia != null ? Boolean.valueOf(anorexia.toString()) : false;
		}

		// Additional getters for PlatilloIngesta properties (aliases for template
		// compatibility)
		public Integer getPorciones() {
			final Object porciones = properties.get("porciones");
			Integer result;
			if (porciones != null) {
				result = porciones instanceof Integer ? (Integer) porciones : Integer.valueOf(porciones.toString());
			} else {
				// Fallback to "portions" if "porciones" not found
				final Object portions = properties.get("portions");
				result = portions instanceof Integer ? (Integer) portions
						: portions != null ? Integer.valueOf(portions.toString()) : 1;
			}
			return result;
		}

		public Integer getCalorias() {
			final Object calorias = properties.get("calorias");
			Integer result;
			if (calorias != null) {
				result = calorias instanceof Integer ? (Integer) calorias : Integer.valueOf(calorias.toString());
			} else {
				// Fallback to "energia" if "calorias" not found
				final Object energia = properties.get("energia");
				result = energia instanceof Integer ? (Integer) energia
						: energia != null ? Integer.valueOf(energia.toString()) : 0;
			}
			return result;
		}

		public Double getProteinas() {
			final Object proteinas = properties.get("proteinas");
			Double result;
			if (proteinas != null) {
				result = proteinas instanceof Double ? (Double) proteinas : Double.valueOf(proteinas.toString());
			} else {
				// Fallback to "proteina" if "proteinas" not found
				final Object proteina = properties.get("proteina");
				result = proteina instanceof Double ? (Double) proteina
						: proteina != null ? Double.valueOf(proteina.toString()) : 0.0;
			}
			return result;
		}

		public Double getHidratos() {
			final Object hidratos = properties.get("hidratos");
			Double result;
			if (hidratos != null) {
				result = hidratos instanceof Double ? (Double) hidratos : Double.valueOf(hidratos.toString());
			} else {
				// Fallback to "hidratosDeCarbono" if "hidratos" not found
				final Object hidratosDeCarbono = properties.get("hidratosDeCarbono");
				result = hidratosDeCarbono instanceof Double ? (Double) hidratosDeCarbono
						: hidratosDeCarbono != null ? Double.valueOf(hidratosDeCarbono.toString()) : 0.0;
			}
			return result;
		}

		// Additional getters for Alimento properties
		public String getNombreAlimento() {
			final Object nombreAlimento = properties.get("nombreAlimento");
			return nombreAlimento != null ? nombreAlimento.toString() : "";
		}

		public String getClasificacion() {
			final Object clasificacion = properties.get("clasificacion");
			return clasificacion != null ? clasificacion.toString() : "";
		}

		// Additional getters for Alimento/AbstractNutrible properties
		public Integer getPesoBrutoRedondeado() {
			final Object pesoBrutoRedondeado = properties.get("pesoBrutoRedondeado");
			return pesoBrutoRedondeado instanceof Integer ? (Integer) pesoBrutoRedondeado
					: pesoBrutoRedondeado != null ? Integer.valueOf(pesoBrutoRedondeado.toString()) : 0;
		}

		public Integer getPesoNeto() {
			final Object pesoNeto = properties.get("pesoNeto");
			return pesoNeto instanceof Integer ? (Integer) pesoNeto
					: pesoNeto != null ? Integer.valueOf(pesoNeto.toString()) : 0;
		}

		public Double getCantSugerida() {
			final Object cantSugerida = properties.get("cantSugerida");
			return cantSugerida instanceof Double ? (Double) cantSugerida
					: cantSugerida != null ? Double.valueOf(cantSugerida.toString()) : 0.0;
		}

		public Double getFibra() {
			final Object fibra = properties.get("fibra");
			return fibra instanceof Double ? (Double) fibra : fibra != null ? Double.valueOf(fibra.toString()) : 0.0;
		}

		public Double getVitA() {
			final Object vitA = properties.get("vitA");
			return vitA instanceof Double ? (Double) vitA : vitA != null ? Double.valueOf(vitA.toString()) : 0.0;
		}

		public Double getAcidoAscorbico() {
			final Object acidoAscorbico = properties.get("acidoAscorbico");
			return acidoAscorbico instanceof Double ? (Double) acidoAscorbico
					: acidoAscorbico != null ? Double.valueOf(acidoAscorbico.toString()) : 0.0;
		}

		public Double getHierroNoHem() {
			final Object hierroNoHem = properties.get("hierroNoHem");
			return hierroNoHem instanceof Double ? (Double) hierroNoHem
					: hierroNoHem != null ? Double.valueOf(hierroNoHem.toString()) : 0.0;
		}

		public Double getPotasio() {
			final Object potasio = properties.get("potasio");
			return potasio instanceof Double ? (Double) potasio
					: potasio != null ? Double.valueOf(potasio.toString()) : 0.0;
		}

		public Double getIndiceGlicemico() {
			final Object indiceGlicemico = properties.get("indiceGlicemico");
			return indiceGlicemico instanceof Double ? (Double) indiceGlicemico
					: indiceGlicemico != null ? Double.valueOf(indiceGlicemico.toString()) : 0.0;
		}

		public Double getCargaGlicemica() {
			final Object cargaGlicemica = properties.get("cargaGlicemica");
			return cargaGlicemica instanceof Double ? (Double) cargaGlicemica
					: cargaGlicemica != null ? Double.valueOf(cargaGlicemica.toString()) : 0.0;
		}

		public Double getAcidoFolico() {
			final Object acidoFolico = properties.get("acidoFolico");
			return acidoFolico instanceof Double ? (Double) acidoFolico
					: acidoFolico != null ? Double.valueOf(acidoFolico.toString()) : 0.0;
		}

		public Double getCalcio() {
			final Object calcio = properties.get("calcio");
			return calcio instanceof Double ? (Double) calcio
					: calcio != null ? Double.valueOf(calcio.toString()) : 0.0;
		}

		public Double getHierro() {
			final Object hierro = properties.get("hierro");
			return hierro instanceof Double ? (Double) hierro
					: hierro != null ? Double.valueOf(hierro.toString()) : 0.0;
		}

		public Double getSodio() {
			final Object sodio = properties.get("sodio");
			return sodio instanceof Double ? (Double) sodio : sodio != null ? Double.valueOf(sodio.toString()) : 0.0;
		}

		public Double getSelenio() {
			final Object selenio = properties.get("selenio");
			return selenio instanceof Double ? (Double) selenio
					: selenio != null ? Double.valueOf(selenio.toString()) : 0.0;
		}

		public Double getFosforo() {
			final Object fosforo = properties.get("fosforo");
			return fosforo instanceof Double ? (Double) fosforo
					: fosforo != null ? Double.valueOf(fosforo.toString()) : 0.0;
		}

		public Double getColesterol() {
			final Object colesterol = properties.get("colesterol");
			return colesterol instanceof Double ? (Double) colesterol
					: colesterol != null ? Double.valueOf(colesterol.toString()) : 0.0;
		}

		public Double getAgSaturados() {
			final Object agSaturados = properties.get("agSaturados");
			return agSaturados instanceof Double ? (Double) agSaturados
					: agSaturados != null ? Double.valueOf(agSaturados.toString()) : 0.0;
		}

		public Double getAgMonoinsaturados() {
			final Object agMonoinsaturados = properties.get("agMonoinsaturados");
			return agMonoinsaturados instanceof Double ? (Double) agMonoinsaturados
					: agMonoinsaturados != null ? Double.valueOf(agMonoinsaturados.toString()) : 0.0;
		}

		public Double getAgPoliinsaturados() {
			final Object agPoliinsaturados = properties.get("agPoliinsaturados");
			return agPoliinsaturados instanceof Double ? (Double) agPoliinsaturados
					: agPoliinsaturados != null ? Double.valueOf(agPoliinsaturados.toString()) : 0.0;
		}

		public Double getEtanol() {
			final Object etanol = properties.get("etanol");
			return etanol instanceof Double ? (Double) etanol
					: etanol != null ? Double.valueOf(etanol.toString()) : 0.0;
		}

		// Setters for field binding
		public void setId(final Long id) {
			properties.put("id", id);
		}

		public void setNombre(final String nombre) {
			properties.put("nombre", nombre);
		}

		public void setName(final String name) {
			properties.put("name", name);
		}

		public void setEmail(final String email) {
			properties.put("email", email);
		}

		public void setPhone(final String phone) {
			properties.put("phone", phone);
		}

		public void setGender(final String gender) {
			properties.put("gender", gender);
		}

		public void setEnergia(final Double energia) {
			properties.put("energia", energia);
		}

		public void setProteina(final Double proteina) {
			properties.put("proteina", proteina);
		}

		public void setLipidos(final Double lipidos) {
			properties.put("lipidos", lipidos);
		}

		public void setHidratosDeCarbono(final Double hidratosDeCarbono) {
			properties.put("hidratosDeCarbono", hidratosDeCarbono);
		}

		public void setAzucarPorEquivalente(final Double azucarPorEquivalente) {
			properties.put("azucarPorEquivalente", azucarPorEquivalente);
		}

		public void setDescription(final String description) {
			properties.put("description", description);
		}

		public void setImageUrl(final String imageUrl) {
			properties.put("imageUrl", imageUrl);
		}

		public void setPdfUrl(final String pdfUrl) {
			properties.put("pdfUrl", pdfUrl);
		}

		public void setVideoUrl(final String videoUrl) {
			properties.put("videoUrl", videoUrl);
		}

		public void setNombreAlimento(final String nombreAlimento) {
			properties.put("nombreAlimento", nombreAlimento);
		}

		public void setClasificacion(final String clasificacion) {
			properties.put("clasificacion", clasificacion);
		}

		public void setPesoBrutoRedondeado(final Integer pesoBrutoRedondeado) {
			properties.put("pesoBrutoRedondeado", pesoBrutoRedondeado);
		}

		public void setPesoNeto(final Integer pesoNeto) {
			properties.put("pesoNeto", pesoNeto);
		}

		public void setCantSugerida(final Double cantSugerida) {
			properties.put("cantSugerida", cantSugerida);
		}

		public void setFibra(final Double fibra) {
			properties.put("fibra", fibra);
		}

		public void setVitA(final Double vitA) {
			properties.put("vitA", vitA);
		}

		public void setAcidoAscorbico(final Double acidoAscorbico) {
			properties.put("acidoAscorbico", acidoAscorbico);
		}

		public void setHierroNoHem(final Double hierroNoHem) {
			properties.put("hierroNoHem", hierroNoHem);
		}

		public void setPotasio(final Double potasio) {
			properties.put("potasio", potasio);
		}

		public void setIndiceGlicemico(final Double indiceGlicemico) {
			properties.put("indiceGlicemico", indiceGlicemico);
		}

		public void setCargaGlicemica(final Double cargaGlicemica) {
			properties.put("cargaGlicemica", cargaGlicemica);
		}

		public void setAcidoFolico(final Double acidoFolico) {
			properties.put("acidoFolico", acidoFolico);
		}

		public void setCalcio(final Double calcio) {
			properties.put("calcio", calcio);
		}

		public void setHierro(final Double hierro) {
			properties.put("hierro", hierro);
		}

		public void setSodio(final Double sodio) {
			properties.put("sodio", sodio);
		}

		public void setSelenio(final Double selenio) {
			properties.put("selenio", selenio);
		}

		public void setFosforo(final Double fosforo) {
			properties.put("fosforo", fosforo);
		}

		public void setColesterol(final Double colesterol) {
			properties.put("colesterol", colesterol);
		}

		public void setAgSaturados(final Double agSaturados) {
			properties.put("agSaturados", agSaturados);
		}

		public void setAgMonoinsaturados(final Double agMonoinsaturados) {
			properties.put("agMonoinsaturados", agMonoinsaturados);
		}

		public void setAgPoliinsaturados(final Double agPoliinsaturados) {
			properties.put("agPoliinsaturados", agPoliinsaturados);
		}

		public void setEtanol(final Double etanol) {
			properties.put("etanol", etanol);
		}

		public void setPeso(final Double peso) {
			properties.put("peso", peso);
		}

		public void setEstatura(final Double estatura) {
			properties.put("estatura", estatura);
		}

		public void setImc(final Double imc) {
			properties.put("imc", imc);
		}

		public void setResponsibleName(final String responsibleName) {
			properties.put("responsibleName", responsibleName);
		}

		public void setParentesco(final String parentesco) {
			properties.put("parentesco", parentesco);
		}

		// Setters for Paciente antecedentes fields
		public void setTipoSanguineo(final String tipoSanguineo) {
			properties.put("tipoSanguineo", tipoSanguineo);
		}

		public void setAntecedentesPrenatales(final String antecedentesPrenatales) {
			properties.put("antecedentesPrenatales", antecedentesPrenatales);
		}

		public void setAntecedentesNatales(final String antecedentesNatales) {
			properties.put("antecedentesNatales", antecedentesNatales);
		}

		public void setAntecedentesPatologicosPersonales(final String antecedentesPatologicosPersonales) {
			properties.put("antecedentesPatologicosPersonales", antecedentesPatologicosPersonales);
		}

		public void setAntecedentesPatologicosFamiliares(final String antecedentesPatologicosFamiliares) {
			properties.put("antecedentesPatologicosFamiliares", antecedentesPatologicosFamiliares);
		}

		public void setComplicaciones(final String complicaciones) {
			properties.put("complicaciones", complicaciones);
		}

		// Setters for Paciente desarrollo e inmunización fields
		public void setHistorialAlimenticio(final String historialAlimenticio) {
			properties.put("historialAlimenticio", historialAlimenticio);
		}

		public void setDesarrolloPsicomotor(final String desarrolloPsicomotor) {
			properties.put("desarrolloPsicomotor", desarrolloPsicomotor);
		}

		public void setAlergias(final String alergias) {
			properties.put("alergias", alergias);
		}

		// Setters for Paciente patologías flags
		public void setEnfermedadesHepaticas(final Boolean enfermedadesHepaticas) {
			properties.put("enfermedadesHepaticas", enfermedadesHepaticas);
		}

		public void setHipertension(final Boolean hipertension) {
			properties.put("hipertension", hipertension);
		}

		public void setDiabetes(final Boolean diabetes) {
			properties.put("diabetes", diabetes);
		}

		public void setHipotiroidismo(final Boolean hipotiroidismo) {
			properties.put("hipotiroidismo", hipotiroidismo);
		}

		public void setObesidad(final Boolean obesidad) {
			properties.put("obesidad", obesidad);
		}

		public void setAnemia(final Boolean anemia) {
			properties.put("anemia", anemia);
		}

		public void setBulimia(final Boolean bulimia) {
			properties.put("bulimia", bulimia);
		}

		public void setAnorexia(final Boolean anorexia) {
			properties.put("anorexia", anorexia);
		}

		// Method to set collection properties
		public void setCollectionProperty(final String propertyName, final java.util.List<?> collection) {
			properties.put(propertyName, collection);
		}

		// Method to get properties map (for validators that need to set nested
		// properties)
		protected Map<String, Object> getProperties() {
			return properties;
		}

	}

	/**
	 * Mock implementation of Thymeleaf's #fields object used for form validation. This
	 * provides methods like hasErrors() and errors() that templates use for displaying
	 * validation errors.
	 */
	public static class MockFields {

		/**
		 * Checks if there are validation errors for the given field. In validation
		 * context, this always returns false since we're not actually validating forms.
		 * @param fieldName the name of the field to check
		 * @return always false in validation context
		 */
		public boolean hasErrors(final String fieldName) {
			return false;
		}

		/**
		 * Gets validation errors for the given field. In validation context, this always
		 * returns an empty list since we're not actually validating forms.
		 * @param fieldName the name of the field to get errors for
		 * @return always an empty list in validation context
		 */
		public java.util.List<String> errors(final String fieldName) {
			return new ArrayList<>();
		}

	}

}
