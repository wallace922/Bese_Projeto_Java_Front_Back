package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.EmpresaDto;
import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.entity.Empresa;
import com.bese.tesouraria.mapper.EmpresaMapper;
import com.bese.tesouraria.service.EmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/API/Empresa")
public class EmpresaController {

    private static final Logger log = LoggerFactory.getLogger(EmpresaController.class);

    private final EmpresaService empresaService;
    private final EmpresaMapper mapper;

    public EmpresaController(EmpresaService empresaService, EmpresaMapper mapper) {
        this.empresaService = empresaService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageDto<EmpresaDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Empresa> empresas = empresaService.findAll(pageable.getPageNumber(), pageable.getPageSize());
        Page<EmpresaDto> empresaDtos = empresas.map(mapper::toDto);

        return ResponseEntity.ok(PageDto.of(empresaDtos));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<EmpresaDto> save(@Valid @RequestBody EmpresaDto empresaDto, HttpServletRequest request) {
        Empresa empresa = empresaService.save(mapper.toEntity(empresaDto));
        log.info("AUDIT_EMPRESA_CREATED | empresaId={} | cnpj={} | ip={}", empresa.getId(), empresa.getCnpj(), request.getRemoteAddr());
        return ResponseEntity.status(201).body(mapper.toDto(empresa));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<EmpresaDto> update(@Valid @RequestBody EmpresaDto empresaDto, HttpServletRequest request) {
        Empresa empresa = empresaService.update(mapper.toEntity(empresaDto));
        log.info("AUDIT_EMPRESA_UPDATED | empresaId={} | cnpj={} | ip={}", empresa.getId(), empresa.getCnpj(), request.getRemoteAddr());
        return ResponseEntity.status(202).body(mapper.toDto(empresa));
    }

    @GetMapping("/{Cnpj}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<EmpresaDto> findByCnpj(@Valid @PathVariable String Cnpj) {
        Empresa empresa = empresaService.findyByCnpj(Cnpj);
        return ResponseEntity.status(200).body(mapper.toDto(empresa));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteById(@NotNull @PathVariable Long id, HttpServletRequest request) {
        empresaService.delete(id);
        log.warn("AUDIT_EMPRESA_DELETED | empresaId={} | ip={}", id, request.getRemoteAddr());
    }

}