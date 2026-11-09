# Plano — Prévia de Impostos da PaymentNote

> Objetivo: permitir que o usuário veja o resultado do cálculo de impostos **antes de salvar** a PaymentNote, possa ajustar valores manualmente e só então cadastrar.
>
> Princípio central: **um único pipeline de cálculo**, reaproveitado por criação, atualização e prévia. Nada de duplicar regra de imposto.

---

## 1. Ideia do plano

Hoje o cálculo de impostos só acontece no `POST /API/Np` (salvar) e no `PUT /API/Np/{id}` (atualizar). No frontend, ao criar uma nota nova, o painel de ajuste manual (`TaxAdjustmentPanel`) nem aparece, porque ele depende de `backendItems`, que só é preenchido quando a nota já veio calculada do backend (fluxo de edição).

A ideia é criar um endpoint de **prévia**:

```
POST /API/Np/preview
```

Ele recebe o mesmo payload do salvamento, executa **todas as etapas de cálculo** (validações, resolução de empresa/beneficiário, busca de regra vigente, snapshot dos impostos, soma dos itens), mas **não persiste nada**. Devolve a nota "calculada em memória" no mesmo formato de resposta do `POST /API/Np`.

No frontend:

1. O usuário preenche os dados da NP, itens e tributação.
2. Clica em **"Prévia dos Impostos"**.
3. O resultado é aplicado ao estado dos itens.
4. O `TaxAdjustmentPanel` aparece com os impostos calculados e permite ajuste manual.
5. O usuário decide: salvar como está ou ajustar.
6. Ao salvar, o fluxo atual continua funcionando (com ou sem `manualAdjustment`).

### O que muda

- **Backend**
  - Extrair do `PaymentNoteService` um método privado único `prepararNota(...)`, usado por `saveNp`, `updateNp` e `previewNp`.
  - Adicionar `previewNp(...)` (sem duplicidade de número/ano, sem auditoria, sem `save`).
  - Adicionar `POST /API/Np/preview` no `PaymentNoteController`.
- **Frontend**
  - Extrair a montagem do payload em `buildPaymentNotePayload(dto)` no `api.ts` (hoje está duplicada em `savePaymentNote` e `updatePaymentNote`).
  - Adicionar `previewPaymentNote(dto)`.
  - Adicionar `applyPreviewToItems(...)` no `NpItemEditor.tsx`.
  - Adicionar o botão "Prévia dos Impostos" e o handler no `Cadastro.tsx`.
  - Extrair a validação do formulário em `validarFormulario()` (hoje está dentro de `openConfirm`).
- **Banco**
  - Nenhuma migration. A prévia não grava nada.

### O que NÃO muda

- Regras de cálculo em `TaxCalculatorService` (OPTANTE, NAO_OPTANTE, PENDING, ajuste manual, múltiplos `codigoReceita`).
- Formato do payload de salvamento.
- Entidades `PaymentNote`, `PaymentNoteItem`, `Tax`, `TaxRule`.
- Migrations existentes.

---

## 2. Caminho que será feito

### 2.1 Fluxo completo (depois da mudança)

```
USUÁRIO
  │
  ├─ preenche Nº NP, Data, Doc. Origem, Empresa, Itens e Tributação
  │
  ├─ clica "Prévia dos Impostos"
  │     │
  │     └─ Cadastro.tsx
  │           handlePreview()
  │             ├─ validarFormulario()
  │             ├─ buildDto()
  │             └─ previewPaymentNote(dto)          [api.ts]
  │                   └─ POST /API/Np/preview
  │                         └─ PaymentNoteController.preview()
  │                               ├─ PaymentNoteMapper.toEntity(dto)     (DTO → entidade)
  │                               └─ PaymentNoteService.previewNp(nota)
  │                                     └─ prepararNota(nota, itens, false)
  │                                           ├─ validarCnpj
  │                                           ├─ validarDataPagamento
  │                                           ├─ resolve empresa da NP
  │                                           ├─ para cada item:
  │                                           │     ├─ valida valor negativo
  │                                           │     ├─ resolve beneficiário por CNPJ
  │                                           │     ├─ valida grupos duplicados
  │                                           │     └─ TaxCalculatorService.calculate(...)
  │                                           │           ├─ manual → mantém calculatedItems
  │                                           │           ├─ OPTANTE → EXEMPT
  │                                           │           ├─ NAO_OPTANTE sem codEfd → PENDING
  │                                           │           └─ NAO_OPTANTE com codEfd → busca regra
  │                                           │                 e gera snapshot
  │                                           └─ soma valores dos itens
  │                               └─ PaymentNoteMapper.toDto(nota)       (entidade → DTO)
  │                                     └─ 200 OK com calculatedItems, taxStatus, codigoReceita
  │
  ├─ applyPreviewToItems(...)  → preenche backendItems/manualItems dos grupos
  │
  ├─ TaxAdjustmentPanel aparece (impostos calculados + ajuste manual)
  │
  ├─ usuário ajusta (opcional) → manualItems + isManualAdjustment = true
  │
  └─ clica "Cadastrar Payment Note"
        └─ POST /API/Np (fluxo atual, sem alteração de comportamento)
```

### 2.2 Mapa do projeto (arquivos envolvidos)

