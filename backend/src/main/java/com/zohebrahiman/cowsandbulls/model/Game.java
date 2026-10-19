package com.zohebrahiman.cowsandbulls.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Game {

	@Id
	@GeneratedValue
	private Long id;

	@NonNull
	@OneToOne
	private Secret secret;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<Guess> guesses;

}
