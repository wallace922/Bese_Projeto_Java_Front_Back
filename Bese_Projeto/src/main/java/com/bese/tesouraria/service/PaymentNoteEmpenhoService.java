package com.bese.tesouraria.service;

import com.bese.tesouraria.dto.PaymentNoteEmpenhoBasicDto;
import com.bese.tesouraria.dto.PaymentNoteVinculacaoDto;
import com.bese.tesouraria.entity.Empenho;
import com.bese.tesouraria.entity.FinancialPlanning;
import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.entity.PaymentNoteEmpenho;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.PaymentNoteEmpenhoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentNoteEmpenhoService {

    private final PaymentNoteEmpenhoRepository paymentNoteEmpenhoRepository;
    private final PaymentNoteService paymentNoteService;
    private final EmpenhoService empenhoService;
    private final FinancialPlanningService financialPlanningService;

    public PaymentNoteEmpenhoService(EmpenhoService empenhoService,
            PaymentNoteEmpenhoRepository paymentNoteEmpenhoRepository,
            PaymentNoteService paymentNoteService,
            FinancialPlanningService financialPlanningService) {
        this.empenhoService = empenhoService;
        this.paymentNoteEmpenhoRepository = paymentNoteEmpenhoRepository;
        this.paymentNoteService = paymentNoteService;
        this.financialPlanningService = financialPlanningService;
    }

    public Page<PaymentNoteEmpenho> findAllFull(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return paymentNoteEmpenhoRepository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional
    public PaymentNoteEmpenho save(PaymentNoteEmpenho paymentNoteEmpenhoFromRequest) {
        validate(paymentNoteEmpenhoFromRequest);

        Empenho empenho = empenhoService.findByNumeroAndAno(
                paymentNoteEmpenhoFromRequest.getEmpenho().getNumero(),
                paymentNoteEmpenhoFromRequest.getEmpenho().getAno());
        PaymentNote paymentNote = paymentNoteService.findByNumeroNpForYear(
                paymentNoteEmpenhoFromRequest.getPaymentNote().getNumeroNp(),
                paymentNoteEmpenhoFromRequest.getPaymentNote().getDataLiquidacao().getYear());

        paymentNoteEmpenhoFromRequest.setEmpenho(empenho);
        paymentNoteEmpenhoFromRequest.setPaymentNote(paymentNote);

        if (paymentNoteEmpenhoFromRequest.getFinancialPlanning() != null
                && paymentNoteEmpenhoFromRequest.getFinancialPlanning().getNumero() != null
                && paymentNoteEmpenhoFromRequest.getFinancialPlanning().getData() != null) {
            Integer numero = paymentNoteEmpenhoFromRequest.getFinancialPlanning().getNumero();
            Integer ano = paymentNoteEmpenhoFromRequest.getFinancialPlanning().getData().getYear();
            FinancialPlanning financialPlanning = financialPlanningService.findByNumeroAndAno(numero, ano);
            paymentNoteEmpenhoFromRequest.setFinancialPlanning(financialPlanning);
        }

        return paymentNoteEmpenhoRepository.save(paymentNoteEmpenhoFromRequest);
    }

    @Transactional
    public PaymentNoteEmpenho update(PaymentNoteEmpenho paymentNoteEmpenhoFromRequest) {
        validate(paymentNoteEmpenhoFromRequest);
        if (paymentNoteEmpenhoFromRequest.getId() == null) {
            throw new BusinessRuleException("O ID da vinculação deve ser informado para a atualização.");
        }

        PaymentNoteEmpenho paymentNoteEmpenhoToUpdate = paymentNoteEmpenhoRepository
                .findById(paymentNoteEmpenhoFromRequest.getId())
                .orElseThrow(() -> new EntityNotFoundException("Vinculação não encontrada."));

        Empenho empenho = empenhoService.findByNumeroAndAno(
                paymentNoteEmpenhoFromRequest.getEmpenho().getNumero(),
                paymentNoteEmpenhoFromRequest.getEmpenho().getAno());
        PaymentNote paymentNote = paymentNoteService.findByNumeroNpForYear(
                paymentNoteEmpenhoFromRequest.getPaymentNote().getNumeroNp(),
                paymentNoteEmpenhoFromRequest.getPaymentNote().getDataLiquidacao().getYear());

        paymentNoteEmpenhoToUpdate.setEmpenho(empenho);
        paymentNoteEmpenhoToUpdate.setPaymentNote(paymentNote);
        paymentNoteEmpenhoToUpdate.setValue(paymentNoteEmpenhoFromRequest.getValue());

        if (paymentNoteEmpenhoFromRequest.getFinancialPlanning() != null
                && paymentNoteEmpenhoFromRequest.getFinancialPlanning().getNumero() != null
                && paymentNoteEmpenhoFromRequest.getFinancialPlanning().getData() != null) {
            Integer numero = paymentNoteEmpenhoFromRequest.getFinancialPlanning().getNumero();
            Integer ano = paymentNoteEmpenhoFromRequest.getFinancialPlanning().getData().getYear();
            FinancialPlanning financialPlanning = financialPlanningService.findByNumeroAndAno(numero, ano);
            paymentNoteEmpenhoToUpdate.setFinancialPlanning(financialPlanning);
        } else {
            paymentNoteEmpenhoToUpdate.setFinancialPlanning(null);
        }

        return paymentNoteEmpenhoRepository.save(paymentNoteEmpenhoToUpdate);
    }

    public void validate(PaymentNoteEmpenho paymentNoteEmpenho) {
        if (paymentNoteEmpenho.getPaymentNote() == null
                || paymentNoteEmpenho.getPaymentNote().getNumeroNp() == null
                || paymentNoteEmpenho.getPaymentNote().getDataLiquidacao() == null) {
            throw new BusinessRuleException(
                    "A Nota de Pagamento e seus detalhes (NumeroNp, DataLiquidacao) não podem ser nulos.");
        }
        if (paymentNoteEmpenho.getEmpenho() == null
                || paymentNoteEmpenho.getEmpenho().getNumero() == null
                || paymentNoteEmpenho.getEmpenho().getAno() == null) {
            throw new BusinessRuleException("O Empenho e seus detalhes (Numero, Ano) não podem ser nulos.");
        }
        if (paymentNoteEmpenho.getValue() == null) {
            throw new BusinessRuleException("O valor da vinculação não pode ser nulo.");
        }
    }

    public void delete(Long id) {
        if (!paymentNoteEmpenhoRepository.existsById(id)) {
            throw new EntityNotFoundException("Associação entre Nota de Pagamento e Empenho não encontrada.");
        }
        paymentNoteEmpenhoRepository.deleteById(id);
    }

    public Page<PaymentNoteVinculacaoDto> findByMesAndAno(Integer mes, Integer ano,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<PaymentNoteEmpenho> resultPage = paymentNoteEmpenhoRepository
                .findByPaymentNoteDatePaymentMonthAndYearAndAllItemsNonOptante(mes, ano, pageable);

        List<PaymentNoteVinculacaoDto> sortedDtos = resultPage.getContent().stream()
                .sorted(Comparator.comparingInt(pne -> pne.getPaymentNote().getItems().stream()
                        .mapToInt(i -> (i.getTax() != null && i.getTax().getCodigoReceita() != null)
                                ? i.getTax().getCodigoReceita()
                                : Integer.MAX_VALUE)
                        .min()
                        .orElse(Integer.MAX_VALUE)))
                .map(pne -> new PaymentNoteVinculacaoDto(
                        pne.getPaymentNote(),
                        pne.getFinancialPlanning().getVinculation()))
                .collect(Collectors.toList());

        return new PageImpl<>(sortedDtos, pageable, resultPage.getTotalElements());
    }

    public Page<PaymentNoteEmpenhoBasicDto> findSemFinancialPlanning(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));

        return paymentNoteEmpenhoRepository
                .findByFinancialPlanningIsNull(pageable)
                .map(PaymentNoteEmpenhoBasicDto::new);
    }
}