| Camada | Arquivo | Papel | Muda? |
|---|---|---|---|
| Front | `Front_Bese_Projeto/src/pages/Cadastro.tsx` | Formulário da NP, validações e submit | **Sim** |
| Front | `Front_Bese_Projeto/src/components/NpItemEditor.tsx` | Estado dos itens/grupos e conversão para DTO | **Sim** |
| Front | `Front_Bese_Projeto/src/components/TaxAdjustmentPanel.tsx` | Exibição e edição manual dos impostos | Não |
| Front | `Front_Bese_Projeto/src/components/TaxItemsDisplay.tsx` | Exibição read-only dos impostos | Não (opcional) |
| Front | `Front_Bese_Projeto/src/services/api.ts` | Chamadas HTTP e serialização | **Sim** |
| Front | `Front_Bese_Projeto/src/types/index.ts` | Tipos `PaymentNoteDto`, `TaxDto`, etc. | Não |
| Back | `Bese_Projeto/src/main/java/.../controller/PaymentNoteController.java` | Endpoints REST | **Sim** |
| Back | `.../service/PaymentNoteService.java` | Validações, resolução, cálculo e persistência | **Sim (refactor)** |
| Back | `.../service/TaxCalculatorService.java` | Regras de cálculo | Não |
| Back | `.../service/TaxCalculator.java` | Interface do calculador | Não |
| Back | `.../mapper/PaymentNoteMapper.java` | DTO ↔ entidade | Não |
| Back | `.../mapper/PaymentNoteItemMapper.java` | Item DTO ↔ entidade | Não |
| Back | `.../mapper/TaxMapper.java` | Grupo de imposto e compatibilidade legada | Não |
| Back | `.../repository/TaxRuleRepository.java` | Regras vigentes por EFD + data | Não |
| Back | `.../entity/PaymentNote.java` | Nota, itens, validações de data/status | Não |
| Back | `.../entity/PaymentNoteItem.java` | Item, lista `taxes`, beneficiário | Não |
| Back | `.../entity/Tax.java` | Grupo, `taxStatus`, snapshot JSON | Não |
| Back | `.../entity/TaxRule.java` | Regra versionada por EFD/receita/data | Não |
| Back | `.../entity/TaxCalculatedItem.java` | Item do snapshot (tipo, alíquota, valor) | Não |
| Back | `.../exception/GlobalExceptionHandler.java` | Mapeamento de erros para HTTP | Não |
| Banco | `src/main/resources/db/migration/*` | Migrations Flyway | Não |

### 2.3 Diagnóstico do fluxo atual (o que motiva a mudança)

1. `PaymentNoteService.saveNp` já faz tudo o que a prévia precisa:
   - valida CNPJ e data de pagamento;
   - resolve a empresa da NP;
   - resolve o beneficiário de cada item;
   - valida duplicidade de grupos;
   - chama `taxCalculator.calculate(...)`;
   - soma os valores dos itens.
2. O único ponto que impede o reaproveitamento hoje é que esse código está **dentro** de `saveNp`, junto com:
   - verificação de duplicidade de número/ano;
   - auditoria (`createdBy`/`updatedBy`);
   - `paymentNoteRepository.save(...)`.
3. No frontend, `TaxAdjustmentPanel` só é renderizado quando `group.backendItems.length > 0`. Em nota nova, esse array começa vazio, então o painel nunca aparece antes do salvamento.
4. `savePaymentNote` e `updatePaymentNote` no `api.ts` repetem a montagem do payload.
5. `openConfirm` no `Cadastro.tsx` concentra todas as validações que a prévia também precisa.

A mudança ataca exatamente esses pontos, sem reescrever regra de imposto.

---

## 3. Decisões de arquitetura

### 3.1 Reutilizar `PaymentNoteBasicDto` como request e response da prévia

- **Request**: o mesmo DTO do salvamento. Isso evita criar um DTO paralelo com os mesmos campos.
- **Response**: o próprio `PaymentNoteBasicDto`, que já expõe:
  - `items[].taxes[]` com `calculatedItems`, `taxStatus`, `codigoReceita`, `taxRuleDescription`;
  - `items[].tax` (legado, primeiro grupo);
  - `value` total da nota.
- **Consequência**: o preview usa `@Valid`, ou seja, exige os campos obrigatórios do formulário (Nº NP, data, empresa, doc. origem, status, itens). O botão de prévia só será habilitado quando o formulário estiver minimamente preenchido. Isso é coerente com o fluxo pedido ("depois que o usuário pôr os dados de impostos").

> Alternativa: criar um `PaymentNotePreviewDto` sem os campos obrigatórios do cabeçalho. Não recomendado agora, pois duplicaria a estrutura e o mapper. Só vale a pena se a prévia precisar funcionar antes de o usuário preencher Nº NP/Doc. Origem.

### 3.2 Extrair `prepararNota(...)` no service

Um único método privado concentra o pipeline:

```java
private void prepararNota(PaymentNote nota, List<PaymentNoteItem> itens, boolean resetItemIds)
```

- `saveNp`: `prepararNota(nota, cópia dos itens, false)` + auditoria + `save`.
- `updateNp`: resolve existente, copia campos, `prepararNota(existing, itensDoRequest, true)` + auditoria + `save`.
- `previewNp`: `prepararNota(nota, cópia dos itens, false)` e retorna `nota` sem salvar.

A cópia (`new ArrayList<>(nota.getItems())`) é necessária porque `prepararNota` limpa `nota.getItems()` antes de reprocessar.

### 3.3 A prévia não valida duplicidade de número/ano

A nota ainda não será salva. Se validasse, uma prévia de edição de uma nota existente falharia (o próprio número já existe). A duplicidade continua sendo validada no `saveNp`.

### 3.4 A prévia não grava auditoria

`createdBy`/`updatedBy` são preenchidos apenas no salvamento. A prévia é somente leitura do ponto de vista da nota.

### 3.5 Extrair `buildPaymentNotePayload` no frontend

Hoje `savePaymentNote` e `updatePaymentNote` repetem a montagem do payload. A prévia precisa da mesma montagem. Extrair uma função elimina a terceira cópia.

### 3.6 Reutilizar o `TaxAdjustmentPanel`

Ele já:
- recebe a lista de impostos calculados;
- permite ativar o modo manual;
- emite `onChange(items, manualAdjustment)`;
- detecta divergência de arredondamento (DARF vs soma das partes);
- permite "Restaurar Automático".

Depois da prévia, basta preencher `backendItems` e o painel aparece. Nenhuma alteração é necessária nele.

### 3.7 Pareamento por índice

O backend devolve os itens na mesma ordem enviada e, dentro de cada item, os grupos na mesma ordem (o mapper cria na ordem do payload e o service itera na ordem). Por isso `applyPreviewToItems` pareia por índice. É simples e determinístico para este fluxo.

### 3.8 Prévia sempre "fresca" ao mudar input que afeta o cálculo

Se o usuário mudar `value`, `tipo`, `codEfd`, `codigoReceita` ou a data de liquidação depois de uma prévia, o resultado fica desatualizado. A solução adotada é **limpar a prévia** desses grupos nesses eventos:

- `value` do item → limpa todos os grupos do item;
- `tipo`/`codEfd`/`codigoReceita` → limpa o grupo;
- data de liquidação → limpa todos os itens.

Isso evita mostrar imposto calculado sobre uma base antiga.

---

## 4. Contrato da API

### 4.1 Endpoint

```
POST /API/Np/preview
Authorization: sessão atual (USER ou ADMIN)
Content-Type: application/json
```

### 4.2 Request (mesmo do `POST /API/Np`)

