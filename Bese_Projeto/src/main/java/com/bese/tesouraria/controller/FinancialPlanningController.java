package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.FinancialPlanningBasicDto;
import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.entity.FinancialPlanning;
import com.bese.tesouraria.mapper.FinancialPlanningMapper;
import com.bese.tesouraria.service.FinancialPlanningService;
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
@RequestMapping("/API/FinancialPlanning")
public class FinancialPlanningController {

    private static final Logger log = LoggerFactory.getLogger(FinancialPlanningController.class);

    private final FinancialPlanningService financialPlanningService;
    private final FinancialPlanningMapper mapper;

    public FinancialPlanningController(FinancialPlanningService financialPlanningService,
            FinancialPlanningMapper mapper) {
        this.financialPlanningService = financialPlanningService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageDto<FinancialPlanningBasicDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<FinancialPlanning> plannings = financialPlanningService.findAll(pageable.getPageNumber(),
                pageable.getPageSize());
        Page<FinancialPlanningBasicDto> planningDtos = plannings.map(mapper::toDto);

        return ResponseEntity.ok(PageDto.of(planningDtos));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FinancialPlanningBasicDto> save(
            @Valid @RequestBody FinancialPlanningBasicDto financialPlanningBasicDto,
            HttpServletRequest request) {
        FinancialPlanning financialPlanning = financialPlanningService.save(mapper.toEntity(financialPlanningBasicDto));
        log.info("AUDIT_FINANCIAL_PLANNING_CREATED | planningId={} | numero={} | ip={}", financialPlanning.getId(),
                financialPlanning.getNumero(), request.getRemoteAddr());
        return ResponseEntity.status(201).body(mapper.toDto(financialPlanning));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FinancialPlanningBasicDto> update(
            @Valid @RequestBody FinancialPlanningBasicDto financialPlanningBasicDto,
            HttpServletRequest request) {
        FinancialPlanning financialPlanning = financialPlanningService
                .update(mapper.toEntity(financialPlanningBasicDto));
        log.info("AUDIT_FINANCIAL_PLANNING_UPDATED | planningId={} | numero={} | ip={}", financialPlanning.getId(),
                financialPlanning.getNumero(), request.getRemoteAddr());
        return ResponseEntity.status(202).body(mapper.toDto(financialPlanning));
    }

    @GetMapping("/{numero}/{ano}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FinancialPlanningBasicDto> findByNumeroAndAno(
            @PathVariable Integer numero,
            @PathVariable Integer ano) {
        FinancialPlanning financialPlanning = financialPlanningService.findByNumeroAndAno(numero, ano);
        return ResponseEntity.ok(mapper.toDto(financialPlanning));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteById(@NotNull @PathVariable Long id, HttpServletRequest request) {
        financialPlanningService.delete(id);
        log.warn("AUDIT_FINANCIAL_PLANNING_DELETED | planningId={} | ip={}", id, request.getRemoteAddr());
    }
}