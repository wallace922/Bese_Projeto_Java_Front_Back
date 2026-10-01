# 🚀 Sistema Tesouraria (BESE) - Monorepo Front & Back

Este repositório unifica o **Backend (Java / Spring Boot)** e o **Frontend (React / Vite / TypeScript)** do Sistema de Tesouraria (BESE).

---

## 📁 Estrutura do Repositório

```text
Bese_Projeto_Java_Front_Back/
├── Bese_Projeto/           # API Backend (Spring Boot, Java 21, JPA/Hibernate, Flyway, MySQL)
├── Front_Bese_Projeto/     # Aplicação Web Frontend (React, Vite, TypeScript, Tailwind CSS)
├── .gitignore              # Configuração global do Git (proteção contra vazamentos de senhas e arquivos temporários)
└── README.md               # Documentação principal do projeto
```

---

## 🛠️ Tecnologias Utilizadas

### Backend (`Bese_Projeto/`)
- **Linguagem:** Java 21 (obrigatório — `pom.xml` define `java.version=21`)
- **Framework:** Spring Boot 3
- **Persistência & BD:** Spring Data JPA, Hibernate, MySQL
- **Migrações:** Flyway Migration (`src/main/resources/db/migration`, `V0__baseline.sql` → `V8`)
- **Segurança & Auth:** Spring Security, JWT (JSON Web Token)
- **Gerenciador de Build:** Maven (usando `mvnw` wrapper, não precisa instalar o Maven)

### Frontend (`Front_Bese_Projeto/`)
- **Framework & Build:** React + Vite
- **Linguagem:** TypeScript
- **Estilização:** Tailwind CSS + PostCSS
- **Requisições HTTP:** Axios
- **Ícones & Componentes:** Lucide React / Componentes customizados

---

## ⚙️ Pré-requisitos

Antes de iniciar, certifique-se de ter instalado em sua máquina:
- **Java JDK 21** (obrigatório — versões diferentes quebram o build/Lombok)
- **Node.js** (v18 ou superior) e **npm**
- **MySQL Server 8** rodando localmente ou via container Docker
- **Git**

---

## 🚀 Como Executar o Projeto (máquina nova, do zero)

### 1. Banco de Dados MySQL — criar database + usuário com permissão

> ⚠️ Não crie tabelas na mão: o Flyway cria tudo sozinho no primeiro boot (`V0__baseline.sql` → `V8`). O banco precisa existir **vazio** e o usuário precisa de `GRANT` — sem isso o boot falha com `Access denied ... 1044`.

Conecte como `root` e rode:
```sql
CREATE DATABASE IF NOT EXISTS tesouraria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'usuario_app'@'localhost' IDENTIFIED BY 'troque_esta_senha';
GRANT ALL PRIVILEGES ON tesouraria.* TO 'usuario_app'@'localhost';
FLUSH PRIVILEGES;
```

> 💡 `localhost` ≠ `127.0.0.1` no MySQL. Se a `DB_URL` usar `localhost`, o grant precisa ser para `'usuario'@'localhost'`.

---

### 2. Backend (Java Spring Boot)

1. Acesse a pasta do backend (**importante:** todos os comandos abaixo partem daqui, pois o `.env` é lido a partir deste diretório):
   ```bash
   cd Bese_Projeto
   ```

2. **Crie o arquivo `.env` dentro de `Bese_Projeto/`** (mesma pasta do `pom.xml`). Os nomes das chaves precisam ser **exatamente** estes (maiúsculas com underline — `jwt.secret` minúsculo **não** funciona):
   ```properties
   DB_URL=jdbc:mysql://localhost:3306/tesouraria
   DB_USERNAME=usuario_app
   DB_PASSWORD=troque_esta_senha
   JWT_SECRET=troque_por_uma_frase_longa_com_mais_de_32_letras_e_numeros_123
   CORS_ALLOWED_ORIGINS=http://localhost:5173
   ```

   | Variável | Descrição | Regras |
   | :--- | :--- | :--- |
   | `DB_URL` | URL do Banco de Dados | Aponta para a database criada no passo 1 |
   | `DB_USERNAME` / `DB_PASSWORD` | Credenciais do MySQL | Iguais às do `CREATE USER` acima |
   | `JWT_SECRET` | Segredo que assina os tokens JWT | **Mínimo 32 caracteres**, só letras/números (evite `\| & ? * : ;`, que quebram o arquivo) |
   | `CORS_ALLOWED_ORIGINS` | Origem do frontend liberada | URL exata onde o `npm run dev` roda (sem `/` no final) |

   Alternativa sem arquivo: exportar no terminal antes de subir (`export DB_USERNAME=...` etc.).

