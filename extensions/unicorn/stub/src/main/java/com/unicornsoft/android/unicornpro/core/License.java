package com.unicornsoft.android.unicornpro.core;

import java.util.Calendar;
import java.util.HashSet;

public class License implements ILicense {
    @Override
    public HashSet<String> getDeviceIds() {
        return null;
    }

    @Override
    public Calendar getExpiredAt() {
        return null;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public int getLife() {
        return 0;
    }

    @Override
    public int getMaxDevice() {
        return 0;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getScope() {
        return "";
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean isTrial() {
        return false;
    }
}