```json
{
  "numeroNp": 2026001,
  "dataLiquidacao": "09/11/2026",
  "empresa": { "cnpj": "12345678000199" },
  "docOrigin": "NF-123",
  "status": "A_PAGAR",
  "items": [
    {
      "description": "Serviço de manutenção",
      "value": 1000.00,
      "taxes": [
        {
          "tipo": "NAO_OPTANTE",
          "codEfd": 1708,
          "codigoReceita": 5952
        }
      ]
    }
  ]
}
```

### 4.3 Response `200 OK`

```json
{
  "id": null,
  "numeroNp": 2026001,
  "dataLiquidacao": "09/11/2026",
  "empresa": { "nome": "Empresa X", "cnpj": "12345678000199" },
  "docOrigin": "NF-123",
  "value": 1000.00,
  "status": "A_PAGAR",
  "items": [
    {
      "description": "Serviço de manutenção",
      "value": 1000.00,
      "tax": {
        "tipo": "NAO_OPTANTE",
        "codEfd": 1708,
        "codigoReceita": 5952,
        "taxRuleDescription": "IR 1,5% + CSLL 1%",
        "taxStatus": "CALCULATED",
        "calculatedItems": [
          { "taxType": "IR", "rate": 0.015, "amount": 15.00 },
          { "taxType": "CSLL", "rate": 0.01, "amount": 10.00 }
        ]
      },
      "taxes": [
        {
          "tipo": "NAO_OPTANTE",
          "codEfd": 1708,
          "codigoReceita": 5952,
          "taxRuleDescription": "IR 1,5% + CSLL 1%",
          "taxStatus": "CALCULATED",
          "calculatedItems": [
            { "taxType": "IR", "rate": 0.015, "amount": 15.00 },
            { "taxType": "CSLL", "rate": 0.01, "amount": 10.00 }
          ]
        }
      ],
      "empresaBeneficiaria": {
        "nome": "Empresa X",
        "cnpj": "12345678000199"
      }
    }
  ]
}
```

Observações:

- `id` continua `null` (nada foi salvo).
- `value` da nota é a soma dos `value` dos itens, nunca dos impostos.
- `taxes[]` traz o snapshot por grupo.
- `tax` (singular) é o primeiro grupo, por compatibilidade.
- `manualAdjustment` não é persistido e não aparece no `TaxDto` de resposta. O frontend preserva o estado manual localmente.

### 4.4 Erros

| Situação | HTTP | Origem |
|---|---|---|
| Campos inválidos (`@Valid`) | `400` | `MethodArgumentNotValidException` |
| CNPJ inválido/ausente | `400` | `BusinessRuleException` |
| Data de pagamento ausente com status PAGA | `400` | `BusinessRuleException` |
| Valor de item negativo | `400` | `BusinessRuleException` |
| Grupo duplicado no mesmo item | `400` | `BusinessRuleException` |
| Nenhuma regra vigente para o `codEfd`/data | `400` | `BusinessRuleException` |
| Múltiplos `codigoReceita` sem seleção | `400` | `BusinessRuleException` |
| `codigoReceita` inválido para o EFD/data | `400` | `BusinessRuleException` |
| Empresa (NP ou beneficiária) não encontrada | `404` | `EntityNotFoundException` |

O `GlobalExceptionHandler` já cobre todos esses casos; **nenhuma alteração** nele é necessária.

---

## 5. Etapas de implementação

> Sugestão de ordem: backend primeiro (etapas 1–2), depois frontend (3–5). Cada etapa é independente e testável.

### Etapa 1 — Refatorar `PaymentNoteService` para extrair o pipeline

**O que fazer**

1. Criar `private void prepararNota(PaymentNote nota, List<PaymentNoteItem> itens, boolean resetItemIds)`.
2. Mover para ele:
   - `validarCnpj(nota)`;
   - `validarDataPagamento(nota)`;
   - resolução da empresa da NP;
   - limpeza de `nota.getItems()`;
   - loop dos itens (validação de valor, resolução de beneficiário, cálculo de impostos, vínculo com a nota, soma).
3. Ajustar `saveNp` para usar `prepararNota(paymentNote, new ArrayList<>(paymentNote.getItems()), false)`.
4. Ajustar `updateNp` para copiar os campos e chamar `prepararNota(existing, paymentNoteFromRequest.getItems(), true)`.
5. Adicionar `previewNp(PaymentNote)` chamando `prepararNota(paymentNote, new ArrayList<>(paymentNote.getItems()), false)` e retornando a nota.
6. Adicionar import de `java.util.ArrayList`.

**Critério de aceite**

- `saveNp` e `updateNp` mantêm o comportamento atual (mesmos erros, mesmo total, mesmos itens).
- `previewNp` retorna a nota com `Tax.taxStatus` e `Tax.calculatedItems` preenchidos, sem `save`.
- Nenhuma regra de `TaxCalculatorService` foi alterada.

**Teste rápido**

- Unitário: `previewNp` com item de R$ 1.000,00 e regra IR 1,5% + CSLL 1% retorna dois `calculatedItems` (15,00 e 10,00) e `taxStatus = CALCULATED`.
- Unitário: `previewNp` com `OPTANTE` retorna `EXEMPT` e `calculatedItems = null`.
- Unitário: `previewNp` com `NAO_OPTANTE` sem `codEfd` retorna `PENDING`.
- Unitário: `previewNp` com `NAO_OPTANTE` e `codEfd` sem regra lança `BusinessRuleException`.

### Etapa 2 — Adicionar `POST /API/Np/preview` no controller

**O que fazer**

1. Adicionar o método `preview(...)` no `PaymentNoteController`.
2. Reutilizar `mapper.toEntity`, `service.previewNp` e `mapper.toDto`.
3. Retornar `200 OK`.

**Critério de aceite**

- `POST /API/Np/preview` com payload válido retorna `200` e o JSON calculado.
- Payload inválido retorna `400` com a mensagem do `GlobalExceptionHandler`.
- Nenhum registro é criado no banco (verificar contagem de `payment_note` antes/depois).
- A rota não conflita com `GET /API/Np/{numero}/{date}` nem com `POST /API/Np`.

**Teste rápido**

