package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.support.SupportMessageRequestDTO;
import com.alumni.academic_management_api.dto.support.SupportMessageResponseDTO;
import com.alumni.academic_management_api.entity.SupportMessage;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.mapper.SupportMessageMapper;
import com.alumni.academic_management_api.repository.SupportMessageRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class SupportMessageService {

    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;
    private final SupportMessageMapper supportMessageMapper;

    public SupportMessageService(
            SupportMessageRepository supportMessageRepository,
            UserRepository userRepository,
            SupportMessageMapper supportMessageMapper) {
        this.supportMessageRepository = supportMessageRepository;
        this.userRepository = userRepository;
        this.supportMessageMapper = supportMessageMapper;
    }

    public SupportMessageResponseDTO send(String userEmail, SupportMessageRequestDTO dto) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("User not found"));

        SupportMessage message = SupportMessage.builder()
                .user(user)
                .name(user.getName())
                .email(user.getEmail())
                .subject(dto.getSubject())
                .message(dto.getMessage())
                .build();

        return supportMessageMapper.toResponseDTO(supportMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public Page<SupportMessageResponseDTO> findAll(Boolean resolved, Pageable pageable) {
        Page<SupportMessage> page = resolved != null
                ? supportMessageRepository.findByResolved(resolved, pageable)
                : supportMessageRepository.findAll(pageable);

        return page.map(supportMessageMapper::toResponseDTO);
    }
}
