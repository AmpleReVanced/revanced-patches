package com.unicornsoft.android.unicornpro.core;

import java.util.Calendar;
import java.util.HashSet;

public interface ILicense {
    HashSet<String> getDeviceIds();

    Calendar getExpiredAt();

    String getId();

    int getLife();

    int getMaxDevice();

    String getName();

    String getScope();

    boolean isExpired();

    boolean isTrial();
}