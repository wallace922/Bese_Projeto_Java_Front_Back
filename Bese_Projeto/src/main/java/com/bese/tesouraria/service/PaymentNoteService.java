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
import java.time.LocalDate;
import java.util.ArrayList;
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

        // Cópia porque prepararNota limpa a coleção antes de reprocessar.
        prepararNota(paymentNote, paymentNote, new ArrayList<>(paymentNote.getItems()), false);

        paymentNote.setCreatedBy(Audit.currentUserId());
        paymentNote.setUpdatedBy(Audit.currentUserId());

        return paymentNoteRepository.save(paymentNote);
    }

    @Transactional
    public PaymentNote updateNp(PaymentNote paymentNoteFromRequest, Long pathId) {
        PaymentNote existing = findByNumeroNpForYear(
                paymentNoteFromRequest.getNumeroNp(),
                paymentNoteFromRequest.getDataLiquidacao().getYear());

        // O registro da chave natural (número+ano) precisa ser o mesmo da URL.
        Audit.requireIdMatch(existing.getId(), pathId, "a Nota de Pagamento");

        existing.setDataLiquidacao(paymentNoteFromRequest.getDataLiquidacao());
        existing.setDocOrigin(paymentNoteFromRequest.getDocOrigin());
        existing.setStatus(paymentNoteFromRequest.getStatus());
        existing.setDatePayment(paymentNoteFromRequest.getDatePayment());

        // resetItemIds = true força INSERT dos itens recriados (evita update indevido).
        prepararNota(existing, paymentNoteFromRequest, paymentNoteFromRequest.getItems(), true);

        existing.setUpdatedBy(Audit.currentUserId());

        return paymentNoteRepository.save(existing);
    }

    /**
     * Executa o MESMO pipeline de validação, resolução de empresas e cálculo
     * de impostos usado por saveNp/updateNp, mas:
     *  - não verifica duplicidade de número/ano (a nota ainda não será salva);
     *  - não grava auditoria;
     *  - não persiste a PaymentNote.
     *
     * O objeto retornado é transiente e já contém, em cada Tax, o taxStatus
     * e o snapshot calculatedItems prontos para o frontend.
     */
    @Transactional
    public PaymentNote previewNp(PaymentNote paymentNote) {
        prepararNota(paymentNote, paymentNote, new ArrayList<>(paymentNote.getItems()), false);
        return paymentNote;
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
     * Pipeline único de preparação da nota, compartilhado por criação,
     * atualização e prévia:
     *
     *  1. valida CNPJ e data de pagamento a partir dos dados de origem;
     *  2. resolve a empresa da NP de forma persistente;
     *  3. associa a empresa persistida à nota alvo;
     *  4. limpa a coleção atual e reprocessa os itens recebidos;
     *  5. resolve o beneficiário de cada item;
     *  6. valida duplicidade de grupos e calcula os impostos;
     *  7. soma apenas os valores dos itens no total da nota.
     *
     * @param notaAlvo      entidade alvo que será salva (nova ou existente gerenciada)
     * @param dadosOrigem   objeto de requisição com os dados a serem validados/resolvidos
     * @param itens         itens a processar (cópia, quando vierem da própria nota)
     * @param resetItemIds  true no update, para forçar INSERT dos itens recriados
     */
    private void prepararNota(PaymentNote notaAlvo, PaymentNote dadosOrigem, List<PaymentNoteItem> itens, boolean resetItemIds) {
        validarCnpj(dadosOrigem);
        validarDataPagamento(dadosOrigem);

        Empresa empresa = empresaService.findyByCnpj(dadosOrigem.getEmpresa().getCnpj());
        notaAlvo.setEmpresa(empresa);

        notaAlvo.getItems().clear();

        BigDecimal total = BigDecimal.ZERO;
        for (PaymentNoteItem item : itens) {
            if (resetItemIds) {
                item.setId(null);
            }

            if (item.getValue() != null && item.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Valor de item não pode ser negativo.");
            }

            // Resolve o beneficiário: usa o CNPJ específico do item ou herda a empresa da NP.
            Empresa beneficiaria = (item.getEmpresaBeneficiaria() != null
                    && item.getEmpresaBeneficiaria().getCnpj() != null
                    && !item.getEmpresaBeneficiaria().getCnpj().isBlank())
                    ? empresaService.findyByCnpj(item.getEmpresaBeneficiaria().getCnpj())
                    : empresa;
            item.setEmpresaBeneficiaria(beneficiaria);

            processarImpostosDoItem(item, notaAlvo.getDataLiquidacao());
            item.setPaymentNote(notaAlvo);
            notaAlvo.getItems().add(item);

            // O total é a soma dos VALORES dos itens — nunca dos impostos.
            // Múltiplos grupos de imposto no mesmo item NÃO duplicam o valor.
            total = total.add(item.getValue());
        }
        notaAlvo.setValue(total);
    }

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
    private void processarImpostosDoItem(PaymentNoteItem item, LocalDate dataLiquidacao) {
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
