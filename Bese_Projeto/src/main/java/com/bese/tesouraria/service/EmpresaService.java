package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.Empresa;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.EmpresaRepository;
import com.bese.tesouraria.repository.PaymentNoteRepository;
import com.bese.tesouraria.security.CryptoService;
import jakarta.persistence.EntityExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PaymentNoteRepository paymentNoteRepository;
    private final CryptoService crypto;

    public EmpresaService(EmpresaRepository empresaRepository, PaymentNoteRepository paymentNoteRepository,
            CryptoService crypto) {
        this.empresaRepository = empresaRepository;
        this.paymentNoteRepository = paymentNoteRepository;
        this.crypto = crypto;
    }

    public Page<Empresa> findAll(int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        return empresaRepository.findAllByOrderByIdDesc(pageable);
    }

    public Empresa save(Empresa empresa){
        if(empresa == null || empresa.getCnpj() == null || !empresa.isValidCnpj(empresa.getCnpj())){
             throw new BusinessRuleException("CNPJ inválido ou não fornecido.");
        }
        String digits = CryptoService.digits(empresa.getCnpj());
        String hash = crypto.hmacHex(digits);
        if (existsAny(empresa.getCnpj(), digits, hash)) {
            throw new EntityExistsException("CNPJ já cadastrado.");
        }
        empresa.setCnpjHash(hash);
        empresa.setCreatedBy(Audit.currentUserId());
        empresa.setUpdatedBy(Audit.currentUserId());
        return empresaRepository.save(empresa);
    }

    public Empresa update(Empresa empresa, Long pathId){
        if(empresa == null || empresa.getCnpj() == null || !empresa.isValidCnpj(empresa.getCnpj())){
             throw new BusinessRuleException("CNPJ inválido ou não fornecido.");
        }
        String digits = CryptoService.digits(empresa.getCnpj());
        if (!existsAny(empresa.getCnpj(), digits, crypto.hmacHex(digits))) {
            throw new EntityNotFoundException("CNPJ não cadastrado.");
        }

        Long resolvedId = findyByCnpjId(empresa.getCnpj());
        Audit.requireIdMatch(resolvedId, pathId, "a Empresa");
        empresa.setId(resolvedId);
        empresa.setCnpjHash(crypto.hmacHex(CryptoService.digits(empresa.getCnpj())));
        empresa.setUpdatedBy(Audit.currentUserId());
        return empresaRepository.save(empresa);
    }

    public Long findyByCnpjId(String cnpj){
        return findyByCnpj(cnpj).getId();
    }

    // Busca por índice cego (HMAC); com fallback para linha legada em claro,
    // que é migrada (cifrada + hash) no primeiro uso.
    @Transactional
    public Empresa findyByCnpj(String cnpj){
        String digits = CryptoService.digits(cnpj);
        String hash = digits == null ? null : crypto.hmacHex(digits);
        if (hash != null) {
            var hit = empresaRepository.findByCnpjHash(hash);
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        Empresa legacy = empresaRepository.findByCnpjLegacy(cnpj)
                .or(() -> digits == null ? java.util.Optional.empty() : empresaRepository.findByCnpjLegacy(digits))
                .orElseThrow(() -> new EntityNotFoundException("Empresa com este CNPJ não encontrada."));
        if (hash != null) {
            legacy.setCnpjHash(hash);
            return empresaRepository.save(legacy);
        }
        return legacy;
    }

    // Existe por hash OU em linha legada (clara, com ou sem formatação).
    private boolean existsAny(String cnpj, String digits, String hash) {
        if (hash != null && empresaRepository.existsByCnpjHash(hash)) {
            return true;
        }
        if (cnpj != null && empresaRepository.findByCnpjLegacy(cnpj).isPresent()) {
            return true;
        }
        return digits != null && empresaRepository.findByCnpjLegacy(digits).isPresent();
    }

    public Empresa findById(Long id){
        return empresaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: ID " + id));
    }

    @Transactional
    public void delete(Long id){
        empresaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: ID " + id));

        if (paymentNoteRepository.existsByEmpresaId(id)) {
            throw new BusinessRuleException("Empresa não pode ser excluída: possui Nota(s) de Pagamento associada(s).");
        }
        if (paymentNoteRepository.existsByEmpresaBeneficiariaId(id)) {
            throw new BusinessRuleException("Empresa não pode ser excluída: é beneficiária de Nota(s) de Pagamento.");
        }

        empresaRepository.deleteById(id);
    }
}