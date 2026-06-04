# Como rodar o projeto

## Criar .env

Crie uma copia do arquivo `.env.example` e renomeie para `.env`. Certifique-se de **configurar as variáveis de ambiente corretamente**, especialmente as relacionadas ao banco de dados e ao MinIO.

## Subir os containers

Certifique-se de ter o Docker instalado e rodando. Em seguida, execute o comando:

```bash
docker-compose up -d
```

Isso sobe o Postgres (`5432`) e o MinIO (`9000` API / `9001` console).

## Rodar a aplicação

```bash
./gradlew bootRun
```

## Documentação da API (Swagger UI)

Com a aplicação rodando, acesse:

| URL | Descrição |
|-----|-----------|
| `http://localhost:8080/swagger-ui/index.html` | Interface visual com todos os endpoints |
| `http://localhost:8080/v3/api-docs` | Especificação OpenAPI em JSON |

Para testar endpoints protegidos, clique em **Authorize** e informe o token no formato:
```
Bearer <seu-token-jwt>
```

---

## Upload de arquivos (MinIO)

A API utiliza MinIO para armazenamento de arquivos (fotos de perfil e capas de notícias).
Veja o guia completo de como testar em: [docs/testing-minio.md](docs/testing-minio.md)

---

## Plugins recomendados (IntelliJ IDEA)

- **Lombok Plugin**: Facilita o uso de anotações do Lombok.
- **Atom Material Icons**: Melhora a aparência dos ícones no projeto.
- **Key Promoter X**: Ajuda a aprender atalhos de teclado para aumentar a produtividade.
- **Rainbow Brackets**: Destaca os colchetes correspondentes para facilitar a leitura do código.
- **SonarQube**: Integração com o SonarQube para análise de código e qualidade.
- **CodeMetrics**: Fornece métricas de código para ajudar a identificar áreas de melhoria.
- **GitToolBox**: Melhora a integração com o Git, mostrando informações de branch e status diretamente no IDE.

---

## Estrutura de código

Este projeto utiliza imports explícitos e não permite wildcard imports (`*`).

Correto:
```java
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
```

Evitar:
```java
import lombok.*;
```

Imports explícitos melhoram a legibilidade, manutenção e seguem boas práticas de projetos profissionais.

### No IntelliJ IDEA

Configure o IntelliJ para evitar `*` automaticamente:

```
File → Settings → Editor → Code Style → Java → Imports
```

Defina:
```
Class count to use import with '*' = 100
Names count to use static import with '*' = 100
```

Depois organize os imports com `Ctrl + Alt + O`.
