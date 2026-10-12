package com.xt.interview.service;

import com.xt.interview.dto.AccountDTO;

import java.util.List;
import java.util.Map;

public interface AccountService {

    AccountDTO createAccount(AccountDTO dto);

    AccountDTO updateAccount(Long id, AccountDTO dto);

    void deleteAccount(Long id);

    AccountDTO getAccountById(Long id);

    List<AccountDTO> getAllAccounts();

    Map<String, Object> getAccountPage(String username, Integer status, Integer roleLevel, int page, int size);

    AccountDTO login(String username, String password);
}
