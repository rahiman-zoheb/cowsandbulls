package com.zohebrahiman.cowsandbulls.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

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
