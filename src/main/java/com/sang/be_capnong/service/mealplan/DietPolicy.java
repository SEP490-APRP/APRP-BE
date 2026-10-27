package com.sang.be_capnong.service.mealplan;

import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.enums.DietPreference;

public interface DietPolicy {
    boolean isCompatible(BuildCombo combo, DietPreference preference);
}
