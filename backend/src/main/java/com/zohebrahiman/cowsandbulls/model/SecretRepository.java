package com.zohebrahiman.cowsandbulls.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretRepository extends JpaRepository<Secret, Long> {

	Secret findByName(String name);

}
