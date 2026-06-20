package com.nutriconsultas.platillos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.model.AbstractNutrible;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class PlatilloServiceImpl implements PlatilloService {

	@Autowired
	private PlatilloRepository platilloRepository;

	@Autowired
	private AlimentosRepository alimentoRepository;

	@Value("${amazon.s3.region}")
	private String awsRegion;

	@Value("${amazon.s3.bucket}")
	private String bucketName;

	@Value("${amazon.s3.key}")
	private String accessKey;

	@Value("${amazon.s3.secret}")
	private String secretKey;

	@Override
	public Platillo findById(@NonNull final Long id) {
		log.info("Retrieving platillo with id: {}", id);
		return platilloRepository.findById(id).orElse(null);
	}

	@Override
	public Platillo findByIdAndUserId(@NonNull final Long id, @NonNull final String userId) {
		log.info("Retrieving platillo with id: {} for userId present", id);
		return platilloRepository.findByIdAndUserId(id, userId).orElse(null);
	}

	@Override
	public List<Platillo> getPlatillosForCatalogFilter(final PlatilloCatalogFilter filter, final String userId) {
		if (filter == null || filter == PlatilloCatalogFilter.TODAS) {
			if (userId == null || userId.isBlank()) {
				log.info("Getting system catalog platillos without userId");
				return platilloRepository.findByUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
			}
			log.info("Getting system and owned platillos for catalog filter todas");
			return platilloRepository.findByUserIdIn(List.of(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID, userId));
		}
		if (filter == PlatilloCatalogFilter.SISTEMA) {
			log.info("Getting system catalog platillos for filter sistema");
			return platilloRepository.findByUserId(PlatilloCatalogConstants.SYSTEM_CATALOG_USER_ID);
		}
		if (userId == null || userId.isBlank()) {
			log.warn("Cannot resolve propias platillos without userId");
			return List.of();
		}
		log.info("Getting owned platillos for catalog filter propias");
		return platilloRepository.findByUserId(userId);
	}

	@Override
	public void deletePlatillo(@NonNull final Long id) {
		log.info("Deleting platillo with id: {}", id);
		platilloRepository.deleteById(id);
	}

	@Override
	public Platillo save(@NonNull final Platillo platillo) {
		log.info("Saving platillo: {}", platillo);
		return platilloRepository.save(platillo);
	}

	@Override
	public List<Platillo> findAll() {
		log.info("Retrieving all platillos from database.");
		return platilloRepository.findAll();
	}

	@Override
	public Page<Platillo> findAll(@NonNull final Pageable pageable) {
		log.info("Retrieving platillos with pagination: {}", pageable);
		return platilloRepository.findAll(pageable);
	}

	@Override
	public Page<Platillo> findBySearchTerm(@NonNull final String searchTerm, @NonNull final Pageable pageable) {
		log.info("Searching platillos with term: {} and pagination: {}", searchTerm, pageable);
		final String searchPattern = "%" + searchTerm + "%";
		return platilloRepository.findBySearchTerm(searchPattern, pageable);
	}

	@Override
	public long count() {
		log.info("Counting all platillos.");
		return platilloRepository.count();
	}

	@Override
	public long countBySearchTerm(@NonNull final String searchTerm) {
		log.info("Counting platillos with search term: {}", searchTerm);
		final String searchPattern = "%" + searchTerm + "%";
		return platilloRepository.countBySearchTerm(searchPattern);
	}

	@Override
	public void deleteIngrediente(@NonNull final Long id, @NonNull final Long ingredienteId) {
		log.info("Deleting ingrediente with id: {}", id);
		// platilloRepository.deleteIngrediente(ingredienteId);

		platilloRepository.findById(id).ifPresent(platillo -> {
			platillo.getIngredientes().removeIf(ingrediente -> Objects.equals(ingrediente.getId(), ingredienteId));
			summarizeMacronutrientesPlatillo(platillo);
			platilloRepository.save(platillo);
		});
	}

	@Override
	public Ingrediente addIngrediente(@NonNull final Long id, @NonNull final Long alimentoId,
			@NonNull final String cantidad, @NonNull final Integer peso) {
		log.info("Start addIngrediente with id: {} to platillo with id: {} as ingrediente", alimentoId, id);

		final Platillo platillo = platilloRepository.findById(id).orElse(null);
		if (platillo == null) {
			log.error("Platillo with id: {} not found.", id);
			return null;
		}
		log.debug("Platillo found to add ingrediente: {}", platillo);

		final Alimento alimento = alimentoRepository.findById(alimentoId).orElse(null);
		if (alimento == null) {
			log.error("Alimento with id: {} not found.", alimentoId);
			return null;
		}
		log.debug("Alimento found to add ingrediente: {}", alimento);

		// Calculate ingrediente values
		final Ingrediente ingrediente = new Ingrediente();
		ingrediente.setAlimento(alimento);
		ingrediente.setUnidad(alimento.getUnidad());

		log.debug("ingrediente initialized: {}", ingrediente);

		// if cantidad is different than cantSugerida, then calculate the new values
		Boolean calculatedFromCantidad = false;
		if (!cantidad.equals(ingrediente.getAlimento().getFractionalCantSugerida())) {
			log.debug("calculating ingrediente from cantidad change.");
			calculateIngredienteFromCantiadChange(cantidad, alimento.getFractionalCantSugerida(), ingrediente,
					alimento);
			log.debug("ingrediente calculated from cantidad change: {}", ingrediente);
			calculatedFromCantidad = true;
		}
		else {
			log.debug("cantidad did not changed, setting ingrediente default values.");
			convertAlimentoToIngrediente(ingrediente, alimento);
			log.debug("ingrediente calculated from alimento: {}", ingrediente);
		}

		// if peso is different than pesoNeto, then calculate the new values
		if (!Objects.equals(peso, ingrediente.getAlimento().getPesoNeto()) && !calculatedFromCantidad) {
			log.debug("calculating ingrediente from peso change.");
			calculateIngredienteFromPesoChange(peso, alimento.getPesoNeto(), ingrediente, alimento);
			log.debug("ingrediente calculated from peso change: {}", ingrediente);
		}
		ingrediente.setPlatillo(platillo);

		log.debug("setting ingrediente in platillo: {}.", ingrediente);
		platillo.getIngredientes().add(ingrediente);
		platilloRepository.save(platillo);
		summarizeMacronutrientesPlatillo(platillo);
		platilloRepository.save(platillo);
		log.info("finish addIngrediente with platillo: {}", platillo);
		return ingrediente;
	}

	@Override
	public void savePicture(@NonNull final Long id, @NonNull final byte[] bytes, @NonNull final String fileExtension) {
		log.info("Starting savePicture with id: {}", id);
		uploadPictureToS3(id, bytes, fileExtension);
		log.info("Finish savePicture with id: {}", id);
	}

	@Override
	@Transactional
	public Platillo duplicatePlatillo(@NonNull final Long id, @NonNull final String userId) {
		log.info("Duplicating platillo with id: {} for user present", id);
		final Platillo original = platilloRepository.findById(id).orElse(null);
		if (original == null) {
			log.warn("Platillo with id {} not found for duplication", id);
			return null;
		}
		if (!PlatilloCatalogConstants.isSystemCatalog(original) && !Objects.equals(userId, original.getUserId())) {
			log.warn("User not allowed to duplicate platillo {}", id);
			return null;
		}

		final Platillo copy = new Platillo();
		final String originalName = original.getName() != null ? original.getName() : "Platillo";
		copy.setName(originalName + " (copia)");
		copy.setUserId(userId);
		copy.setDescription(original.getDescription());
		copy.setImageUrl(original.getImageUrl());
		copy.setVideoUrl(original.getVideoUrl());
		copy.setPdfUrl(original.getPdfUrl());
		copy.setIngestasSugeridas(original.getIngestasSugeridas());
		copyNutrientFields(original, copy);

		final List<Ingrediente> newIngredientes = new ArrayList<>();
		for (final Ingrediente originalIngrediente : original.getIngredientes()) {
			final Ingrediente newIngrediente = copyIngrediente(originalIngrediente);
			newIngrediente.setPlatillo(copy);
			newIngredientes.add(newIngrediente);
		}
		copy.setIngredientes(newIngredientes);

		final Platillo saved = platilloRepository.save(copy);
		log.info("Successfully duplicated platillo {} to new platillo {}", id, saved.getId());
		return saved;
	}

	private Ingrediente copyIngrediente(final Ingrediente original) {
		final Ingrediente copy = new Ingrediente();
		copy.setDescription(original.getDescription());
		copy.setAlimento(original.getAlimento());
		copy.setUnidad(original.getUnidad());
		copy.setCantSugerida(original.getCantSugerida());
		copyNutrientFields(original, copy);
		return copy;
	}

	private void copyNutrientFields(final AbstractNutrible source, final AbstractNutrible target) {
		target.setEnergia(source.getEnergia());
		target.setProteina(source.getProteina());
		target.setLipidos(source.getLipidos());
		target.setHidratosDeCarbono(source.getHidratosDeCarbono());
		target.setPesoBrutoRedondeado(source.getPesoBrutoRedondeado());
		target.setPesoNeto(source.getPesoNeto());
		target.setFibra(source.getFibra());
		target.setVitA(source.getVitA());
		target.setAcidoAscorbico(source.getAcidoAscorbico());
		target.setHierroNoHem(source.getHierroNoHem());
		target.setPotasio(source.getPotasio());
		target.setIndiceGlicemico(source.getIndiceGlicemico());
		target.setCargaGlicemica(source.getCargaGlicemica());
		target.setAcidoFolico(source.getAcidoFolico());
		target.setCalcio(source.getCalcio());
		target.setHierro(source.getHierro());
		target.setSodio(source.getSodio());
		target.setAzucarPorEquivalente(source.getAzucarPorEquivalente());
		target.setSelenio(source.getSelenio());
		target.setFosforo(source.getFosforo());
		target.setColesterol(source.getColesterol());
		target.setAgSaturados(source.getAgSaturados());
		target.setAgMonoinsaturados(source.getAgMonoinsaturados());
		target.setAgPoliinsaturados(source.getAgPoliinsaturados());
		target.setEtanol(source.getEtanol());
	}

	@Override
	public void savePdf(@NonNull final Long id, final byte[] bytes) {
		log.info("Starting savePdf with id: {}", id);
		final S3Client s3Client = getClient();
		final String key = "platillo/" + id + "/instrucciones.pdf";

		try {
			if (keyExists(key)) {
				log.debug("Deleting existing pdf with key: {}", key);
				s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
			}

			s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
					RequestBody.fromBytes(bytes));

			final Platillo platillo = platilloRepository.findById(id).orElse(null);
			if (platillo != null) {
				platillo.setPdfUrl(key);
				platilloRepository.save(platillo);
			}
		}
		catch (final S3Exception e) {
			log.error("Error uploading pdf to S3", e);
		}
	}

	@Override
	public byte[] getPicture(@NonNull final Long id, @NonNull final String fileName) throws IOException {
		log.info("Starting getPicture with id: {}", id);
		final S3Client s3Client = getClient();
		final String key = "platillo/" + id + "/" + fileName;
		byte[] result = null;
		try {
			result = s3Client.getObject(builder -> builder.bucket(bucketName).key(key)).readAllBytes();
		}
		catch (final S3Exception e) {
			log.error("Error getting picture from S3", e);
		}
		return result;
	}

	private void uploadPictureToS3(@NonNull final Long id, final byte[] bytes, final String fileExtension) {
		final String key = "platillo/" + id + "/picture." + fileExtension;
		log.info("Uploading picture to S3 with key: {}", key);
		final S3Client s3Client = getClient();
		try {
			// delete image if exists
			if (imageExists(id, fileExtension)) {
				log.debug("Deleting existing image with key: {}", key);
				s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
			}

			s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
					RequestBody.fromBytes(bytes));

			// update the file name in the db
			final Platillo platillo = platilloRepository.findById(id).orElse(null);
			if (platillo != null) {
				platillo.setImageUrl(key);
				platilloRepository.save(platillo);
			}
		}
		catch (final S3Exception e) {
			log.error("Error uploading picture to S3", e);
		}
	}

	private boolean imageExists(final Long id, final String fileExtension) {
		final String key = "platillo/" + id + "/picture." + fileExtension;
		return keyExists(key);
	}

	private boolean keyExists(final String key) {
		final S3Client s3Client = getClient();
		try {
			final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucketName).key(key).build();
			final HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
			final String contentType = headObjectResponse.contentType();
			return contentType.length() > 0;
		}
		catch (final AwsServiceException | SdkClientException e) {
			return false;
		}
	}

	private S3Client getClient() {
		return S3Client.builder().region(Region.of(awsRegion)).credentialsProvider(new AwsCredentialsProvider() {
			@Override
			public software.amazon.awssdk.auth.credentials.AwsCredentials resolveCredentials() {
				return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey);
			}
		}).build();
	}

	// summarize platillo macronutrientes
	private void summarizeMacronutrientesPlatillo(final Platillo platillo) {
		resetMacronutrientesPlatillo(platillo);
		for (final Ingrediente i : platillo.getIngredientes()) {
			addMacronutrientesPlatillo(platillo, i);
		}
	}

	// reset platillo macronutrientes
	private void resetMacronutrientesPlatillo(final Platillo platillo) {
		platillo.setAcidoAscorbico(0d);
		platillo.setAcidoFolico(0d);
		platillo.setAgMonoinsaturados(0d);
		platillo.setAgPoliinsaturados(0d);
		platillo.setAgSaturados(0d);
		platillo.setAzucarPorEquivalente(0d);
		platillo.setCalcio(0d);
		platillo.setCargaGlicemica(0d);
		platillo.setColesterol(0d);
		platillo.setEnergia(0);
		platillo.setFibra(0d);
		platillo.setFosforo(0d);
		platillo.setHierro(0d);
		platillo.setHierroNoHem(0d);
		platillo.setIndiceGlicemico(0d);
		platillo.setEtanol(0d);
		platillo.setHidratosDeCarbono(0d);
		platillo.setLipidos(0d);
		platillo.setPotasio(0d);
		platillo.setProteina(0d);
		platillo.setSelenio(0d);
		platillo.setSodio(0d);
		platillo.setVitA(0d);
	}

	// add macronutrientes to platillo
	private void addMacronutrientesPlatillo(final Platillo platillo, final Ingrediente ingrediente) {
		if (ingrediente.getAcidoAscorbico() != null) {
			platillo.setAcidoAscorbico(platillo.getAcidoAscorbico() + ingrediente.getAcidoAscorbico());
		}
		if (ingrediente.getAcidoFolico() != null) {
			platillo.setAcidoFolico(platillo.getAcidoFolico() + ingrediente.getAcidoFolico());
		}
		if (ingrediente.getAgMonoinsaturados() != null) {
			platillo.setAgMonoinsaturados(platillo.getAgMonoinsaturados() + ingrediente.getAgMonoinsaturados());
		}
		if (ingrediente.getAgPoliinsaturados() != null) {
			platillo.setAgPoliinsaturados(platillo.getAgPoliinsaturados() + ingrediente.getAgPoliinsaturados());
		}
		if (ingrediente.getAgSaturados() != null) {
			platillo.setAgSaturados(platillo.getAgSaturados() + ingrediente.getAgSaturados());
		}
		if (ingrediente.getAzucarPorEquivalente() != null) {
			platillo
				.setAzucarPorEquivalente(platillo.getAzucarPorEquivalente() + ingrediente.getAzucarPorEquivalente());
		}
		if (ingrediente.getCalcio() != null) {
			platillo.setCalcio(platillo.getCalcio() + ingrediente.getCalcio());
		}
		if (ingrediente.getCargaGlicemica() != null) {
			platillo.setCargaGlicemica(platillo.getCargaGlicemica() + ingrediente.getCargaGlicemica());
		}
		if (ingrediente.getColesterol() != null) {
			platillo.setColesterol(platillo.getColesterol() + ingrediente.getColesterol());
		}
		if (ingrediente.getEnergia() != null) {
			platillo.setEnergia(platillo.getEnergia() + ingrediente.getEnergia());
		}
		if (ingrediente.getFibra() != null) {
			platillo.setFibra(platillo.getFibra() + ingrediente.getFibra());
		}
		if (ingrediente.getFosforo() != null) {
			platillo.setFosforo(platillo.getFosforo() + ingrediente.getFosforo());
		}
		if (ingrediente.getHierro() != null) {
			platillo.setHierro(platillo.getHierro() + ingrediente.getHierro());
		}
		if (ingrediente.getHierroNoHem() != null) {
			platillo.setHierroNoHem(platillo.getHierroNoHem() + ingrediente.getHierroNoHem());
		}
		if (ingrediente.getIndiceGlicemico() != null) {
			platillo.setIndiceGlicemico(platillo.getIndiceGlicemico() + ingrediente.getIndiceGlicemico());
		}
		if (ingrediente.getEtanol() != null) {
			platillo.setEtanol(platillo.getEtanol() + ingrediente.getEtanol());
		}
		if (ingrediente.getHidratosDeCarbono() != null) {
			platillo.setHidratosDeCarbono(platillo.getHidratosDeCarbono() + ingrediente.getHidratosDeCarbono());
		}
		if (ingrediente.getLipidos() != null) {
			platillo.setLipidos(platillo.getLipidos() + ingrediente.getLipidos());
		}
		if (ingrediente.getPotasio() != null) {
			platillo.setPotasio(platillo.getPotasio() + ingrediente.getPotasio());
		}
		if (ingrediente.getProteina() != null) {
			platillo.setProteina(platillo.getProteina() + ingrediente.getProteina());
		}
		if (ingrediente.getSelenio() != null) {
			platillo.setSelenio(platillo.getSelenio() + ingrediente.getSelenio());
		}
		if (ingrediente.getSodio() != null) {
			platillo.setSodio(platillo.getSodio() + ingrediente.getSodio());
		}
		if (ingrediente.getVitA() != null) {
			platillo.setVitA(platillo.getVitA() + ingrediente.getVitA());
		}
	}

	// calculate ingrediente from cantidad change
	private void calculateIngredienteFromCantiadChange(final String given, final String suggested,
			final Ingrediente ingrediente, final Alimento alimento) {
		final String trimmedGiven = given.trim();
		final Boolean hasInteger = trimmedGiven.contains(" ") || !trimmedGiven.contains("/");
		final Boolean hasFraction = trimmedGiven.contains("/");
		final Integer iGivenIntPart = hasInteger ? Integer.parseInt(trimmedGiven.split(" ")[0]) : 0;
		final Integer iGivenNumeratorPart = hasInteger
				? hasFraction ? Integer.parseInt(trimmedGiven.split(" ")[1].split("/")[0]) : 0
				: Integer.parseInt(trimmedGiven.split("/")[0]);
		final Integer iGivenDenominatorPart = hasInteger
				? hasFraction ? Integer.parseInt(trimmedGiven.split(" ")[1].split("/")[1]) : 0
				: Integer.parseInt(trimmedGiven.split("/")[1]);
		final Double dGiven = iGivenIntPart.doubleValue()
				+ (hasFraction ? (iGivenNumeratorPart.doubleValue() / iGivenDenominatorPart.doubleValue()) : 0d);
		final Double dFactor = dGiven / alimento.getCantSugerida();

		log.debug("setting cantSugerida to {}.", dGiven);
		ingrediente.setCantSugerida(dGiven);
		if (alimento.getAcidoAscorbico() != null) {
			ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico() * dFactor);
		}

		if (alimento.getAcidoFolico() != null) {
			ingrediente.setAcidoFolico(alimento.getAcidoFolico() * dFactor);
		}

		if (alimento.getAgMonoinsaturados() != null) {
			ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados() * dFactor);
		}

		if (alimento.getAgPoliinsaturados() != null) {
			ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados() * dFactor);
		}

		if (alimento.getAgSaturados() != null) {
			ingrediente.setAgSaturados(alimento.getAgSaturados() * dFactor);
		}

		if (alimento.getAzucarPorEquivalente() != null) {
			ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente() * dFactor);
		}

		if (alimento.getCalcio() != null) {
			ingrediente.setCalcio(alimento.getCalcio() * dFactor);
		}

		if (alimento.getCargaGlicemica() != null) {
			ingrediente.setCargaGlicemica(alimento.getCargaGlicemica() * dFactor);
		}

		if (alimento.getColesterol() != null) {
			ingrediente.setColesterol(alimento.getColesterol() * dFactor);
		}

		if (alimento.getEnergia() != null) {
			ingrediente.setEnergia(alimento.getEnergia() * dFactor.intValue());
		}

		if (alimento.getFibra() != null) {
			ingrediente.setFibra(alimento.getFibra() * dFactor);
		}

		if (alimento.getFosforo() != null) {
			ingrediente.setFosforo(alimento.getFosforo() * dFactor);
		}

		if (alimento.getHierro() != null) {
			ingrediente.setHierro(alimento.getHierro() * dFactor);
		}

		if (alimento.getHierroNoHem() != null) {
			ingrediente.setHierroNoHem(alimento.getHierroNoHem() * dFactor);
		}

		if (alimento.getIndiceGlicemico() != null) {
			ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico() * dFactor);
		}

		if (alimento.getEtanol() != null) {
			ingrediente.setEtanol(alimento.getEtanol() * dFactor);
		}

		if (alimento.getHidratosDeCarbono() != null) {
			ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * dFactor);
		}

		if (alimento.getLipidos() != null) {
			ingrediente.setLipidos(alimento.getLipidos() * dFactor);
		}

		if (alimento.getPotasio() != null) {
			ingrediente.setPotasio(alimento.getPotasio() * dFactor);
		}

		if (alimento.getProteina() != null) {
			ingrediente.setProteina(alimento.getProteina() * dFactor);
		}

		if (alimento.getSelenio() != null) {
			ingrediente.setSelenio(alimento.getSelenio() * dFactor);
		}

		if (alimento.getSodio() != null) {
			ingrediente.setSodio(alimento.getSodio() * dFactor);
		}

		if (alimento.getVitA() != null) {
			ingrediente.setVitA(alimento.getVitA() * dFactor);
		}

		if (alimento.getPesoBrutoRedondeado() != null) {
			ingrediente.setPesoBrutoRedondeado((int) Math.round(alimento.getPesoBrutoRedondeado() * dFactor));
		}

		if (alimento.getPesoNeto() != null) {
			ingrediente.setPesoNeto((int) Math.round(alimento.getPesoNeto() * dFactor));
		}
	}

	// convert alimento to ingrediente
	private void convertAlimentoToIngrediente(final Ingrediente ingrediente, final Alimento alimento) {
		ingrediente.setCantSugerida(alimento.getCantSugerida());
		ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico());
		ingrediente.setAcidoFolico(alimento.getAcidoAscorbico());
		ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados());
		ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados());
		ingrediente.setAgSaturados(alimento.getAgSaturados());
		ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente());
		ingrediente.setCalcio(alimento.getCalcio());
		ingrediente.setCargaGlicemica(alimento.getCargaGlicemica());
		ingrediente.setColesterol(alimento.getColesterol());
		ingrediente.setEnergia(alimento.getEnergia());
		ingrediente.setFibra(alimento.getFibra());
		ingrediente.setFosforo(alimento.getFosforo());
		ingrediente.setHierro(alimento.getHierro());
		ingrediente.setHierroNoHem(alimento.getHierroNoHem());
		ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico());
		ingrediente.setEtanol(alimento.getEtanol());
		ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono());
		ingrediente.setLipidos(alimento.getLipidos());
		ingrediente.setPotasio(alimento.getPotasio());
		ingrediente.setProteina(alimento.getProteina());
		ingrediente.setSelenio(alimento.getSelenio());
		ingrediente.setSodio(alimento.getSodio());
		ingrediente.setVitA(alimento.getVitA());
		ingrediente.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado());
		ingrediente.setPesoNeto(alimento.getPesoNeto());
	}

	// calculate ingrediente from peso change
	private void calculateIngredienteFromPesoChange(final Integer given, final Integer suggested,
			final Ingrediente ingrediente, final Alimento alimento) {
		final Double dFactor = (double) given / (double) suggested;

		ingrediente.setCantSugerida(dFactor * alimento.getCantSugerida());

		if (alimento.getAcidoAscorbico() != null) {
			ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico() * dFactor);
		}

		if (alimento.getAcidoFolico() != null) {
			ingrediente.setAcidoFolico(alimento.getAcidoAscorbico() * dFactor);
		}

		if (alimento.getAgMonoinsaturados() != null) {
			ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados() * dFactor);
		}

		if (alimento.getAgPoliinsaturados() != null) {
			ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados() * dFactor);
		}

		if (alimento.getAgSaturados() != null) {
			ingrediente.setAgSaturados(alimento.getAgSaturados() * dFactor);
		}

		if (alimento.getAzucarPorEquivalente() != null) {
			ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente() * dFactor);
		}

		if (alimento.getCalcio() != null) {
			ingrediente.setCalcio(alimento.getCalcio() * dFactor);
		}

		if (alimento.getCargaGlicemica() != null) {
			ingrediente.setCargaGlicemica(alimento.getCargaGlicemica() * dFactor);
		}

		if (alimento.getColesterol() != null) {
			ingrediente.setColesterol(alimento.getColesterol() * dFactor);
		}

		if (alimento.getEnergia() != null) {
			ingrediente.setEnergia(alimento.getEnergia() * dFactor.intValue());
		}

		if (alimento.getFibra() != null) {
			ingrediente.setFibra(alimento.getFibra() * dFactor);
		}

		if (alimento.getFosforo() != null) {
			ingrediente.setFosforo(alimento.getFosforo() * dFactor);
		}

		if (alimento.getHierro() != null) {
			ingrediente.setHierro(alimento.getHierro() * dFactor);
		}

		if (alimento.getHierroNoHem() != null) {
			ingrediente.setHierroNoHem(alimento.getHierroNoHem() * dFactor);
		}

		if (alimento.getIndiceGlicemico() != null) {
			ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico() * dFactor);
		}

		if (alimento.getEtanol() != null) {
			ingrediente.setEtanol(alimento.getEtanol() * dFactor);
		}

		if (alimento.getHidratosDeCarbono() != null) {
			ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * dFactor);
		}

		if (alimento.getLipidos() != null) {
			ingrediente.setLipidos(alimento.getLipidos() * dFactor);
		}

		if (alimento.getPotasio() != null) {
			ingrediente.setPotasio(alimento.getPotasio() * dFactor);
		}

		if (alimento.getProteina() != null) {
			ingrediente.setProteina(alimento.getProteina() * dFactor);
		}

		if (alimento.getSelenio() != null) {
			ingrediente.setSelenio(alimento.getSelenio() * dFactor);
		}

		if (alimento.getSodio() != null) {
			ingrediente.setSodio(alimento.getSodio() * dFactor);
		}

		if (alimento.getVitA() != null) {
			ingrediente.setVitA(alimento.getVitA() * dFactor);
		}

		if (alimento.getPesoBrutoRedondeado() != null) {
			ingrediente.setPesoBrutoRedondeado((int) Math.round(alimento.getPesoBrutoRedondeado() * dFactor));
		}

		if (alimento.getPesoNeto() != null) {
			ingrediente.setPesoNeto((int) Math.round(alimento.getPesoNeto() * dFactor));
		}
	}

}
