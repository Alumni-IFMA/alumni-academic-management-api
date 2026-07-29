# Design: Approve User Account Endpoint

**Date:** 2026-07-17
**Branch:** feature/approve-user-account
**Status:** Approved

---

## Context

Users are registered via `POST /auth/register` with `accountStatus = PENDING_VERIFICATION`
(`UserService.createUser`). There is currently no endpoint to transition an account out of
`PENDING_VERIFICATION`, so newly registered users have no way to be activated. This feature adds
an endpoint that lets an admin approve a pending account, moving it to `ACTIVE`.

The project already has an unused `EmailService.sendApprovalEmail(to, name)` method and a
`templates/email/approval.html` Thymeleaf template — both appear to have been built for this
exact use case but are not called anywhere in the codebase. This feature wires them up.

---

## Endpoint

```
PATCH /auth/users/{id}/approve
Authorization: Bearer <admin JWT>
Body: none
Response: 200 UserSimpleDTO
```

Protected by `hasRole("ADMIN")` in `SecurityConfig`, same pattern as `/auth/users/{id}/role`.

---

## Components

### Service — `UserService.approveUser`

```java
public UserSimpleDTO approveUser(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
        throw new BusinessException("Only pending verification accounts can be approved");
    }

    user.setAccountStatus(AccountStatus.ACTIVE);
    User savedUser = userRepository.save(user);
    emailService.sendApprovalEmail(savedUser.getEmail(), savedUser.getName());
    return userMapper.toSimpleDTO(savedUser);
}
```

- Follows the same shape as `updateUserRole`: `findById` → state validation → mutation → `save` →
  mapper.
- Requires injecting `EmailService` into `UserService`'s constructor (new dependency).
- `sendApprovalEmail` is `@Async` and already swallows/logs its own exceptions internally — a
  failed email send must not block or fail the approval, so no try/catch is needed at the call
  site.

### Controller — `UserController`

New method, no `@AuthenticationPrincipal` needed (no self-approval restriction exists in this
feature):

```java
@PatchMapping("/users/{id}/approve")
@Operation(summary = "Aprovar cadastro de usuário", description = "Acesso restrito a administradores.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Usuário aprovado com sucesso",
        content = @Content(schema = @Schema(implementation = UserSimpleDTO.class))),
    @ApiResponse(responseCode = "400", description = "Conta não está pendente de verificação",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
    @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
})
public ResponseEntity<UserSimpleDTO> approveUser(@PathVariable Long id) {
    log.debug("REST request to approve user: {}", id);
    UserSimpleDTO response = userService.approveUser(id);
    return ResponseEntity.ok(response);
}
```

### SecurityConfig

Add before `anyRequest().authenticated()`, alongside the existing `/role` rule:

```java
.requestMatchers(HttpMethod.PATCH, "/auth/users/*/approve").hasRole("ADMIN")
```

---

## Error cases

| Scenario | HTTP |
|---|---|
| Admin approves a `PENDING_VERIFICATION` account | 200 |
| Admin tries to approve an `ACTIVE` or `SUSPENDED` account | 400 |
| Non-admin user calls the endpoint | 403 |
| No token | 401 |
| User id does not exist | 404 |

---

## Tests

### `UserServiceTest` (`@Nested class ApproveUser`)

- `givenPendingUser_whenApproveUser_thenSetsActiveAndSendsEmail`
- `givenNonExistentUser_whenApproveUser_thenThrowsResourceNotFoundException`
- `givenActiveUser_whenApproveUser_thenThrowsBusinessException`
- `givenSuspendedUser_whenApproveUser_thenThrowsBusinessException`

### `RoleBasedSecurityIT` (`@Nested class ApproveUser`)

Authorization/role-gated admin endpoints (like `/role`) are tested here, using real JWTs via the
existing `loginAndGetToken` helper — not in `UserControllerIT`.

- `givenNoToken_whenApproveUser_thenReturn401`
- `givenAdminToken_whenApprovePendingUser_thenReturn200AndStatusActive`
- `givenAdminToken_whenApproveAlreadyActiveUser_thenReturn400`
- `givenAlumniToken_whenApproveUser_thenReturn403`
- `givenAdminToken_whenApproveNonExistentUser_thenReturn404`