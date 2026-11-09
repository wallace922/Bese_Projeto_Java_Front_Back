package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.mapper.PaymentNoteMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import com.bese.tesouraria.dto.PaymentNoteBasicDto;
import com.bese.tesouraria.service.PaymentNoteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/API/Np")
public class PaymentNoteController {

	private static final Logger log = LoggerFactory.getLogger(PaymentNoteController.class);

	private final PaymentNoteService service;
	private final PaymentNoteMapper mapper;

	public PaymentNoteController(PaymentNoteService service, PaymentNoteMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PageDto<PaymentNoteBasicDto>> findAll(
			@PageableDefault(size = 20) Pageable pageable) {
		Page<PaymentNote> notes = service.findAll(pageable.getPageNumber(), pageable.getPageSize());
		Page<PaymentNoteBasicDto> noteDtos = notes.map(mapper::toDto);

		return ResponseEntity.ok(PageDto.of(noteDtos));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PaymentNoteBasicDto> save(@Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto,
			HttpServletRequest request) {
		PaymentNote paymentNoteToSave = mapper.toEntity(paymentNoteBasicDto);
		PaymentNote savedPaymentNote = service.saveNp(paymentNoteToSave);
		log.info("AUDIT_NP_CREATED | npId={} | npNumber={} | value={} | ip={}", savedPaymentNote.getId(),
				savedPaymentNote.getNumeroNp(), savedPaymentNote.getValue(), request.getRemoteAddr());
		return ResponseEntity.status(201).body(mapper.toDto(savedPaymentNote));
	}

	/**
	 * Prévia de impostos: executa o mesmo pipeline do save, sem persistir.
	 *
	 * <p>Usa o mesmo DTO de entrada e o mesmo DTO de saída do POST /API/Np,
	 * para não duplicar contrato. A resposta já traz, em cada item, a lista
	 * {@code taxes} com {@code calculatedItems}, {@code taxStatus},
	 * {@code codigoReceita} e {@code taxRuleDescription}.
	 */
	@PostMapping("/preview")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PaymentNoteBasicDto> preview(
			@Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto) {
		PaymentNote paymentNote = mapper.toEntity(paymentNoteBasicDto);
		PaymentNote preview = service.previewNp(paymentNote);
		return ResponseEntity.ok(mapper.toDto(preview));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PaymentNoteBasicDto> update(@NotNull @PathVariable Long id,
			@Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto,
			HttpServletRequest request) {
		PaymentNote paymentNoteToUpdate = mapper.toEntity(paymentNoteBasicDto);
		PaymentNote updatedPaymentNote = service.updateNp(paymentNoteToUpdate, id);
		log.info("AUDIT_NP_UPDATED | npId={} | npNumber={} | ip={}", updatedPaymentNote.getId(),
				updatedPaymentNote.getNumeroNp(), request.getRemoteAddr());
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
	public void deleteById(@PathVariable Long id, HttpServletRequest request) {
		service.deleteNp(id);
		log.warn("AUDIT_NP_DELETED | npId={} | ip={}", id, request.getRemoteAddr());
	}

}