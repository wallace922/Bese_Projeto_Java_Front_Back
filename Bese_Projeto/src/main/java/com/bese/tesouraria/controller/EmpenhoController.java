package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.entity.Empenho;
import com.bese.tesouraria.mapper.EmpenhoMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bese.tesouraria.dto.EmpenhoDto;
import com.bese.tesouraria.service.EmpenhoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/API/Empenho")
public class EmpenhoController {

	private static final Logger log = LoggerFactory.getLogger(EmpenhoController.class);

	private final EmpenhoService empenhoService;
	private final EmpenhoMapper mapper;

	public EmpenhoController(EmpenhoService empenhoService, EmpenhoMapper mapper) {
		this.empenhoService = empenhoService;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<PageDto<EmpenhoDto>> findByAll(
			@PageableDefault(size = 20) Pageable pageable) {
		Page<Empenho> empenhos = empenhoService.findAll(pageable.getPageNumber(), pageable.getPageSize());
		Page<EmpenhoDto> empenhoDtos = empenhos.map(mapper::toDto);

		return ResponseEntity.ok(PageDto.of(empenhoDtos));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<EmpenhoDto> save(@Valid @RequestBody EmpenhoDto empDto, HttpServletRequest request) {
		Empenho empenho = empenhoService.save(mapper.toEntity(empDto));
		log.info("AUDIT_EMPENHO_CREATED | empenhoId={} | numero={} | ip={}", empenho.getId(), empenho.getNumero(), request.getRemoteAddr());
		return ResponseEntity.status(201).body(mapper.toDto(empenho));
	}

	@PutMapping
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<EmpenhoDto> update(@Valid @RequestBody EmpenhoDto empenhoDto, HttpServletRequest request) {
		Empenho empenho = empenhoService.update(mapper.toEntity(empenhoDto));
		log.info("AUDIT_EMPENHO_UPDATED | empenhoId={} | numero={} | ip={}", empenho.getId(), empenho.getNumero(), request.getRemoteAddr());
		return ResponseEntity.status(202).body(mapper.toDto(empenho));
	}

	@GetMapping("/{numero}/{date}")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<EmpenhoDto> findByNumeroAndAno(@NotNull @PathVariable Integer numero,
			@Valid @Min(value = 1900, message = "O ano deve ter 4 dígitos e ser maior ou igual a 1900") @Max(value = 2050, message = "O ano não pode ser maior que 2050") @NotNull @PathVariable Integer date) {
		if (numero == null || numero <= 0 || date == null) {
			return ResponseEntity.badRequest().build();
		} else {
			Empenho empenho = empenhoService.findByNumeroAndAno(numero, date);
			return ResponseEntity.status(200).body(mapper.toDto(empenho));
		}
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN')")
	public void deleteById(@NotNull @PathVariable Long id, HttpServletRequest request) {
		empenhoService.delete(id);
		log.warn("AUDIT_EMPENHO_DELETED | empenhoId={} | ip={}", id, request.getRemoteAddr());
	}

}