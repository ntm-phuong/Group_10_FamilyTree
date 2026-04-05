package com.family.app.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Tạo thành viên mới và gắn quan hệ vợ/chồng với thành viên đã chọn.
 * Giới tính trong body phải trùng {@code MALE}/{@code FEMALE} và khác giới với đối tác.
 */
@Data
public class CreateSpouseRequest {
    private String fullName;
    private String email;
    /** Bắt buộc MALE hoặc FEMALE, trái với giới tính của đối tác. */
    private String gender;
    private LocalDate dob;
    private String phoneNumber;
    private String hometown;
    private String currentAddress;
    private String occupation;
    private String bio;
}