```bash
curl -i -X POST http://localhost:8080/API/Np/preview \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "numeroNp": 2026001,
    "dataLiquidacao": "09/11/2026",
    "empresa": { "cnpj": "12345678000199" },
    "docOrigin": "NF-123",
    "status": "A_PAGAR",
    "items": [{
      "description": "Serviço",
      "value": 1000.00,
      "taxes": [{ "tipo": "NAO_OPTANTE", "codEfd": 1708, "codigoReceita": 5952 }]
    }]
  }'
```

### Etapa 3 — Refatorar `api.ts` e adicionar `previewPaymentNote`

**O que fazer**

1. Criar `buildPaymentNotePayload(dto, includeId = false)`.
2. Usar em `savePaymentNote`, `updatePaymentNote` e `previewPaymentNote`.
3. Adicionar `previewPaymentNote(dto)` chamando `POST /API/Np/preview`.
4. Exportar a nova função.

**Critério de aceite**

- `savePaymentNote` e `updatePaymentNote` continuam enviando exatamente o mesmo payload de antes.
- `previewPaymentNote` usa a mesma serialização.
- Nenhuma duplicação de montagem de payload.

### Etapa 4 — Adicionar `applyPreviewToItems` no `NpItemEditor.tsx`

**O que fazer**

1. Criar a função exportada `applyPreviewToItems(current, previewItems)`.
2. Ela pareia os itens por índice e os grupos por índice.
3. Em modo automático: preenche `backendItems` e `manualItems` com `calculatedItems`.
4. Em modo manual: preserva o baseline automático e confirma os `manualItems`.
5. Criar o helper `semPrevia(group)` para limpar a prévia de um grupo.
6. Usar `semPrevia` em:
   - mudança de `value` do item (todos os grupos);
   - mudança de `tipo`, `codEfd` ou `codigoReceita` do grupo.

**Critério de aceite**

- Após a prévia, `backendItems.length > 0` e o `TaxAdjustmentPanel` aparece.
- Após editar manualmente, `isManualAdjustment = true` e `manualItems` contém os valores editados.
- Após "Restaurar Automático", o painel volta ao baseline calculado.
- Ao mudar o valor do item ou o `codEfd`, a prévia daquele grupo some (evita dado velho).

### Etapa 5 — Adicionar o botão e o handler no `Cadastro.tsx`

**O que fazer**

1. Adicionar estados `previewLoading` e `previewError`.
2. Importar `previewPaymentNote` e `applyPreviewToItems`.
3. Extrair `validarFormulario()` com as validações que hoje estão em `openConfirm`.
4. Ajustar `openConfirm` para usar `validarFormulario()`.
5. Criar `buildDto()` para montar o `PaymentNoteDto`.
6. Criar `handlePreview()`:
   - valida;
   - chama `previewPaymentNote(buildDto())`;
   - em caso de sucesso, `setNpItems(prev => applyPreviewToItems(prev, result.data.items))`;
   - em caso de erro, exibe `previewError`.
7. Adicionar o botão "Prévia dos Impostos" ao lado de "Cadastrar Payment Note".
8. Exibir `previewError` com `Alert`.
9. Opcional: limpar as prévias quando a data de liquidação mudar.

**Critério de aceite**

- O botão calcula e exibe os impostos sem salvar.
- Erros de regra (ex.: EFD sem regra) aparecem para o usuário.
- Depois da prévia, é possível ajustar manualmente e salvar.
- O salvamento continua usando o fluxo atual.

### Etapa 6 — (Opcional) Resumo consolidado da prévia

**O que fazer**

- Exibir, abaixo dos itens ou perto do botão, um resumo:
  - total dos itens;
  - total de impostos por item;
  - total geral retido.
- Pode ser computado no frontend a partir de `npItems` (sem novo endpoint) ou incluído no response.

**Critério de aceite**

- O resumo reflete exatamente os `calculatedItems` retornados.
- Não duplica a lógica de cálculo (apenas soma valores já calculados).

### Etapa 7 — Testes e validação final

**Backend**

- [ ] `POST /API/Np/preview` não cria registro.
- [ ] Regra automática com 1 código de receita calcula sem pedir seleção.
- [ ] Regra com múltiplos códigos exige `codigoReceita`.
- [ ] `OPTANTE` retorna `EXEMPT`.
- [ ] `NAO_OPTANTE` sem `codEfd` retorna `PENDING`.
- [ ] Ajuste manual mantém os `calculatedItems` enviados.
- [ ] Item com múltiplos grupos usa o mesmo `value` para todos.
- [ ] Total da nota = soma dos valores dos itens.
- [ ] Erros retornam os status esperados.

**Frontend**

- [ ] Botão "Prévia dos Impostos" chama o endpoint correto.
- [ ] Painel de ajuste aparece após a prévia.
- [ ] Ajuste manual é enviado no save.
- [ ] Mudança de valor/codEfd limpa a prévia.
- [ ] Erros são exibidos sem quebrar o formulário.
- [ ] `savePaymentNote` e `updatePaymentNote` continuam funcionando.

---

## 6. Código

> Os trechos abaixo assumem que os arquivos atuais serão editados. Onde o arquivo é grande, mostro apenas o bloco relevante.

### 6.1 Backend — `PaymentNoteService.java` (arquivo completo refatorado)

```java
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
        prepararNota(paymentNote, new ArrayList<>(paymentNote.getItems()), false);

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
        existing.setEmpresa(paymentNoteFromRequest.getEmpresa());

        // resetItemIds = true força INSERT dos itens recriados (evita update indevido).
        prepararNota(existing, paymentNoteFromRequest.getItems(), true);

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
        prepararNota(paymentNote, new ArrayList<>(paymentNote.getItems()), false);
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

    // ── Pipeline compartilhado ─────────────────────────────────────────────────

    /**
     * Pipeline único de preparação da nota, compartilhado por criação,
     * atualização e prévia:
     *
     *  1. valida CNPJ e data de pagamento;
     *  2. resolve a empresa da NP;
     *  3. limpa a coleção atual e reprocessa os itens recebidos;
     *  4. resolve o beneficiário de cada item;
     *  5. valida duplicidade de grupos e calcula os impostos;
     *  6. soma apenas os valores dos itens no total da nota.
     *
     * @param nota          entidade alvo (nova ou existente)
     * @param itens         itens a processar (cópia, quando vierem da própria nota)
     * @param resetItemIds  true no update, para forçar INSERT dos itens recriados
     */
    private void prepararNota(PaymentNote nota, List<PaymentNoteItem> itens, boolean resetItemIds) {
        validarCnpj(nota);
        validarDataPagamento(nota);

        Empresa empresa = empresaService.findyByCnpj(nota.getEmpresa().getCnpj());
        nota.setEmpresa(empresa);

        nota.getItems().clear();

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
                    && item.getEmpresaBeneficiaria().getCnpj() != null)
                    ? empresaService.findyByCnpj(item.getEmpresaBeneficiaria().getCnpj())
                    : empresa;
            item.setEmpresaBeneficiaria(beneficiaria);

            processarImpostosDoItem(item, nota.getDataLiquidacao());
            item.setPaymentNote(nota);
            nota.getItems().add(item);

            // O total é a soma dos VALORES dos itens — nunca dos impostos.
            // Múltiplos grupos de imposto no mesmo item NÃO duplicam o valor.
            total = total.add(item.getValue());
        }
        nota.setValue(total);
    }

    /**
     * Processa todos os grupos de imposto de um item.
     *
     * <p>Para cada grupo ({@link Tax}), chama {@link TaxCalculator#calculate}
     * com o {@code value} do item como base — todos os grupos são calculados
     * sobre o <b>mesmo</b> valor, sem duplicar o total da NP.
     *
     * <p>Valida que o mesmo par {@code (codEfdUsed, codigoReceita)} não se repete
     * no mesmo item.
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
     * uma vez no mesmo item.
     *
     * <p>Permitido: mesmo {@code codEfd} com {@code codigoReceita} diferentes.
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
```

