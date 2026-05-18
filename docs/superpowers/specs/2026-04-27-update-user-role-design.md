# Design: Update User Role Endpoint

**Date:** 2026-04-27  
**Branch:** feature/user-roles  
**Status:** Approved

---

## Context

Users are registered with `Role.ALUMNI` by default. There is no way to change a user's role after creation. This feature adds an endpoint that allows an admin to promote or demote any other user's role.

---

## Endpoint

```
PATCH /auth/users/{id}/role
Authorization: Bearer <admin JWT>
Body: { "role": "ADMIN" | "ALUMNI" }
Response: 200 UserSimpleDTO
```

Protected by `hasRole("ADMIN")` in `SecurityConfig`.

---

## Components

### DTO — `UpdateRoleRequestDTO`
- Package: `dto/user/`
- Fields: `@NotNull Role role`
- Annotations: `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`

### Service — `UserService.updateUserRole`
```
updateUserRole(Long targetId, Role newRole, Long authenticatedUserId)
```
- Throws `ResourceNotFoundException` if user not found (404)
- Throws `BusinessException` if `targetId == authenticatedUserId` (400) — admin cannot change their own role
- Saves and returns updated `UserSimpleDTO`

### Controller — `UserController`
New method `updateUserRole(@PathVariable Long id, @RequestBody @Valid UpdateRoleRequestDTO request, @AuthenticationPrincipal UserDetails userDetails)`

The authenticated user's ID is retrieved from `UserDetails` to pass to the service.

### SecurityConfig
Add rule before `anyRequest().authenticated()`:
```java
.requestMatchers(HttpMethod.PATCH, "/auth/users/*/role").hasRole("ADMIN")
```

---

## Error cases

| Scenario | HTTP |
|----------|------|
| Admin changes another user's role | 200 |
| Admin tries to change own role | 400 |
| Non-admin calls the endpoint | 403 |
| User ID not found | 404 |

---

## Tests (`UserControllerIT`)

- `givenAdminToken_whenUpdateOtherUserRole_thenReturn200`
- `givenAdminToken_whenUpdateOwnRole_thenReturn400`
- `givenAlumniToken_whenUpdateRole_thenReturn403`
- `givenAdminToken_whenUpdateNonExistentUser_thenReturn404`
