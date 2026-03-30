package com.family.app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.family.app.dto.UserResponse;
import com.family.app.model.User;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;
    public UserResponse getProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        return convertToDTO(user);
    }

    public UserResponse updateProfile(String userId, UserResponse updateData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng để cập nhật"));

        if (updateData.getFullName() != null) user.setFullName(updateData.getFullName());
        if (updateData.getPhoneNumber() != null) user.setPhoneNumber(updateData.getPhoneNumber());
        if (updateData.getCurrentAddress() != null) user.setCurrentAddress(updateData.getCurrentAddress());
        if (updateData.getOccupation() != null) user.setOccupation(updateData.getOccupation());
        if (updateData.getBio() != null) user.setBio(updateData.getBio());
        if (updateData.getGender() != null) user.setGender(updateData.getGender());
        if (updateData.getDob() != null) user.setDob(updateData.getDob());

        userRepository.save(user);
        return convertToDTO(user);
    }

    @Autowired
    private Cloudinary cloudinary;

    public String uploadAvatarToCloudinary(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto"));

        return uploadResult.get("url").toString();
    }

    public UserResponse updateAvatar(String userId, String avatarUrl) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return convertToDTO(user);
    }

    public UserResponse convertToDTO(User user) {
        UserResponse dto = new UserResponse();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setDob(user.getDob());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setHometown(user.getHometown());
        dto.setCurrentAddress(user.getCurrentAddress());
        dto.setOccupation(user.getOccupation());
        dto.setBio(user.getBio());
        dto.setAvatar(user.getAvatar());

        if (user.getFamily() != null) {
            dto.setFamilyName(user.getFamily().getFamilyName());
        }
        if (user.getRole() != null) {
            dto.setRoleName(user.getRole().getRoleName());
        }

        return dto;
    }
}