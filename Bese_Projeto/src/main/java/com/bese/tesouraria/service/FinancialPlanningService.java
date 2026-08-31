package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.FinancialPlanning;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.FinancialPlanningRepository;
import jakarta.persistence.EntityExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialPlanningService {

    private final FinancialPlanningRepository planningRepository;

    public FinancialPlanningService(FinancialPlanningRepository planningRepository){
        this.planningRepository = planningRepository;
    }

    public Page<FinancialPlanning> findAll(int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        return planningRepository.findAllByOrderByIdDesc(pageable);
    }

    public FinancialPlanning save(FinancialPlanning financialPlanning){
        if (financialPlanning.getData() == null) {
            throw new BusinessRuleException("A data é obrigatória para determinar o ano.");
        }
        
        if(planningRepository.existsByNumeroAndAno(financialPlanning.getNumero(), financialPlanning.getData().getYear())){
            throw new EntityExistsException("Planejamento financeiro já cadastrado para este número e ano.");
        }

        return planningRepository.save(financialPlanning);
    }

    @Transactional
    public FinancialPlanning update(FinancialPlanning planningFromRequest){
        if (planningFromRequest.getNumero() == null || planningFromRequest.getData() == null) {
            throw new BusinessRuleException("Número e data são obrigatórios para a atualização.");
        }
        
        FinancialPlanning planningToUpdate = findByNumeroAndAno(planningFromRequest.getNumero(), planningFromRequest.getData().getYear());
        
        planningToUpdate.setData(planningFromRequest.getData());
        planningToUpdate.setOrigin(planningFromRequest.getOrigin());
        planningToUpdate.setVinculation(planningFromRequest.getVinculation());

        return planningRepository.save(planningToUpdate);
    }

    public FinancialPlanning findByNumeroAndAno(Integer numero, Integer ano){
        return planningRepository.findByNumeroAndAno(numero, ano)
                .orElseThrow(()-> new EntityNotFoundException(
                "Planejamento financeiro não encontrado: numero=" + numero + ", ano=" + ano) );
    }

    public void delete(Long id){
        planningRepository.deleteById(id);
    }

}