> **Atenção ao `updateNp`**: o método original já fazia `existing.getItems().clear()` e recriava os itens. O refactor mantém esse comportamento, apenas concentra a lógica em `prepararNota` e garante `item.setId(null)` via `resetItemIds`.

### 6.2 Backend — `PaymentNoteController.java` (arquivo completo)

```java
package com.bese.tesouraria.controller;

import com.bese.tesouraria.dto.PageDto;
import com.bese.tesouraria.entity.PaymentNote;
import com.bese.tesouraria.mapper.PaymentNoteMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import com.bese.tesouraria.dto.PaymentNoteBasicDto;
import com.bese.tesouraria.service.PaymentNoteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/API/Np")
public class PaymentNoteController {

    private static final Logger log = LoggerFactory.getLogger(PaymentNoteController.class);

    private final PaymentNoteService service;
    private final PaymentNoteMapper mapper;

    public PaymentNoteController(PaymentNoteService service, PaymentNoteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PageDto<PaymentNoteBasicDto>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PaymentNote> notes = service.findAll(pageable.getPageNumber(), pageable.getPageSize());
        Page<PaymentNoteBasicDto> noteDtos = notes.map(mapper::toDto);

        return ResponseEntity.ok(PageDto.of(noteDtos));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentNoteBasicDto> save(@Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto,
            HttpServletRequest request) {
        PaymentNote paymentNoteToSave = mapper.toEntity(paymentNoteBasicDto);
        PaymentNote savedPaymentNote = service.saveNp(paymentNoteToSave);
        log.info("AUDIT_NP_CREATED | npId={} | npNumber={} | value={} | ip={}", savedPaymentNote.getId(),
                savedPaymentNote.getNumeroNp(), savedPaymentNote.getValue(), request.getRemoteAddr());
        return ResponseEntity.status(201).body(mapper.toDto(savedPaymentNote));
    }

    /**
     * Prévia de impostos: executa o mesmo pipeline do save, sem persistir.
     *
     * <p>Usa o mesmo DTO de entrada e o mesmo DTO de saída do POST /API/Np,
     * para não duplicar contrato. A resposta já traz, em cada item, a lista
     * {@code taxes} com {@code calculatedItems}, {@code taxStatus},
     * {@code codigoReceita} e {@code taxRuleDescription}.
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentNoteBasicDto> preview(
            @Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto) {
        PaymentNote paymentNote = mapper.toEntity(paymentNoteBasicDto);
        PaymentNote preview = service.previewNp(paymentNote);
        return ResponseEntity.ok(mapper.toDto(preview));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentNoteBasicDto> update(@NotNull @PathVariable Long id,
            @Valid @RequestBody PaymentNoteBasicDto paymentNoteBasicDto,
            HttpServletRequest request) {
        PaymentNote paymentNoteToUpdate = mapper.toEntity(paymentNoteBasicDto);
        PaymentNote updatedPaymentNote = service.updateNp(paymentNoteToUpdate, id);
        log.info("AUDIT_NP_UPDATED | npId={} | npNumber={} | ip={}", updatedPaymentNote.getId(),
                updatedPaymentNote.getNumeroNp(), request.getRemoteAddr());
        return ResponseEntity.status(202).body(mapper.toDto(updatedPaymentNote));
    }

    @GetMapping("/{numero}/{date}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentNoteBasicDto> findByNumeroNp(@Valid @PathVariable @NotNull Integer numero,
            @Valid @PathVariable @NotNull Integer date) {
        return ResponseEntity.status(200).body(mapper.toDto(service.findByNumeroNpForYear(numero, date)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteById(@PathVariable Long id, HttpServletRequest request) {
        service.deleteNp(id);
        log.warn("AUDIT_NP_DELETED | npId={} | ip={}", id, request.getRemoteAddr());
    }
}
```

### 6.3 Frontend — `src/services/api.ts` (bloco de PaymentNote refatorado)

