# Testando o MinIO pelo Insomnia

## 1. Garantir que o `.env` está configurado

Confirme que o `.env` tem as variáveis do MinIO preenchidas (os valores padrão do `.env.example` já funcionam para ambiente local):

```env
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=alumni-files
```

---

## 2. Subir os containers

```bash
docker-compose up -d
```

Isso sobe o Postgres (`5432`) e o MinIO (`9000` API + `9001` console).

---

## 3. Verificar o MinIO Console (opcional mas útil)

Abra `http://localhost:9001` no browser.

- **Login:** `minioadmin` / `minioadmin`
- O bucket `alumni-files` será criado automaticamente ao iniciar a API.

---

## 4. Subir a API

```bash
./gradlew bootRun
```

---

## 5. Obter um token JWT

**Crie uma request no Insomnia:**

| Campo  | Valor                              |
| ------ | ---------------------------------- |
| Method | `POST`                             |
| URL    | `http://localhost:8080/auth/login` |

**Headers:**
```
Content-Type: application/json
```

**Body → JSON:**
```json
{
  "email": "seu@email.com",
  "password": "suasenha"
}
```

**Resposta esperada:**
```json
{
  "token": "eyJhbGci..."
}
```

Copie o valor do `token` — ele será usado nos próximos passos.

---

## 6. Testar upload de foto de perfil

**Crie uma request no Insomnia:**

| Campo  | Valor                                                   |
| ------ | ------------------------------------------------------- |
| Method | `POST`                                                  |
| URL    | `http://localhost:8080/auth/users/{id}/profile-picture` |

Substitua `{id}` pelo ID do usuário (ex: `1`).

**Headers:**
```
Authorization: Bearer SEU_TOKEN_AQUI
```

> Não adicione `Content-Type` manualmente — o Insomnia define automaticamente para `multipart/form-data`.

**Body → Multipart Form:**

 Type | Value                                     |
 ---- | ----------------------------------------- |
 File | _(selecione uma imagem do seu computador)_ |

**Resposta esperada `200 OK`:**
```json
{
  "profilePictureUrl": "http://localhost:9000/alumni-files/profile-pictures/<uuid>.jpg"
}
```

---

## 7. Testar upload de capa de notícia

**Crie uma request no Insomnia:**

| Campo  | Valor                                         |
| ------ | --------------------------------------------- |
| Method | `POST`                                        |
| URL    | `http://localhost:8080/news/{id}/cover-image` |

Substitua `{id}` pelo ID de uma notícia existente (ex: `1`).

**Headers:**
```
Authorization: Bearer SEU_TOKEN_AQUI
```

**Body → Multipart Form:**

| Key  | Type | Value                                     |
| ---- | ---- | ----------------------------------------- |
| file | File | _(selecione uma imagem do seu computador)_ |

**Resposta esperada `200 OK`:**
```json
{
  "coverImageUrl": "http://localhost:9000/alumni-files/news-images/<uuid>.jpg"
}
```

---

## 8. Confirmar o arquivo no MinIO Console

Após o upload, acesse `http://localhost:9001` → **Buckets** → `alumni-files` → **Browse** e confirme que o arquivo aparece na pasta `profile-pictures/` ou `news-images/`.

---

## 9. Acessar o arquivo diretamente

Como o bucket tem política pública de leitura, cole a URL retornada diretamente no browser. Você deve ver a imagem sem precisar de autenticação.

Também é possível criar uma request `GET` no Insomnia com a URL retornada para confirmar o `200 OK`.