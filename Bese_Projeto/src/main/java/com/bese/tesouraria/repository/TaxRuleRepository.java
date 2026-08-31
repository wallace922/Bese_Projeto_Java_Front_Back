package com.bese.tesouraria.repository;

import com.bese.tesouraria.entity.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {

    /**
     * Busca a regra de imposto vigente para um determinado código EFD e data.
     * Uma regra é considerada vigente se a data fornecida está entre o início e o fim de sua vigência.
     * O fim da vigência (data_fim_vigencia) pode ser NULL, significando que a regra não tem data para expirar.
     *
     * @param codEfd O código EFD.
     * @param data   A data para a qual a regra deve ser válida (geralmente, a data da nota).
     * @return Optional com a TaxRule vigente.
     * @deprecated Use {@link #findAllVigenteByCodEfdAndDate} para suportar múltiplos códigos de receita por EFD.
     */
    @Deprecated
    @Query("SELECT r FROM TaxRule r WHERE r.codEfd = :codEfd " +
           "AND r.dataInicioVigencia <= :data " +
           "AND (r.dataFimVigencia IS NULL OR r.dataFimVigencia >= :data)")
    Optional<TaxRule> findVigenteByCodEfdAndDate(@Param("codEfd") Integer codEfd, @Param("data") LocalDate data);

    /**
     * Busca TODAS as regras vigentes para um determinado código EFD e data,
     * independente do código de receita. Pode retornar mais de uma regra quando
     * o EFD possui múltiplos códigos de receita cadastrados.
     *
     * @param codEfd O código EFD.
     * @param data   A data para a qual a regra deve ser válida.
     * @return Lista de TaxRule vigentes para o EFD naquela data.
     */
    @Query("SELECT r FROM TaxRule r WHERE r.codEfd = :codEfd " +
           "AND r.dataInicioVigencia <= :data " +
           "AND (r.dataFimVigencia IS NULL OR r.dataFimVigencia >= :data)")
    List<TaxRule> findAllVigenteByCodEfdAndDate(@Param("codEfd") Integer codEfd, @Param("data") LocalDate data);

    /**
     * Busca a regra vigente para um EFD + código de receita + data específicos.
     * Usado para encerrar a vigência da regra anterior ao salvar uma nova.
     *
     * @param codEfd         O código EFD.
     * @param codigoReceita  O código de receita.
     * @param data           A data para a qual a regra deve ser válida.
     * @return Optional com a TaxRule vigente para aquele EFD/receita.
     */
    @Query("SELECT r FROM TaxRule r WHERE r.codEfd = :codEfd " +
           "AND r.codigoReceita = :codigoReceita " +
           "AND r.dataInicioVigencia <= :data " +
           "AND (r.dataFimVigencia IS NULL OR r.dataFimVigencia >= :data)")
    Optional<TaxRule> findVigenteByCodEfdAndCodigoReceitaAndDate(
        @Param("codEfd") Integer codEfd,
        @Param("codigoReceita") Integer codigoReceita,
        @Param("data") LocalDate data);

    /**
     * Verifica se existe alguma regra mais recente para o mesmo par (codEfd + codigoReceita),
     * excluindo a própria regra avaliada. Usado para impedir a reabertura de uma vigência
     * quando já existe uma versão mais nova ativa para o mesmo par.
     *
     * @param codEfd              O código EFD da regra.
     * @param codigoReceita       O código de receita da regra.
     * @param excludeId           O ID da regra atual (excluída da busca para não conflitar consigo mesma).
     * @param dataInicioVigencia  A data de início da regra atual — busca apenas regras com data posterior.
     * @return {@code true} se existir versão mais recente; {@code false} caso contrário.
     */
    @Query("SELECT COUNT(r) > 0 FROM TaxRule r " +
           "WHERE r.codEfd = :codEfd " +
           "AND r.codigoReceita = :codigoReceita " +
           "AND r.id <> :excludeId " +
           "AND r.dataInicioVigencia > :dataInicioVigencia")
    boolean existsNewerVersion(
        @Param("codEfd") Integer codEfd,
        @Param("codigoReceita") Integer codigoReceita,
        @Param("excludeId") Long excludeId,
        @Param("dataInicioVigencia") LocalDate dataInicioVigencia
    );

    /**
     * Verifica se já existe uma regra com o codEfd informado (ativa ou não).
     * Usado para evitar duplicatas ao cadastrar nova regra.
     *
     * @param codEfd código EFD a verificar
     * @return true se já existe, false se não
     */
    boolean existsByCodEfd(Integer codEfd);
}
