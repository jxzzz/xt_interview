-- ==================== 账号表新增所属部门字段 ====================
ALTER TABLE `sys_account`
    ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER `is_super`,
    ADD INDEX `idx_account_department` (`department_id`),
    ADD CONSTRAINT `fk_account_department` FOREIGN KEY (`department_id`) REFERENCES `sys_department` (`id`);
