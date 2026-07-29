# Approve User Account Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `PATCH /auth/users/{id}/approve` endpoint that lets an admin move a user's account from `PENDING_VERIFICATION` to `ACTIVE`, sending the (already-built but unused) approval email.

**Architecture:** Follows the exact shape of the existing `updateUserRole` feature — a new `UserService.approveUser` method (findById → validate state → mutate → save → map), a new `UserController` REST method with no request body, and an ADMIN-only rule in `SecurityConfig`. Authorization tests go in `RoleBasedSecurityIT`, matching where the `/role` endpoint's authorization tests already live.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, MapStruct, JUnit 5, Mockito, AssertJ, MockMvc.

## Global Constraints

- Endpoint: `PATCH /auth/users/{id}/approve`, `hasRole("ADMIN")`, no request body, returns `200 UserSimpleDTO`.
- Only accounts with `accountStatus == AccountStatus.PENDING_VERIFICATION` may be approved; otherwise throw `BusinessException("Only pending verification accounts can be approved")` (→ 400 via `GlobalExceptionHandler`).
- Unknown user id → `ResourceNotFoundException("User not found with id: " + id)` (→ 404).
- On success, call `emailService.sendApprovalEmail(user.getEmail(), user.getName())` after `save`. `sendApprovalEmail` is `@Async` and already catches/logs its own exceptions — do not wrap it in try/catch.
- No new DTOs — reuse `UserSimpleDTO` via `userMapper.toSimpleDTO(...)`.
- Checkstyle: max 120 char lines, no unused imports, always braces (`./gradlew checkstyleMain checkstyleTest`).
- Full spec: `docs/superpowers/specs/2026-07-17-approve-user-account-design.md`.

---

## File Structure

- Modify: `src/main/java/com/alumni/academic_management_api/service/UserService.java` — add `EmailService` dependency, add `approveUser` method.
- Modify: `src/main/java/com/alumni/academic_management_api/controller/UserController.java` — add `approveUser` REST method.
- Modify: `src/main/java/com/alumni/academic_management_api/config/SecurityConfig.java` — add ADMIN-only rule for the new route.
- Modify: `src/test/java/com/alumni/academic_management_api/service/UserServiceTest.java` — add `EmailService` mock, add `ApproveUser` nested test class.
- Modify: `src/test/java/com/alumni/academic_management_api/controller/RoleBasedSecurityIT.java` — add `ApproveUser` nested test class.

---

### Task 1: `UserService.approveUser`

**Files:**
- Modify: `src/main/java/com/alumni/academic_management_api/service/UserService.java`
- Test: `src/test/java/com/alumni/academic_management_api/service/UserServiceTest.java`

**Interfaces:**
- Consumes: `EmailService.sendApprovalEmail(String to, String name): void` (existing, `src/main/java/com/alumni/academic_management_api/service/EmailService.java:38`)
- Produces: `UserService.approveUser(Long id): UserSimpleDTO` — throws `ResourceNotFoundException` (user not found), `BusinessException` (account not `PENDING_VERIFICATION`)

- [x] **Step 1: Write the failing unit tests**

In `src/test/java/com/alumni/academic_management_api/service/UserServiceTest.java`, add a mock field right after the existing `fileStorageService` mock (after line 51):

```java
    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;
```

Then add a new `@Nested` class after the closing brace of `UpdateUserRole` (after line 334, before `@Nested class UploadProfilePicture`):

```java
    @Nested
    class ApproveUser {

        @Test
        void givenPendingUser_whenApproveUser_thenSetsActiveAndSendsEmail() {
            Long userId = 1L;
            User user = User.builder()
                    .id(userId)
                    .name("João")
                    .email("joao@email.com")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .build();

            UserSimpleDTO expectedDTO = new UserSimpleDTO(
                    userId,
                    "João",
                    "joao@email.com",
                    List.of(),
                    AccountStatus.ACTIVE,
                    Role.ALUMNI
            );

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(userRepository.save(user)).thenReturn(user);
            Mockito.when(userMapper.toSimpleDTO(user)).thenReturn(expectedDTO);

            UserSimpleDTO result = userService.approveUser(userId);

            assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            Mockito.verify(userRepository).save(user);
            Mockito.verify(emailService).sendApprovalEmail("joao@email.com", "João");
        }

        @Test
        void givenNonExistentUser_whenApproveUser_thenThrowsResourceNotFoundException() {
            Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.approveUser(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
            Mockito.verify(emailService, Mockito.never()).sendApprovalEmail(Mockito.any(), Mockito.any());
        }

        @Test
        void givenActiveUser_whenApproveUser_thenThrowsBusinessException() {
            Long userId = 2L;
            User user = User.builder()
                    .id(userId)
                    .name("Maria")
                    .email("maria@email.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.approveUser(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only pending verification accounts can be approved");

            Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
            Mockito.verify(emailService, Mockito.never()).sendApprovalEmail(Mockito.any(), Mockito.any());
        }

        @Test
        void givenSuspendedUser_whenApproveUser_thenThrowsBusinessException() {
            Long userId = 3L;
            User user = User.builder()
                    .id(userId)
                    .name("Pedro")
                    .email("pedro@email.com")
                    .accountStatus(AccountStatus.SUSPENDED)
                    .build();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.approveUser(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only pending verification accounts can be approved");

            Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
        }
    }
```

- [x] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.alumni.academic_management_api.service.UserServiceTest" -Dspring.profiles.active=default`

Expected: compilation FAILS with `cannot find symbol: method approveUser(java.lang.Long)` (and `EmailService` unused-but-present mock is fine — the failure is the missing method on `UserService`).

- [x] **Step 3: Implement `UserService.approveUser`**

