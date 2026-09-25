package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.TaxRuleDto;
import com.bese.tesouraria.dto.TaxRuleUpdateDto;
import com.bese.tesouraria.entity.TaxRule;
import com.bese.tesouraria.mapper.TaxRuleMapper;
import com.bese.tesouraria.service.TaxRuleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/API/TaxRule")
public class TaxRuleController {

    private static final Logger log = LoggerFactory.getLogger(TaxRuleController.class);

    private final TaxRuleService taxRuleService;
    private final TaxRuleMapper taxRuleMapper;

    public TaxRuleController(TaxRuleService taxRuleService, TaxRuleMapper taxRuleMapper) {
        this.taxRuleService = taxRuleService;
        this.taxRuleMapper = taxRuleMapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<TaxRuleDto>> findAll() {
        List<TaxRuleDto> dtos = taxRuleService.findAll().stream()
                .map(taxRuleMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaxRuleDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taxRuleMapper.toDto(taxRuleService.findById(id)));
    }

    /**
     * GET /API/TaxRule/opcoes?codEfd={n}&data={yyyy-MM-dd}
     * Retorna todas as regras de imposto vigentes para um EFD em uma data
     * específica.
     * <ul>
     * <li>0 resultados → nenhuma regra ativa para este EFD/data.</li>
     * <li>1 resultado → o front pode preencher o código de receita
     * automaticamente.</li>
     * <li>N resultados → o front deve exibir um select para o usuário
     * escolher.</li>
     * </ul>
     *
     * @param codEfd O código EFD.
     * @param data   A data de referência (formato ISO: yyyy-MM-dd).
     * @return Lista de TaxRuleDto vigentes.
     */
    @GetMapping("/opcoes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<TaxRuleDto>> findOpcoesPorEfd(
            @RequestParam Integer codEfd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<TaxRuleDto> dtos = taxRuleService.findOpcoesPorEfd(codEfd, data).stream()
                .map(taxRuleMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /API/TaxRule
     * Cria uma nova versão de uma regra de imposto.
     * Se uma regra anterior para o mesmo codEfd estava em aberto (dataFimVigencia =
     * null),
     * ela será encerrada um dia antes do início da nova regra.
     *
     * @param dto DTO da nova regra, incluindo a data de início da vigência.
     * @return A regra criada, com status 201.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaxRuleDto> save(@Valid @RequestBody TaxRuleDto dto, HttpServletRequest request) {
        TaxRule ruleToSave = taxRuleMapper.toEntity(dto);
        TaxRule savedRule = taxRuleService.save(ruleToSave);
        log.info("AUDIT_TAX_RULE_CREATED | ruleId={} | codEfd={} | codigoReceita={} | ip={}", savedRule.getId(), savedRule.getCodEfd(), savedRule.getCodigoReceita(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRuleMapper.toDto(savedRule));
    }

    /**
     * PUT /API/TaxRule/{id}
     * Atualiza os campos editáveis de uma regra existente: {@code description},
     * {@code codigoReceita}, {@code items} e {@code dataFimVigencia}.
     * O {@code codEfd} não é editável e não deve ser enviado neste payload.
     * Para criar uma nova versão com data de início diferente, use o POST.
     *
     * @param id  ID da regra a ser atualizada.
     * @param dto DTO com os novos dados (sem codEfd).
     * @return A regra atualizada.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaxRuleDto> update(@PathVariable Long id, @Valid @RequestBody TaxRuleUpdateDto dto, HttpServletRequest request) {
        TaxRule updatedRule = taxRuleService.update(id, dto);
        log.info("AUDIT_TAX_RULE_UPDATED | ruleId={} | codigoReceita={} | ip={}", id, dto.getCodigoReceita(), request.getRemoteAddr());
        return ResponseEntity.ok(taxRuleMapper.toDto(updatedRule));
    }
}
