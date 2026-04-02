CREATE DATABASE  IF NOT EXISTS `family_management_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `family_management_db`;
-- MySQL dump 10.13  Distrib 8.0.31, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: family_management_db
-- ------------------------------------------------------
-- Server version	8.0.31

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES ('cat-001','Thông báo',NULL,NULL),('cat-002','Lễ hội',NULL,NULL);
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `families`
--

DROP TABLE IF EXISTS `families`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `families` (
  `family_id` varchar(36) NOT NULL,
  `family_name` varchar(255) NOT NULL,
  `description` text,
  `privacy_setting` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `families`
--

LOCK TABLES `families` WRITE;
/*!40000 ALTER TABLE `families` DISABLE KEYS */;
INSERT INTO `families` VALUES ('fam-001','Dòng họ Nguyễn',NULL,NULL,'2026-03-31 06:18:06',NULL),('fam-nguyen-001','Dòng họ Nguyễn','Gia phả mẫu 5 thế hệ để test hệ thống','PUBLIC','2026-03-30 08:40:44',NULL);
/*!40000 ALTER TABLE `families` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `news_events`
--

DROP TABLE IF EXISTS `news_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news_events` (
  `id` varchar(36) NOT NULL,
  `family_id` varchar(36) DEFAULT NULL,
  `category_id` varchar(36) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `summary` text,
  `content` longtext,
  `start_at` datetime DEFAULT NULL,
  `end_at` datetime DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `remind_before` int DEFAULT NULL,
  `visibility` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `family_id` (`family_id`),
  KEY `category_id` (`category_id`),
  KEY `fk_news_user` (`user_id`),
  CONSTRAINT `FK54a5derh5it9diti7390p6ity` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_news_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `news_events_ibfk_1` FOREIGN KEY (`family_id`) REFERENCES `families` (`family_id`) ON DELETE CASCADE,
  CONSTRAINT `news_events_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `news_events`
--

