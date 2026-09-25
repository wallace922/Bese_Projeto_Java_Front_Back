package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.dto.PaymentNoteEmpenhoBasicDto;
import com.bese.tesouraria.dto.PaymentNoteVinculacaoDto;
import com.bese.tesouraria.entity.PaymentNoteEmpenho;
import com.bese.tesouraria.mapper.PaymentNoteEmpenhoMapper;
import com.bese.tesouraria.service.PaymentNoteEmpenhoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/API/PaymentEmpenho")
public class PaymentNoteEmpenhoController {

    private final PaymentNoteEmpenhoService service;
    private final PaymentNoteEmpenhoMapper mapper;

    public PaymentNoteEmpenhoController(PaymentNoteEmpenhoService service,
            PaymentNoteEmpenhoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageDto<PaymentNoteEmpenhoBasicDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PaymentNoteEmpenho> entitiesPage = service.findAllFull(pageable.getPageNumber(), pageable.getPageSize());

        Page<PaymentNoteEmpenhoBasicDto> dtoPage = entitiesPage.map(mapper::toDto);

        return ResponseEntity.ok(PageDto.of(dtoPage));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentNoteEmpenhoBasicDto> save(
            @Valid @RequestBody PaymentNoteEmpenhoBasicDto dto) {
        PaymentNoteEmpenho paymentNoteEmpenho = service.save(mapper.toEntity(dto));
        return ResponseEntity.status(201).body(mapper.toDto(paymentNoteEmpenho));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentNoteEmpenhoBasicDto> update(
            @Valid @RequestBody PaymentNoteEmpenhoBasicDto dto) {
        PaymentNoteEmpenho paymentNoteEmpenho = service.update(mapper.toEntity(dto));

        return ResponseEntity.status(202).body(mapper.toDto(paymentNoteEmpenho));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteById(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/por-mes-ano")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageDto<PaymentNoteVinculacaoDto>> findByMesAndAno(
            @RequestParam @Min(value = 1, message = "Mês deve ser entre 1 e 12") @Max(value = 12, message = "Mês deve ser entre 1 e 12") Integer mes,
            @RequestParam @Min(value = 1900, message = "Ano deve ter 4 dígitos e ser >= 1900") @Max(value = 2050, message = "Ano não pode ser maior que 2050") Integer ano,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PaymentNoteVinculacaoDto> resultado = service.findByMesAndAno(mes, ano,
                pageable.getPageNumber(), pageable.getPageSize());

        return ResponseEntity.ok(PageDto.of(resultado));
    }

    @GetMapping("/sem-planejamento")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageDto<PaymentNoteEmpenhoBasicDto>> findSemFinancialPlanning(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PaymentNoteEmpenhoBasicDto> resultado = service.findSemFinancialPlanning(
                pageable.getPageNumber(), pageable.getPageSize());

        return ResponseEntity.ok(PageDto.of(resultado));
    }
}
