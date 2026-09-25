package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.Empresa;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.EmpresaRepository;
import com.bese.tesouraria.repository.PaymentNoteRepository;
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

    public EmpresaService(EmpresaRepository empresaRepository, PaymentNoteRepository paymentNoteRepository) {
        this.empresaRepository = empresaRepository;
        this.paymentNoteRepository = paymentNoteRepository;
    }

    public Page<Empresa> findAll(int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        return empresaRepository.findAllByOrderByIdDesc(pageable);
    }

    public Empresa save(Empresa empresa){
        if(empresa == null || empresa.getCnpj() == null || !empresa.isValidCnpj(empresa.getCnpj())){
             throw new BusinessRuleException("CNPJ inválido ou não fornecido.");
        }
        if(empresaRepository.existsByCnpj(empresa.getCnpj())){
            throw new EntityExistsException("CNPJ já cadastrado.");
        }
        return empresaRepository.save(empresa);
    }

    public Empresa update(Empresa empresa){
        if(empresa == null || empresa.getCnpj() == null || !empresa.isValidCnpj(empresa.getCnpj())){
             throw new BusinessRuleException("CNPJ inválido ou não fornecido.");
        }
        if (!empresaRepository.existsByCnpj(empresa.getCnpj())) {
            throw new EntityNotFoundException("CNPJ não cadastrado.");
        }

        empresa.setId(findyByCnpjId(empresa.getCnpj()));
        return empresaRepository.save(empresa);
    }

    public Long findyByCnpjId(String cnpj){
        Empresa empresa = empresaRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new EntityNotFoundException("Empresa com este CNPJ não encontrada."));
        return empresa.getId();
    }

    public Empresa findyByCnpj(String cnpj){
        return empresaRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new EntityNotFoundException("Empresa com este CNPJ não encontrada."));
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