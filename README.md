# 🚀 Sistema Tesouraria (BESE) - Monorepo Front & Back

Este repositório unifica o **Backend (Java / Spring Boot)** e o **Frontend (React / Vite / TypeScript)** do Sistema de Tesouraria (BESE).

---

## 📁 Estrutura do Repositório

```text
Bese_Projeto_Java_Front_Back/
├── Bese_Projeto/           # API Backend (Spring Boot, Java 17+, JPA/Hibernate, Flyway, MySQL)
├── Front_Bese_Projeto/     # Aplicação Web Frontend (React, Vite, TypeScript, Tailwind CSS)
├── .gitignore              # Configuração global do Git (proteção contra vazamentos de senhas e arquivos temporários)
└── README.md               # Documentação principal do projeto
```

---

## 🛠️ Tecnologias Utilizadas

### Backend (`Bese_Projeto/`)
- **Linguagem:** Java 17+
- **Framework:** Spring Boot 3
- **Persistência & BD:** Spring Data JPA, Hibernate, MySQL
- **Migrações:** Flyway Migration (`src/main/resources/db/migration`)
- **Segurança & Auth:** Spring Security, JWT (JSON Web Token)
- **Gerenciador de Build:** Maven (usando `mvnw` wrapper)

### Frontend (`Front_Bese_Projeto/`)
- **Framework & Build:** React + Vite
- **Linguagem:** TypeScript
- **Estilização:** Tailwind CSS + PostCSS
- **Requisições HTTP:** Axios
- **Ícones & Componentes:** Lucide React / Componentes customizados

---

## ⚙️ Pré-requisitos

Antes de iniciar, certifique-se de ter instalado em sua máquina:
- **Java JDK 17** ou superior
- **Node.js** (v18 ou superior) e **npm**
- **MySQL Server** rodando localmente ou via container Docker
- **Git**

---

## 🚀 Como Executar o Projeto

### 1. Configurando o Banco de Dados MySQL

Crie a base de dados no seu MySQL:
```sql
CREATE DATABASE tesouraria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### 2. Rodando o Backend (Java Spring Boot)

1. Acesse a pasta do backend:
   ```bash
   cd Bese_Projeto
   ```

2. **Configuração de Variáveis de Ambiente (Segurança):**
   O projeto utiliza variáveis de ambiente para evitar expor senhas no controle de versão. Você pode definí-las no terminal ou na sua IDE:

   | Variável | Valor Padrão Local | Descrição |
   | :--- | :--- | :--- |
   | `DB_URL` | `jdbc:mysql://localhost:3306/tesouraria` | URL do Banco de Dados |
   | `DB_USERNAME` | `usuario_app` | Usuário do MySQL |
   | `DB_PASSWORD` | `senha_db_local` | Senha do MySQL |
   | `JWT_SECRET` | `sua_chave_secreta_jwt_desenvolvimento_local_12345` | Segredo para assinar os tokens JWT |

   *Exemplo de execução exportando as variáveis:*
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=sua_senha_mysql
   export JWT_SECRET=minha_chave_jwt_super_segura_123
   ```

3. Execute o servidor Spring Boot com o Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   * O Backend estará acessível em: `http://localhost:8080`

---

### 3. Rodando o Frontend (React / Vite)

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

---

## 🔒 Boas Práticas de Segurança

- **NUNCA** comite arquivos com senhas de produção, arquivos `.env` com dados sensíveis ou segredos JWT em texto puro.
- O arquivo `.gitignore` na raiz deste projeto está configurado para ignorar automaticamente:
  - Artefatos de compilação (`target/`, `dist/`, `node_modules/`)
  - Configurações locais de IDE (`.vscode/`, `.idea/`)
  - Chaves privadas e variáveis de ambiente (`.env`, `*.pem`, `*.key`)
