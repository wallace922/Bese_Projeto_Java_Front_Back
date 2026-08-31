package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.Empenho;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.EmpenhoRepository;
import jakarta.persistence.EntityExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpenhoService {
	
	private final EmpenhoRepository empRepository;

	public EmpenhoService(EmpenhoRepository empRepository) {
		this.empRepository = empRepository;
	}
	
	public Page<Empenho> findAll(int page, int size){
        Pageable pageable = PageRequest.of(page, size);
		return empRepository.findAllByOrderByIdDesc(pageable);
	}
	
	public Empenho save(Empenho emp) {
		if(empRepository.existsByNumeroAndAno(emp.getNumero(), emp.getAno())){
			throw new EntityExistsException("Empenho já cadastrado com este número e ano.");
		}
        return empRepository.save(emp);
	}

    @Transactional
    public Empenho update(Empenho empenhoFromRequest){
        Empenho empenhoToUpdate = findByNumeroAndAno(empenhoFromRequest.getNumero(), empenhoFromRequest.getAno());

        empenhoToUpdate.setInternalPlan(empenhoFromRequest.getInternalPlan());
        empenhoToUpdate.setNature(empenhoFromRequest.getNature());
        empenhoToUpdate.setFontDeOrigin(empenhoFromRequest.getFontDeOrigin());

		return empRepository.save(empenhoToUpdate);
    }

	public Empenho findByNumeroAndAno(Integer numeroEmpenho, Integer ano) {
		return empRepository.findByNumeroAndAno(numeroEmpenho, ano)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Empenho nº " + numeroEmpenho + " não encontrado para o ano " + ano));
	}

	public Long findByIdNumber(Integer numero, Integer ano){
		Empenho empenho = findByNumeroAndAno(numero, ano);
		return empenho.getId();
	}
	
	public void delete(Long id) {
        empRepository.deleteById(id);
	}

}