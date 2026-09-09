package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.mapper.PaymentNoteMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import com.bese.tesouraria.dto.PaymentNoteBasicDto;
import com.bese.tesouraria.service.PaymentNoteService;

@RestController
@RequestMapping("/API/Np")
public class PaymentNoteController {

	private final PaymentNoteService service;
	private final PaymentNoteMapper mapper;

	public PaymentNoteController(PaymentNoteService service, PaymentNoteMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PageDto<PaymentNoteBasicDto>> findAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Page<PaymentNote> notes = service.findAll(page, size);
		Page<PaymentNoteBasicDto> noteDtos = notes.map(mapper::toDto);

		PageDto<PaymentNoteBasicDto> pageDto = new PageDto<>(
				noteDtos.getContent(),
				notes.getNumber(),
				notes.getSize(),
				notes.getTotalElements(),
				notes.getTotalPages(),
				notes.isLast());

		return ResponseEntity.ok(pageDto);
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PaymentNoteBasicDto> save(@Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto) {
		PaymentNote paymentNoteToSave = mapper.toEntity(paymentNoteBasicDto);
		PaymentNote savedPaymentNote = service.saveNp(paymentNoteToSave);
		return ResponseEntity.status(201).body(mapper.toDto(savedPaymentNote));
	}

	@PutMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PaymentNoteBasicDto> update(@Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto) {
		PaymentNote paymentNoteToUpdate = mapper.toEntity(paymentNoteBasicDto);
		PaymentNote updatedPaymentNote = service.updateNp(paymentNoteToUpdate);
		return ResponseEntity.status(202).body(mapper.toDto(updatedPaymentNote));
	}

	@GetMapping("/{numero}/{date}")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PaymentNoteBasicDto> findByNumeroNp(@Valid @PathVariable @NotNull Integer numero,
			@Valid @PathVariable @NotNull Integer date) {
		return ResponseEntity.status(200).body(mapper.toDto(service.findByNumeroNpForYear(numero, date)));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN')")
	public void deleteById(@PathVariable Long id) {
		service.deleteNp(id);
	}

}