LOCK TABLES `news_events` WRITE;
/*!40000 ALTER TABLE `news_events` DISABLE KEYS */;
INSERT INTO `news_events` VALUES ('n1','fam-001','cat-001','Thông báo họp họ 2026',NULL,'Nội dung...',NULL,NULL,NULL,NULL,NULL,'2026-03-31 06:18:06',NULL,'user-001'),('n2','fam-001','cat-002','Lễ hội đầu xuân',NULL,'Nội dung...',NULL,NULL,NULL,NULL,NULL,'2026-03-31 06:18:06',NULL,'user-001'),('n3','fam-nguyen-001',NULL,'Lễ Giỗ Tổ',NULL,NULL,'2026-04-15 08:00:00',NULL,NULL,NULL,NULL,'2026-03-31 09:59:01',NULL,NULL),('n4','fam-nguyen-001',NULL,'Tin tức xây nhà thờ',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-31 09:59:01',NULL,NULL),('n5','fam-nguyen-001','cat-002','Mừng Thọ ',NULL,NULL,'2026-04-15 08:00:00',NULL,NULL,NULL,NULL,'2026-03-31 10:14:45',NULL,NULL),('n6','fam-nguyen-001','cat-002','Ăn hỏi ',NULL,NULL,'2026-02-15 08:00:00',NULL,NULL,NULL,NULL,'2026-03-31 10:16:42',NULL,NULL);
/*!40000 ALTER TABLE `news_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `permission_id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `relationships`
--

DROP TABLE IF EXISTS `relationships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `relationships` (
  `id` varchar(36) NOT NULL,
  `person_1_id` varchar(36) DEFAULT NULL,
  `person_2_id` varchar(36) DEFAULT NULL,
  `rel_type` varchar(50) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `note` text,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `person_1_id` (`person_1_id`),
  KEY `person_2_id` (`person_2_id`),
  CONSTRAINT `relationships_ibfk_1` FOREIGN KEY (`person_1_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `relationships_ibfk_2` FOREIGN KEY (`person_2_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `relationships`
--

LOCK TABLES `relationships` WRITE;
/*!40000 ALTER TABLE `relationships` DISABLE KEYS */;
INSERT INTO `relationships` VALUES ('r1','u1','u2','SPOUSE',NULL,NULL,NULL,NULL,NULL),('r10','u1','u5','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r11','u2','u5','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r12','u5','u11','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r13','u6','u11','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r14','u8','u9','SPOUSE',NULL,NULL,NULL,NULL,NULL),('r15','u8','u15','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r16','u9','u15','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r17','u11','u12','SPOUSE',NULL,NULL,NULL,NULL,NULL),('r18','u12','u16','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r19','u11','u16','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r2','u1','u3','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r20','u15','u18','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r3','u2','u3','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r4','u3','u4','SPOUSE',NULL,NULL,NULL,NULL,NULL),('r5','u3','u8','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r6','u4','u8','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r7','u3','u10','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r8','u4','u10','PARENT_CHILD',NULL,NULL,NULL,NULL,NULL),('r9','u5','u6','SPOUSE',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `relationships` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `role_id` varchar(36) NOT NULL,
  `permission_id` varchar(36) NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `permission_id` (`permission_id`),
  CONSTRAINT `role_permissions_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`) ON DELETE CASCADE,
  CONSTRAINT `role_permissions_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`permission_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` varchar(36) NOT NULL,
  `role_name` varchar(50) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` varchar(36) NOT NULL,
  `family_id` varchar(36) DEFAULT NULL,
  `full_name` varchar(255) NOT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `dod` date DEFAULT NULL,
  `hometown` varchar(255) DEFAULT NULL,
  `current_address` varchar(255) DEFAULT NULL,
  `occupation` varchar(100) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `bio` text,
  `password` varchar(255) DEFAULT NULL,
  `role_id` varchar(36) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `order_in_family` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `branch` varchar(255) DEFAULT NULL,
  `generation` int DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  KEY `family_id` (`family_id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`family_id`) REFERENCES `families` (`family_id`) ON DELETE SET NULL,
  CONSTRAINT `users_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('admin-id','fam-nguyen-001','Admin Dòng Họ','MALE',NULL,NULL,NULL,NULL,NULL,NULL,'admin@gmail.com',NULL,'$2a$10$8RUCbKP8bUE9MuY4LhYyW.DxbSMUJWBbt9mteOvC4kgwrxpWpyIUK',NULL,1,NULL,'2026-03-30 16:00:40.619428','2026-03-30 16:00:40.619428',NULL,'Hội đồng',1),('u1','fam-nguyen-001','Nguyen Van Truong','MALE',NULL,NULL,NULL,NULL,'Tiền bối',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Ngành trưởng',1),('u10','fam-nguyen-001','Nguyen Van Duc','MALE',NULL,NULL,NULL,NULL,'Giảng viên',NULL,NULL,NULL,NULL,NULL,1,2,NULL,NULL,NULL,'Chi 1',3),('u11','fam-nguyen-001','Nguyen Thi Ha','FEMALE',NULL,NULL,NULL,NULL,'Bác sĩ',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 2',3),('u12','fam-nguyen-001','Pham Van Nam','MALE',NULL,NULL,NULL,NULL,'Kiến trúc sư',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Thông gia',3),('u13','fam-nguyen-001','Nguyen Van Khoa','MALE',NULL,NULL,NULL,NULL,'Lập trình viên',NULL,NULL,NULL,NULL,NULL,1,2,NULL,NULL,NULL,'Chi 2',3),('u14','fam-nguyen-001','Nguyen Thi Thu','FEMALE',NULL,NULL,NULL,NULL,'Thiết kế đồ họa',NULL,NULL,NULL,NULL,NULL,1,3,NULL,NULL,NULL,'Chi 3',3),('u15','fam-nguyen-001','Nguyen Minh Quan','MALE',NULL,NULL,NULL,NULL,'Học sinh',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 1',4),('u16','fam-nguyen-001','Nguyen Gia Bao','MALE',NULL,NULL,NULL,NULL,'Học sinh',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 2',4),('u17','fam-nguyen-001','Nguyen Ngoc Anh','FEMALE',NULL,NULL,NULL,NULL,'Trẻ em',NULL,NULL,NULL,NULL,NULL,1,2,NULL,NULL,NULL,'Chi 2',4),('u18','fam-nguyen-001','Nguyen Bao Chau','FEMALE',NULL,NULL,NULL,NULL,'Trẻ em',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 1',5),('u2','fam-nguyen-001','Pham Thi Hoa','FEMALE',NULL,NULL,NULL,NULL,'Nội trợ',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Ngành trưởng',1),('u3','fam-nguyen-001','Nguyen Van Son','MALE',NULL,NULL,NULL,NULL,'Cán bộ hưu trí',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 1',2),('u4','fam-nguyen-001','Le Thi Lan','FEMALE',NULL,NULL,NULL,NULL,'Giáo viên',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 1',2),('u5','fam-nguyen-001','Nguyen Van Binh','MALE',NULL,NULL,NULL,NULL,'Kỹ sư xây dựng',NULL,NULL,NULL,NULL,NULL,1,2,NULL,NULL,NULL,'Chi 2',2),('u6','fam-nguyen-001','Tran Thi Mai','FEMALE',NULL,NULL,NULL,NULL,'Kế toán',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 2',2),('u7','fam-nguyen-001','Nguyen Thi Huong','FEMALE',NULL,NULL,NULL,NULL,'Dược sĩ',NULL,NULL,NULL,NULL,NULL,1,3,NULL,NULL,NULL,'Chi 3',2),('u8','fam-nguyen-001','Nguyen Van Anh','MALE',NULL,NULL,NULL,NULL,'Doanh nhân',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 1',3),('u9','fam-nguyen-001','Vu Thi Yen','FEMALE',NULL,NULL,NULL,NULL,'Ngân hàng',NULL,NULL,NULL,NULL,NULL,1,1,NULL,NULL,NULL,'Chi 1',3),('user-001',NULL,'Phùng Văn A',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'phung@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-31 19:32:04
