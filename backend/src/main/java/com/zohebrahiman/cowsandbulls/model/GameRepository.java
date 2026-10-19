package com.zohebrahiman.cowsandbulls.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GameRepository extends JpaRepository<Game, UUID> {

	/** Deletes games idle since before {@code cutoff}; their guesses go via ON DELETE CASCADE. */
	@Modifying
	@Query("delete from Game g where g.lastActivityAt < :cutoff")
	int deleteIdleSince(Instant cutoff);
}
