package com.jerry.salesmanagement.pojo;

public interface EntryAuditable {
    Long getEntryUser();

    void setEntryUser(Long entryUser);

    Long getAuditorUser();

    void setAuditorUser(Long auditorUser);
}