In `src/main/java/com/alumni/academic_management_api/service/UserService.java`, add the field after `fileStorageService` (after line 32):

```java
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
```

Update the constructor (replace lines 34-48):

```java
    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            UserValidator userValidator,
            AcademicProfileRepository academicProfileRepository,
            CampusesCourseRepository campusesCourseRepository,
            FileStorageService fileStorageService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userValidator = userValidator;
        this.academicProfileRepository = academicProfileRepository;
        this.campusesCourseRepository = campusesCourseRepository;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
    }
```

Add the method after `updateUserRole` (after line 124, before the closing brace of the class):

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

- [x] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.alumni.academic_management_api.service.UserServiceTest" -Dspring.profiles.active=default`

Expected: PASS, all `ApproveUser` tests green.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/alumni/academic_management_api/service/UserService.java src/test/java/com/alumni/academic_management_api/service/UserServiceTest.java
git commit -m "feat: add UserService.approveUser to activate pending accounts"
```

---

### Task 2: Endpoint, authorization rule, and integration tests

**Files:**
- Modify: `src/main/java/com/alumni/academic_management_api/controller/UserController.java`
- Modify: `src/main/java/com/alumni/academic_management_api/config/SecurityConfig.java`
- Test: `src/test/java/com/alumni/academic_management_api/controller/RoleBasedSecurityIT.java`

**Interfaces:**
- Consumes: `UserService.approveUser(Long id): UserSimpleDTO` (Task 1)
- Produces: `PATCH /auth/users/{id}/approve` → `200 UserSimpleDTO` (ADMIN only)

- [x] **Step 1: Write the failing integration tests**

In `src/test/java/com/alumni/academic_management_api/controller/RoleBasedSecurityIT.java`, add a new `@Nested` class after the closing brace of `UpdateUserRole` (after line 222, before the final closing brace of the outer class):

```java
    @Nested
    class ApproveUser {

        private static final String URL = "/auth/users/{id}/approve";

        @Test
        void givenNoToken_whenApproveUser_thenReturn401() throws Exception {
            mockMvc.perform(patch(URL, 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAdminToken_whenApprovePendingUser_thenReturn200AndStatusActive() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "88888888881");

            User target = userRepository.save(User.builder()
                    .name("Pending User")
                    .cpf("88888888882")
                    .email("pending@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build());

            mockMvc.perform(patch(URL, target.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            User updatedUser = userRepository.findById(target.getId()).orElseThrow();
            assertThat(updatedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void givenAdminToken_whenApproveAlreadyActiveUser_thenReturn400() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "88888888883");

            User target = userRepository.save(User.builder()
                    .name("Active User")
                    .cpf("88888888884")
                    .email("active@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());

            mockMvc.perform(patch(URL, target.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenAlumniToken_whenApproveUser_thenReturn403() throws Exception {
            String alumniToken = loginAndGetToken("alumni@test.com", Role.ALUMNI, "88888888885");

            User target = userRepository.save(User.builder()
                    .name("Pending User 2")
                    .cpf("88888888886")
                    .email("pending2@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build());

            mockMvc.perform(patch(URL, target.getId())
                            .header("Authorization", "Bearer " + alumniToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenApproveNonExistentUser_thenReturn404() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "88888888887");

            mockMvc.perform(patch(URL, 999999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }
```

- [x] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.alumni.academic_management_api.controller.RoleBasedSecurityIT" -Dspring.profiles.active=default`

Expected: `givenNoToken_whenApproveUser_thenReturn401` PASSES already (caught by the generic `anyRequest().authenticated()` rule), but `givenAdminToken_whenApprovePendingUser_thenReturn200AndStatusActive`, `givenAdminToken_whenApproveAlreadyActiveUser_thenReturn400` and `givenAdminToken_whenApproveNonExistentUser_thenReturn404` FAIL with 404 (no such mapping yet), and `givenAlumniToken_whenApproveUser_thenReturn403` FAILS because the unmapped route returns 404 instead of 403.

- [x] **Step 3: Add the controller endpoint**

In `src/main/java/com/alumni/academic_management_api/controller/UserController.java`, add this method after `updateUserRole` (after line 149, before the closing brace of the class). No new imports are needed — `PatchMapping`, `PathVariable`, `Content`, `Schema`, `ApiResponse`, `ApiResponses`, `SecurityRequirement`, and `Operation` are already imported.

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
    public ResponseEntity<UserSimpleDTO> approveUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        log.debug("REST request to approve user: {}", id);
        UserSimpleDTO response = userService.approveUser(id);
        return ResponseEntity.ok(response);
    }
```

- [x] **Step 4: Add the SecurityConfig rule**

In `src/main/java/com/alumni/academic_management_api/config/SecurityConfig.java`, add a line right after the existing `/role` rule (line 53):

```java
                        .requestMatchers(HttpMethod.PATCH, "/auth/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/auth/users/*/approve").hasRole("ADMIN")
```

- [x] **Step 5: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.alumni.academic_management_api.controller.RoleBasedSecurityIT" -Dspring.profiles.active=default`

Expected: PASS, all `ApproveUser` tests green.

- [x] **Step 6: Run the full test suite and checkstyle**

Run: `./gradlew check -Dspring.profiles.active=default`

Expected: BUILD SUCCESSFUL — no Checkstyle violations, no regressions in `UserServiceTest`, `UserControllerIT`, or `RoleBasedSecurityIT`.

- [x] **Step 7: Commit**

```bash
git add src/main/java/com/alumni/academic_management_api/controller/UserController.java src/main/java/com/alumni/academic_management_api/config/SecurityConfig.java src/test/java/com/alumni/academic_management_api/controller/RoleBasedSecurityIT.java
git commit -m "feat: add PATCH /auth/users/{id}/approve endpoint restricted to admins"
```
