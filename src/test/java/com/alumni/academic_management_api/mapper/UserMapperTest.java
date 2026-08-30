package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Nested
    class ToSimpleDTO {

        @Test
        void givenUserWithProfilePicture_whenToSimpleDTO_thenMapsProfilePictureUrl() {
            User user = User.builder()
                    .id(1L)
                    .name("Aluno Teste")
                    .email("aluno@teste.com")
                    .profilePictureUrl("https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/aluno.jpg")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            UserSimpleDTO result = userMapper.toSimpleDTO(user);

            assertThat(result.getProfilePictureUrl())
                    .isEqualTo("https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/aluno.jpg");
        }
    }
}
