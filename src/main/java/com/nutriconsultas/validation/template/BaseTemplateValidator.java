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
		Map<String, Object> variables = new HashMap<>();

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
	protected MockBean createMockBean(Object... properties) {
		return new MockBean(properties);
	}

	/**
	 * Simple mock bean class that allows property access via getters. This is used to
	 * provide mock objects for Thymeleaf field binding expressions like
	 * `th:field="*{id}"`.
	 */
	public static class MockBean {

		private final Map<String, Object> properties = new HashMap<>();

		public MockBean(Object... keyValuePairs) {
			for (int i = 0; i < keyValuePairs.length; i += 2) {
				if (i + 1 < keyValuePairs.length) {
					properties.put(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
				}
			}
		}

		// Generic getter that OGNL can use to access properties
		public Object get(String propertyName) {
			return properties.get(propertyName);
		}

		// Specific getters for common properties that Thymeleaf field binding expects
		public Long getId() {
			Object id = properties.get("id");
			return id instanceof Long ? (Long) id : id != null ? Long.valueOf(id.toString()) : 0L;
		}

		public String getNombre() {
			Object nombre = properties.get("nombre");
			return nombre != null ? nombre.toString() : "";
		}

		public String getName() {
			Object name = properties.get("name");
			return name != null ? name.toString() : "";
		}

		public String getEmail() {
			Object email = properties.get("email");
			return email != null ? email.toString() : "";
		}

		public String getPhone() {
			Object phone = properties.get("phone");
			return phone != null ? phone.toString() : "";
		}

		public String getGender() {
			Object gender = properties.get("gender");
			return gender != null ? gender.toString() : "";
		}

		public Double getEnergia() {
			Object energia = properties.get("energia");
			return energia instanceof Double ? (Double) energia
					: energia != null ? Double.valueOf(energia.toString()) : 0.0;
		}

		public Double getProteina() {
			Object proteina = properties.get("proteina");
			return proteina instanceof Double ? (Double) proteina
					: proteina != null ? Double.valueOf(proteina.toString()) : 0.0;
		}

		public Double getLipidos() {
			Object lipidos = properties.get("lipidos");
			return lipidos instanceof Double ? (Double) lipidos
					: lipidos != null ? Double.valueOf(lipidos.toString()) : 0.0;
		}

		public Double getHidratosDeCarbono() {
			Object hidratosDeCarbono = properties.get("hidratosDeCarbono");
			return hidratosDeCarbono instanceof Double ? (Double) hidratosDeCarbono
					: hidratosDeCarbono != null ? Double.valueOf(hidratosDeCarbono.toString()) : 0.0;
		}

		public Double getAzucarPorEquivalente() {
			Object azucarPorEquivalente = properties.get("azucarPorEquivalente");
			return azucarPorEquivalente instanceof Double ? (Double) azucarPorEquivalente
					: azucarPorEquivalente != null ? Double.valueOf(azucarPorEquivalente.toString()) : 0.0;
		}

		public String getDescription() {
			Object description = properties.get("description");
			return description != null ? description.toString() : "";
		}

		public String getImageUrl() {
			Object imageUrl = properties.get("imageUrl");
			return imageUrl != null ? imageUrl.toString() : "";
		}

		public String getPdfUrl() {
			Object pdfUrl = properties.get("pdfUrl");
			return pdfUrl != null ? pdfUrl.toString() : "";
		}

		public String getVideoUrl() {
			Object videoUrl = properties.get("videoUrl");
			return videoUrl != null ? videoUrl.toString() : "";
		}

		// Collection getters
		public java.util.List<?> getIngestas() {
			Object ingestas = properties.get("ingestas");
			if (ingestas instanceof java.util.List) {
				java.util.List<?> result = (java.util.List<?>) ingestas;
				return result;
			}
			return new ArrayList<>();
		}

		public java.util.List<?> getPlatillos() {
			Object platillos = properties.get("platillos");
			if (platillos instanceof java.util.List) {
				java.util.List<?> result = (java.util.List<?>) platillos;
				return result;
			}
			return new ArrayList<>();
		}

		public java.util.List<?> getAlimentos() {
			Object alimentos = properties.get("alimentos");
			if (alimentos instanceof java.util.List) {
				java.util.List<?> result = (java.util.List<?>) alimentos;
				return result;
			}
			return new ArrayList<>();
		}

		// Additional getters for Paciente properties
		public Double getPeso() {
			Object peso = properties.get("peso");
			return peso instanceof Double ? (Double) peso : peso != null ? Double.valueOf(peso.toString()) : null;
		}

		public Double getEstatura() {
			Object estatura = properties.get("estatura");
			return estatura instanceof Double ? (Double) estatura
					: estatura != null ? Double.valueOf(estatura.toString()) : null;
		}

		public Double getImc() {
			Object imc = properties.get("imc");
			return imc instanceof Double ? (Double) imc : imc != null ? Double.valueOf(imc.toString()) : null;
		}

		public String getResponsibleName() {
			Object responsibleName = properties.get("responsibleName");
			return responsibleName != null ? responsibleName.toString() : "";
		}

		public String getParentesco() {
			Object parentesco = properties.get("parentesco");
			return parentesco != null ? parentesco.toString() : "";
		}

		// Getters for Paciente antecedentes fields
		public String getTipoSanguineo() {
			Object tipoSanguineo = properties.get("tipoSanguineo");
			return tipoSanguineo != null ? tipoSanguineo.toString() : "";
		}

		public String getAntecedentesPrenatales() {
			Object antecedentesPrenatales = properties.get("antecedentesPrenatales");
			return antecedentesPrenatales != null ? antecedentesPrenatales.toString() : "";
		}

		public String getAntecedentesNatales() {
			Object antecedentesNatales = properties.get("antecedentesNatales");
			return antecedentesNatales != null ? antecedentesNatales.toString() : "";
		}

		public String getAntecedentesPatologicosPersonales() {
			Object antecedentesPatologicosPersonales = properties.get("antecedentesPatologicosPersonales");
			return antecedentesPatologicosPersonales != null ? antecedentesPatologicosPersonales.toString() : "";
		}

		public String getAntecedentesPatologicosFamiliares() {
			Object antecedentesPatologicosFamiliares = properties.get("antecedentesPatologicosFamiliares");
			return antecedentesPatologicosFamiliares != null ? antecedentesPatologicosFamiliares.toString() : "";
		}

		public String getComplicaciones() {
			Object complicaciones = properties.get("complicaciones");
			return complicaciones != null ? complicaciones.toString() : "";
		}

		// Getters for Paciente desarrollo e inmunización fields
		public String getHistorialAlimenticio() {
			Object historialAlimenticio = properties.get("historialAlimenticio");
			return historialAlimenticio != null ? historialAlimenticio.toString() : "";
		}

		public String getDesarrolloPsicomotor() {
			Object desarrolloPsicomotor = properties.get("desarrolloPsicomotor");
			return desarrolloPsicomotor != null ? desarrolloPsicomotor.toString() : "";
		}

		public String getAlergias() {
			Object alergias = properties.get("alergias");
			return alergias != null ? alergias.toString() : "";
		}

		// Getters for Paciente patologías flags
		public Boolean getEnfermedadesHepaticas() {
			Object enfermedadesHepaticas = properties.get("enfermedadesHepaticas");
			return enfermedadesHepaticas instanceof Boolean ? (Boolean) enfermedadesHepaticas
					: enfermedadesHepaticas != null ? Boolean.valueOf(enfermedadesHepaticas.toString()) : false;
		}

		public Boolean getHipertension() {
			Object hipertension = properties.get("hipertension");
			return hipertension instanceof Boolean ? (Boolean) hipertension
					: hipertension != null ? Boolean.valueOf(hipertension.toString()) : false;
		}

		public Boolean getDiabetes() {
			Object diabetes = properties.get("diabetes");
			return diabetes instanceof Boolean ? (Boolean) diabetes
					: diabetes != null ? Boolean.valueOf(diabetes.toString()) : false;
		}

		public Boolean getHipotiroidismo() {
			Object hipotiroidismo = properties.get("hipotiroidismo");
			return hipotiroidismo instanceof Boolean ? (Boolean) hipotiroidismo
					: hipotiroidismo != null ? Boolean.valueOf(hipotiroidismo.toString()) : false;
		}

		public Boolean getObesidad() {
			Object obesidad = properties.get("obesidad");
			return obesidad instanceof Boolean ? (Boolean) obesidad
					: obesidad != null ? Boolean.valueOf(obesidad.toString()) : false;
		}

		public Boolean getAnemia() {
			Object anemia = properties.get("anemia");
			return anemia instanceof Boolean ? (Boolean) anemia
					: anemia != null ? Boolean.valueOf(anemia.toString()) : false;
		}

		public Boolean getBulimia() {
			Object bulimia = properties.get("bulimia");
			return bulimia instanceof Boolean ? (Boolean) bulimia
					: bulimia != null ? Boolean.valueOf(bulimia.toString()) : false;
		}

		public Boolean getAnorexia() {
			Object anorexia = properties.get("anorexia");
			return anorexia instanceof Boolean ? (Boolean) anorexia
					: anorexia != null ? Boolean.valueOf(anorexia.toString()) : false;
		}

		// Additional getters for PlatilloIngesta properties (aliases for template
		// compatibility)
		public Integer getPorciones() {
			Object porciones = properties.get("porciones");
			if (porciones != null) {
				return porciones instanceof Integer ? (Integer) porciones : Integer.valueOf(porciones.toString());
			}
			// Fallback to "portions" if "porciones" not found
			Object portions = properties.get("portions");
			return portions instanceof Integer ? (Integer) portions
					: portions != null ? Integer.valueOf(portions.toString()) : 1;
		}

		public Integer getCalorias() {
			Object calorias = properties.get("calorias");
			if (calorias != null) {
				return calorias instanceof Integer ? (Integer) calorias : Integer.valueOf(calorias.toString());
			}
			// Fallback to "energia" if "calorias" not found
			Object energia = properties.get("energia");
			return energia instanceof Integer ? (Integer) energia
					: energia != null ? Integer.valueOf(energia.toString()) : 0;
		}

		public Double getProteinas() {
			Object proteinas = properties.get("proteinas");
			if (proteinas != null) {
				return proteinas instanceof Double ? (Double) proteinas : Double.valueOf(proteinas.toString());
			}
			// Fallback to "proteina" if "proteinas" not found
			Object proteina = properties.get("proteina");
			return proteina instanceof Double ? (Double) proteina
					: proteina != null ? Double.valueOf(proteina.toString()) : 0.0;
		}

		public Double getHidratos() {
			Object hidratos = properties.get("hidratos");
			if (hidratos != null) {
				return hidratos instanceof Double ? (Double) hidratos : Double.valueOf(hidratos.toString());
			}
			// Fallback to "hidratosDeCarbono" if "hidratos" not found
			Object hidratosDeCarbono = properties.get("hidratosDeCarbono");
			return hidratosDeCarbono instanceof Double ? (Double) hidratosDeCarbono
					: hidratosDeCarbono != null ? Double.valueOf(hidratosDeCarbono.toString()) : 0.0;
		}

		// Additional getters for Alimento properties
		public String getNombreAlimento() {
			Object nombreAlimento = properties.get("nombreAlimento");
			return nombreAlimento != null ? nombreAlimento.toString() : "";
		}

		public String getClasificacion() {
			Object clasificacion = properties.get("clasificacion");
			return clasificacion != null ? clasificacion.toString() : "";
		}

		// Additional getters for Alimento/AbstractNutrible properties
		public Integer getPesoBrutoRedondeado() {
			Object pesoBrutoRedondeado = properties.get("pesoBrutoRedondeado");
			return pesoBrutoRedondeado instanceof Integer ? (Integer) pesoBrutoRedondeado
					: pesoBrutoRedondeado != null ? Integer.valueOf(pesoBrutoRedondeado.toString()) : 0;
		}

		public Integer getPesoNeto() {
			Object pesoNeto = properties.get("pesoNeto");
			return pesoNeto instanceof Integer ? (Integer) pesoNeto
					: pesoNeto != null ? Integer.valueOf(pesoNeto.toString()) : 0;
		}

		public Double getCantSugerida() {
			Object cantSugerida = properties.get("cantSugerida");
			return cantSugerida instanceof Double ? (Double) cantSugerida
					: cantSugerida != null ? Double.valueOf(cantSugerida.toString()) : 0.0;
		}

		public Double getFibra() {
			Object fibra = properties.get("fibra");
			return fibra instanceof Double ? (Double) fibra : fibra != null ? Double.valueOf(fibra.toString()) : 0.0;
		}

		public Double getVitA() {
			Object vitA = properties.get("vitA");
			return vitA instanceof Double ? (Double) vitA : vitA != null ? Double.valueOf(vitA.toString()) : 0.0;
		}

		public Double getAcidoAscorbico() {
			Object acidoAscorbico = properties.get("acidoAscorbico");
			return acidoAscorbico instanceof Double ? (Double) acidoAscorbico
					: acidoAscorbico != null ? Double.valueOf(acidoAscorbico.toString()) : 0.0;
		}

		public Double getHierroNoHem() {
			Object hierroNoHem = properties.get("hierroNoHem");
			return hierroNoHem instanceof Double ? (Double) hierroNoHem
					: hierroNoHem != null ? Double.valueOf(hierroNoHem.toString()) : 0.0;
		}

		public Double getPotasio() {
			Object potasio = properties.get("potasio");
			return potasio instanceof Double ? (Double) potasio
					: potasio != null ? Double.valueOf(potasio.toString()) : 0.0;
		}

		public Double getIndiceGlicemico() {
			Object indiceGlicemico = properties.get("indiceGlicemico");
			return indiceGlicemico instanceof Double ? (Double) indiceGlicemico
					: indiceGlicemico != null ? Double.valueOf(indiceGlicemico.toString()) : 0.0;
		}

		public Double getCargaGlicemica() {
			Object cargaGlicemica = properties.get("cargaGlicemica");
			return cargaGlicemica instanceof Double ? (Double) cargaGlicemica
					: cargaGlicemica != null ? Double.valueOf(cargaGlicemica.toString()) : 0.0;
		}

		public Double getAcidoFolico() {
			Object acidoFolico = properties.get("acidoFolico");
			return acidoFolico instanceof Double ? (Double) acidoFolico
					: acidoFolico != null ? Double.valueOf(acidoFolico.toString()) : 0.0;
		}

		public Double getCalcio() {
			Object calcio = properties.get("calcio");
			return calcio instanceof Double ? (Double) calcio
					: calcio != null ? Double.valueOf(calcio.toString()) : 0.0;
		}

		public Double getHierro() {
			Object hierro = properties.get("hierro");
			return hierro instanceof Double ? (Double) hierro
					: hierro != null ? Double.valueOf(hierro.toString()) : 0.0;
		}

		public Double getSodio() {
			Object sodio = properties.get("sodio");
			return sodio instanceof Double ? (Double) sodio : sodio != null ? Double.valueOf(sodio.toString()) : 0.0;
		}

		public Double getSelenio() {
			Object selenio = properties.get("selenio");
			return selenio instanceof Double ? (Double) selenio
					: selenio != null ? Double.valueOf(selenio.toString()) : 0.0;
		}

		public Double getFosforo() {
			Object fosforo = properties.get("fosforo");
			return fosforo instanceof Double ? (Double) fosforo
					: fosforo != null ? Double.valueOf(fosforo.toString()) : 0.0;
		}

		public Double getColesterol() {
			Object colesterol = properties.get("colesterol");
			return colesterol instanceof Double ? (Double) colesterol
					: colesterol != null ? Double.valueOf(colesterol.toString()) : 0.0;
		}

		public Double getAgSaturados() {
			Object agSaturados = properties.get("agSaturados");
			return agSaturados instanceof Double ? (Double) agSaturados
					: agSaturados != null ? Double.valueOf(agSaturados.toString()) : 0.0;
		}

		public Double getAgMonoinsaturados() {
			Object agMonoinsaturados = properties.get("agMonoinsaturados");
			return agMonoinsaturados instanceof Double ? (Double) agMonoinsaturados
					: agMonoinsaturados != null ? Double.valueOf(agMonoinsaturados.toString()) : 0.0;
		}

		public Double getAgPoliinsaturados() {
			Object agPoliinsaturados = properties.get("agPoliinsaturados");
			return agPoliinsaturados instanceof Double ? (Double) agPoliinsaturados
					: agPoliinsaturados != null ? Double.valueOf(agPoliinsaturados.toString()) : 0.0;
		}

		public Double getEtanol() {
			Object etanol = properties.get("etanol");
			return etanol instanceof Double ? (Double) etanol
					: etanol != null ? Double.valueOf(etanol.toString()) : 0.0;
		}

		// Setters for field binding
		public void setId(Long id) {
			properties.put("id", id);
		}

		public void setNombre(String nombre) {
			properties.put("nombre", nombre);
		}

		public void setName(String name) {
			properties.put("name", name);
		}

		public void setEmail(String email) {
			properties.put("email", email);
		}

		public void setPhone(String phone) {
			properties.put("phone", phone);
		}

		public void setGender(String gender) {
			properties.put("gender", gender);
		}

		public void setEnergia(Double energia) {
			properties.put("energia", energia);
		}

		public void setProteina(Double proteina) {
			properties.put("proteina", proteina);
		}

		public void setLipidos(Double lipidos) {
			properties.put("lipidos", lipidos);
		}

		public void setHidratosDeCarbono(Double hidratosDeCarbono) {
			properties.put("hidratosDeCarbono", hidratosDeCarbono);
		}

		public void setAzucarPorEquivalente(Double azucarPorEquivalente) {
			properties.put("azucarPorEquivalente", azucarPorEquivalente);
		}

		public void setDescription(String description) {
			properties.put("description", description);
		}

		public void setImageUrl(String imageUrl) {
			properties.put("imageUrl", imageUrl);
		}

		public void setPdfUrl(String pdfUrl) {
			properties.put("pdfUrl", pdfUrl);
		}

		public void setVideoUrl(String videoUrl) {
			properties.put("videoUrl", videoUrl);
		}

		public void setNombreAlimento(String nombreAlimento) {
			properties.put("nombreAlimento", nombreAlimento);
		}

		public void setClasificacion(String clasificacion) {
			properties.put("clasificacion", clasificacion);
		}

		public void setPesoBrutoRedondeado(Integer pesoBrutoRedondeado) {
			properties.put("pesoBrutoRedondeado", pesoBrutoRedondeado);
		}

		public void setPesoNeto(Integer pesoNeto) {
			properties.put("pesoNeto", pesoNeto);
		}

		public void setCantSugerida(Double cantSugerida) {
			properties.put("cantSugerida", cantSugerida);
		}

		public void setFibra(Double fibra) {
			properties.put("fibra", fibra);
		}

		public void setVitA(Double vitA) {
			properties.put("vitA", vitA);
		}

		public void setAcidoAscorbico(Double acidoAscorbico) {
			properties.put("acidoAscorbico", acidoAscorbico);
		}

		public void setHierroNoHem(Double hierroNoHem) {
			properties.put("hierroNoHem", hierroNoHem);
		}

		public void setPotasio(Double potasio) {
			properties.put("potasio", potasio);
		}

		public void setIndiceGlicemico(Double indiceGlicemico) {
			properties.put("indiceGlicemico", indiceGlicemico);
		}

		public void setCargaGlicemica(Double cargaGlicemica) {
			properties.put("cargaGlicemica", cargaGlicemica);
		}

		public void setAcidoFolico(Double acidoFolico) {
			properties.put("acidoFolico", acidoFolico);
		}

		public void setCalcio(Double calcio) {
			properties.put("calcio", calcio);
		}

		public void setHierro(Double hierro) {
			properties.put("hierro", hierro);
		}

		public void setSodio(Double sodio) {
			properties.put("sodio", sodio);
		}

		public void setSelenio(Double selenio) {
			properties.put("selenio", selenio);
		}

		public void setFosforo(Double fosforo) {
			properties.put("fosforo", fosforo);
		}

		public void setColesterol(Double colesterol) {
			properties.put("colesterol", colesterol);
		}

		public void setAgSaturados(Double agSaturados) {
			properties.put("agSaturados", agSaturados);
		}

		public void setAgMonoinsaturados(Double agMonoinsaturados) {
			properties.put("agMonoinsaturados", agMonoinsaturados);
		}

		public void setAgPoliinsaturados(Double agPoliinsaturados) {
			properties.put("agPoliinsaturados", agPoliinsaturados);
		}

		public void setEtanol(Double etanol) {
			properties.put("etanol", etanol);
		}

		public void setPeso(Double peso) {
			properties.put("peso", peso);
		}

		public void setEstatura(Double estatura) {
			properties.put("estatura", estatura);
		}

		public void setImc(Double imc) {
			properties.put("imc", imc);
		}

		public void setResponsibleName(String responsibleName) {
			properties.put("responsibleName", responsibleName);
		}

		public void setParentesco(String parentesco) {
			properties.put("parentesco", parentesco);
		}

		// Setters for Paciente antecedentes fields
		public void setTipoSanguineo(String tipoSanguineo) {
			properties.put("tipoSanguineo", tipoSanguineo);
		}

		public void setAntecedentesPrenatales(String antecedentesPrenatales) {
			properties.put("antecedentesPrenatales", antecedentesPrenatales);
		}

		public void setAntecedentesNatales(String antecedentesNatales) {
			properties.put("antecedentesNatales", antecedentesNatales);
		}

		public void setAntecedentesPatologicosPersonales(String antecedentesPatologicosPersonales) {
			properties.put("antecedentesPatologicosPersonales", antecedentesPatologicosPersonales);
		}

		public void setAntecedentesPatologicosFamiliares(String antecedentesPatologicosFamiliares) {
			properties.put("antecedentesPatologicosFamiliares", antecedentesPatologicosFamiliares);
		}

		public void setComplicaciones(String complicaciones) {
			properties.put("complicaciones", complicaciones);
		}

		// Setters for Paciente desarrollo e inmunización fields
		public void setHistorialAlimenticio(String historialAlimenticio) {
			properties.put("historialAlimenticio", historialAlimenticio);
		}

		public void setDesarrolloPsicomotor(String desarrolloPsicomotor) {
			properties.put("desarrolloPsicomotor", desarrolloPsicomotor);
		}

		public void setAlergias(String alergias) {
			properties.put("alergias", alergias);
		}

		// Setters for Paciente patologías flags
		public void setEnfermedadesHepaticas(Boolean enfermedadesHepaticas) {
			properties.put("enfermedadesHepaticas", enfermedadesHepaticas);
		}

		public void setHipertension(Boolean hipertension) {
			properties.put("hipertension", hipertension);
		}

		public void setDiabetes(Boolean diabetes) {
			properties.put("diabetes", diabetes);
		}

		public void setHipotiroidismo(Boolean hipotiroidismo) {
			properties.put("hipotiroidismo", hipotiroidismo);
		}

		public void setObesidad(Boolean obesidad) {
			properties.put("obesidad", obesidad);
		}

		public void setAnemia(Boolean anemia) {
			properties.put("anemia", anemia);
		}

		public void setBulimia(Boolean bulimia) {
			properties.put("bulimia", bulimia);
		}

		public void setAnorexia(Boolean anorexia) {
			properties.put("anorexia", anorexia);
		}

		// Method to set collection properties
		public void setCollectionProperty(String propertyName, java.util.List<?> collection) {
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
		public boolean hasErrors(String fieldName) {
			return false;
		}

		/**
		 * Gets validation errors for the given field. In validation context, this always
		 * returns an empty list since we're not actually validating forms.
		 * @param fieldName the name of the field to get errors for
		 * @return always an empty list in validation context
		 */
		public java.util.List<String> errors(String fieldName) {
			return new ArrayList<>();
		}

	}

}
