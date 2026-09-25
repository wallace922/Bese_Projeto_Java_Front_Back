package com.bese.tesouraria.service;

import com.bese.tesouraria.entity.Empresa;
import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.entity.PaymentNoteItem;
import com.bese.tesouraria.entity.Tax;
import com.bese.tesouraria.enun.StatusPaymentNote;
import com.bese.tesouraria.exception.BusinessRuleException;
import com.bese.tesouraria.exception.EntityNotFoundException;
import com.bese.tesouraria.repository.PaymentNoteEmpenhoRepository;
import com.bese.tesouraria.repository.PaymentNoteRepository;
import jakarta.persistence.EntityExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PaymentNoteService {

    private final PaymentNoteRepository paymentNoteRepository;
    private final PaymentNoteEmpenhoRepository paymentNoteEmpenhoRepository;
    private final TaxCalculator taxCalculator;
    private final EmpresaService empresaService;

    public PaymentNoteService(
            PaymentNoteRepository paymentNoteRepository,
            PaymentNoteEmpenhoRepository paymentNoteEmpenhoRepository,
            TaxCalculator taxCalculator,
            EmpresaService empresaService) {
        this.paymentNoteRepository = paymentNoteRepository;
        this.paymentNoteEmpenhoRepository = paymentNoteEmpenhoRepository;
        this.taxCalculator = taxCalculator;
        this.empresaService = empresaService;
    }

    public Page<PaymentNote> findAll(int page, int size) {
        return paymentNoteRepository.findAllByOrderByIdDesc(PageRequest.of(page, size));
    }

    @Transactional
    public PaymentNote saveNp(PaymentNote paymentNote) {
        if (paymentNote.getDataLiquidacao() != null && paymentNoteRepository.existsByNumeroNpAndYear(
                paymentNote.getNumeroNp(),
                paymentNote.getDataLiquidacao().getYear())) {
            throw new EntityExistsException(
                    "PaymentNote já registrada com este número e ano.");
        }

        validarCnpj(paymentNote);
        validarDataPagamento(paymentNote);

        Empresa empresa = empresaService.findyByCnpj(paymentNote.getEmpresa().getCnpj());
        paymentNote.setEmpresa(empresa);

        BigDecimal total = BigDecimal.ZERO;
        for (PaymentNoteItem item : paymentNote.getItems()) {
            // Resolve o beneficiário: usa o CNPJ específico do item ou herda a empresa da NP.
            Empresa beneficiaria = (item.getEmpresaBeneficiaria() != null
                    && item.getEmpresaBeneficiaria().getCnpj() != null)
                    ? empresaService.findyByCnpj(item.getEmpresaBeneficiaria().getCnpj())
                    : empresa;
            item.setEmpresaBeneficiaria(beneficiaria);

            processarImpostosDoItem(item, paymentNote.getDataLiquidacao());
            item.setPaymentNote(paymentNote);

            // O total é a soma dos VALORES dos itens — nunca dos impostos.
            // Múltiplos grupos de imposto no mesmo item NÃO duplicam o valor.
            total = total.add(item.getValue());
        }
        paymentNote.setValue(total);

        return paymentNoteRepository.save(paymentNote);
    }

    @Transactional
    public PaymentNote updateNp(PaymentNote paymentNoteFromRequest) {
        PaymentNote existing = findByNumeroNpForYear(
                paymentNoteFromRequest.getNumeroNp(),
                paymentNoteFromRequest.getDataLiquidacao().getYear());

        validarCnpj(paymentNoteFromRequest);
        validarDataPagamento(paymentNoteFromRequest);

        Empresa empresa = empresaService.findyByCnpj(paymentNoteFromRequest.getEmpresa().getCnpj());
        existing.setEmpresa(empresa);

        existing.setDataLiquidacao(paymentNoteFromRequest.getDataLiquidacao());
        existing.setDocOrigin(paymentNoteFromRequest.getDocOrigin());
        existing.setStatus(paymentNoteFromRequest.getStatus());
        existing.setDatePayment(paymentNoteFromRequest.getDatePayment());

        existing.getItems().clear();

        BigDecimal total = BigDecimal.ZERO;
        for (PaymentNoteItem item : paymentNoteFromRequest.getItems()) {
            item.setId(null);

            // Resolve o beneficiário: usa o CNPJ específico do item ou herda a empresa da NP.
            Empresa beneficiaria = (item.getEmpresaBeneficiaria() != null
                    && item.getEmpresaBeneficiaria().getCnpj() != null)
                    ? empresaService.findyByCnpj(item.getEmpresaBeneficiaria().getCnpj())
                    : empresa;
            item.setEmpresaBeneficiaria(beneficiaria);

            processarImpostosDoItem(item, paymentNoteFromRequest.getDataLiquidacao());
            item.setPaymentNote(existing);
            existing.getItems().add(item);

            total = total.add(item.getValue());
        }
        existing.setValue(total);

        return paymentNoteRepository.save(existing);
    }

    public PaymentNote findByNumeroNpForYear(Integer numeroNp, Integer ano) {
        return paymentNoteRepository.findByNumeroNpForYear(numeroNp, ano)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PaymentNote nº " + numeroNp + " não encontrada para o ano " + ano));
    }

    @Transactional
    public void deleteNp(@NonNull Long id) {
        paymentNoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PaymentNote não encontrada: ID " + id));

        if (paymentNoteEmpenhoRepository.existsByPaymentNoteId(id)) {
            throw new BusinessRuleException("PaymentNote não pode ser excluída: possui vinculação(ões) com Empenho(s).");
        }

        paymentNoteRepository.deleteById(id);
    }

    // ── Métodos privados ───────────────────────────────────────────────────────

    /**
     * Processa todos os grupos de imposto de um item.
     *
     * <p>Para cada grupo ({@link Tax}), chama {@link TaxCalculator#calculate}
     * com o {@code value} do item como base — todos os grupos são calculados
     * sobre o <b>mesmo</b> valor, sem duplicar o total da NP.
     *
     * <p>Valida que o mesmo par {@code (codEfdUsed, codigoReceita)} não se repete
     * no mesmo item (o que indicaria uma duplicação acidental do mesmo imposto).
     * <b>Permitido:</b> mesmo {@code codEfd} com {@code codigoReceita} diferentes.
     */
    private void processarImpostosDoItem(PaymentNoteItem item, java.time.LocalDate dataLiquidacao) {
        List<Tax> taxes = item.getTaxes();

        validarDuplicidadeDeGrupos(taxes);

        for (Tax tax : taxes) {
            boolean isManual = tax.isManualTaxAdjustment();
            Tax calculado = taxCalculator.calculate(tax, item.getValue(), dataLiquidacao, isManual);
            calculado.setPaymentNoteItem(item);
        }
    }

    /**
     * Impede que o mesmo par {@code (codEfdUsed, codigoReceita)} apareça mais de
     * uma vez no mesmo item — o que representaria a duplicação acidental do mesmo
     * imposto.
     *
     * <p>Permitido: mesmo {@code codEfd} com {@code codigoReceita} diferentes
     * (caso de uso válido confirmado no plano).
     */
    private void validarDuplicidadeDeGrupos(List<Tax> taxes) {
        Set<String> paresVistos = new HashSet<>();
        for (Tax tax : taxes) {
            String par = tax.getCodEfdUsed() + ":" + tax.getCodigoReceita();
            if (!paresVistos.add(par)) {
                throw new BusinessRuleException(
                        "O mesmo par (codEfd=" + tax.getCodEfdUsed()
                        + ", codigoReceita=" + tax.getCodigoReceita()
                        + ") aparece mais de uma vez no mesmo item. "
                        + "Remova o grupo duplicado.");
            }
        }
    }

    private void validarCnpj(PaymentNote paymentNote) {
        if (paymentNote.getEmpresa() == null
                || paymentNote.getEmpresa().getCnpj() == null
                || !paymentNote.getEmpresa().isValidCnpj(paymentNote.getEmpresa().getCnpj())) {
            throw new BusinessRuleException("CNPJ inválido ou não informado.");
        }
    }

    private void validarDataPagamento(PaymentNote paymentNote) {
        if (paymentNote.getStatus() == StatusPaymentNote.PAGA && paymentNote.getDatePayment() == null) {
            throw new BusinessRuleException("A data de pagamento é obrigatória quando o status da Nota de Pagamento é PAGA.");
        }
    }
}
