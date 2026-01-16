package com.cvbuilder.service;

import com.cvbuilder.dto.UserDTO;
import com.cvbuilder.dto.UserRequest;
import com.cvbuilder.entity.User;
import com.cvbuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // ================== REGISTER ==================
    @Override
    @Transactional
    public UserDTO registerUser(UserRequest userRequest) {
        log.info("📍 Yeni kullanıcı kaydı: {}", userRequest.getEmail());

        // 1) Email zaten var mı?
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new RuntimeException("Bu e-posta ile kayıtlı kullanıcı zaten var: " + userRequest.getEmail());
        }

        // 2) User nesnesi oluştur
        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword()); // TODO: ileride hashle
        user.setFullName(userRequest.getFullName());
        user.setTitle(userRequest.getTitle());
        user.setExperienceYears(userRequest.getExperienceYears());
        user.setSummary(userRequest.getSummary());
        user.setLocation(userRequest.getLocation());
        user.setPhone(userRequest.getPhone());

        // 3) Kaydet
        User savedUser = userRepository.save(user);
        log.info("✅ Kullanıcı başarıyla kaydedildi: {}", savedUser.getEmail());

        // 4) DTO dön
        return convertToDTO(savedUser);
    }

    // ================== LOGIN ==================
    @Override
    public UserDTO loginUser(String email, String password) {
        log.info("📍 Giriş denemesi: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + email));

        // ❌ Şimdilik basit kontrol – sonra password encoder ekle
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Şifre hatalı");
        }

        log.info("✅ Giriş başarılı: {}", email);
        return convertToDTO(user);
    }

    // ================== LIST / GET ==================
    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + id));
        return convertToDTO(user);
    }

    // ================== UPDATE ==================
    @Override
    @Transactional
    public UserDTO updateUserProfile(Long id, UserRequest userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + id));

        user.setFullName(userRequest.getFullName());
        user.setTitle(userRequest.getTitle());
        user.setExperienceYears(userRequest.getExperienceYears());
        user.setSummary(userRequest.getSummary());
        user.setLocation(userRequest.getLocation());
        user.setPhone(userRequest.getPhone());

        User updatedUser = userRepository.save(user);
        log.info("✅ Kullanıcı güncellendi: {}", updatedUser.getEmail());

        return convertToDTO(updatedUser);
    }

    // ================== DELETE ==================
    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Kullanıcı bulunamadı: " + id);
        }
        userRepository.deleteById(id);
        log.info("✅ Kullanıcı silindi: {}", id);
    }

    // ================== EXISTS ==================
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ================== MAPPER ==================
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setTitle(user.getTitle());
        dto.setExperienceYears(user.getExperienceYears());
        dto.setSummary(user.getSummary());
        dto.setLocation(user.getLocation());
        dto.setPhone(user.getPhone());
        return dto;
    }
}