3. Suba o servidor (**pare com `Ctrl+C` e suba de novo a cada mudança no `.env`** — ele só é lido no boot):
   ```bash
   ./mvnw spring-boot:run
   ```
   * No Windows: `mvnw.cmd spring-boot:run`
   * O Backend estará acessível em: `http://localhost:8080`
   * No primeiro boot o Flyway aplica `V0 → V8` e o Hibernate valida o schema (`ddl-auto=validate`, ele não cria nada).

### 3. Criar o primeiro usuário ADMIN (banco novo não tem login)

> ⚠️ O banco nasce **sem nenhum usuário** e só `ADMIN` pode criar usuários — ou seja, sem este passo ninguém consegue logar. Use este seed **somente em ambiente local/dev** e troque a senha em seguida.

Com o backend já subido uma vez (tabelas criadas), rode no MySQL:
```sql
INSERT INTO `user` (name, cpf, password, role)
VALUES ('Administrador', '11144477735', '$2b$10$A3fx7fqPXBVACLti4CXEAOvAzib8I7SXIlh0DkOVz6rJLIDm3EXLy', 'ADMIN');
```
* Login: CPF `11144477735` / senha `Trocar@123` (hash BCrypt, custo 10 — igual ao do app).
* Após logar, crie seu usuário real em `/admin` e **delete ou troque a senha deste seed**.

---

### 4. Frontend (React / Vite)

1. Abra um novo terminal na raiz e acesse a pasta do frontend:
   ```bash
   cd Front_Bese_Projeto
   ```

2. Instale as dependências da aplicação:
   ```bash
   npm install
   ```

3. Inicie o servidor de desenvolvimento:
   ```bash
   npm run dev
   ```
   * O Frontend estará acessível em: `http://localhost:5173`

> ⚠️ A URL da API está fixa em `src/services/api.ts` (`baseURL: 'http://localhost:8080'`). Se o backend estiver em outra máquina/host, ajuste esse arquivo e rode `npm run dev` de novo. Em produção, use HTTPS (o cookie de sessão exige canal seguro).

---

## 🧰 Solução de problemas comuns (erros já vistos neste projeto)

| Erro (último `Caused by` do log) | Onde está | O que fazer |
| :--- | :--- | :--- |
| `Could not resolve placeholder 'JWT_SECRET'` | `.env` ausente, com nome de chave errado ou app iniciado fora de `Bese_Projeto/` | Conferir `Bese_Projeto/.env` com as 5 chaves em maiúsculo; rodar a partir de `Bese_Projeto/`; restart completo |
| `Access denied for user ... 1044` | Falta `GRANT` no MySQL | Rodar o bloco `GRANT ALL PRIVILEGES ON tesouraria.*` do passo 1 (senha igual ao `DB_PASSWORD`) |
| `Access denied ... 1045` | Senha errada | Conferir `DB_PASSWORD` vs senha do `CREATE USER` |
| Flyway `V1 ... 1824 Failed to open table 'payment_note'` | Banco criado antes da `V0__baseline.sql` existir | Este projeto já inclui a `V0`; em banco novo não acontece. Se acontecer, o banco está em estado antigo — recrie vazio |
| Flyway `Checksum mismatch` / `Detected failed migration` | Arquivo de migration editado após rodar, ou migration falha registrada | Em dev com banco descartável: recriar o banco vazio e subir de novo. **Nunca** marque `success=1` na mão no `flyway_schema_history` |
| Hibernate `Schema-validation: missing column [x]` | Coluna fora do padrão snake_case no banco | As migrations oficiais já seguem o padrão; acontece se alguma tabela foi criada manualmente |
| Frontend `Network Error` / `CORS` | `CORS_ALLOWED_ORIGINS` divergente ou `baseURL` apontando p/ host errado | Alinhar `CORS_ALLOWED_ORIGINS` com a URL do `npm run dev` (sem barra final) |

---

## 🔒 Boas Práticas de Segurança

- **NUNCA** comite arquivos com senhas de produção, arquivos `.env` com dados sensíveis ou segredos JWT em texto puro.
- O arquivo `.gitignore` na raiz deste projeto está configurado para ignorar automaticamente:
  - Artefatos de compilação (`target/`, `dist/`, `node_modules/`)
  - Configurações locais de IDE (`.vscode/`, `.idea/`)
  - Chaves privadas e variáveis de ambiente (`.env`, `*.pem`, `*.key`)
