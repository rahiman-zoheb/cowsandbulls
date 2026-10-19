package com.zohebrahiman.cowsandbulls.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zohebrahiman.cowsandbulls.model.Secret;
import com.zohebrahiman.cowsandbulls.model.SecretRepository;

@RestController
@RequestMapping("/api")
public class SecretController {
	private final Logger log = LoggerFactory.getLogger(SecretController.class);
	private SecretRepository secretRepository;

	public SecretController(SecretRepository secretRepository) {
		this.secretRepository = secretRepository;
	}

	@GetMapping("/secrets")
	Collection<Secret> secrets() {
		return secretRepository.findAll();
	}

	@GetMapping("/secret/{id}")
	ResponseEntity<?> getSecret(@PathVariable Long id) {
		Optional<Secret> secret = secretRepository.findById(id);
		return secret.map(response -> ResponseEntity.ok().body(response))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@PostMapping("/secret")
	ResponseEntity<Secret> createSecret(@Valid @RequestBody Secret secret) throws URISyntaxException {
		log.info("Request to create secret: {}", secret);
		Secret result = secretRepository.save(secret);
		return ResponseEntity.created(new URI("/api/secret/" + result.getId())).body(result);
	}

	@PutMapping("/secret/{id}")
	ResponseEntity<Secret> updateSecret(@Valid @RequestBody Secret secret) {
		log.info("Request to update secret: {}", secret);
		Secret result = secretRepository.save(secret);
		return ResponseEntity.ok().body(result);
	}

	@DeleteMapping("/secret/{id}")
	public ResponseEntity<?> deleteSecret(@PathVariable Long id) {
		log.info("Request to delete secret: {}", id);
		secretRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}
}