```ts
// ── Helpers internos ─────────────────────────────────────────────────────────

function serializeTaxPayload(tax?: TaxDto | null) {
  if (!tax) return null;
  if (tax.manualAdjustment) {
    return {
      tipo: tax.tipo,
      codEfd: tax.codEfd,
      codigoReceita: tax.codigoReceita ?? null,
      manualAdjustment: true,
      calculatedItems: tax.calculatedItems ?? [],
    };
  }
  return {
    tipo: tax.tipo,
    codEfd: tax.codEfd,
    ...(tax.codigoReceita != null ? { codigoReceita: tax.codigoReceita } : {}),
  };
}

function serializeItem(item: PaymentNoteItemDto) {
  const taxList = item.taxes && item.taxes.length > 0
    ? item.taxes
    : item.tax ? [item.tax] : [];

  const serializedTaxes = taxList
    .map(serializeTaxPayload)
    .filter((t): t is NonNullable<typeof t> => t !== null);

  return {
    ...(item.id !== undefined ? { id: item.id } : {}),
    description: item.description ?? '',
    value: item.value,
    taxes: serializedTaxes,
    tax: serializedTaxes[0] ?? null,
    manualAdjustment: item.manualAdjustment ?? false,
    ...(item.empresaBeneficiaria?.cnpj
      ? { empresaBeneficiaria: { cnpj: item.empresaBeneficiaria.cnpj } }
      : {}),
  };
}

/**
 * Monta o payload compartilhado por POST, PUT e prévia.
 * Evita repetir a montagem em três funções.
 */
function buildPaymentNotePayload(dto: PaymentNoteDto, includeId = false) {
  return {
    ...(includeId && dto.id !== undefined ? { id: dto.id } : {}),
    numeroNp: dto.numeroNp,
    dataLiquidacao: formatDate(dto.dataLiquidacao),
    empresa: { cnpj: dto.empresa.cnpj },
    docOrigin: dto.docOrigin,
    status: dto.status,
    items: (dto.items ?? []).map(serializeItem),
    // datePayment só é aceito pelo backend quando status === 'PAGA'
    ...(dto.status === 'PAGA' && dto.datePayment
      ? { datePayment: formatDate(dto.datePayment) }
      : {}),
  };
}

// ── PaymentNote (NP) ──────────────────────────────────────────────────────────

export async function savePaymentNote(dto: PaymentNoteDto): Promise<ApiResult<PaymentNoteDto>> {
  try {
    const res = await api.post<PaymentNoteDto>('/API/Np', buildPaymentNotePayload(dto));
    return { data: res.data, status: res.status, errorMessage: null };
  } catch (e) { return handleError(e); }
}

/**
 * POST /API/Np/preview
 * Executa o mesmo cálculo de impostos do save, mas sem persistir.
 * A resposta tem o mesmo formato de PaymentNoteDto.
 */
export async function previewPaymentNote(dto: PaymentNoteDto): Promise<ApiResult<PaymentNoteDto>> {
  try {
    const res = await api.post<PaymentNoteDto>('/API/Np/preview', buildPaymentNotePayload(dto));
    return { data: res.data, status: res.status, errorMessage: null };
  } catch (e) { return handleError(e); }
}

export async function updatePaymentNote(dto: PaymentNoteDto): Promise<ApiResult<PaymentNoteDto>> {
  try {
    const res = await api.put<PaymentNoteDto>(
      `/API/Np/${dto.id}`,
      buildPaymentNotePayload(dto, true),
    );
    return { data: res.data, status: res.status, errorMessage: null };
  } catch (e) { return handleError(e); }
}
```

### 6.4 Frontend — `src/components/NpItemEditor.tsx` (adições)

Adicionar após `editStateToItem`:

```ts
/**
 * Limpa a prévia de um grupo.
 * Usar sempre que um input que afeta o cálculo mudar.
 */
function semPrevia(group: TaxGroupState): TaxGroupState {
  return {
    ...group,
    backendItems: [],
    manualItems: [],
    isManualAdjustment: false,
  };
}

/**
 * Aplica o resultado da prévia (itens retornados pelo backend) sobre o estado
 * de edição atual.
 *
 * Regras:
 * - O backend devolve os itens na mesma ordem enviada; cada item devolve os
 *   grupos de imposto na mesma ordem. O pareamento é por índice.
 * - Se o grupo está em ajuste manual, preservamos o baseline automático
 *   (backendItems) e apenas confirmamos os valores manuais (manualItems).
 * - Se o grupo está automático, atualizamos baseline e valores manuais.
 */
export function applyPreviewToItems(
  current: ItemEditState[],
  previewItems: PaymentNoteItemDto[] | undefined,
): ItemEditState[] {
  if (!previewItems || previewItems.length === 0) return current;

  return current.map((item, idx) => {
    const previewItem = previewItems[idx];
    if (!previewItem) return item;

    const previewTaxes = resolveTaxDtos(previewItem);

    const taxGroups = item.taxGroups.map((group, groupIdx) => {
      const previewTax = previewTaxes[groupIdx];
      if (!previewTax) return group;

      const calculado = (previewTax.calculatedItems ?? []).map(i => ({ ...i }));

      if (group.isManualAdjustment) {
        return {
          ...group,
          // mantém o baseline automático anterior para o botão "Restaurar"
          manualItems: calculado.length > 0 ? calculado : group.manualItems,
        };
      }

      return {
        ...group,
        backendItems: calculado,
        manualItems: calculado.map(i => ({ ...i })),
        isManualAdjustment: false,
      };
    });

    return { ...item, taxGroups };
  });
}
```

Alterar o input de valor do item (dentro de `NpItemEditor`):

```tsx
<Input
  label="Valor (R$)"
  type="text"
  placeholder="Ex: 1.500,00"
  value={item.value}
  onChange={e => {
    const value = e.target.value;
    set({
      value,
      // O cálculo depende do valor: invalida a prévia de todos os grupos.
      taxGroups: item.taxGroups.map(semPrevia),
    });
  }}
/>
```

Alterar o `onChange` de `taxTipo` (dentro de `TaxGroupEditor`):

```tsx
onChange={e => {
  set(semPrevia({
    ...group,
    taxTipo: e.target.value as OptanteStatus,
    codEfd: '',
    codigoReceita: null,
  }));
  setOpcoes([]);
  setOpcoesError(null);
  lastFetchedKey.current = '';
}}
```

Alterar o `onChange` de `codEfd`:

```tsx
onChange={e => {
  set(semPrevia({
    ...group,
    codEfd: e.target.value,
    codigoReceita: null,
  }));
  lastFetchedKey.current = '';
  setOpcoes([]);
  setOpcoesError(null);
}}
```

Alterar o `onChange` de `codigoReceita`:

```tsx
onChange={e => set(semPrevia({
  ...group,
  codigoReceita: e.target.value ? Number(e.target.value) : null,
}))}
```

### 6.5 Frontend — `src/pages/Cadastro.tsx` (alterações)

#### 6.5.1 Imports

```ts
import { NpItemEditor, editStateToItem, applyPreviewToItems, DEFAULT_ITEM } from '../components/NpItemEditor';
import type { ItemEditState } from '../components/NpItemEditor';
```

No bloco de imports de `../services/api`, adicionar:

```ts
  previewPaymentNote,
```

#### 6.5.2 Estados

Dentro de `FormPaymentNote`:

```ts
const [previewLoading, setPreviewLoading] = useState(false);
const [previewError, setPreviewError] = useState<string | null>(null);
```

#### 6.5.3 Validação compartilhada

Substituir o conteúdo de `openConfirm` por:

