package com.example.support;

public interface PolicySearch {
  Policy find(String intent, String planCode);
}

/**
 * Gather Evidence only. Constructor must NOT take CrmSend.
 */
