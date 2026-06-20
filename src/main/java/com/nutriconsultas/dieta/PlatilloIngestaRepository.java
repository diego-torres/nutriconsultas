package com.nutriconsultas.dieta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatilloIngestaRepository extends JpaRepository<PlatilloIngesta, Long> {

	@Query("SELECT COUNT(pi) FROM PlatilloIngesta pi WHERE pi.sourcePlatilloId = :platilloId")
	long countBySourcePlatilloId(@Param("platilloId") Long platilloId);

	@Query("SELECT COUNT(pi) FROM PlatilloIngesta pi JOIN pi.ingesta i JOIN i.dieta d "
			+ "WHERE pi.sourcePlatilloId = :platilloId AND d.userId = :userId")
	long countBySourcePlatilloIdAndDietaUserId(@Param("platilloId") Long platilloId, @Param("userId") String userId);

}