```ts
function validarFormulario(): string | null {
  if (!numeroNp || !dataLiq || !docOrigin) {
    return 'Preencha todos os campos obrigatórios: Nº NP, Data e Doc. Origem.';
  }

  const dateRegex = /^\d{2}\/\d{2}\/\d{4}$/;
  if (!dateRegex.test(dataLiq)) {
    return 'Data Liquidação inválida. Use o formato DD/MM/YYYY.';
  }

  if (!cnpjValid) {
    return 'Valide o CNPJ antes de salvar.';
  }

  if (status === 'PAGA' && !datePayment) {
    return 'A Data de Pagamento é obrigatória quando o status é PAGA.';
  }

  if (npItems.length === 0) {
    return 'Adicione pelo menos um item.';
  }

  if (npItems.some(it => !it.value || parseBRCurrency(it.value) <= 0)) {
    return 'Todos os itens devem ter um valor maior que zero.';
  }

  // Valida que todos os grupos NAO_OPTANTE com codEfd têm código de receita selecionado
  for (const it of npItems) {
    for (const g of it.taxGroups) {
      if (g.taxTipo === 'NAO_OPTANTE' && g.codEfd && g.codigoReceita == null) {
        return `Selecione o Código de Receita para o grupo com Cód. EFD ${g.codEfd}.`;
      }
    }
  }

  return null;
}

function openConfirm() {
  setError(null);
  setSuccess(null);

  const msg = validarFormulario();
  if (msg) {
    setError(msg);
    return;
  }

  setConfirmOpen(true);
}
```

#### 6.5.4 Montagem do DTO e handler da prévia

```ts
function buildDto(): PaymentNoteDto {
  return {
    numeroNp: parseInt(numeroNp, 10),
    dataLiquidacao: dataLiq,
    empresa: { nome: empresaNome, cnpj: cnpj.replace(/\D/g, '') },
    docOrigin: docOrigin.trim(),
    status,
    items: npItems.map(editStateToItem),
    datePayment: status === 'PAGA' && datePayment ? datePayment : null,
  };
}

async function handlePreview() {
  setPreviewError(null);
  setError(null);

  const msg = validarFormulario();
  if (msg) {
    setPreviewError(msg);
    return;
  }

  setPreviewLoading(true);
  const result = await previewPaymentNote(buildDto());
  setPreviewLoading(false);

  if (!result.data) {
    setPreviewError(result.errorMessage ?? 'Não foi possível calcular a prévia.');
    return;
  }

  setNpItems(prev => applyPreviewToItems(prev, result.data?.items));
  toastSuccess('Prévia dos impostos calculada.');
}
```

E ajustar `handleSaveConfirmed` para usar `buildDto()`:

```ts
async function handleSaveConfirmed() {
  setConfirmOpen(false);
  setError(null);
  setSuccess(null);
  setLoading(true);

  const result = await savePaymentNote(buildDto());

  if (result.data) {
    setSuccess(result.data);
    toastSuccess('Payment Note cadastrada com sucesso!');
    setNumeroNp(''); setDataLiq(''); setDocOrigin('');
    setCnpj(''); setCnpjValid(null); setEmpresaNome('');
    setStatus('A_PAGAR'); setDatePayment('');
    setNpItems([{ ...DEFAULT_ITEM }]);
    setPreviewError(null);
  } else {
    setError(result.errorMessage ?? 'Erro ao salvar Payment Note.');
  }

  setLoading(false);
}
```

#### 6.5.5 (Opcional) Limpar prévia ao mudar a data de liquidação

```ts
function resetPreviews() {
  setNpItems(prev => prev.map(it => ({
    ...it,
    taxGroups: it.taxGroups.map(g => ({
      ...g,
      backendItems: [],
      manualItems: [],
      isManualAdjustment: false,
    })),
  })));
}
```

No `Input` da data:

```tsx
<Input
  label="Data Liquidação"
  type="text"
  inputMode="numeric"
  placeholder="DD/MM/YYYY"
  value={dataLiq}
  onChange={(e) => {
    setDataLiq(applyDateMask(e.target.value));
    resetPreviews();
  }}
  maxLength={10}
/>
```

#### 6.5.6 Botões e exibição de erro

Substituir o bloco atual:

```tsx
<div className="space-y-3">
  {error && <Alert variant="error" message={error} onClose={() => setError(null)} />}
  <Button variant="primary" loading={loading} disabled={cnpjValid === false || loading} onClick={openConfirm}>
    Cadastrar Payment Note
  </Button>
</div>
```

por:

```tsx
<div className="space-y-3">
  {error && <Alert variant="error" message={error} onClose={() => setError(null)} />}
  {previewError && (
    <Alert variant="error" message={previewError} onClose={() => setPreviewError(null)} />
  )}

  <div className="flex flex-wrap gap-2">
    <Button
      variant="secondary"
      loading={previewLoading}
      disabled={loading || previewLoading || cnpjValid === false}
      onClick={handlePreview}
    >
      Prévia dos Impostos
    </Button>

    <Button
      variant="primary"
      loading={loading}
      disabled={cnpjValid === false || loading || previewLoading}
      onClick={openConfirm}
    >
      Cadastrar Payment Note
    </Button>
  </div>
</div>
```

> O `Button` já suporta `variant="secondary"` e a prop `loading` (spinner embutido). Não é necessário alterar o componente.

### 6.6 (Opcional) Resumo consolidado no frontend

Se quiser um resumo acima dos botões, sem novo endpoint:

```tsx
{npItems.some(it => it.taxGroups.some(g => g.backendItems.length > 0)) && (
  <div className="rounded-lg border border-white/10 bg-black/30 p-3 space-y-1">
    <div className="flex justify-between text-xs text-stone-400">
      <span>Total dos itens</span>
      <span className="font-mono text-gray-200">
        {formatCurrency(npItems.reduce((s, it) => s + (parseBRCurrency(it.value) || 0), 0))}
      </span>
    </div>
    <div className="flex justify-between text-xs text-stone-400">
      <span>Total de impostos (prévia)</span>
      <span className="font-mono text-amber-400">
        {formatCurrency(
          npItems.reduce(
            (s, it) => s + it.taxGroups.reduce(
              (gs, g) => gs + g.backendItems.reduce((ts, t) => ts + t.amount, 0),
              0,
            ),
            0,
          ),
        )}
      </span>
    </div>
  </div>
)}
```

---

