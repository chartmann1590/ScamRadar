package com.charles.scamradar.app.security;

import android.content.Context;
import android.content.Intent;

public final class ExplicitIntents {
    private ExplicitIntents() {
    }

    public static Intent activity(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        intent.setPackage(context.getPackageName());
        return intent;
    }
}
