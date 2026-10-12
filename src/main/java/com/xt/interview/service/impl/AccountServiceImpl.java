package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.AccountService;
import com.xt.interview.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        if (accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }
        Account account = new Account();
        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        String password = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_PASSWORD : dto.getPassword();
        account.setPassword(MD5Util.encode(password));
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        account.setDepartmentId(validateDepartment(dto.getDepartmentId()));
        account.setDeleted(NOT_DELETED);        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getUsername().equals(dto.getUsername()) &&
            accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }

        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            account.setPassword(MD5Util.encode(dto.getPassword()));
        }
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        // 仅在部门变更时校验：已挂在禁用部门下的用户编辑其他字段时不受影响
        if (!java.util.Objects.equals(account.getDepartmentId(), dto.getDepartmentId())) {
            account.setDepartmentId(validateDepartment(dto.getDepartmentId()));
        }
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        account.setDeleted(DELETED);
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        return convertToDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        Map<Long, String> departmentNames = departmentRepository.findByDeleted(NOT_DELETED).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        return accountRepository.findByDeleted(NOT_DELETED).stream()
                .map(account -> convertToDTO(account, departmentNames))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAccountPage(String username, Integer status, Integer roleLevel, int page, int size) {
        String keyword = (username == null || username.trim().isEmpty()) ? null : username.trim();
        Page<Account> result = accountRepository.findPage(NOT_DELETED, keyword, status, roleLevel,
                PageRequest.of(page, size));
        Map<Long, String> departmentNames = departmentRepository.findByDeleted(NOT_DELETED).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        Map<String, Object> response = new HashMap<>();
        response.put("list", result.getContent().stream()
                .map(account -> convertToDTO(account, departmentNames))
                .collect(Collectors.toList()));
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("currentPage", result.getNumber());
        response.put("pageSize", result.getSize());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String username, String password) {
        Account account = accountRepository.findByUsernameAndPasswordAndDeleted(
                username, MD5Util.encode(password), NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (account.getStatus() != 1) {
            throw new RuntimeException("账号已禁用");
        }
        return convertToDTO(account);
    }

    private Long validateDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = departmentRepository.findByIdAndDeleted(departmentId, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("所选部门不存在"));
        if (department.getStatus() != 1) {
            throw new RuntimeException("所选部门已禁用");
        }
        return departmentId;
    }

    private AccountDTO convertToDTO(Account account) {
        return convertToDTO(account, null);
    }

    private AccountDTO convertToDTO(Account account, Map<Long, String> departmentNames) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setName(account.getName());
        dto.setGender(account.getGender());
        dto.setPhone(account.getPhone());
        dto.setRoleLevel(account.getRoleLevel());
        dto.setStatus(account.getStatus());
        dto.setIsSuper(account.getIsSuper());
        dto.setDepartmentId(account.getDepartmentId());
        if (account.getDepartmentId() != null) {
            String name = departmentNames != null
                    ? departmentNames.get(account.getDepartmentId())
                    : departmentRepository.findByIdAndDeleted(account.getDepartmentId(), NOT_DELETED)
                            .map(Department::getName).orElse(null);
            dto.setDepartmentName(name);
        }
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