## 7. Testes sugeridos

### 7.1 Backend — unitários do `PaymentNoteService`

Criar/ajustar uma classe de teste com mocks de `PaymentNoteRepository`, `PaymentNoteEmpenhoRepository`, `TaxCalculator` e `EmpresaService`.

Casos:

1. `previewNp` não chama `paymentNoteRepository.save`.
2. `previewNp` não chama `existsByNumeroNpAndYear`.
3. `previewNp` calcula impostos para cada item.
4. `previewNp` soma apenas os valores dos itens.
5. `previewNp` resolve o beneficiário por CNPJ quando informado.
6. `previewNp` herda a empresa da NP quando o beneficiário é nulo.
7. `previewNp` lança `BusinessRuleException` para valor negativo.
8. `previewNp` lança `BusinessRuleException` para grupo duplicado.
9. `saveNp` continua validando duplicidade de número/ano.
10. `saveNp` continua preenchendo auditoria.
11. `updateNp` continua limpando e recriando os itens.

### 7.2 Backend — integração do controller

- `POST /API/Np/preview` retorna `200` e o JSON esperado.
- `POST /API/Np/preview` não incrementa a tabela `payment_note`.
- `POST /API/Np/preview` com EFD sem regra retorna `400`.
- `POST /API/Np/preview` com CNPJ inexistente retorna `404`.
- `POST /API/Np/preview` com payload inválido retorna `400`.
- `POST /API/Np` continua retornando `201` e persistindo.

### 7.3 Frontend — testes manuais

1. Preencher NP com item de R$ 1.000,00 e um grupo `NAO_OPTANTE`/EFD válido.
2. Clicar em "Prévia dos Impostos".
3. Confirmar que o painel de impostos aparece com os valores calculados.
4. Ativar "Ajuste Manual", alterar um valor e confirmar que o estado muda.
5. Clicar em "Cadastrar Payment Note" e confirmar que o valor ajustado é persistido.
6. Repetir com `OPTANTE` (deve aparecer "Isento").
7. Repetir com `NAO_OPTANTE` sem `codEfd` (deve aparecer "Pendente").
8. Mudar o valor do item após a prévia e confirmar que o painel some (prévia invalidada).
9. Mudar o `codEfd` após a prévia e confirmar que o painel some.

---

## 8. Checklist de aceite

- [ ] Existe `POST /API/Np/preview`.
- [ ] A prévia não persiste nada.
- [ ] A prévia reutiliza o mesmo cálculo do save (sem duplicar regra).
- [ ] `saveNp` e `updateNp` mantêm o comportamento atual.
- [ ] O frontend tem o botão "Prévia dos Impostos".
- [ ] Após a prévia, o `TaxAdjustmentPanel` aparece.
- [ ] O ajuste manual continua funcionando e é enviado no save.
- [ ] O total da nota continua sendo a soma dos valores dos itens, nunca dos impostos.
- [ ] Múltiplos grupos de imposto continuam incidindo sobre o mesmo `value`.
- [ ] Erros de regra são exibidos ao usuário.
- [ ] Nenhuma migration nova é necessária.
- [ ] Build backend com Java 21 compila.
- [ ] Build frontend (`npm run build` / `tsc`) passa.

---

## 9. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Refatorar `saveNp`/`updateNp` e mudar comportamento | Comparar o código atual com o refactor; manter a ordem de validações e o `clear` + recriação de itens. Testes de regressão para save/update. |
| `previewNp` com `@Transactional` abrir transação desnecessária | É uma transação curta, sem `save` da nota. Se preferir, usar `@Transactional(readOnly = true)` — mas atenção: `EmpresaService.findyByCnpj` pode gravar a migração de empresa legada; por isso o plano usa `@Transactional` normal. |
| Prévia com dados desatualizados após edição | `semPrevia` limpa o grupo ao mudar valor/tipo/codEfd/codigoReceita; `resetPreviews` limpa ao mudar a data. |
| Pareamento por índice quebrar se o backend reordenar | Hoje a ordem é determinística (mapper + loop). Documentar que a ordem do array de resposta deve ser preservada. Se isso mudar, parear por `(tipo, codEfd, codigoReceita)` numa evolução futura. |
| `@Valid` exigir campos do cabeçalho | O botão de prévia valida o formulário antes de chamar. Se for necessário prévia antes do cabeçalho, criar um DTO específico (não recomendado agora). |
| Ajuste manual perder o baseline automático | `applyPreviewToItems` preserva `backendItems` quando o grupo está em modo manual. |
| Divergência de arredondamento (DARF) | O `TaxAdjustmentPanel` já detecta e sinaliza; a prévia apenas expõe os valores. |

---

## 10. Ordem de implementação e rollback

### Ordem recomendada

1. Backend: refactor do `PaymentNoteService` (etapa 1).
2. Backend: endpoint de prévia (etapa 2).
3. Testar via `curl`/Postman sem frontend.
4. Frontend: `api.ts` (etapa 3).
5. Frontend: `applyPreviewToItems` (etapa 4).
6. Frontend: botão e handler (etapa 5).
7. Testes manuais e ajustes de UX (etapa 6, opcional).

### Rollback

- **Backend**: remover o método `preview` do controller e o `previewNp` do service. O refactor de `prepararNota` pode permanecer, pois preserva o comportamento de save/update.
- **Frontend**: remover o botão e o handler; `previewPaymentNote` pode permanecer sem uso. `buildPaymentNotePayload` e `applyPreviewToItems` são aditivos e não quebram nada.
- **Banco**: nada a reverter (nenhuma migration).

---

## 11. Observações finais

- A prévia é **stateless**: cada clique recalcula do zero a partir do payload atual. Não há cache no backend.
- O frontend pode chamar a prévia quantas vezes quiser, inclusive depois de ajustes manuais. No modo manual, o backend devolve os valores enviados e apenas completa `codigoReceita`/`taxRuleDescription` quando encontra a regra.
- O `TaxAdjustmentPanel` não precisa de alteração: ele já recebe `items` calculados e emite `onChange(items, manualAdjustment)`.
- O endpoint de prévia pode ser usado também para validar uma nota antes de salvar (ex.: detectar EFD sem regra, beneficiário inexistente, grupo duplicado).
- Se no futuro a prévia precisar mostrar um resumo por tipo de imposto agregado entre itens, isso pode ser calculado no frontend a partir de `items[].taxes[].calculatedItems`, sem novo endpoint.
```
