package com.family.app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.family.app.dto.UserResponse;
import com.family.app.model.User;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

@Service
public class ProfileService {
    private static final int NEWS_COVER_WIDTH = 1200;
    private static final int NEWS_COVER_HEIGHT = 750;

    @Autowired
    private UserRepository userRepository;
    @Transactional(readOnly = true)
    public UserResponse getProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        return convertToDTO(user);
    }

    @Transactional
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

    public String uploadNewsCoverToCloudinary(MultipartFile file) throws IOException {
        validateNewsCoverImage(file);
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", "family-app/news",
                        "transformation", "c_fill,g_auto,w_" + NEWS_COVER_WIDTH + ",h_" + NEWS_COVER_HEIGHT + "/q_auto/f_auto"
                ));
        return uploadResult.get("url").toString();
    }

    public void validateNewsCoverImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Thiếu file ảnh.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("File tải lên phải là ảnh.");
        }
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("Không đọc được dữ liệu ảnh.");
        }
    }

    @Transactional
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
        // if (user.getRole() != null) {
        //     dto.setRoleName(user.getRole().getRoleName());
        // }

        return dto;
    }
}