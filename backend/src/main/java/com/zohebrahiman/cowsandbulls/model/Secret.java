package com.zohebrahiman.cowsandbulls.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class Secret {

	@Id
	@GeneratedValue
	private Long id;

	@NonNull
	private String name;

}
