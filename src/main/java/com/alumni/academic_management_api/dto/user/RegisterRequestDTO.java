package com.alumni.academic_management_api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDTO implements Serializable {

    String name;

    String cpf;

    String email;

    Long campusCourseId;

    Integer entryYear;

    Integer conclusionYear;
}
